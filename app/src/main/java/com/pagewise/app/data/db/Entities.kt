package com.pagewise.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class BookStatus { WANT_TO_READ, READING, FINISHED, DNF }

enum class BookFormat { PAPER, EBOOK, AUDIO }

@Serializable
@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val authors: String = "",
    val isbn: String? = null,
    val coverUrl: String? = null,
    val description: String? = null,
    val publisher: String? = null,
    val pageCount: Int = 0,
    val currentPage: Int = 0,
    val status: BookStatus = BookStatus.READING,
    val format: BookFormat = BookFormat.PAPER,
    val addedAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
    /** Marked as read before the user started tracking with the app. */
    val readInPast: Boolean = false,
    val lentTo: String? = null,
    val borrowedFrom: String? = null,
    /** Personal deadline for finishing this book. */
    val targetDate: Long? = null,
    val isPinned: Boolean = false,
)

@Serializable
@Entity(
    tableName = "sessions",
    foreignKeys = [ForeignKey(Book::class, ["id"], ["bookId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("bookId"), Index("startTime")],
)
data class ReadingSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val startTime: Long,
    val durationSec: Long,
    val startPage: Int,
    val endPage: Int,
    val reflection: String? = null,
    /** 1..5 how the session felt, null if skipped. */
    val mood: Int? = null,
) {
    val pagesRead: Int get() = (endPage - startPage).coerceAtLeast(0)
}

@Serializable
@Entity(
    tableName = "quotes",
    foreignKeys = [ForeignKey(Book::class, ["id"], ["bookId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("bookId")],
)
data class Quote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val text: String,
    val page: Int? = null,
    val imagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "thoughts",
    foreignKeys = [ForeignKey(Book::class, ["id"], ["bookId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("bookId")],
)
data class Thought(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val text: String,
    val page: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "words",
    foreignKeys = [ForeignKey(Book::class, ["id"], ["bookId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("bookId")],
)
data class WordEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val word: String,
    val definition: String,
    val page: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(tableName = "shelves")
data class Shelf(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Long = 0xFF5C6BC0,
    val sortOrder: Int = 0,
)

@Serializable
@Entity(
    tableName = "shelf_books",
    primaryKeys = ["shelfId", "bookId"],
    foreignKeys = [
        ForeignKey(Shelf::class, ["id"], ["shelfId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(Book::class, ["id"], ["bookId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("bookId")],
)
data class ShelfBook(val shelfId: Long, val bookId: Long)

enum class RatingAspect(val label: String) {
    OVERALL("Overall"),
    STORY("Story"),
    CHARACTERS("Characters"),
    WRITING("Writing"),
    PACING("Pacing"),
    IMPACT("Emotional impact"),
}

@Serializable
@Entity(
    tableName = "ratings",
    primaryKeys = ["bookId", "aspect"],
    foreignKeys = [ForeignKey(Book::class, ["id"], ["bookId"], onDelete = ForeignKey.CASCADE)],
)
data class BookRating(val bookId: Long, val aspect: RatingAspect, val stars: Float)

enum class GoalPeriod { DAILY, MONTHLY, YEARLY }

enum class GoalMetric(val unit: String) { MINUTES("min"), PAGES("pages"), BOOKS("books") }

@Serializable
@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val period: GoalPeriod,
    val metric: GoalMetric,
    val target: Int,
    /** Year the goal belongs to (monthly/yearly goals). */
    val year: Int,
    /** 1..12 for monthly goals, 0 otherwise. */
    val month: Int = 0,
    /** Grace days bought with ink, extending a monthly goal. */
    val extraDays: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
)

@Serializable
@Entity(tableName = "readathons")
data class Readathon(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val startDate: Long,
    val endDate: Long,
    val metric: GoalMetric,
    val target: Int,
    val remind: Boolean = true,
    val reportNote: String? = null,
)

@Serializable
@Entity(
    tableName = "readathon_prompts",
    foreignKeys = [ForeignKey(Readathon::class, ["id"], ["readathonId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("readathonId")],
)
data class ReadathonPrompt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val readathonId: Long,
    val text: String,
    val bookId: Long? = null,
    val done: Boolean = false,
)

@Serializable
@Entity(tableName = "earned_achievements")
data class EarnedAchievement(@PrimaryKey val key: String, val earnedAt: Long = System.currentTimeMillis())
