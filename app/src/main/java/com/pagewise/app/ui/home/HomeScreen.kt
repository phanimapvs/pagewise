package com.pagewise.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.pagewise.app.data.UserPrefs
import com.pagewise.app.data.db.Book
import com.pagewise.app.data.db.BookStatus
import com.pagewise.app.domain.Assistant
import com.pagewise.app.domain.Goals
import com.pagewise.app.domain.Stats
import com.pagewise.app.domain.formatDuration
import com.pagewise.app.service.ReadingTimer
import com.pagewise.app.ui.Routes
import com.pagewise.app.ui.switchTab
import com.pagewise.app.ui.components.BookCover
import com.pagewise.app.ui.components.EmptyState
import com.pagewise.app.ui.components.LinearBar
import com.pagewise.app.ui.components.LocalRepo
import com.pagewise.app.ui.components.Outfit
import com.pagewise.app.ui.components.Pip
import com.pagewise.app.ui.components.ProgressRing
import com.pagewise.app.ui.timer.LogSessionDialog
import com.pagewise.app.ui.timer.StartSessionDialog
import java.time.LocalTime

@Composable
fun HomeScreen(nav: NavController, prefs: UserPrefs) {
    val repo = LocalRepo.current
    val books by repo.books.collectAsStateWithLifecycle(emptyList())
    val sessions by repo.sessions.collectAsStateWithLifecycle(emptyList())
    val goals by repo.goals.collectAsStateWithLifecycle(emptyList())
    val timer by ReadingTimer.state.collectAsStateWithLifecycle()
    val outfit = Outfit.of(prefs.activeOutfit)

    val reading = books.filter { it.status == BookStatus.READING }
    val streak = Stats.streak(sessions)
    val daily = Goals.currentDaily(goals)?.let { Goals.progress(it, books, sessions) }
    val tip = remember(books, sessions, goals) { Assistant.suggestions(books, sessions, goals).firstOrNull() }

    var startFor by remember { mutableStateOf<Book?>(null) }
    var logFor by remember { mutableStateOf<Book?>(null) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(greeting() + if (prefs.name.isNotBlank()) ", ${prefs.name}" else "", style = MaterialTheme.typography.headlineMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocalFireDepartment, null, tint = if (streak.readToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                        Text("${streak.current}-day streak · ${prefs.ink} ink", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Pip(outfit, 72.dp, Modifier.clickable { nav.navigate(Routes.ASSISTANT) })
            }
        }

        timer?.let { t ->
            item {
                Card(
                    onClick = { nav.navigate(if (t.stopRequested) Routes.END else Routes.TIMER) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Timer, null)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Session in progress", style = MaterialTheme.typography.titleSmall)
                            Text(t.bookTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(if (t.paused) "Paused" else "Open")
                    }
                }
            }
        }

        item {
            if (reading.isEmpty()) {
                EmptyState(
                    "Nothing on the go",
                    "Add a book you're reading to start tracking sessions, pages and streaks.",
                    outfit = outfit,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { nav.navigate(Routes.add()) }) { Icon(Icons.Filled.Add, null); Text(" Add book") }
                        OutlinedButton(onClick = { nav.navigate(Routes.add(scan = true)) }) { Icon(Icons.Filled.QrCodeScanner, null); Text(" Scan") }
                    }
                }
            } else {
                val pager = rememberPagerState { reading.size }
                Column {
                    HorizontalPager(pager, contentPadding = PaddingValues(end = 32.dp), pageSpacing = 12.dp) { page ->
                        val book = reading[page]
                        CurrentBookCard(
                            book = book,
                            remaining = Stats.timeRemaining(book, sessions.filter { it.bookId == book.id }, sessions),
                            onOpen = { nav.navigate(Routes.book(book.id)) },
                            onStart = { startFor = book },
                            onLog = { logFor = book },
                            timerBusy = timer != null,
                        )
                    }
                    if (reading.size > 1) Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
                        repeat(reading.size) { i ->
                            Box(
                                Modifier.padding(3.dp).size(if (i == pager.currentPage) 8.dp else 6.dp).clip(CircleShape)
                                    .background(if (i == pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(modifier = Modifier.weight(1f), onClick = { nav.switchTab(Routes.GOALS) }) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        ProgressRing(daily?.fraction ?: 0f, Modifier.size(52.dp), stroke = 6.dp) {
                            Text(daily?.let { "${(it.fraction * 100).toInt()}%" } ?: "–", style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Today", style = MaterialTheme.typography.titleSmall)
                            Text(
                                daily?.let { "${it.value}/${it.goal.target} ${it.goal.metric.unit}" } ?: "Set a daily goal",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                Card(modifier = Modifier.weight(1f), onClick = { nav.switchTab(Routes.STATS) }) {
                    Column(Modifier.padding(14.dp)) {
                        Text("This week", style = MaterialTheme.typography.titleSmall)
                        val week = Stats.dailyMinutes(sessions, 7)
                        Text(formatDuration(week.sumOf { it.second } * 60L), style = MaterialTheme.typography.headlineSmall)
                        Text("${week.count { it.second > 0 }} of 7 days", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        tip?.let { s ->
            item {
                Card(
                    onClick = { s.bookId?.let { nav.navigate(Routes.book(it)) } ?: nav.navigate(Routes.ASSISTANT) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Pip(outfit, 44.dp, animate = false)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(s.title, style = MaterialTheme.typography.titleSmall)
                            Text(s.body, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        val upNext = books.filter { it.status == BookStatus.WANT_TO_READ }.take(6)
        if (upNext.isNotEmpty()) item {
            Column {
                Text("Up next", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    upNext.forEach { b -> BookCover(b, Modifier.clickable { nav.navigate(Routes.book(b.id)) }, width = 64.dp) }
                }
            }
        }
    }

    startFor?.let { StartSessionDialog(it, nav) { startFor = null } }
    logFor?.let { LogSessionDialog(it) { logFor = null } }
}

@Composable
private fun CurrentBookCard(book: Book, remaining: Long?, onOpen: () -> Unit, onStart: () -> Unit, onLog: () -> Unit, timerBusy: Boolean) {
    Card(onClick = onOpen, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.padding(14.dp)) {
            BookCover(book, width = 92.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(book.authors, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                val frac = if (book.pageCount > 0) book.currentPage.toFloat() / book.pageCount else 0f
                LinearBar(frac)
                Text(
                    if (book.pageCount > 0) "Page ${book.currentPage} of ${book.pageCount} · ${(frac * 100).toInt()}%" else "Page ${book.currentPage}",
                    style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp),
                )
                remaining?.let { Text("~${formatDuration(it)} left", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onStart, enabled = !timerBusy) { Icon(Icons.Filled.PlayArrow, null); Text("Read") }
                    FilledTonalButton(onClick = onLog) { Icon(Icons.Filled.EditNote, null) }
                }
            }
        }
    }
}

private fun greeting() = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    else -> "Good evening"
}
