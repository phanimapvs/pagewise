package com.pagewise.app.data

import com.pagewise.app.data.db.AppDatabase
import com.pagewise.app.data.db.Book
import com.pagewise.app.data.db.BookRating
import com.pagewise.app.data.db.BookStatus
import com.pagewise.app.data.db.EarnedAchievement
import com.pagewise.app.data.db.Goal
import com.pagewise.app.data.db.GoalPeriod
import com.pagewise.app.data.db.Quote
import com.pagewise.app.data.db.RatingAspect
import com.pagewise.app.data.db.Readathon
import com.pagewise.app.data.db.ReadathonPrompt
import com.pagewise.app.data.db.ReadingSession
import com.pagewise.app.data.db.Shelf
import com.pagewise.app.data.db.ShelfBook
import com.pagewise.app.data.db.Thought
import com.pagewise.app.data.db.WordEntry
import com.pagewise.app.data.remote.BookCandidate
import com.pagewise.app.data.remote.RemoteSource
import com.pagewise.app.domain.Achievement
import com.pagewise.app.domain.AchievementInput
import com.pagewise.app.domain.Achievements
import com.pagewise.app.domain.Goals
import com.pagewise.app.domain.Ink
import com.pagewise.app.domain.Levels
import com.pagewise.app.domain.Stats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.Serializable
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Things worth celebrating in the UI right after they happen. */
sealed interface AppEvent {
    data class AchievementUnlocked(val achievement: Achievement) : AppEvent
    data class LevelUp(val level: Int) : AppEvent
    data class GoalCompleted(val goal: Goal) : AppEvent
}

data class SessionResult(
    val session: ReadingSession,
    val inkEarned: Int,
    val bookFinished: Boolean,
)

@Serializable
data class Backup(
    val version: Int = 1,
    val createdAt: Long,
    val prefs: UserPrefs,
    val books: List<Book>,
    val sessions: List<ReadingSession>,
    val quotes: List<Quote>,
    val thoughts: List<Thought>,
    val words: List<WordEntry>,
    val shelves: List<Shelf>,
    val shelfBooks: List<ShelfBook>,
    val ratings: List<BookRating>,
    val goals: List<Goal>,
    val readathons: List<Readathon>,
    val prompts: List<ReadathonPrompt>,
    val achievements: List<EarnedAchievement>,
)

/** A backup read from disk, with its quote photos keyed by file name. */
class ParsedBackup(val backup: Backup, val images: Map<String, ByteArray>)

private const val ARCHIVE_JSON = "backup.json"

