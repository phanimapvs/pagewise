package com.pagewise.app.ui.more

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pagewise.app.data.ThemeMode
import com.pagewise.app.data.UserPrefs
import com.pagewise.app.data.db.Goal
import com.pagewise.app.data.db.GoalMetric
import com.pagewise.app.data.db.GoalPeriod
import com.pagewise.app.service.Reminders
import com.pagewise.app.ui.components.BackTopBar
import com.pagewise.app.ui.components.ConfirmDialog
import com.pagewise.app.ui.components.LocalRepo
import com.pagewise.app.ui.components.Outfit
import com.pagewise.app.ui.components.Pip
import com.pagewise.app.ui.components.TextInputDialog
import com.pagewise.app.ui.goals.ReminderDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(nav: NavController, prefs: UserPrefs) {
    val repo = LocalRepo.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editingName by remember { mutableStateOf(false) }
    var reminder by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val json = repo.exportJson()
                withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) } }
                repo.prefs.setLastBackup(System.currentTimeMillis())
            }.onSuccess { message = "Backup saved." }.onFailure { message = "Couldn't save the backup: ${it.message}" }
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            pendingImport = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
            }
            if (pendingImport == null) message = "Couldn't read that file."
        }
    }

    Scaffold(topBar = { BackTopBar("Settings", { nav.popBackStack() }) }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState())) {
            Section("Profile")
            ListItem(
                headlineContent = { Text("Name") },
                supportingContent = { Text(prefs.name.ifBlank { "Not set" }) },
                modifier = Modifier.clickable { editingName = true },
            )
            ListItem(
                headlineContent = { Text("Daily reminder") },
                supportingContent = { Text(if (prefs.reminderEnabled) "%02d:%02d".format(prefs.reminderMinute / 60, prefs.reminderMinute % 60) else "Off") },
                modifier = Modifier.clickable { reminder = true },
            )

            Section("Appearance")
            AppearanceSettings(prefs)

            Section("Backup")
            Text(
                "Your data lives on this phone. Save a backup file to Drive or anywhere else, and restore it on a new phone." +
                    if (prefs.lastBackupAt > 0) "\nLast backup: ${Instant.ofEpochMilli(prefs.lastBackupAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm"))}" else "",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { exporter.launch("pagewise-backup-${LocalDate.now()}.json") }) { Text("Save backup") }
                OutlinedButton(onClick = { importer.launch(arrayOf("application/json", "*/*")) }) { Text("Restore") }
            }

            Section("About")
            Text(
                "Pagewise 2.0 · A reading tracker. Book data from Google Books and Open Library, definitions from the Free Dictionary API. " +
                    "Fonts Literata, Lora, Playfair Display, Nunito and Atkinson Hyperlegible are used under the SIL Open Font License 1.1.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(16.dp),
            )
        }
    }

    if (editingName) TextInputDialog("Your name", prefs.name, "Name", onDismiss = { editingName = false }) {
        scope.launch { repo.prefs.setName(it) }
        editingName = false
    }
    if (reminder) ReminderDialog(prefs) { reminder = false }
    pendingImport?.let { text ->
        ConfirmDialog(
            "Restore backup?", "This replaces everything currently in the app with the backup's contents.",
            confirm = "Restore", onDismiss = { pendingImport = null },
        ) {
            scope.launch {
                runCatching { repo.importJson(text) }
                    .onSuccess {
                        val p = repo.prefs.current()
                        Reminders.scheduleDaily(context, p.reminderEnabled, p.reminderMinute)
                        message = "Backup restored."
                    }
                    .onFailure { message = "That file isn't a Pagewise backup." }
            }
        }
    }
    message?.let { m ->
        AlertDialog(onDismissRequest = { message = null }, text = { Text(m) }, confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } })
    }
}

@Composable
private fun Section(title: String) {
    HorizontalDivider(Modifier.padding(top = 8.dp))
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp))
}

/** First-run flow: name, a daily goal and notifications. */
@Composable
fun OnboardingScreen() {
    val repo = LocalRepo.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState { 3 }
    var name by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableStateOf("20") }
    val notify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        scope.launch { finishOnboarding(repo, context, name, minutes, granted) }
    }

    Column(Modifier.fillMaxSize().imePadding().navigationBarsPadding().padding(24.dp)) {
        HorizontalPager(pager, Modifier.weight(1f), userScrollEnabled = false) { page ->
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                when (page) {
                    0 -> {
                        Pip(Outfit.CLASSIC, 160.dp, animate = true)
                        Spacer(Modifier.height(16.dp))
                        Text("Hi, I'm Pip!", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "I'll help you track your books, time your reading sessions, keep streaks and hit your goals.",
                            textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(Modifier.height(24.dp))
                        OutlinedTextField(name, { name = it }, label = { Text("What should I call you?") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    1 -> {
                        Pip(Outfit.GLASSES, 140.dp, animate = true)
                        Spacer(Modifier.height(16.dp))
                        Text("A small daily goal", style = MaterialTheme.typography.headlineMedium)
                        Text("Readers who set a daily target read more often. You can change it any time.", textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("10", "20", "30", "60").forEach { m -> FilterChip(minutes == m, { minutes = m }, label = { Text("$m min") }) }
                        }
                        OutlinedTextField(
                            minutes, { minutes = it.filter(Char::isDigit).take(3) }, label = { Text("Minutes a day") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    else -> {
                        Pip(Outfit.HEADPHONES, 140.dp, animate = true)
                        Spacer(Modifier.height(16.dp))
                        Text("Stay on track", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Allow notifications so the reading timer can run from your notification shade and I can send a gentle evening reminder.",
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
        Button(
            onClick = {
                if (pager.currentPage < 2) scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                else if (Build.VERSION.SDK_INT >= 33) notify.launch(Manifest.permission.POST_NOTIFICATIONS)
                else scope.launch { finishOnboarding(repo, context, name, minutes, true) }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (pager.currentPage < 2) "Next" else "Let's read") }
    }
}

private suspend fun finishOnboarding(repo: com.pagewise.app.data.Repository, context: android.content.Context, name: String, minutes: String, remind: Boolean) {
    repo.prefs.setName(name.trim())
    minutes.toIntOrNull()?.takeIf { it > 0 }?.let {
        repo.saveGoal(Goal(period = GoalPeriod.DAILY, metric = GoalMetric.MINUTES, target = it, year = LocalDate.now().year))
    }
    repo.prefs.setReminder(remind, 20 * 60)
    Reminders.scheduleDaily(context, remind, 20 * 60)
    repo.prefs.setOnboarded()
}
