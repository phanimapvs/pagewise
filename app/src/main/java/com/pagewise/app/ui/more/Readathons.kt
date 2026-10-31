package com.pagewise.app.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.pagewise.app.data.db.Book
import com.pagewise.app.data.db.GoalMetric
import com.pagewise.app.data.db.Readathon
import com.pagewise.app.data.db.ReadingSession
import com.pagewise.app.domain.Stats
import com.pagewise.app.domain.formatDuration
import com.pagewise.app.domain.toLocalDate
import com.pagewise.app.service.Reminders
import com.pagewise.app.ui.Routes
import com.pagewise.app.ui.components.BackTopBar
import com.pagewise.app.ui.components.BarChart
import com.pagewise.app.ui.components.ConfirmDialog
import com.pagewise.app.ui.components.EmptyState
import com.pagewise.app.ui.components.LinearBar
import com.pagewise.app.ui.components.LocalRepo
import com.pagewise.app.ui.components.Pill
import com.pagewise.app.ui.components.ProgressRing
import com.pagewise.app.ui.components.SectionHeader
import com.pagewise.app.ui.components.TextInputDialog
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val rangeFmt = DateTimeFormatter.ofPattern("d MMM")

enum class ReadathonState { UPCOMING, ACTIVE, ENDED }

fun Readathon.state(today: LocalDate = LocalDate.now()): ReadathonState = when {
    today.isBefore(startDate.toLocalDate()) -> ReadathonState.UPCOMING
    today.isAfter(endDate.toLocalDate()) -> ReadathonState.ENDED
    else -> ReadathonState.ACTIVE
}

/** Progress counts every session (and finished book) inside the readathon's dates. */
fun readathonProgress(r: Readathon, books: List<Book>, sessions: List<ReadingSession>): Int {
    val from = r.startDate.toLocalDate()
    val to = r.endDate.toLocalDate()
    val inRange = Stats.sessionsBetween(sessions, from, to)
    return when (r.metric) {
        GoalMetric.MINUTES -> (inRange.sumOf { it.durationSec } / 60).toInt()
        GoalMetric.PAGES -> inRange.sumOf { it.pagesRead }
        GoalMetric.BOOKS -> Stats.finishedBetween(books, from, to).size
    }
}

