package com.pagewise.app.ui.goals

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pagewise.app.data.UserPrefs
import com.pagewise.app.data.db.Goal
import com.pagewise.app.data.db.GoalMetric
import com.pagewise.app.data.db.GoalPeriod
import com.pagewise.app.domain.GoalProgress
import com.pagewise.app.domain.Goals
import com.pagewise.app.service.Reminders
import com.pagewise.app.ui.components.ConfirmDialog
import com.pagewise.app.ui.components.EmptyState
import com.pagewise.app.ui.components.LocalRepo
import com.pagewise.app.ui.components.Outfit
import com.pagewise.app.ui.components.ProgressRing
import com.pagewise.app.ui.components.SectionHeader
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

const val EXTRA_DAYS = 5
const val EXTRA_DAYS_COST = 80

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(prefs: UserPrefs) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val goals by repo.goals.collectAsStateWithLifecycle(emptyList())
    val books by repo.books.collectAsStateWithLifecycle(emptyList())
    val sessions by repo.sessions.collectAsStateWithLifecycle(emptyList())
    var editing by remember { mutableStateOf<Goal?>(null) }
    var deleting by remember { mutableStateOf<Goal?>(null) }
    var buying by remember { mutableStateOf<Goal?>(null) }
    var reminder by remember { mutableStateOf(false) }

    val progress = goals.map { Goals.progress(it, books, sessions) }
    val daily = Goals.currentDaily(goals)
    val active = progress.filter { it.goal.period != GoalPeriod.DAILY && it.active }
    val past = progress.filter { it.goal.period != GoalPeriod.DAILY && !it.active }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Goals") }, actions = {
                IconButton(onClick = { reminder = true }) { Icon(Icons.Filled.Notifications, "Reminder") }
            })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = newGoal(GoalPeriod.MONTHLY) }) { Icon(Icons.Filled.Add, "New goal") }
        },
    ) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 88.dp)) {
            item { SectionHeader("Daily") }
            item {
                if (daily == null) {
                    Card(onClick = { editing = newGoal(GoalPeriod.DAILY) }) {
                        Text("Set a daily goal to build a reading habit →", Modifier.padding(16.dp))
                    }
                } else {
                    GoalCard(Goals.progress(daily, books, sessions), onEdit = { editing = daily }, onDelete = { deleting = daily }, onBuy = null)
                }
            }
            item { SectionHeader("Active") }
            if (active.isEmpty()) item {
                EmptyState("No monthly or yearly goals", "Pick a target like 4 books this month or 30 books this year.", outfit = Outfit.of(prefs.activeOutfit))
            }
            items(active, key = { it.goal.id }) { p ->
                GoalCard(p, onEdit = { editing = p.goal }, onDelete = { deleting = p.goal },
                    onBuy = if (p.goal.period == GoalPeriod.MONTHLY && !p.done) ({ buying = p.goal }) else null)
                Spacer(Modifier.padding(4.dp))
            }
            if (past.isNotEmpty()) {
                item { SectionHeader("History") }
                items(past, key = { it.goal.id }) { p ->
                    GoalCard(p, onEdit = null, onDelete = { deleting = p.goal }, onBuy = null)
                    Spacer(Modifier.padding(4.dp))
                }
            }
        }
    }

    editing?.let { g -> GoalDialog(g, onDismiss = { editing = null }) { scope.launch { repo.saveGoal(it) }; editing = null } }
    deleting?.let { g -> ConfirmDialog("Delete goal?", "Its progress is calculated from your sessions, so nothing else is lost.", onDismiss = { deleting = null }) { scope.launch { repo.deleteGoal(g) } } }
    buying?.let { g ->
        var failed by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { buying = null },
            title = { Text("Need more time?") },
            text = {
                Text(
                    if (failed) "Not enough ink. You have ${prefs.ink}; this costs $EXTRA_DAYS_COST. Read with the timer to earn more."
                    else "Add $EXTRA_DAYS extra days to this monthly goal for $EXTRA_DAYS_COST ink. You have ${prefs.ink}.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { if (repo.buyExtraDays(g, EXTRA_DAYS, EXTRA_DAYS_COST)) buying = null else failed = true }
                }, enabled = !failed) { Text("Buy") }
            },
            dismissButton = { TextButton(onClick = { buying = null }) { Text("Cancel") } },
        )
    }
    if (reminder) ReminderDialog(prefs) { reminder = false }
}

