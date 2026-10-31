package com.pagewise.app.ui.components

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import com.pagewise.app.service.AmbientSound
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private class Particle(val x: Float, val phase: Float, val speed: Float, val size: Float, val sway: Float)

/**
 * Quiet animated layer behind the timer: rain streaks for wet soundscapes,
 * slow drifting dust motes in lamplight otherwise. Freezes while paused and
 * when the system has animations turned off.
 */
@Composable
fun AmbientBackdrop(sound: AmbientSound?, running: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val motionOff = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val rainy = sound == AmbientSound.RAIN || sound == AmbientSound.THUNDER
    val particles = remember(rainy) {
        val rnd = Random(7)
        List(if (rainy) 70 else 28) {
            Particle(
                x = rnd.nextFloat(),
                phase = rnd.nextFloat(),
                speed = if (rainy) 0.45f + rnd.nextFloat() * 0.35f else 0.012f + rnd.nextFloat() * 0.02f,
                size = if (rainy) 0.025f + rnd.nextFloat() * 0.035f else 1.2f + rnd.nextFloat() * 2.4f,
                sway = rnd.nextFloat() * 0.03f,
            )
        }
    }
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(running, motionOff) {
        if (!running || motionOff) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) t += (now - last) / 1e9f
                last = now
            }
        }
    }

    val rainColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val moteColor = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        if (rainy) {
            for (p in particles) {
                // Streaks fall slightly slanted, wrapping from bottom back to top.
                val y = ((p.phase + t * p.speed) % 1.15f - 0.1f) * h
                val x = p.x * w + y * 0.08f
                drawLine(rainColor, Offset(x, y), Offset(x + p.size * h * 0.08f, y + p.size * h), strokeWidth = 2f, cap = StrokeCap.Round)
            }
        } else {
            for (p in particles) {
                // Motes rise slowly, swaying, and fade in and out at the edges.
                val life = (p.phase + t * p.speed) % 1f
                val y = (1f - life) * h
                val x = (p.x + sin(2 * PI * (t * 0.05f + p.phase)).toFloat() * p.sway) * w
                val fade = sin(PI * life).toFloat()
                drawCircle(moteColor.copy(alpha = 0.10f * fade), radius = p.size * 4f, center = Offset(x, y))
                drawCircle(moteColor.copy(alpha = 0.35f * fade), radius = p.size, center = Offset(x, y))
            }
        }
    }
}
