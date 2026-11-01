package com.pagewise.app.service

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.pagewise.app.PagewiseApp
import com.pagewise.app.data.BackupSettings
import com.pagewise.app.data.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Automatic backups. Every run writes a zip to app storage (keeping the newest few), refreshes
 * [latestFile] for Android's own cloud backup, and copies the zip into the user's folder if one is set.
 */
object AutoBackup {
    const val PERIODIC_WORK = "auto_backup"
    const val NOW_WORK = "backup_now"
    private const val AUTO_PREFIX = "pagewise-auto-"
    private const val SAFETY_PREFIX = "pagewise-before-restore-"
    /** Automatic backups kept, here and in the user's folder; older ones are deleted. */
    const val KEEP = 3
    private const val SAFETY_KEEP = 2
    private val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss")

    fun dir(context: Context) = File(context.filesDir, "backups").apply { mkdirs() }

    /** The single file Android Auto Backup uploads (see res/xml/data_extraction_rules.xml). */
    fun latestFile(context: Context) = File(context.filesDir, "autobackup/latest.zip")

    /** Backups in app storage, newest first. Includes the copies saved just before a restore. */
    fun list(context: Context): List<File> =
        dir(context).listFiles { f -> f.name.endsWith(".zip") && (f.name.startsWith(AUTO_PREFIX) || f.name.startsWith(SAFETY_PREFIX)) }
            .orEmpty().sortedByDescending { it.lastModified() }

    fun isSafetyCopy(f: File) = f.name.startsWith(SAFETY_PREFIX)

    /** When the backup in [f] was taken. Read from inside it: Android's restore doesn't keep file times. */
    suspend fun createdAt(repo: Repository, f: File): Long? = withContext(Dispatchers.IO) {
        runCatching { repo.readArchive(f.readBytes()).backup.createdAt }.getOrNull()
    }

    /** [replace] resets the schedule (settings changed); otherwise an existing schedule is kept. */
    fun schedule(context: Context, s: BackupSettings, replace: Boolean = true) {
        val wm = WorkManager.getInstance(context)
        if (!s.enabled) {
            wm.cancelUniqueWork(PERIODIC_WORK)
            return
        }
        val constraints = Constraints.Builder()
            .setRequiresCharging(s.chargingOnly)
            .setRequiresStorageNotLow(true)
            .build()
        val req = PeriodicWorkRequestBuilder<BackupWorker>(s.frequency.days, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()
        wm.enqueueUniquePeriodicWork(PERIODIC_WORK, if (replace) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP, req)
    }

    fun runNow(context: Context) {
        val req = OneTimeWorkRequestBuilder<BackupWorker>().setInputData(workDataOf(BackupWorker.FORCE to true)).build()
        WorkManager.getInstance(context).enqueueUniqueWork(NOW_WORK, ExistingWorkPolicy.REPLACE, req)
    }

    /**
     * Writes an automatic backup unless nothing changed since the last one (or [force] is set).
     * Throws if the app-storage copy can't be written. Returns an error message if only the
     * copy to the user's folder failed, null otherwise.
     */
    suspend fun backUp(context: Context, force: Boolean): String? = withContext(Dispatchers.IO) {
        val repo = (context.applicationContext as PagewiseApp).repository
        val s = repo.prefs.backupSettings()
        val snap = repo.snapshot()
        val hash = repo.fingerprint(snap)
        if (!force && hash == s.lastHash && latestFile(context).isFile) return@withContext null

        val name = "$AUTO_PREFIX${LocalDateTime.now().format(stamp)}.zip"
        val file = File(dir(context), name)
        writeAtomically(file) { repo.writeArchive(snap, it.outputStream()) }
        rotate(list(context).filter { !isSafetyCopy(it) }, KEEP)
        latestFile(context).parentFile?.mkdirs()
        writeAtomically(latestFile(context)) { tmp -> file.copyTo(tmp, overwrite = true) }

        val folderError = s.folderUri?.let { uri ->
            runCatching { copyToFolder(context, Uri.parse(uri), file) }
                .exceptionOrNull()?.let { "Couldn't copy to your backup folder. Choose it again in Settings." }
        }
        val now = System.currentTimeMillis()
        repo.prefs.setLastBackup(now)
        repo.prefs.setBackupResult(now, folderError, hash)
        folderError
    }

    /** Saves the current data aside, then replaces it with the backup at [uri]. Throws if [uri] isn't a Pagewise backup. */
    suspend fun restore(context: Context, repo: Repository, uri: Uri) {
        val parsed = withContext(Dispatchers.IO) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Can't open file")
            repo.readArchive(bytes)
        }
        withContext(Dispatchers.IO) {
            val current = repo.snapshot()
            if (current.books.isNotEmpty() || current.goals.isNotEmpty()) {
                val safety = File(dir(context), "$SAFETY_PREFIX${LocalDateTime.now().format(stamp)}.zip")
                writeAtomically(safety) { repo.writeArchive(current, it.outputStream()) }
                rotate(list(context).filter(::isSafetyCopy), SAFETY_KEEP)
            }
        }
        repo.restore(parsed, File(context.filesDir, "images"))
        val p = repo.prefs.current()
        Reminders.scheduleDaily(context, p.reminderEnabled, p.reminderMinute)
    }

    private fun rotate(newestFirst: List<File>, keep: Int) = newestFirst.drop(keep).forEach { it.delete() }

    private fun copyToFolder(context: Context, tree: Uri, file: File) {
        val folder = DocumentFile.fromTreeUri(context, tree)?.takeIf { it.canWrite() } ?: error("Folder not writable")
        val target = folder.createFile("application/zip", file.name) ?: error("Couldn't create file")
        context.contentResolver.openOutputStream(target.uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
            ?: error("Couldn't open file")
        folder.listFiles().filter { it.name?.startsWith(AUTO_PREFIX) == true }
            .sortedByDescending { it.name }.drop(KEEP).forEach { it.delete() }
    }

    /** Writes via a temp file and renames, so a crash never leaves a half-written backup behind. */
    private inline fun writeAtomically(target: File, write: (File) -> Unit) {
        val tmp = File(target.parentFile, "${target.name}.tmp")
        try {
            write(tmp)
            if (!tmp.renameTo(target)) {
                tmp.copyTo(target, overwrite = true)
            }
        } finally {
            tmp.delete()
        }
    }
}

class BackupWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val force = inputData.getBoolean(FORCE, false)
        val prefs = (applicationContext as PagewiseApp).repository.prefs
        return runCatching { AutoBackup.backUp(applicationContext, force) }.fold(
            onSuccess = { folderError ->
                folderError?.let { Notifications.backupFailed(applicationContext, it) }
                Result.success()
            },
            onFailure = { e ->
                // Scheduled runs get a couple of retries before the user is told.
                if (!force && runAttemptCount < 2) return Result.retry()
                val msg = "Automatic backup failed: ${e.message ?: e.javaClass.simpleName}"
                prefs.setBackupResult(System.currentTimeMillis(), msg, null)
                Notifications.backupFailed(applicationContext, msg)
                Result.failure()
            },
        )
    }

    companion object {
        const val FORCE = "force"
    }
}
