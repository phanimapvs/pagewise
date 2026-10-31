package com.pagewise.app.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.pagewise.app.data.db.ReadingSession
import com.pagewise.app.domain.Levels
import com.pagewise.app.domain.Stats
import com.pagewise.app.domain.formatDuration
import com.pagewise.app.domain.toLocalDate
import com.pagewise.app.ui.Routes
import com.pagewise.app.ui.components.BackTopBar
import com.pagewise.app.ui.components.BarChart
import com.pagewise.app.ui.components.LinearBar
import com.pagewise.app.ui.components.LocalRepo
import com.pagewise.app.ui.components.SectionHeader
import com.pagewise.app.ui.components.StatTile
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(nav: NavController) {
    val repo = LocalRepo.current
    val books by repo.books.collectAsStateWithLifecycle(emptyList())
    val sessions by repo.sessions.collectAsStateWithLifecycle(emptyList())
    val quotes by repo.allQuotes.collectAsStateWithLifecycle(emptyList())
    val thoughts by repo.allThoughts.collectAsStateWithLifecycle(emptyList())
    val o = Stats.overall(books, sessions)
    val level = Levels.level(Levels.xp(o.totalSeconds, o.booksFinished, quotes.size + thoughts.size))
    var chart by rememberSaveable { mutableStateOf(0) }
    var month by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }

    Scaffold(topBar = { TopAppBar(title = { Text("Stats") }, actions = { TextButton(onClick = { nav.navigate(Routes.SESSIONS) }) { Text("All sessions") } }) }) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Level ${level.level} · ${Levels.title(level.level)}", style = MaterialTheme.typography.titleMedium)
                        LinearBar(level.fraction, Modifier.padding(vertical = 8.dp), MaterialTheme.colorScheme.secondary)
                        Text("${level.needed - level.into} XP to level ${level.level + 1}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Total time", formatDuration(o.totalSeconds), Modifier.weight(1f))
                    StatTile("Pages read", o.totalPages.toString(), Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Books finished", o.booksFinished.toString(), Modifier.weight(1f))
                    StatTile("Reading speed", if (o.pagesPerHour > 0) "%.0f".format(o.pagesPerHour) else "–", Modifier.weight(1f), "pages / hour")
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Current streak", "${o.streak.current}d", Modifier.weight(1f), "Best ${o.streak.longest}d")
                    StatTile("Avg session", formatDuration(o.avgSessionSeconds), Modifier.weight(1f), "${o.sessions} sessions")
                }
            }
            o.bestDay?.let { d ->
                item { StatTile("Best day", formatDuration(o.bestDaySeconds), Modifier.fillMaxWidth(), d.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))) }
            }

            item { SectionHeader("Progress") }
            item {
                PrimaryTabRow(selectedTabIndex = chart) {
                    listOf("7 days", "30 days", "12 months").forEachIndexed { i, t -> Tab(chart == i, { chart = i }, text = { Text(t) }) }
                }
            }
            item {
                when (chart) {
                    0 -> BarChart(Stats.dailyMinutes(sessions, 7).map { (d, m) -> d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()) to m }, Modifier.padding(top = 8.dp))
                    1 -> BarChart(
                        Stats.dailyMinutes(sessions, 30).chunked(5).map { chunk -> chunk.first().first.dayOfMonth.toString() to chunk.sumOf { it.second } },
                        Modifier.padding(top = 8.dp),
                    )
                    else -> BarChart(
                        Stats.monthlyPages(sessions, 12).map { (m, p) -> m.month.getDisplayName(TextStyle.NARROW, Locale.getDefault()) to p },
                        Modifier.padding(top = 8.dp),
                    )
                }
                Text(
                    if (chart == 2) "Pages per month" else if (chart == 1) "Minutes per 5 days" else "Minutes per day",
                    style = MaterialTheme.typography.labelSmall, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                )
            }

            item { SectionHeader("Reading calendar") }
            item {
                val ym = YearMonth.parse(month)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { month = ym.minusMonths(1).toString() }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous") }
                    Text(ym.format(DateTimeFormatter.ofPattern("MMMM yyyy")), Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { month = ym.plusMonths(1).toString() }, enabled = ym < YearMonth.now()) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next") }
                }
                CalendarHeatmap(ym, sessions)
            }

            item { SectionHeader("Books finished by year") }
            item {
                val byYear = books.mapNotNull { b -> b.finishedAt?.takeIf { b.status == com.pagewise.app.data.db.BookStatus.FINISHED }?.toLocalDate()?.year }
                    .groupingBy { it }.eachCount().toSortedMap()
                if (byYear.isEmpty()) Text("Finish a book to see it here.") else BarChart(byYear.map { (y, c) -> "'${y % 100}" to c }, highlightLast = true)
            }
        }
    }
}

@Composable
private fun CalendarHeatmap(month: YearMonth, sessions: List<ReadingSession>) {
    val minutes = sessions.filter { YearMonth.from(it.startTime.toLocalDate()) == month }
        .groupBy { it.startTime.toLocalDate().dayOfMonth }
        .mapValues { (_, s) -> (s.sumOf { it.durationSec } / 60).toInt() }
    val max = (minutes.values.maxOrNull() ?: 0).coerceAtLeast(1)
    val offset = month.atDay(1).dayOfWeek.value - 1
    val cells = List(offset) { null } + (1..month.lengthOfMonth()).toList()
    val base = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.surfaceContainerHigh
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DayOfWeek.entries.forEach { d ->
                Text(d.getDisplayName(TextStyle.NARROW, Locale.getDefault()), Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
            }
        }
        cells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { day ->
                    val m = day?.let { minutes[it] } ?: 0
                    Box(
                        Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(6.dp))
                            .background(if (day == null) androidx.compose.ui.graphics.Color.Transparent else if (m == 0) empty else base.copy(alpha = 0.25f + 0.75f * m / max)),
                        contentAlignment = Alignment.Center,
                    ) { if (day != null) Text(day.toString(), style = MaterialTheme.typography.labelSmall) }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun SessionRow(s: ReadingSession, bookTitle: String?, onDelete: () -> Unit) {
    val fmt = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm")
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                bookTitle?.let { Text(it, style = MaterialTheme.typography.titleSmall) }
                Text(java.time.Instant.ofEpochMilli(s.startTime).atZone(java.time.ZoneId.systemDefault()).format(fmt), style = MaterialTheme.typography.bodySmall)
                Text("${formatDuration(s.durationSec)} · pages ${s.startPage}–${s.endPage} (${s.pagesRead})", style = MaterialTheme.typography.bodyMedium)
                s.reflection?.let { Text("“$it”", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            s.mood?.let { Text(listOf("😴", "😐", "🙂", "😃", "🤩")[it - 1]) }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Delete session") }
        }
    }
}

@Composable
fun SessionsScreen(nav: NavController) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val sessions by repo.sessions.collectAsStateWithLifecycle(emptyList())
    val books by repo.books.collectAsStateWithLifecycle(emptyList())
    val titles = books.associate { it.id to it.title }
    Scaffold(topBar = { BackTopBar("All sessions", { nav.popBackStack() }) }) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (sessions.isEmpty()) item { Text("No sessions yet.") }
            items(sessions, key = { it.id }) { s -> SessionRow(s, titles[s.bookId]) { scope.launch { repo.deleteSession(s) } } }
        }
    }
}
