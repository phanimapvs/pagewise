package com.pagewise.app.ui.timer

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.pagewise.app.data.UserPrefs
import com.pagewise.app.service.AmbientSound
import com.pagewise.app.service.ReadingTimer
import com.pagewise.app.service.TimerState
import com.pagewise.app.ui.Routes
import com.pagewise.app.ui.components.AmbientBackdrop
import com.pagewise.app.ui.components.BackTopBar
import com.pagewise.app.ui.components.LocalRepo
import com.pagewise.app.ui.components.Outfit
import com.pagewise.app.ui.components.Pip
import com.pagewise.app.ui.components.ProgressRing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimerScreen(nav: NavController, prefs: UserPrefs) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val state by ReadingTimer.state.collectAsStateWithLifecycle()
    val s = state
    if (s == null) {
        LaunchedEffect(Unit) { nav.popBackStack() }
        return
    }
    var sound by remember { mutableStateOf(ReadingTimer.ambient.current) }
    var volume by remember { mutableFloatStateOf(ReadingTimer.ambient.volume) }

    LaunchedEffect(Unit) {
        // Restore the user's preferred sound when a fresh session starts.
        if (sound == null && prefs.defaultSound != null && s.elapsedMs() < 2000) {
            AmbientSound.entries.firstOrNull { it.name == prefs.defaultSound }?.let {
                ReadingTimer.ambient.play(it); sound = it
            }
        }
    }

    Scaffold(topBar = { BackTopBar("Reading", { nav.popBackStack() }) }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
        AmbientBackdrop(sound, running = !s.paused, modifier = Modifier.matchParentSize())
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(s.bookTitle, style = MaterialTheme.typography.titleLarge)
            Text("Started on page ${s.startPage}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))

            TimerDial(s, Outfit.of(prefs.activeOutfit))
            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                LargeFloatingActionButton(onClick = { if (s.paused) ReadingTimer.resume() else ReadingTimer.pause() }) {
                    Icon(if (s.paused) Icons.Filled.PlayArrow else Icons.Filled.Pause, if (s.paused) "Resume" else "Pause", Modifier.size(36.dp))
                }
                FilledIconButton(onClick = { ReadingTimer.requestStop(); nav.navigate(Routes.END) { popUpTo(Routes.TIMER) { inclusive = true } } }, modifier = Modifier.size(64.dp)) {
                    Icon(Icons.Filled.Stop, "Stop", Modifier.size(32.dp))
                }
            }

            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, null)
                Text(" Ambient sound", style = MaterialTheme.typography.titleMedium)
            }
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = sound == null, onClick = { ReadingTimer.ambient.stop(); sound = null; scope.launch { repo.prefs.setDefaultSound(null) } }, label = { Text("Off") })
                AmbientSound.entries.forEach { a ->
                    FilterChip(
                        selected = sound == a,
                        onClick = {
                            ReadingTimer.ambient.play(a)
                            sound = a
                            scope.launch {
                                repo.prefs.setDefaultSound(a.name)
                                repo.flag("ambient")
                            }
                        },
                        label = { Text("${a.emoji} ${a.label}") },
                    )
                }
            }
            if (sound != null) Slider(volume, { volume = it; ReadingTimer.ambient.volume = it }, Modifier.fillMaxWidth())
        }
        }
    }
}

/** The ring and clock own the once-a-second tick, so the rest of the screen stays still. */
@Composable
private fun TimerDial(s: TimerState, outfit: Outfit) {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(s.paused) {
        while (!s.paused) {
            now = SystemClock.elapsedRealtime()
            delay(1000 - (s.elapsedMs(now) % 1000))
        }
    }
    val elapsed = s.elapsedMs(now)
    val limit = s.limitMs
    ProgressRing(
        fraction = if (limit != null) elapsed.toFloat() / limit else (elapsed % 3_600_000L) / 3_600_000f,
        modifier = Modifier.size(240.dp),
        stroke = 14.dp,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Pip(outfit, 72.dp, animate = !s.paused)
            Text(clock(elapsed), fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 40.sp)
            if (limit != null) Text("of ${limit / 60000} min", style = MaterialTheme.typography.labelMedium)
            Text("+${com.pagewise.app.domain.Ink.forSession(elapsed / 1000)} ink", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

fun clock(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val sec = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
}
