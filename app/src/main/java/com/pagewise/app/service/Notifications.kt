package com.pagewise.app.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.pagewise.app.MainActivity
import com.pagewise.app.PagewiseApp
import com.pagewise.app.R
import com.pagewise.app.data.db.Readathon
import com.pagewise.app.domain.Stats
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object Notifications {
    const val TIMER_CHANNEL = "timer"
    const val REMINDER_CHANNEL = "reminders"
    const val BACKUP_CHANNEL = "backups"
    const val TIMER_ID = 1
    private const val REMINDER_ID = 2
    private const val TIME_UP_ID = 3
    private const val BACKUP_ID = 4

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(TIMER_CHANNEL, "Reading timer", NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(REMINDER_CHANNEL, "Reminders", NotificationManager.IMPORTANCE_DEFAULT))
        nm.createNotificationChannel(NotificationChannel(BACKUP_CHANNEL, "Backup problems", NotificationManager.IMPORTANCE_DEFAULT))
    }

    private fun canPost(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED ||
            android.os.Build.VERSION.SDK_INT < 33

    fun post(context: Context, id: Int, title: String, text: String, channel: String = REMINDER_CHANNEL) {
        if (!canPost(context)) return
        val open = PendingIntent.getActivity(
            context, id, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_book)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        @Suppress("MissingPermission")
        NotificationManagerCompat.from(context).notify(id, n)
    }

    fun alertTimeUp(context: Context, title: String) =
        post(context, TIME_UP_ID, "Time's up!", "Your reading session for \"$title\" is done. Tap to save it.")

    fun backupFailed(context: Context, text: String) = post(context, BACKUP_ID, "Backup problem", text, BACKUP_CHANNEL)

    fun postReminder(context: Context, text: String) = post(context, REMINDER_ID, "Time to read 📖", text)
}

object Reminders {
    private const val DAILY = "daily_reminder"

    fun scheduleDaily(context: Context, enabled: Boolean, minuteOfDay: Int) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) {
            wm.cancelUniqueWork(DAILY)
            return
        }
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(minuteOfDay / 60, minuteOfDay % 60)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next).toMinutes()
        val req = PeriodicWorkRequestBuilder<DailyReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MINUTES)
            .build()
        wm.enqueueUniquePeriodicWork(DAILY, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    fun scheduleReadathon(context: Context, r: Readathon) {
        val name = "readathon_${r.id}"
        val wm = WorkManager.getInstance(context)
        val delay = r.startDate - System.currentTimeMillis()
        if (!r.remind || delay <= 0) {
            wm.cancelUniqueWork(name)
            return
        }
        val req = OneTimeWorkRequestBuilder<ReadathonReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("name" to r.name))
            .build()
        wm.enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, req)
    }
}

class DailyReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val repo = (applicationContext as PagewiseApp).repository
        val sessions = repo.sessions.first()
        val streak = Stats.streak(sessions)
        if (streak.readToday) return Result.success()
        val text = if (streak.current > 0) "Keep your ${streak.current}-day streak going with a few pages."
        else "Just a friendly nudge — your book misses you."
        Notifications.postReminder(applicationContext, text)
        return Result.success()
    }
}

class ReadathonReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val name = inputData.getString("name") ?: "Your readathon"
        Notifications.post(applicationContext, name.hashCode(), "Readathon starting!", "$name starts now. Let's read!")
        return Result.success()
    }
}
