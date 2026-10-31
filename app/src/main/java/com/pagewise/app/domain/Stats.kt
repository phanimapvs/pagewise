package com.pagewise.app.domain

import com.pagewise.app.data.db.Book
import com.pagewise.app.data.db.BookStatus
import com.pagewise.app.data.db.Goal
import com.pagewise.app.data.db.GoalMetric
import com.pagewise.app.data.db.GoalPeriod
import com.pagewise.app.data.db.ReadingSession
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

fun LocalDate.startMillis(): Long = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m"
        else -> "${seconds}s"
    }
}

data class Streak(val current: Int, val longest: Int, val readToday: Boolean)

data class OverallStats(
    val totalSeconds: Long,
    val totalPages: Int,
    val sessions: Int,
    val booksFinished: Int,
    val booksReading: Int,
    val pagesPerHour: Double,
    val avgSessionSeconds: Long,
    val bestDay: LocalDate?,
    val bestDaySeconds: Long,
    val streak: Streak,
)

object Stats {
    fun readingDays(sessions: List<ReadingSession>): Set<LocalDate> =
        sessions.mapTo(HashSet()) { it.startTime.toLocalDate() }

    fun streak(sessions: List<ReadingSession>, today: LocalDate = LocalDate.now()): Streak {
        val days = readingDays(sessions)
        val readToday = today in days
        var cursor = if (readToday) today else today.minusDays(1)
        var current = 0
        while (cursor in days) {
            current++
            cursor = cursor.minusDays(1)
        }
        var longest = 0
        var run = 0
        var prev: LocalDate? = null
        for (d in days.sorted()) {
            run = if (prev != null && prev.plusDays(1) == d) run + 1 else 1
            longest = maxOf(longest, run)
            prev = d
        }
        return Streak(current, longest, readToday)
    }

    fun overall(books: List<Book>, sessions: List<ReadingSession>): OverallStats {
        val totalSeconds = sessions.sumOf { it.durationSec }
        val totalPages = sessions.sumOf { it.pagesRead }
        val byDay = sessions.groupBy { it.startTime.toLocalDate() }.mapValues { (_, s) -> s.sumOf { it.durationSec } }
        val best = byDay.maxByOrNull { it.value }
        return OverallStats(
            totalSeconds = totalSeconds,
            totalPages = totalPages,
            sessions = sessions.size,
            booksFinished = books.count { it.status == BookStatus.FINISHED },
            booksReading = books.count { it.status == BookStatus.READING },
            pagesPerHour = pagesPerHour(sessions),
            avgSessionSeconds = if (sessions.isEmpty()) 0 else totalSeconds / sessions.size,
            bestDay = best?.key,
            bestDaySeconds = best?.value ?: 0,
            streak = streak(sessions),
        )
    }

    fun pagesPerHour(sessions: List<ReadingSession>): Double {
        val timed = sessions.filter { it.durationSec > 0 && it.pagesRead > 0 }
        val secs = timed.sumOf { it.durationSec }
        return if (secs == 0L) 0.0 else timed.sumOf { it.pagesRead } * 3600.0 / secs
    }

    /** Estimated seconds left to finish [book], using the book's own pace or the overall pace. */
    fun timeRemaining(book: Book, bookSessions: List<ReadingSession>, all: List<ReadingSession>): Long? {
        val left = book.pageCount - book.currentPage
        if (book.pageCount <= 0 || left <= 0) return null
        val pace = pagesPerHour(bookSessions).takeIf { it > 0 } ?: pagesPerHour(all).takeIf { it > 0 } ?: return null
        return (left / pace * 3600).toLong()
    }

    /** Minutes read on each of the last [days] days, oldest first. */
    fun dailyMinutes(sessions: List<ReadingSession>, days: Int, today: LocalDate = LocalDate.now()): List<Pair<LocalDate, Int>> {
        val byDay = sessions.groupBy { it.startTime.toLocalDate() }
        return (days - 1 downTo 0).map { offset ->
            val d = today.minusDays(offset.toLong())
            d to ((byDay[d]?.sumOf { it.durationSec } ?: 0) / 60).toInt()
        }
    }

