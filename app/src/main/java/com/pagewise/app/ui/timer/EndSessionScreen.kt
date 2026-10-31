package com.pagewise.app.ui.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.pagewise.app.data.SessionResult
import com.pagewise.app.data.UserPrefs
import com.pagewise.app.data.db.BookStatus
import com.pagewise.app.data.db.RatingAspect
import com.pagewise.app.domain.Goals
import com.pagewise.app.domain.Stats
import com.pagewise.app.domain.formatDuration
import com.pagewise.app.service.ReadingTimer
import com.pagewise.app.ui.Routes
import com.pagewise.app.ui.components.LocalRepo
import com.pagewise.app.ui.components.Outfit
import com.pagewise.app.ui.components.Pip
import com.pagewise.app.ui.components.ProgressRing
import com.pagewise.app.ui.components.StarRating
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private enum class Step { DETAILS, SUMMARY, FINISHED, RATE }

private val moods = listOf("😴", "😐", "🙂", "😃", "🤩")

@Composable
fun EndSessionScreen(nav: NavController, prefs: UserPrefs) {
    val repo = LocalRepo.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val outfit = Outfit.of(prefs.activeOutfit)

    // Snapshot the timer once; it is cleared when the session is saved.
    val timer = remember { ReadingTimer.state.value }
    var step by rememberSaveable { mutableStateOf(Step.DETAILS) }
    var result by remember { mutableStateOf<SessionResult?>(null) }

    val bookId = timer?.bookId ?: result?.session?.bookId
    val book by remember(bookId) { bookId?.let { repo.book(it) } ?: flowOf(null) }.collectAsStateWithLifecycle(null)
    val sessions by repo.sessions.collectAsStateWithLifecycle(emptyList())
    val books by repo.books.collectAsStateWithLifecycle(emptyList())
    val goals by repo.goals.collectAsStateWithLifecycle(emptyList())

    if (timer == null && result == null) {
        LaunchedEffect(Unit) { nav.popBackStack() }
        return
    }
    val done = {
        nav.popBackStack(Routes.HOME, inclusive = false)
        Unit
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when (step) {
            Step.DETAILS -> {
                val t = timer!!
                var endPage by rememberSaveable { mutableStateOf(t.startPage.toString()) }
                var mood by rememberSaveable { mutableStateOf<Int?>(null) }
                var reflection by rememberSaveable { mutableStateOf("") }
                val maxPage = book?.pageCount?.takeIf { it > 0 } ?: Int.MAX_VALUE

                Pip(outfit, 100.dp, animate = true)
                Text("Session complete", style = MaterialTheme.typography.headlineMedium)
                Text(formatDuration(t.accumulatedMs / 1000), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                Text("Which page did you stop on?", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalIconButton(onClick = { endPage = ((endPage.toIntOrNull() ?: 0) - 1).coerceAtLeast(0).toString() }) { Icon(Icons.Filled.Remove, "Less") }
                    OutlinedTextField(
                        endPage, { endPage = it.filter(Char::isDigit).take(6) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.size(width = 110.dp, height = 60.dp),
                        textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center),
                    )
                    FilledTonalIconButton(onClick = { endPage = ((endPage.toIntOrNull() ?: 0) + 1).coerceAtMost(maxPage).toString() }) { Icon(Icons.Filled.Add, "More") }
                }
                book?.takeIf { it.pageCount > 0 }?.let { Text("of ${it.pageCount}", style = MaterialTheme.typography.bodySmall) }

                Text("How did it feel?", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    moods.forEachIndexed { i, e ->
                        val selected = mood == i + 1
                        Box(
                            Modifier.size(48.dp).clip(CircleShape)
                                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                            contentAlignment = Alignment.Center,
                        ) {
                            TextButton(onClick = { mood = if (selected) null else i + 1 }) { Text(e, fontSize = 22.sp) }
                        }
                    }
                }
                OutlinedTextField(
                    reflection, { reflection = it }, label = { Text("Reflection (optional)") },
                    placeholder = { Text("What stood out in this session?") }, minLines = 2, modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        scope.launch {
                            val final = ReadingTimer.finish(context) ?: return@launch
                            val end = (endPage.toIntOrNull() ?: final.startPage).coerceAtMost(maxPage)
                            result = repo.saveSession(
                                bookId = final.bookId, startTime = final.startedAt, durationSec = final.accumulatedMs / 1000,
                                startPage = final.startPage, endPage = end, timed = true,
                                reflection = reflection.ifBlank { null }, mood = mood,
                            )
                            step = Step.SUMMARY
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Save session") }
                TextButton(onClick = { ReadingTimer.finish(context); done() }) { Text("Discard session") }
            }

            Step.SUMMARY -> {
                val r = result!!
                val streak = Stats.streak(sessions)
                Pip(outfit, 110.dp, animate = true)
                Text("Nicely done!", style = MaterialTheme.typography.headlineMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Metric("Time", formatDuration(r.session.durationSec))
                    Metric("Pages", r.session.pagesRead.toString())
                    Metric("Ink", "+${r.inkEarned}")
                }
                Text("🔥 ${streak.current}-day streak", style = MaterialTheme.typography.titleLarge)
                WeekStrip(Stats.readingDays(sessions))
                Goals.currentDaily(goals)?.let { g ->
                    val p = Goals.progress(g, books, sessions)
                    ProgressRing(p.fraction, Modifier.size(120.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${p.value}/${g.target}", style = MaterialTheme.typography.titleLarge)
                            Text(g.metric.unit, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Text(if (p.done) "Daily goal reached. See you tomorrow!" else "${g.target - p.value} ${g.metric.unit} left for today")
                }
                Button(onClick = { if (r.bookFinished) step = Step.FINISHED else done() }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (r.bookFinished) "Continue" else "Done")
                }
            }

            Step.FINISHED -> {
                val b = book ?: return@Column
                Pip(outfit, 120.dp, animate = true)
                Text("You reached the last page!", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Text("Mark \"${b.title}\" as finished?", textAlign = TextAlign.Center)
                Button(onClick = { scope.launch { repo.setStatus(b, BookStatus.FINISHED); step = Step.RATE } }, modifier = Modifier.fillMaxWidth()) {
                    Text("Yes, I finished it")
                }
                TextButton(onClick = done) { Text("Not yet") }
            }

            Step.RATE -> {
                val b = book ?: return@Column
                val values = remember { mutableStateMapOf<RatingAspect, Float>() }
                Text("Rate \"${b.title}\"", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                RatingAspect.entries.forEach { a ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(a.label, Modifier.weight(1f))
                        StarRating(values[a] ?: 0f, { values[a] = it })
                    }
                }
                Button(onClick = {
                    scope.launch {
                        repo.saveRatings(b.id, values.toMap())
                        nav.navigate(Routes.infographic(b.id)) { popUpTo(Routes.HOME) }
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Save & create infographic") }
                OutlinedButton(onClick = { scope.launch { repo.saveRatings(b.id, values.toMap()); done() } }, modifier = Modifier.fillMaxWidth()) {
                    Text("Save")
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Monday-to-Sunday dots for the current week, filled on days with reading. */
@Composable
fun WeekStrip(days: Set<LocalDate>) {
    val today = LocalDate.now()
    val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (0..6).forEach { i ->
            val d = monday.plusDays(i.toLong())
            val read = d in days
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(30.dp).clip(CircleShape).background(
                        when {
                            read -> MaterialTheme.colorScheme.primary
                            d == today -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    ),
                    contentAlignment = Alignment.Center,
                ) { if (read) Text("✓", color = MaterialTheme.colorScheme.onPrimary) }
                Text(d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