class Repository(
    val db: AppDatabase,
    val prefs: Prefs,
    val remote: RemoteSource,
) {
    private val _events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<AppEvent> = _events

    // region observe
    val books = db.books().observeAll()
    val sessions = db.sessions().observeAll()
    val shelves = db.shelves().observeAll()
    val shelfLinks = db.shelves().observeLinks()
    val goals = db.goals().observeAll()
    val readathons = db.readathons().observeAll()
    val earned = db.achievements().observeAll()
    val allRatings = db.ratings().observeAll()
    val allQuotes = db.notes().allQuotes()
    val allThoughts = db.notes().allThoughts()

    fun book(id: Long) = db.books().observe(id)
    fun sessionsFor(bookId: Long) = db.sessions().observeForBook(bookId)
    fun quotes(bookId: Long) = db.notes().quotes(bookId)
    fun thoughts(bookId: Long) = db.notes().thoughts(bookId)
    fun words(bookId: Long) = db.notes().words(bookId)
    fun ratings(bookId: Long) = db.ratings().forBook(bookId)
    fun readathon(id: Long) = db.readathons().observe(id)
    fun prompts(readathonId: Long) = db.readathons().prompts(readathonId)
    // endregion

    // region books
    suspend fun addBook(c: BookCandidate, status: BookStatus = BookStatus.READING, readInPast: Boolean = false): Long {
        val now = System.currentTimeMillis()
        val id = db.books().insert(
            Book(
                title = c.title, authors = c.authors, isbn = c.isbn, coverUrl = c.coverUrl,
                description = c.description, publisher = c.publisher, pageCount = c.pageCount,
                status = status, readInPast = readInPast,
                startedAt = if (status == BookStatus.READING) now else null,
                finishedAt = if (status == BookStatus.FINISHED) now else null,
                currentPage = if (status == BookStatus.FINISHED) c.pageCount else 0,
            ),
        )
        checkProgress()
        return id
    }

    suspend fun saveBook(book: Book) {
        db.books().upsert(book)
        checkProgress()
    }

    suspend fun deleteBook(book: Book) = db.books().delete(book)

    suspend fun pin(book: Book) = db.books().pinOnly(book.id)

    suspend fun setStatus(book: Book, status: BookStatus) {
        val now = System.currentTimeMillis()
        val updated = when (status) {
            BookStatus.FINISHED -> book.copy(status = status, finishedAt = now, currentPage = maxOf(book.currentPage, book.pageCount))
            BookStatus.READING -> book.copy(status = status, startedAt = book.startedAt ?: now, finishedAt = null)
            BookStatus.DNF -> book.copy(status = status, finishedAt = now)
            BookStatus.WANT_TO_READ -> book.copy(status = status, finishedAt = null)
        }
        db.books().upsert(updated)
        if (status == BookStatus.FINISHED && book.status != BookStatus.FINISHED) prefs.addInk(Ink.BOOK_FINISHED_BONUS)
        checkProgress()
    }

    /** Start the book over: progress resets, past sessions stay in the history. */
    suspend fun restart(book: Book) = db.books().upsert(
        book.copy(status = BookStatus.READING, currentPage = 0, startedAt = System.currentTimeMillis(), finishedAt = null),
    )
    // endregion

    // region sessions
    suspend fun saveSession(
        bookId: Long,
        startTime: Long,
        durationSec: Long,
        startPage: Int,
        endPage: Int,
        timed: Boolean,
        reflection: String? = null,
        mood: Int? = null,
    ): SessionResult {
        val book = db.books().get(bookId) ?: error("Book $bookId not found")
        val session = ReadingSession(
            bookId = bookId, startTime = startTime, durationSec = durationSec,
            startPage = startPage, endPage = endPage, reflection = reflection, mood = mood,
        )
        val id = db.sessions().insert(session)
        val finished = book.pageCount > 0 && endPage >= book.pageCount && book.status != BookStatus.FINISHED
        db.books().upsert(
            book.copy(
                currentPage = maxOf(endPage, 0),
                status = if (book.status == BookStatus.WANT_TO_READ) BookStatus.READING else book.status,
                startedAt = book.startedAt ?: startTime,
            ),
        )
        val ink = if (timed) Ink.forSession(durationSec) else 0
        if (ink > 0) prefs.addInk(ink)
        checkProgress()
        return SessionResult(session.copy(id = id), ink, finished)
    }

    suspend fun updateSession(session: ReadingSession) = db.sessions().upsert(session)

    suspend fun deleteSession(session: ReadingSession) = db.sessions().delete(session)
    // endregion

    // region notes
    suspend fun saveQuote(q: Quote) { db.notes().upsertQuote(q); checkProgress() }
    suspend fun saveThought(t: Thought) { db.notes().upsertThought(t); checkProgress() }
    suspend fun saveWord(w: WordEntry) { db.notes().upsertWord(w); checkProgress() }
    suspend fun deleteQuote(q: Quote) = db.notes().deleteQuote(q)
    suspend fun deleteThought(t: Thought) = db.notes().deleteThought(t)
    suspend fun deleteWord(w: WordEntry) = db.notes().deleteWord(w)
    // endregion

    // region shelves
    suspend fun saveShelf(s: Shelf) { db.shelves().upsert(s); checkProgress() }
    suspend fun deleteShelf(s: Shelf) = db.shelves().delete(s)
    suspend fun setOnShelf(shelfId: Long, bookId: Long, on: Boolean) =
        if (on) db.shelves().link(ShelfBook(shelfId, bookId)) else db.shelves().unlink(ShelfBook(shelfId, bookId))

    suspend fun reorderShelves(ordered: List<Shelf>) = db.tx {
        ordered.forEachIndexed { i, s -> db.shelves().upsert(s.copy(sortOrder = i)) }
    }
    // endregion

    // region ratings
    suspend fun saveRatings(bookId: Long, values: Map<RatingAspect, Float>) = db.tx {
        db.ratings().clear(bookId)
        values.filterValues { it > 0f }.forEach { (a, v) -> db.ratings().upsert(BookRating(bookId, a, v)) }
    }
    // endregion

    // region goals
    suspend fun saveGoal(goal: Goal) {
        // There is a single standing daily goal; a new one replaces the old.
        if (goal.period == GoalPeriod.DAILY && goal.id == 0L) {
            db.goals().all().filter { it.period == GoalPeriod.DAILY }.forEach { db.goals().delete(it) }
        }
        db.goals().upsert(goal)
        checkProgress()
    }
    suspend fun deleteGoal(goal: Goal) = db.goals().delete(goal)

    /** Buys grace days for a monthly goal. Returns false when there isn't enough ink. */
    suspend fun buyExtraDays(goal: Goal, days: Int, cost: Int): Boolean {
        if (goal.period != GoalPeriod.MONTHLY || !prefs.spendInk(cost)) return false
        db.goals().upsert(goal.copy(extraDays = goal.extraDays + days))
        return true
    }
    // endregion

    // region readathons
    suspend fun saveReadathon(r: Readathon, prompts: List<String>): Long = db.tx {
        val id = if (r.id == 0L) db.readathons().upsert(r) else r.id.also { db.readathons().upsert(r) }
        prompts.filter { it.isNotBlank() }.forEach { db.readathons().upsertPrompt(ReadathonPrompt(readathonId = id, text = it.trim())) }
        id
    }.also { checkProgress() }

    suspend fun deleteReadathon(r: Readathon) = db.readathons().delete(r)
    suspend fun savePrompt(p: ReadathonPrompt) = db.readathons().upsertPrompt(p)
    // endregion

    // region store
    suspend fun buyOutfit(id: String, price: Int): Boolean {
        if (!prefs.spendInk(price)) return false
        prefs.unlockOutfit(id)
        prefs.setOutfit(id)
        return true
    }

    suspend fun flag(name: String) { prefs.addFlag(name); checkProgress() }
    // endregion

    // region progress
    suspend fun achievementInput(): AchievementInput {
        val books = db.books().all()
        val sessions = db.sessions().all()
        val goals = db.goals().all()
        val flags = prefs.current().flags
        return AchievementInput(
            sessions = sessions.size,
            totalHours = sessions.sumOf { it.durationSec } / 3600.0,
            totalPages = sessions.sumOf { it.pagesRead },
            booksFinished = books.count { it.status == BookStatus.FINISHED },
            pastBooks = books.count { it.readInPast },
            longestStreak = Stats.streak(sessions).longest,
            quotes = db.notes().quotesList().size,
            thoughts = db.notes().thoughtsList().size,
            words = db.notes().wordsList().size,
            goalsCompleted = goals.count { it.completedAt != null },
            usedAmbient = "ambient" in flags,
            sharedInfographic = "infographic" in flags,
            shelves = db.shelves().all().size,
            readathonsJoined = db.readathons().all().size,
        )
    }

    suspend fun xp(): Int {
        val i = achievementInput()
        return Levels.xp((i.totalHours * 3600).toLong(), i.booksFinished, i.quotes + i.thoughts + i.words)
    }

    /** Marks finished goals, unlocks achievements, detects level ups; emits [AppEvent]s. */
    private suspend fun checkProgress() {
        val levelBefore = lastKnownLevel
        val books = db.books().all()
        val sessions = db.sessions().all()
        db.goals().all().filter { it.completedAt == null && it.period != GoalPeriod.DAILY }.forEach { g ->
            if (Goals.progress(g, books, sessions).done) {
                val done = g.copy(completedAt = System.currentTimeMillis())
                db.goals().upsert(done)
                _events.tryEmit(AppEvent.GoalCompleted(done))
            }
        }

        val input = achievementInput()
        val have = db.achievements().all().mapTo(HashSet()) { it.key }
        Achievements.unlocked(input).filter { it.key !in have }.forEach { a ->
            if (db.achievements().insert(EarnedAchievement(a.key)) != -1L) {
                prefs.addInk(a.reward)
                _events.tryEmit(AppEvent.AchievementUnlocked(a))
            }
        }

        val level = Levels.level(xp()).level
        if (levelBefore != null && level > levelBefore) _events.tryEmit(AppEvent.LevelUp(level))
        lastKnownLevel = level
    }

    private var lastKnownLevel: Int? = null

    suspend fun warmUp() {
        lastKnownLevel = Levels.level(xp()).level
    }
    // endregion

    // region backup
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    suspend fun snapshot(): Backup {
        val d = db
        return Backup(
            version = 2,
            createdAt = System.currentTimeMillis(),
            prefs = prefs.current(),
            books = d.books().all(),
            sessions = d.sessions().all(),
            quotes = d.notes().quotesList(),
            thoughts = d.notes().thoughtsList(),
            words = d.notes().wordsList(),
            shelves = d.shelves().all(),
            shelfBooks = d.shelves().links(),
            ratings = d.ratings().all(),
            goals = d.goals().all(),
            readathons = d.readathons().all(),
            prompts = d.readathons().allPrompts(),
            achievements = d.achievements().all(),
        )
    }

    /** SHA-256 of the data in [b], ignoring when it was taken, so unchanged data gives the same value. */
    fun fingerprint(b: Backup): String {
        val stable = b.copy(createdAt = 0, prefs = b.prefs.copy(lastBackupAt = 0))
        val md = MessageDigest.getInstance("SHA-256")
        md.update(json.encodeToString(Backup.serializer(), stable).toByteArray())
        b.quotes.mapNotNull { it.imagePath }.sorted().forEach { path ->
            val f = File(path)
            md.update("${f.name}:${f.length()}".toByteArray())
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Writes [b] as a zip: `backup.json` plus every quote photo under `images/`.
     * Photos that no longer exist on disk are skipped.
     */
    fun writeArchive(b: Backup, out: OutputStream) {
        ZipOutputStream(out.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(ARCHIVE_JSON))
            zip.write(json.encodeToString(Backup.serializer(), b).toByteArray())
            zip.closeEntry()
            b.quotes.mapNotNull { it.imagePath }.map(::File).filter { it.isFile }.distinctBy { it.name }.forEach { f ->
                zip.putNextEntry(ZipEntry("images/${f.name}"))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    /** Parses a backup file: a v2 zip, or a v1 plain JSON file. Throws if it isn't a Pagewise backup. */
    fun readArchive(bytes: ByteArray): ParsedBackup {
        val isZip = bytes.size > 1 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()
        if (!isZip) return ParsedBackup(json.decodeFromString(Backup.serializer(), bytes.decodeToString()), emptyMap())
        var backup: Backup? = null
        val images = mutableMapOf<String, ByteArray>()
        ZipInputStream(bytes.inputStream()).use { zip ->
            generateSequence { zip.nextEntry }.forEach { e ->
                when {
                    e.name == ARCHIVE_JSON -> backup = json.decodeFromString(Backup.serializer(), zip.readBytes().decodeToString())
                    // Only plain file names, so an entry can't write outside the images folder.
                    e.name.startsWith("images/") && !e.isDirectory -> {
                        val name = e.name.removePrefix("images/")
                        if (name.isNotEmpty() && '/' !in name && '\\' !in name && name != "..") images[name] = zip.readBytes()
                    }
                }
            }
        }
        return ParsedBackup(backup ?: error("No $ARCHIVE_JSON in archive"), images)
    }

    /** Replaces all local data with the backup's contents, saving its photos into [imagesDir]. */
    suspend fun restore(parsed: ParsedBackup, imagesDir: File) = withContext(Dispatchers.IO) {
        val b = parsed.backup
        imagesDir.mkdirs()
        parsed.images.forEach { (name, data) -> File(imagesDir, name).writeBytes(data) }
        // Photo paths in the backup point at the old phone; re-point them at the restored files.
        val quotes = b.quotes.map { q ->
            val name = q.imagePath?.let { File(it).name }
            q.copy(imagePath = name?.let { File(imagesDir, it) }?.takeIf { it.isFile }?.absolutePath)
        }
        // clearAllTables() blocks, and Room refuses it on the main thread.
        db.clearAllTables()
        db.tx {
            b.books.forEach { db.books().upsert(it) }
            b.sessions.forEach { db.sessions().upsert(it) }
            quotes.forEach { db.notes().upsertQuote(it) }
            b.thoughts.forEach { db.notes().upsertThought(it) }
            b.words.forEach { db.notes().upsertWord(it) }
            b.shelves.forEach { db.shelves().upsert(it) }
            b.shelfBooks.forEach { db.shelves().link(it) }
            b.ratings.forEach { db.ratings().upsert(it) }
            b.goals.forEach { db.goals().upsert(it) }
            b.readathons.forEach { db.readathons().upsert(it) }
            b.prompts.forEach { db.readathons().upsertPrompt(it) }
            b.achievements.forEach { db.achievements().insert(it) }
        }
        prefs.restore(b.prefs)
        warmUp()
    }
    // endregion
}
