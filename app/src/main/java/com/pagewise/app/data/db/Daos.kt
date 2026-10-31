package com.pagewise.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY isPinned DESC, addedAt DESC")
    fun observeAll(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun observe(id: Long): Flow<Book?>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun get(id: Long): Book?

    @Query("SELECT * FROM books")
    suspend fun all(): List<Book>

    @Insert
    suspend fun insert(book: Book): Long

    @Upsert
    suspend fun upsert(book: Book): Long

    @Delete
    suspend fun delete(book: Book)

    @Query("UPDATE books SET isPinned = (id = :id)")
    suspend fun pinOnly(id: Long)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun observeAll(): Flow<List<ReadingSession>>

    @Query("SELECT * FROM sessions WHERE bookId = :bookId ORDER BY startTime DESC")
    fun observeForBook(bookId: Long): Flow<List<ReadingSession>>

    @Query("SELECT * FROM sessions")
    suspend fun all(): List<ReadingSession>

    @Insert
    suspend fun insert(session: ReadingSession): Long

    @Upsert
    suspend fun upsert(session: ReadingSession)

    @Delete
    suspend fun delete(session: ReadingSession)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM quotes WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun quotes(bookId: Long): Flow<List<Quote>>

    @Query("SELECT * FROM quotes ORDER BY createdAt DESC")
    fun allQuotes(): Flow<List<Quote>>

    @Query("SELECT * FROM thoughts WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun thoughts(bookId: Long): Flow<List<Thought>>

    @Query("SELECT * FROM thoughts ORDER BY createdAt DESC")
    fun allThoughts(): Flow<List<Thought>>

    @Query("SELECT * FROM words WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun words(bookId: Long): Flow<List<WordEntry>>

    @Upsert suspend fun upsertQuote(q: Quote)
    @Upsert suspend fun upsertThought(t: Thought)
    @Upsert suspend fun upsertWord(w: WordEntry)
    @Delete suspend fun deleteQuote(q: Quote)
    @Delete suspend fun deleteThought(t: Thought)
    @Delete suspend fun deleteWord(w: WordEntry)

    @Query("SELECT * FROM quotes") suspend fun quotesList(): List<Quote>
    @Query("SELECT * FROM thoughts") suspend fun thoughtsList(): List<Thought>
    @Query("SELECT * FROM words") suspend fun wordsList(): List<WordEntry>
}

@Dao
interface ShelfDao {
    @Query("SELECT * FROM shelves ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<Shelf>>

    @Query("SELECT * FROM shelf_books")
    fun observeLinks(): Flow<List<ShelfBook>>

    @Query("SELECT * FROM shelves") suspend fun all(): List<Shelf>
    @Query("SELECT * FROM shelf_books") suspend fun links(): List<ShelfBook>

    @Upsert suspend fun upsert(shelf: Shelf): Long
    @Delete suspend fun delete(shelf: Shelf)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun link(link: ShelfBook)

    @Delete suspend fun unlink(link: ShelfBook)
}

@Dao
interface RatingDao {
    @Query("SELECT * FROM ratings WHERE bookId = :bookId")
    fun forBook(bookId: Long): Flow<List<BookRating>>

    @Query("SELECT * FROM ratings")
    fun observeAll(): Flow<List<BookRating>>

    @Query("SELECT * FROM ratings") suspend fun all(): List<BookRating>

    @Upsert suspend fun upsert(rating: BookRating)

    @Query("DELETE FROM ratings WHERE bookId = :bookId")
    suspend fun clear(bookId: Long)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY year DESC, month DESC, createdAt DESC")
    fun observeAll(): Flow<List<Goal>>

    @Query("SELECT * FROM goals") suspend fun all(): List<Goal>
    @Upsert suspend fun upsert(goal: Goal): Long
    @Delete suspend fun delete(goal: Goal)
}

@Dao
interface ReadathonDao {
    @Query("SELECT * FROM readathons ORDER BY startDate DESC")
    fun observeAll(): Flow<List<Readathon>>

    @Query("SELECT * FROM readathons WHERE id = :id")
    fun observe(id: Long): Flow<Readathon?>

    @Query("SELECT * FROM readathon_prompts WHERE readathonId = :id")
    fun prompts(id: Long): Flow<List<ReadathonPrompt>>

    @Query("SELECT * FROM readathons") suspend fun all(): List<Readathon>
    @Query("SELECT * FROM readathon_prompts") suspend fun allPrompts(): List<ReadathonPrompt>

    @Upsert suspend fun upsert(r: Readathon): Long
    @Delete suspend fun delete(r: Readathon)
    @Upsert suspend fun upsertPrompt(p: ReadathonPrompt)
    @Delete suspend fun deletePrompt(p: ReadathonPrompt)
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM earned_achievements")
    fun observeAll(): Flow<List<EarnedAchievement>>

    @Query("SELECT * FROM earned_achievements") suspend fun all(): List<EarnedAchievement>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(a: EarnedAchievement): Long
}
