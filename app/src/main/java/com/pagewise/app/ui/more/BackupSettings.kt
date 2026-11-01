package com.pagewise.app.ui.more

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.pagewise.app.PagewiseApp
import com.pagewise.app.data.BackupFrequency
import com.pagewise.app.data.BackupSettings
import com.pagewise.app.data.UserPrefs
import com.pagewise.app.service.AutoBackup
import com.pagewise.app.ui.components.ConfirmDialog
import com.pagewise.app.ui.components.LocalRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val whenFormat = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")

private fun formatTime(millis: Long) = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(whenFormat)

private class PendingRestore(val uri: Uri, val label: String)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BackupSettings(prefs: UserPrefs) {
    val repo = LocalRepo.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by repo.prefs.backupFlow.collectAsStateWithLifecycle(BackupSettings())
    val nowWork by remember { WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(AutoBackup.NOW_WORK) }
        .collectAsStateWithLifecycle(emptyList())
    val backingUp = nowWork.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
    var pending by remember { mutableStateOf<PendingRestore?>(null) }
    var restoring by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    // Re-list after each backup or restore; a restore also writes a safety copy.
    var listVersion by remember { mutableStateOf(0) }
    val recent by produceState(emptyList<File>(), settings.lastRunAt, listVersion, backingUp) {
        value = withContext(Dispatchers.IO) { AutoBackup.list(context) }
    }
    val folderName by produceState<String?>(null, settings.folderUri) {
        value = settings.folderUri?.let { uri ->
            withContext(Dispatchers.IO) { runCatching { DocumentFile.fromTreeUri(context, Uri.parse(uri))?.name }.getOrNull() } ?: "Chosen folder"
        }
    }

    fun update(s: BackupSettings) = scope.launch {
        repo.prefs.setBackupSchedule(s.enabled, s.frequency, s.chargingOnly)
        AutoBackup.schedule(context, s)
    }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val snap = repo.snapshot()
                withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { repo.writeArchive(snap, it) } ?: error("Can't open file") }
                repo.prefs.setLastBackup(System.currentTimeMillis())
            }.onSuccess { message = "Backup saved." }.onFailure { message = "Couldn't save the backup: ${it.message}" }
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pending = PendingRestore(uri, "the file you picked")
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) scope.launch {
            settings.folderUri?.let { releaseFolder(context, it) }
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            repo.prefs.setBackupFolder(uri.toString())
            if (settings.enabled) AutoBackup.runNow(context)
        }
    }

    Column {
        Text(
            "Your data lives on this phone. Automatic backups keep recent copies here, and can also copy them to a folder " +
                "and to your Google account's Android backup so a new phone can restore them." +
                if (prefs.lastBackupAt > 0) "\nLast backup: ${formatTime(prefs.lastBackupAt)}" else "",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        settings.lastError?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 0.dp))
        }

        ListItem(
            headlineContent = { Text("Automatic backups") },
            supportingContent = { Text(if (settings.enabled) "${settings.frequency.label} · keeps the last ${AutoBackup.KEEP}" else "Off") },
            trailingContent = {
                Switch(settings.enabled, { on ->
                    update(settings.copy(enabled = on))
                    // Start with a fresh backup so there's one to restore right away.
                    if (on) AutoBackup.runNow(context)
                })
            },
        )
        if (settings.enabled) {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BackupFrequency.entries.forEach { f ->
                        FilterChip(settings.frequency == f, { update(settings.copy(frequency = f)) }, label = { Text(f.label) })
                    }
                }
            }
            ListItem(
                headlineContent = { Text("Only while charging") },
                trailingContent = { Switch(settings.chargingOnly, { update(settings.copy(chargingOnly = it)) }) },
            )
            ListItem(
                headlineContent = { Text("Also copy to a folder") },
                supportingContent = { Text(folderName ?: "Not set · tap to choose, e.g. a synced folder or SD card") },
                trailingContent = {
                    if (settings.folderUri != null) TextButton(onClick = {
                        scope.launch {
                            settings.folderUri?.let { releaseFolder(context, it) }
                            repo.prefs.setBackupFolder(null)
                        }
                    }) { Text("Remove") }
                },
                modifier = Modifier.clickable { folderPicker.launch(null) },
            )
        }

        FlowRow(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { AutoBackup.runNow(context) }, enabled = !backingUp) {
                if (backingUp) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Back up now")
            }
            OutlinedButton(onClick = { exporter.launch("pagewise-backup-${LocalDate.now()}.zip") }) { Text("Save to file") }
            OutlinedButton(onClick = { importer.launch(arrayOf("application/zip", "application/json", "application/octet-stream", "*/*")) }) { Text("Restore from file") }
        }

        if (recent.isNotEmpty()) {
            Text("Recent backups", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 16.dp))
            recent.forEach { f ->
                val label = formatTime(f.lastModified())
                ListItem(
                    headlineContent = { Text(label) },
                    supportingContent = {
                        val size = Formatter.formatShortFileSize(context, f.length())
                        Text(if (AutoBackup.isSafetyCopy(f)) "Saved before a restore · $size" else size)
                    },
                    modifier = Modifier.clickable(enabled = !restoring) { pending = PendingRestore(Uri.fromFile(f), "the backup from $label") },
                )
            }
        }
    }

    pending?.let { p ->
        ConfirmDialog(
            "Restore backup?",
            "This replaces everything in the app with ${p.label}. Your current data is saved first and shows up under Recent backups, so you can undo this.",
            confirm = "Restore", onDismiss = { pending = null },
        ) {
            restoring = true
            scope.launch {
                runCatching { AutoBackup.restore(context, repo, p.uri) }
                    .onSuccess { message = "Backup restored." }
                    .onFailure { message = "That file isn't a Pagewise backup." }
                restoring = false
                listVersion++
            }
        }
    }
    message?.let { m ->
        AlertDialog(onDismissRequest = { message = null }, text = { Text(m) }, confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } })
    }
}

