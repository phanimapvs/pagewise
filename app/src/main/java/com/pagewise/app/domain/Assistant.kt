package com.pagewise.app.domain

import com.pagewise.app.data.db.Book
import com.pagewise.app.data.db.BookStatus
import com.pagewise.app.data.db.Goal
import com.pagewise.app.data.db.GoalPeriod
import com.pagewise.app.data.db.ReadingSession
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class SuggestionKind { STREAK, GOAL, BOOK, HABIT, DEADLINE, CELEBRATE }

data class Suggestion(val kind: SuggestionKind, val title: String, val body: String, val bookId: Long? = null)

enum class ReportPeriod(val label: String) { WEEK("Week"), MONTH("Month"), YEAR("Year") }

data class PeriodReport(
    val period: ReportPeriod,
    val from: LocalDate,
    val to: LocalDate,
    val seconds: Long,
    val pages: Int,
    val sessions: Int,
    val daysRead: Int,
    val finished: List<Book>,
    val prevSeconds: Long,
    val prevPages: Int,
    val topBook: Book?,
    val pagesPerHour: Double,
) {
    val secondsChange: Int? get() = pct(seconds.toDouble(), prevSeconds.toDouble())
    val pagesChange: Int? get() = pct(pages.toDouble(), prevPages.toDouble())

    private fun pct(now: Double, before: Double) = if (before <= 0) null else ((now - before) / before * 100).toInt()
}

/** Pip, the in-app reading assistant: rule-based nudges and periodic reports. */
object Assistant {
    fun suggestions(
        books: List<Book>,
        sessions: List<ReadingSession>,
        goals: List<Goal>,
        today: LocalDate = LocalDate.now(),
    ): List<Suggestion> {
        val out = mutableListOf<Suggestion>()
        val streak = Stats.streak(sessions, today)

        if (streak.current > 0 && !streak.readToday) {
            out += Suggestion(SuggestionKind.STREAK, "Keep your ${streak.current}-day streak alive", "A few pages today keeps the chain going.")
        } else if (streak.readToday && streak.current >= 3) {
            out += Suggestion(SuggestionKind.CELEBRATE, "${streak.current} days in a row!", "You're building a real habit. Nice work.")
        }

        Goals.currentDaily(goals)?.let { g ->
            val p = Goals.progress(g, books, sessions, today)
            if (!p.done) out += Suggestion(
                SuggestionKind.GOAL, "Daily goal: ${p.value}/${g.target} ${g.metric.unit}",
                "${g.target - p.value} ${g.metric.unit} to go today.",
            )
        }

        goals.filter { it.period != GoalPeriod.DAILY }.map { Goals.progress(it, books, sessions, today) }
            .filter { it.active && !it.done }
            .forEach { p ->
                val totalDays = (p.to.toEpochDay() - p.from.toEpochDay() + 1).toFloat()
                val elapsed = (today.toEpochDay() - p.from.toEpochDay() + 1).coerceAtLeast(1)
                val expected = p.goal.target * (elapsed / totalDays)
                if (p.value < expected * 0.8f) {
                    val daysLeft = (p.to.toEpochDay() - today.toEpochDay() + 1).coerceAtLeast(1)
                    val perDay = (p.goal.target - p.value).toFloat() / daysLeft
                    out += Suggestion(
                        SuggestionKind.GOAL,
                        "Behind on your ${p.goal.period.name.lowercase()} goal",
                        "Aim for about ${"%.1f".format(perDay)} ${p.goal.metric.unit} a day to catch up.",
                    )
                }
            }

        val reading = books.filter { it.status == BookStatus.READING }
        val lastReadAt = sessions.groupBy { it.bookId }.mapValues { (_, s) -> s.maxOf { it.startTime } }
        reading.filter { b -> (lastReadAt[b.id] ?: b.addedAt).toLocalDate().isBefore(today.minusDays(14)) }
            .take(2)
            .forEach { b ->
                out += Suggestion(SuggestionKind.BOOK, "Still reading \"${b.title}\"?", "You haven't opened it in two weeks. Pick it back up or mark it as not finished.", b.id)
            }

        reading.filter { it.pageCount > 0 && it.pageCount - it.currentPage in 1..40 }.take(1).forEach { b ->
            out += Suggestion(SuggestionKind.BOOK, "Almost there!", "Only ${b.pageCount - b.currentPage} pages left in \"${b.title}\".", b.id)
        }

        reading.filter { it.targetDate != null }.forEach { b ->
            val days = b.targetDate!!.toLocalDate().toEpochDay() - today.toEpochDay()
            val left = b.pageCount - b.currentPage
            if (days in 0..30 && left > 0) {
                out += Suggestion(SuggestionKind.DEADLINE, "Deadline for \"${b.title}\"", "Read ${left / (days + 1).toInt().coerceAtLeast(1)} pages a day to finish on time.", b.id)
            }
        }

        bestHour(sessions)?.let { h ->
            out += Suggestion(SuggestionKind.HABIT, "Your best reading time", "You read most around ${"%02d:00".format(h)}. Try setting your reminder then.")
        }

        if (reading.isEmpty()) {
            books.firstOrNull { it.status == BookStatus.WANT_TO_READ }?.let { b ->
                out += Suggestion(SuggestionKind.BOOK, "Ready for the next one?", "\"${b.title}\" is waiting on your to-read list.", b.id)
            }
        }
        return out
    }

    private fun bestHour(sessions: List<ReadingSession>): Int? {
        if (sessions.size < 5) return null
        return sessions.groupBy { java.time.Instant.ofEpochMilli(it.startTime).atZone(java.time.ZoneId.systemDefault()).hour }
            .maxByOrNull { (_, s) -> s.sumOf { it.durationSec } }?.key
    }

    fun range(period: ReportPeriod, offset: Int, today: LocalDate = LocalDate.now()): Pair<LocalDate, LocalDate> = when (period) {
        ReportPeriod.WEEK -> {
            val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(offset.toLong())
            start to start.plusDays(6)
        }
        ReportPeriod.MONTH -> {
            val start = today.withDayOfMonth(1).minusMonths(offset.toLong())
            start to start.plusMonths(1).minusDays(1)
        }
        ReportPeriod.YEAR -> {
            val start = today.withDayOfYear(1).minusYears(offset.toLong())
            start to start.plusYears(1).minusDays(1)
        }
    }

    fun report(period: ReportPeriod, offset: Int, books: List<Book>, sessions: List<ReadingSession>): PeriodReport {
        val (from, to) = range(period, offset)
        val (pFrom, pTo) = range(period, offset + 1)
        val cur = Stats.sessionsBetween(sessions, from, to)
        val prev = Stats.sessionsBetween(sessions, pFrom, pTo)
        val topBookId = cur.groupBy { it.bookId }.maxByOrNull { (_, s) -> s.sumOf { it.durationSec } }?.key
        return PeriodReport(
            period = period,
            from = from,
            to = to,
            seconds = cur.sumOf { it.durationSec },
            pages = cur.sumOf { it.pagesRead },
            sessions = cur.size,
            daysRead = Stats.readingDays(cur).size,
            finished = Stats.finishedBetween(books, from, to),
            prevSeconds = prev.sumOf { it.durationSec },
            prevPages = prev.sumOf { it.pagesRead },
            topBook = books.firstOrNull { it.id == topBookId },
            pagesPerHour = Stats.pagesPerHour(cur),
        )
    }
}
