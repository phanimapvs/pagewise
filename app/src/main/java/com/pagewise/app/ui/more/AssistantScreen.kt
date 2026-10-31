package com.pagewise.app.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.pagewise.app.data.UserPrefs
import com.pagewise.app.domain.Assistant
import com.pagewise.app.domain.PeriodReport
import com.pagewise.app.domain.ReportPeriod
import com.pagewise.app.domain.SuggestionKind
import com.pagewise.app.domain.formatDuration
import com.pagewise.app.ui.Routes
import com.pagewise.app.ui.components.BackTopBar
import com.pagewise.app.ui.components.BookCover
import com.pagewise.app.ui.components.Outfit
import com.pagewise.app.ui.components.Pip
import com.pagewise.app.ui.components.SectionHeader
import com.pagewise.app.ui.components.StatTile
import java.time.format.DateTimeFormatter

private fun SuggestionKind.icon(): ImageVector = when (this) {
    SuggestionKind.STREAK -> Icons.Filled.LocalFireDepartment
    SuggestionKind.GOAL -> Icons.Filled.Flag
    SuggestionKind.BOOK -> Icons.AutoMirrored.Filled.MenuBook
    SuggestionKind.HABIT -> Icons.Filled.Schedule
    SuggestionKind.DEADLINE -> Icons.Filled.CalendarMonth
    SuggestionKind.CELEBRATE -> Icons.Filled.Celebration
}

@Composable
fun AssistantScreen(nav: NavController, prefs: UserPrefs) {
    val repo = com.pagewise.app.ui.components.LocalRepo.current
    val books by repo.books.collectAsStateWithLifecycle(emptyList())
    val sessions by repo.sessions.collectAsStateWithLifecycle(emptyList())
    val goals by repo.goals.collectAsStateWithLifecycle(emptyList())
    var period by rememberSaveable { mutableStateOf(ReportPeriod.WEEK) }
    var offset by rememberSaveable { mutableIntStateOf(0) }
    val suggestions = Assistant.suggestions(books, sessions, goals)
    val report = Assistant.report(period, offset, books, sessions)

    Scaffold(topBar = { BackTopBar("Pip's corner", { nav.popBackStack() }) }) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pip(Outfit.of(prefs.activeOutfit), 84.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (suggestions.isEmpty()) "All caught up! Read a little and I'll have more ideas for you."
                        else "Here's what I noticed${if (prefs.name.isNotBlank()) ", ${prefs.name}" else ""}:",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            items(suggestions) { s ->
                Card(
                    onClick = { s.bookId?.let { nav.navigate(Routes.book(it)) } },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(s.kind.icon(), null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(s.title, style = MaterialTheme.typography.titleSmall)
                            Text(s.body, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            item { SectionHeader("Reading report") }
            item {
                PrimaryTabRow(selectedTabIndex = period.ordinal) {
                    ReportPeriod.entries.forEach { p -> Tab(period == p, { period = p; offset = 0 }, text = { Text(p.label) }) }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { offset++ }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Earlier") }
                    Text(rangeLabel(report, offset), Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { offset-- }, enabled = offset > 0) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Later") }
                }
            }
            item { ReportBody(report) { nav.navigate(Routes.book(it)) } }
        }
    }
}

private fun rangeLabel(r: PeriodReport, offset: Int): String {
    val f = DateTimeFormatter.ofPattern("d MMM")
    val current = when (r.period) {
        ReportPeriod.WEEK -> "This week"
        ReportPeriod.MONTH -> "This month"
        ReportPeriod.YEAR -> "This year"
    }
    return when {
        offset == 0 -> current
        r.period == ReportPeriod.YEAR -> r.from.year.toString()
        r.period == ReportPeriod.MONTH -> r.from.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        else -> "${r.from.format(f)} – ${r.to.format(f)}"
    }
}

private fun change(pct: Int?) = when {
    pct == null -> null
    pct >= 0 -> "▲ $pct% vs previous"
    else -> "▼ ${-pct}% vs previous"
}

@Composable
private fun ReportBody(r: PeriodReport, openBook: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Time read", formatDuration(r.seconds), Modifier.weight(1f), change(r.secondsChange))
            StatTile("Pages", r.pages.toString(), Modifier.weight(1f), change(r.pagesChange))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Days read", r.daysRead.toString(), Modifier.weight(1f), "${r.sessions} sessions")
            StatTile("Speed", if (r.pagesPerHour > 0) "%.0f".format(r.pagesPerHour) else "–", Modifier.weight(1f), "pages / hour")
        }
        Text(verdict(r), style = MaterialTheme.typography.bodyLarge)
        r.topBook?.let { b ->
            Card(onClick = { openBook(b.id) }) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    BookCover(b, width = 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Most read", style = MaterialTheme.typography.labelMedium)
                        Text(b.title, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
        if (r.finished.isNotEmpty()) {
            Text("Finished: " + r.finished.joinToString { it.title }, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun verdict(r: PeriodReport): String {
    val c = r.secondsChange
    return when {
        r.seconds == 0L -> "No reading logged in this period. A short session is all it takes to get going."
        c == null -> "A fresh start: ${formatDuration(r.seconds)} across ${r.daysRead} days."
        c >= 25 -> "A big step up from the period before. Whatever you changed is working!"
        c >= 0 -> "Steady and consistent, a little more than last time."
        c > -25 -> "Slightly quieter than before. That's fine; consistency beats volume."
        else -> "A slower stretch. Try a 10-minute session tomorrow to rebuild momentum."
    }
}