private fun releaseFolder(context: Context, uri: String) {
    runCatching {
        context.contentResolver.releasePersistableUriPermission(Uri.parse(uri), Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }
}

/**
 * Shown during onboarding when Android restored Pagewise's latest automatic backup onto this
 * phone (a reinstall or a new device). Restoring brings back the profile too, which ends onboarding.
 */
@Composable
fun WelcomeBackRestore(modifier: Modifier = Modifier) {
    val repo = LocalRepo.current
    val context = LocalContext.current
    val latest = remember { AutoBackup.latestFile(context).takeIf { it.isFile } } ?: return
    val takenAt by produceState<Long?>(null, latest) { value = AutoBackup.createdAt(repo, latest) }
    var restoring by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Welcome back!", style = MaterialTheme.typography.titleMedium)
            Text(
                if (failed) "That backup couldn't be read. You can start fresh instead."
                else "There's a backup of your books" + (takenAt?.let { " from ${formatTime(it)}" } ?: "") + ".",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (!failed) Button(
                onClick = {
                    restoring = true
                    // App scope: a successful restore ends onboarding, which disposes this screen and its scope.
                    val app = context.applicationContext as PagewiseApp
                    app.appScope.launch {
                        runCatching { AutoBackup.restore(app, repo, Uri.fromFile(latest)) }
                            .onSuccess {
                                // They had automatic backups on before, so keep them on.
                                val s = repo.prefs.backupSettings().copy(enabled = true)
                                repo.prefs.setBackupSchedule(s.enabled, s.frequency, s.chargingOnly)
                                AutoBackup.schedule(app, s)
                            }
                            .onFailure { failed = true }
                        restoring = false
                    }
                },
                enabled = !restoring,
            ) { Text(if (restoring) "Restoring…" else "Restore my books") }
        }
    }
}