private fun newGoal(period: GoalPeriod): Goal {
    val today = LocalDate.now()
    return Goal(
        period = period,
        metric = if (period == GoalPeriod.DAILY) GoalMetric.MINUTES else GoalMetric.BOOKS,
        target = when (period) { GoalPeriod.DAILY -> 20; GoalPeriod.MONTHLY -> 2; GoalPeriod.YEARLY -> 24 },
        year = today.year,
        month = if (period == GoalPeriod.MONTHLY) today.monthValue else 0,
    )
}

private val shortDate = DateTimeFormatter.ofPattern("d MMM")

private fun GoalProgress.title() = when (goal.period) {
    GoalPeriod.DAILY -> "Every day"
    GoalPeriod.MONTHLY -> Month.of(goal.month).getDisplayName(TextStyle.FULL, Locale.getDefault()) + " ${goal.year}"
    GoalPeriod.YEARLY -> "${goal.year}"
}

@Composable
private fun GoalCard(p: GoalProgress, onEdit: (() -> Unit)?, onDelete: () -> Unit, onBuy: (() -> Unit)?) {
    Card(colors = CardDefaults.cardColors(containerColor = if (p.done) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(p.fraction, Modifier.size(64.dp), stroke = 7.dp) {
                Text("${(p.fraction * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(p.title(), style = MaterialTheme.typography.titleMedium)
                Text("${p.value} / ${p.goal.target} ${p.goal.metric.unit}", style = MaterialTheme.typography.bodyMedium)
                if (p.goal.period != GoalPeriod.DAILY) Text(
                    "${p.from.format(shortDate)} – ${p.to.format(shortDate)}" + if (p.goal.extraDays > 0) " (+${p.goal.extraDays} days)" else "",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (p.done) Text("Completed 🎉", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                if (onBuy != null) TextButton(onClick = onBuy, contentPadding = PaddingValues(0.dp)) { Text("+$EXTRA_DAYS days for $EXTRA_DAYS_COST ink") }
            }
            if (onEdit != null) IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Edit") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Delete") }
        }
    }
}

@Composable
private fun GoalDialog(initial: Goal, onDismiss: () -> Unit, onSave: (Goal) -> Unit) {
    var period by rememberSaveable { mutableStateOf(initial.period) }
    var metric by rememberSaveable { mutableStateOf(initial.metric) }
    var target by rememberSaveable { mutableStateOf(initial.target.toString()) }
    val today = LocalDate.now()
    val metrics = if (period == GoalPeriod.DAILY) listOf(GoalMetric.MINUTES, GoalMetric.PAGES) else GoalMetric.entries
    if (metric !in metrics) metric = metrics.first()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "New goal" else "Edit goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (initial.id == 0L) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GoalPeriod.entries.forEach { p -> FilterChip(period == p, { period = p }, label = { Text(p.name.lowercase().replaceFirstChar { it.uppercase() }) }) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    metrics.forEach { m -> FilterChip(metric == m, { metric = m }, label = { Text(m.unit) }) }
                }
                OutlinedTextField(
                    target, { target = it.filter(Char::isDigit).take(5) }, label = { Text("Target (${metric.unit})") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    when (period) {
                        GoalPeriod.DAILY -> "Applies every day from now on."
                        GoalPeriod.MONTHLY -> "For ${today.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${today.year}."
                        GoalPeriod.YEARLY -> "For ${today.year}."
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = (target.toIntOrNull() ?: 0) > 0, onClick = {
                onSave(
                    if (initial.id == 0L) initial.copy(
                        period = period, metric = metric, target = target.toInt(), year = today.year,
                        month = if (period == GoalPeriod.MONTHLY) today.monthValue else 0,
                    ) else initial.copy(metric = metric, target = target.toInt(), completedAt = null),
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDialog(prefs: UserPrefs, onDismiss: () -> Unit) {
    val repo = LocalRepo.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var enabled by remember { mutableStateOf(prefs.reminderEnabled) }
    val time = rememberTimePickerState(prefs.reminderMinute / 60, prefs.reminderMinute % 60)
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Daily reminder") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Remind me to read", Modifier.weight(1f))
                    Switch(enabled, {
                        enabled = it
                        if (it && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    })
                }
                if (enabled) TimePicker(time)
                Text("Skipped automatically on days you've already read.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val minute = time.hour * 60 + time.minute
                scope.launch { repo.prefs.setReminder(enabled, minute) }
                Reminders.scheduleDaily(context, enabled, minute)
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
