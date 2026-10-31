package com.pagewise.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction

@Database(
    entities = [
        Book::class, ReadingSession::class, Quote::class, Thought::class, WordEntry::class,
        Shelf::class, ShelfBook::class, BookRating::class, Goal::class,
        Readathon::class, ReadathonPrompt::class, EarnedAchievement::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun books(): BookDao
    abstract fun sessions(): SessionDao
    abstract fun notes(): NoteDao
    abstract fun shelves(): ShelfDao
    abstract fun ratings(): RatingDao
    abstract fun goals(): GoalDao
    abstract fun readathons(): ReadathonDao
    abstract fun achievements(): AchievementDao

    suspend fun <T> tx(block: suspend () -> T): T = withTransaction(block)

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "pagewise.db").build()
    }
}
