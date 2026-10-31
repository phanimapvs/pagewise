package com.pagewise.app.domain

/** Everything an achievement may look at. */
data class AchievementInput(
    val sessions: Int,
    val totalHours: Double,
    val totalPages: Int,
    val booksFinished: Int,
    val pastBooks: Int,
    val longestStreak: Int,
    val quotes: Int,
    val thoughts: Int,
    val words: Int,
    val goalsCompleted: Int,
    val usedAmbient: Boolean,
    val sharedInfographic: Boolean,
    val shelves: Int,
    val readathonsJoined: Int,
)

enum class AchievementTier { BRONZE, SILVER, GOLD }

data class Achievement(
    val key: String,
    val title: String,
    val description: String,
    val tier: AchievementTier,
    val reward: Int,
    val progress: (AchievementInput) -> Pair<Int, Int>,
)

private fun count(target: Int, f: (AchievementInput) -> Number) = { i: AchievementInput -> f(i).toInt().coerceAtMost(target) to target }

object Achievements {
    val all: List<Achievement> = listOf(
        Achievement("first_session", "Opening Line", "Log your first reading session", AchievementTier.BRONZE, 10, count(1) { it.sessions }),
        Achievement("first_book", "The End", "Finish a book", AchievementTier.BRONZE, 15, count(1) { it.booksFinished }),
        Achievement("books_5", "Stack of Five", "Finish 5 books", AchievementTier.BRONZE, 25, count(5) { it.booksFinished }),
        Achievement("books_25", "Well Read", "Finish 25 books", AchievementTier.SILVER, 60, count(25) { it.booksFinished }),
        Achievement("books_100", "Century Shelf", "Finish 100 books", AchievementTier.GOLD, 200, count(100) { it.booksFinished }),
        Achievement("books_250", "Living Library", "Finish 250 books", AchievementTier.GOLD, 400, count(250) { it.booksFinished }),
        Achievement("hours_1", "First Hour", "Read for a total of 1 hour", AchievementTier.BRONZE, 10, count(1) { it.totalHours }),
        Achievement("hours_24", "Full Day", "Read for a total of 24 hours", AchievementTier.SILVER, 50, count(24) { it.totalHours }),
        Achievement("hours_100", "Hundred Hours", "Read for a total of 100 hours", AchievementTier.SILVER, 120, count(100) { it.totalHours }),
        Achievement("hours_500", "Deep Diver", "Read for a total of 500 hours", AchievementTier.GOLD, 300, count(500) { it.totalHours }),
        Achievement("pages_500", "Paper Trail", "Read 500 pages", AchievementTier.BRONZE, 15, count(500) { it.totalPages }),
        Achievement("pages_10k", "Ten Thousand Leaves", "Read 10,000 pages", AchievementTier.SILVER, 100, count(10_000) { it.totalPages }),
        Achievement("pages_50k", "Page Mountain", "Read 50,000 pages", AchievementTier.GOLD, 300, count(50_000) { it.totalPages }),
        Achievement("streak_7", "Week Warrior", "Read 7 days in a row", AchievementTier.BRONZE, 30, count(7) { it.longestStreak }),
        Achievement("streak_30", "Habit Formed", "Read 30 days in a row", AchievementTier.SILVER, 100, count(30) { it.longestStreak }),
        Achievement("streak_100", "Unbroken", "Read 100 days in a row", AchievementTier.GOLD, 250, count(100) { it.longestStreak }),
        Achievement("streak_365", "Year of Pages", "Read every day for a year", AchievementTier.GOLD, 500, count(365) { it.longestStreak }),
        Achievement("quotes_10", "Collector", "Save 10 quotes", AchievementTier.BRONZE, 20, count(10) { it.quotes }),
        Achievement("thoughts_10", "Margin Notes", "Write 10 thoughts", AchievementTier.BRONZE, 20, count(10) { it.thoughts }),
        Achievement("words_20", "Lexicon", "Look up 20 new words", AchievementTier.SILVER, 40, count(20) { it.words }),
        Achievement("past_10", "Backlist", "Add 10 books you read before", AchievementTier.BRONZE, 15, count(10) { it.pastBooks }),
        Achievement("goals_1", "On Target", "Complete a goal", AchievementTier.BRONZE, 20, count(1) { it.goalsCompleted }),
        Achievement("goals_10", "Goal Getter", "Complete 10 goals", AchievementTier.SILVER, 80, count(10) { it.goalsCompleted }),
        Achievement("ambient", "Soundscape", "Read with an ambient sound", AchievementTier.BRONZE, 10, count(1) { if (it.usedAmbient) 1 else 0 }),
        Achievement("infographic", "Show and Tell", "Share a book infographic", AchievementTier.BRONZE, 10, count(1) { if (it.sharedInfographic) 1 else 0 }),
        Achievement("shelves_3", "Organizer", "Create 3 shelves", AchievementTier.BRONZE, 10, count(3) { it.shelves }),
        Achievement("readathon", "Marathoner", "Join a readathon", AchievementTier.BRONZE, 20, count(1) { it.readathonsJoined }),
    )

    fun byKey(key: String) = all.firstOrNull { it.key == key }

    fun unlocked(input: AchievementInput): List<Achievement> =
        all.filter { a -> a.progress(input).let { (v, t) -> v >= t } }
}