@Composable
fun ReadathonsScreen(nav: NavController) {
    val repo = LocalRepo.current
    val readathons by repo.readathons.collectAsStateWithLifecycle(emptyList())
    val books by repo.books.collectAsStateWithLifecycle(emptyList())
    val sessions by repo.sessions.collectAsStateWithLifecycle(emptyList())
    var creating by remember { mutableStateOf(false) }
    val grouped = readathons.groupBy { it.state() }

    Scaffold(
        topBar = { BackTopBar("Readathons", { nav.popBackStack() }) },
        floatingActionButton = { FloatingActionButton(onClick = { creating = true }) { Icon(Icons.Filled.Add, "New readathon") } },
    ) { pad ->
        if (readathons.isEmpty()) {
            EmptyState(
                "No readathons yet",
                "A readathon is a reading sprint with a start and end date, a target and optional prompts like \"read a book set at sea\".",
                Modifier.padding(pad),
            )
            return@Scaffold
        }
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 88.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(ReadathonState.ACTIVE to "Happening now", ReadathonState.UPCOMING to "Upcoming", ReadathonState.ENDED to "Finished").forEach { (state, label) ->
                val list = grouped[state].orEmpty()
                if (list.isNotEmpty()) {
                    item { SectionHeader(label) }
                    items(list, key = { it.id }) { r ->
                        val value = readathonProgress(r, books, sessions)
                        Card(onClick = { nav.navigate(Routes.readathon(r.id)) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                            Column(Modifier.padding(14.dp)) {
                                Text(r.name, style = MaterialTheme.typography.titleMedium)
                                Text("${r.startDate.toLocalDate().format(rangeFmt)} – ${r.endDate.toLocalDate().format(rangeFmt)}", style = MaterialTheme.typography.bodySmall)
                                if (state != ReadathonState.UPCOMING) {
                                    LinearBar(value.toFloat() / r.target, Modifier.padding(vertical = 6.dp))
                                    Text("$value / ${r.target} ${r.metric.unit}", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (creating) CreateReadathonDialog(onDismiss = { creating = false }) { id ->
        creating = false
        nav.navigate(Routes.readathon(id))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateReadathonDialog(onDismiss: () -> Unit, onCreated: (Long) -> Unit) {
    val repo = LocalRepo.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var metric by rememberSaveable { mutableStateOf(GoalMetric.PAGES) }
    var target by rememberSaveable { mutableStateOf("500") }
    var remind by rememberSaveable { mutableStateOf(true) }
    var start by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var end by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var picking by rememberSaveable { mutableStateOf(false) }
    val prompts = remember { mutableStateListOf<String>() }
    var addingPrompt by remember { mutableStateOf(false) }
    val valid = name.isNotBlank() && start != null && end != null && (target.toIntOrNull() ?: 0) > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New readathon") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(description, { description = it }, label = { Text("Description (optional)") }, modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (start != null && end != null) "${start!!.format(rangeFmt)} – ${end!!.format(rangeFmt)}" else "Pick dates")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GoalMetric.entries.forEach { m -> FilterChip(metric == m, { metric = m }, label = { Text(m.unit) }) }
                }
                OutlinedTextField(target, { target = it.filter(Char::isDigit).take(6) }, label = { Text("Target (${metric.unit})") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Text("Prompts (optional)", style = MaterialTheme.typography.labelLarge)
                prompts.forEachIndexed { i, p ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("• $p", Modifier.weight(1f))
                        IconButton(onClick = { prompts.removeAt(i) }) { Icon(Icons.Filled.Close, "Remove") }
                    }
                }
                TextButton(onClick = { addingPrompt = true }) { Text("+ Add prompt") }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Remind me when it starts", Modifier.weight(1f))
                    Switch(remind, { remind = it })
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                scope.launch {
                    val zone = ZoneId.systemDefault()
                    val r = Readathon(
                        name = name.trim(), description = description.trim(),
                        startDate = start!!.atStartOfDay(zone).toInstant().toEpochMilli(),
                        endDate = end!!.atTime(23, 59).atZone(zone).toInstant().toEpochMilli(),
                        metric = metric, target = target.toInt(), remind = remind,
                    )
                    val id = repo.saveReadathon(r, prompts.toList())
                    Reminders.scheduleReadathon(context, r.copy(id = id))
                    onCreated(id)
                }
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )

    if (picking) {
        val state = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(enabled = state.selectedEndDateMillis != null, onClick = {
                    val utc = ZoneId.of("UTC")
                    start = state.selectedStartDateMillis?.let { Instant.ofEpochMilli(it).atZone(utc).toLocalDate() }
                    end = state.selectedEndDateMillis?.let { Instant.ofEpochMilli(it).atZone(utc).toLocalDate() }
                    picking = false
                }) { Text("OK") }
            },
        ) { DateRangePicker(state, Modifier.heightIn(max = 520.dp)) }
    }
    if (addingPrompt) TextInputDialog("New prompt", label = "e.g. A book with a blue cover", onDismiss = { addingPrompt = false }) {
        prompts += it
        addingPrompt = false
    }
}

@Composable
fun ReadathonDetailScreen(nav: NavController, id: Long) {
    val repo = LocalRepo.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val readathon by repo.readathon(id).collectAsStateWithLifecycle(null)
    val prompts by repo.prompts(id).collectAsStateWithLifecycle(emptyList())
    val books by repo.books.collectAsStateWithLifecycle(emptyList())
    val sessions by repo.sessions.collectAsStateWithLifecycle(emptyList())
    var deleting by remember { mutableStateOf(false) }
    var addingPrompt by remember { mutableStateOf(false) }
    var writingReport by remember { mutableStateOf(false) }
    val r = readathon ?: return
    val state = r.state()
    val from = r.startDate.toLocalDate()
    val to = r.endDate.toLocalDate()
    val inRange = Stats.sessionsBetween(sessions, from, to)
    val value = readathonProgress(r, books, sessions)

    Scaffold(topBar = {
        BackTopBar(r.name, { nav.popBackStack() }) {
            IconButton(onClick = { shareReadathon(context, r, value) }) { Icon(Icons.Filled.Share, "Share") }
            IconButton(onClick = { deleting = true }) { Icon(Icons.Filled.Delete, "Delete") }
        }
    }) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(value.toFloat() / r.target, Modifier.size(110.dp), stroke = 10.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$value", style = MaterialTheme.typography.headlineSmall)
                            Text("of ${r.target} ${r.metric.unit}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Pill(when (state) { ReadathonState.UPCOMING -> "Upcoming"; ReadathonState.ACTIVE -> "Happening now"; ReadathonState.ENDED -> "Finished" })
                        Text("${from.format(rangeFmt)} – ${to.format(rangeFmt)}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
                        when (state) {
                            ReadathonState.UPCOMING -> Text("Starts in ${from.toEpochDay() - LocalDate.now().toEpochDay()} days")
                            ReadathonState.ACTIVE -> {
                                val daysLeft = (to.toEpochDay() - LocalDate.now().toEpochDay() + 1).coerceAtLeast(1)
                                val left = (r.target - value).coerceAtLeast(0)
                                Text(if (left == 0) "Target reached! 🎉" else "$left ${r.metric.unit} to go · ~${(left + daysLeft - 1) / daysLeft}/day")
                            }
                            ReadathonState.ENDED -> Text(if (value >= r.target) "You hit the target! 🎉" else "Ended at ${(value * 100 / r.target)}% of target")
                        }
                    }
                }
            }
            if (r.description.isNotBlank()) item { Text(r.description, style = MaterialTheme.typography.bodyMedium) }
            if (state != ReadathonState.UPCOMING) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        com.pagewise.app.ui.components.StatTile("Time", formatDuration(inRange.sumOf { it.durationSec }), Modifier.weight(1f))
                        com.pagewise.app.ui.components.StatTile("Pages", inRange.sumOf { it.pagesRead }.toString(), Modifier.weight(1f))
                        com.pagewise.app.ui.components.StatTile("Books", Stats.finishedBetween(books, from, to).size.toString(), Modifier.weight(1f))
                    }
                }
                item {
                    val days = (0..(minOf(to, LocalDate.now()).toEpochDay() - from.toEpochDay()).toInt().coerceAtLeast(0)).map { from.plusDays(it.toLong()) }.takeLast(14)
                    val byDay = inRange.groupBy { it.startTime.toLocalDate() }
                    BarChart(days.map { d -> d.dayOfMonth.toString() to (byDay[d]?.sumOf { it.durationSec }?.div(60) ?: 0).toInt() })
                    Text("Minutes per day", style = MaterialTheme.typography.labelSmall)
                }
            }

            item { SectionHeader("Prompts", action = { TextButton(onClick = { addingPrompt = true }) { Text("+ Add") } }) }
            if (prompts.isEmpty()) item { Text("No prompts. Add challenges like \"a book by a debut author\".", style = MaterialTheme.typography.bodySmall) }
            items(prompts, key = { it.id }) { p ->
                var menu by remember { mutableStateOf(false) }
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(p.done, { scope.launch { repo.savePrompt(p.copy(done = it)) } })
                        Column(Modifier.weight(1f)) {
                            Text(p.text)
                            TextButton(onClick = { menu = true }, contentPadding = PaddingValues(0.dp)) {
                                Text(books.firstOrNull { it.id == p.bookId }?.title ?: "Choose a book", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
                            }
                            DropdownMenu(menu, { menu = false }) {
                                DropdownMenuItem(text = { Text("None") }, onClick = { scope.launch { repo.savePrompt(p.copy(bookId = null)) }; menu = false })
                                books.forEach { b ->
                                    DropdownMenuItem(text = { Text(b.title, maxLines = 1) }, onClick = { scope.launch { repo.savePrompt(p.copy(bookId = b.id)) }; menu = false })
                                }
                            }
                        }
                    }
                }
            }

            if (state == ReadathonState.ENDED) {
                item { SectionHeader("Wrap-up", action = { TextButton(onClick = { writingReport = true }) { Text(if (r.reportNote == null) "Write" else "Edit") } }) }
                item {
                    Text(
                        r.reportNote ?: "How did it go? Write a short wrap-up: favourite read, what you'd change next time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (r.reportNote == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }

    if (addingPrompt) TextInputDialog("New prompt", label = "Prompt", onDismiss = { addingPrompt = false }) {
        scope.launch { repo.savePrompt(com.pagewise.app.data.db.ReadathonPrompt(readathonId = r.id, text = it)) }
        addingPrompt = false
    }
    if (writingReport) TextInputDialog("Readathon wrap-up", initial = r.reportNote.orEmpty(), label = "Your notes", singleLine = false, onDismiss = { writingReport = false }) {
        scope.launch { repo.saveReadathon(r.copy(reportNote = it), emptyList()) }
        writingReport = false
    }
    if (deleting) ConfirmDialog("Delete readathon?", "Your reading sessions are kept.", onDismiss = { deleting = false }) {
        scope.launch {
            Reminders.scheduleReadathon(context, r.copy(remind = false))
            repo.deleteReadathon(r)
            nav.popBackStack()
        }
    }
}

private fun shareReadathon(context: android.content.Context, r: Readathon, value: Int) {
    val text = "📚 ${r.name}: $value / ${r.target} ${r.metric.unit} " +
        "(${r.startDate.toLocalDate().format(rangeFmt)} – ${r.endDate.toLocalDate().format(rangeFmt)}). Tracking it with Pagewise!"
    context.startActivity(
        android.content.Intent.createChooser(
            android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, text),
            "Share readathon",
        ),
    )
}
