package com.pagewise.app.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.pagewise.app.MainActivity
import com.pagewise.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class TimerState(
    val bookId: Long,
    val bookTitle: String,
    val startPage: Int,
    /** Wall-clock time the session started, saved with the session. */
    val startedAt: Long,
    /** Elapsed milliseconds from previous running stretches. */
    val accumulatedMs: Long = 0,
    /** elapsedRealtime when the current running stretch began, null while paused. */
    val runningSince: Long? = SystemClock.elapsedRealtime(),
    /** Set when the user stops from the notification; the UI then shows the end-session flow. */
    val stopRequested: Boolean = false,
    /** Stop automatically after this many ms of reading, if set. */
    val limitMs: Long? = null,
) {
    val paused get() = runningSince == null

    fun elapsedMs(now: Long = SystemClock.elapsedRealtime()) = accumulatedMs + (runningSince?.let { now - it } ?: 0)
}

/** Process-wide reading timer; the foreground service mirrors it in a notification. */
object ReadingTimer {
    private val _state = MutableStateFlow<TimerState?>(null)
    val state: StateFlow<TimerState?> = _state

    val ambient = AmbientPlayer()

    fun start(context: Context, bookId: Long, title: String, startPage: Int, limitMinutes: Int? = null) {
        _state.value = TimerState(
            bookId = bookId, bookTitle = title, startPage = startPage, startedAt = System.currentTimeMillis(),
            limitMs = limitMinutes?.let { it * 60_000L },
        )
        ContextCompat.startForegroundService(context, Intent(context, ReadingService::class.java))
    }

    fun pause() = _state.update { s ->
        if (s == null || s.paused) s else s.copy(accumulatedMs = s.elapsedMs(), runningSince = null)
    }

    fun resume() = _state.update { s ->
        if (s == null || !s.paused) s else s.copy(runningSince = SystemClock.elapsedRealtime())
    }

    fun requestStop() {
        pause()
        _state.update { it?.copy(stopRequested = true) }
    }

    fun setLimit(minutes: Int?) = _state.update { it?.copy(limitMs = minutes?.let { m -> m * 60_000L }) }

    /** Ends the timer and returns the final state so the caller can save the session. */
    fun finish(context: Context): TimerState? {
        val s = _state.value?.let { it.copy(accumulatedMs = it.elapsedMs(), runningSince = null) }
        _state.value = null
        ambient.stop()
        context.stopService(Intent(context, ReadingService::class.java))
        return s
    }
}

class ReadingService : Service() {
    private var ticker: Thread? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> ReadingTimer.pause()
            ACTION_RESUME -> ReadingTimer.resume()
            ACTION_STOP -> ReadingTimer.requestStop()
        }
        val state = ReadingTimer.state.value ?: run {
            stopSelf()
            return START_NOT_STICKY
        }
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, Notifications.TIMER_ID, build(state), type)
        if (ticker == null) startTicker()
        return START_NOT_STICKY
    }

    /** Refreshes the notification when state changes and enforces the optional time limit. */
    private fun startTicker() {
        ticker = Thread {
            var last: TimerState? = null
            try {
                while (!Thread.currentThread().isInterrupted) {
                    val s = ReadingTimer.state.value ?: break
                    val limit = s.limitMs
                    if (limit != null && !s.paused && s.elapsedMs() >= limit) {
                        ReadingTimer.requestStop()
                        Notifications.alertTimeUp(this, s.bookTitle)
                    }
                    val now = ReadingTimer.state.value ?: break
                    if (now != last) {
                        notify(now)
                        last = now
                    }
                    Thread.sleep(1000)
                }
            } catch (_: InterruptedException) {
            }
        }.apply { isDaemon = true; start() }
    }

    @SuppressLint("MissingPermission")
    private fun notify(state: TimerState) {
        runCatching {
            androidx.core.app.NotificationManagerCompat.from(this).notify(Notifications.TIMER_ID, build(state))
        }
    }

    private fun build(state: TimerState): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        fun action(a: String, code: Int) = PendingIntent.getService(
            this, code, Intent(this, ReadingService::class.java).setAction(a), PendingIntent.FLAG_IMMUTABLE,
        )
        val base = System.currentTimeMillis() - state.elapsedMs()
        return NotificationCompat.Builder(this, Notifications.TIMER_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_book)
            .setContentTitle(state.bookTitle)
            .setContentText(if (state.stopRequested) "Session ended — tap to save" else if (state.paused) "Paused" else "Reading…")
            .setOngoing(!state.stopRequested)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .setWhen(base)
            .setShowWhen(!state.paused)
            .setUsesChronometer(!state.paused)
            .apply {
                if (!state.stopRequested) {
                    if (state.paused) addAction(0, "Resume", action(ACTION_RESUME, 1))
                    else addAction(0, "Pause", action(ACTION_PAUSE, 2))
                    addAction(0, "Stop", action(ACTION_STOP, 3))
                }
            }
            .build()
    }

    override fun onDestroy() {
        ticker?.interrupt()
        ticker = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_PAUSE = "com.pagewise.app.PAUSE"
        const val ACTION_RESUME = "com.pagewise.app.RESUME"
        const val ACTION_STOP = "com.pagewise.app.STOP"
    }
}