    /** Pages read in each of the last [months] months, oldest first. */
    fun monthlyPages(sessions: List<ReadingSession>, months: Int, now: YearMonth = YearMonth.now()): List<Pair<YearMonth, Int>> {
        val byMonth = sessions.groupBy { YearMonth.from(it.startTime.toLocalDate()) }
        return (months - 1 downTo 0).map { offset ->
            val m = now.minusMonths(offset.toLong())
            m to (byMonth[m]?.sumOf { it.pagesRead } ?: 0)
        }
    }

    fun sessionsBetween(sessions: List<ReadingSession>, from: LocalDate, toInclusive: LocalDate) =
        sessions.filter { val d = it.startTime.toLocalDate(); !d.isBefore(from) && !d.isAfter(toInclusive) }

    fun finishedBetween(books: List<Book>, from: LocalDate, toInclusive: LocalDate) =
        books.filter { b ->
            b.status == BookStatus.FINISHED && b.finishedAt?.toLocalDate()?.let { !it.isBefore(from) && !it.isAfter(toInclusive) } == true
        }
}

data class GoalProgress(val goal: Goal, val value: Int, val from: LocalDate, val to: LocalDate) {
    val fraction: Float get() = if (goal.target <= 0) 0f else (value.toFloat() / goal.target).coerceIn(0f, 1f)
    val done: Boolean get() = value >= goal.target
    val active: Boolean get() = !LocalDate.now().isAfter(to)
}

object Goals {
    fun window(goal: Goal, today: LocalDate = LocalDate.now()): Pair<LocalDate, LocalDate> = when (goal.period) {
        GoalPeriod.DAILY -> today to today
        GoalPeriod.MONTHLY -> {
            val ym = YearMonth.of(goal.year, goal.month.coerceIn(1, 12))
            ym.atDay(1) to ym.atEndOfMonth().plusDays(goal.extraDays.toLong())
        }
        GoalPeriod.YEARLY -> LocalDate.of(goal.year, 1, 1) to LocalDate.of(goal.year, 12, 31)
    }

    fun progress(goal: Goal, books: List<Book>, sessions: List<ReadingSession>, today: LocalDate = LocalDate.now()): GoalProgress {
        val (from, to) = window(goal, today)
        val inWindow = Stats.sessionsBetween(sessions, from, to)
        val value = when (goal.metric) {
            GoalMetric.MINUTES -> (inWindow.sumOf { it.durationSec } / 60).toInt()
            GoalMetric.PAGES -> inWindow.sumOf { it.pagesRead }
            GoalMetric.BOOKS -> Stats.finishedBetween(books, from, to).size
        }
        return GoalProgress(goal, value, from, to)
    }

    /** The daily goal is a standing goal: the most recent one applies to every day. */
    fun currentDaily(goals: List<Goal>) = goals.filter { it.period == GoalPeriod.DAILY }.maxByOrNull { it.createdAt }
}

/** Experience and levels: 1 XP per minute read, 25 per finished book, 5 per note. */
object Levels {
    fun xp(totalSeconds: Long, booksFinished: Int, notes: Int) = (totalSeconds / 60).toInt() + booksFinished * 25 + notes * 5

    /** XP needed to go from level n to n+1 grows linearly. */
    private fun cost(level: Int) = 60 + (level - 1) * 40

    data class Level(val level: Int, val into: Int, val needed: Int) {
        val fraction get() = into.toFloat() / needed
    }

    fun level(xp: Int): Level {
        var lvl = 1
        var rest = xp
        while (rest >= cost(lvl)) {
            rest -= cost(lvl)
            lvl++
        }
        return Level(lvl, rest, cost(lvl))
    }

    fun title(level: Int) = when {
        level < 3 -> "Page Turner"
        level < 6 -> "Chapter Chaser"
        level < 10 -> "Story Seeker"
        level < 15 -> "Shelf Explorer"
        level < 25 -> "Library Regular"
        else -> "Grand Reader"
    }
}

/** Ink is the in-app currency: 1 drop per 4 minutes of timed reading, capped per session. */
object Ink {
    const val PER_MINUTES = 4
    const val SESSION_CAP = 40
    const val BOOK_FINISHED_BONUS = 20

    fun forSession(durationSec: Long) = ((durationSec / 60) / PER_MINUTES).toInt().coerceAtMost(SESSION_CAP)
}
