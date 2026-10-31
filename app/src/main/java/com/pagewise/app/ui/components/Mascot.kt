package com.pagewise.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Outfits for Pip the fox, bought with ink in the shop. */
enum class Outfit(val id: String, val label: String, val price: Int) {
    CLASSIC("classic", "Classic Pip", 0),
    SCARF("scarf", "Cozy Scarf", 120),
    GLASSES("glasses", "Reading Glasses", 200),
    BERET("beret", "Artist Beret", 350),
    HEADPHONES("headphones", "Audiobook Fan", 500),
    WIZARD("wizard", "Spellbook Wizard", 800),
    DETECTIVE("detective", "Mystery Detective", 1100),
    ASTRONAUT("astronaut", "Sci-fi Explorer", 1500),
    CROWN("crown", "Literary Royalty", 2400);

    companion object {
        fun of(id: String) = entries.firstOrNull { it.id == id } ?: CLASSIC
    }
}

private val Fur = Color(0xFFE8833A)
private val FurDark = Color(0xFFB85A1F)
private val Cream = Color(0xFFFFF3E2)
private val Nose = Color(0xFF2B2B33)

@Composable
fun Pip(outfit: Outfit, size: Dp = 96.dp, modifier: Modifier = Modifier, animate: Boolean = false) {
    val bob = if (animate) {
        rememberInfiniteTransition(label = "pip").animateFloat(0f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "bob")
    } else null
    // The bob is read in the layer block, so each frame only moves the layer:
    // no recomposition and no re-drawing of the fox.
    Canvas(
        modifier.size(size).graphicsLayer {
            if (bob != null) translationY = -bob.value * this.size.minDimension * 0.03f
        },
    ) { drawFox(this.size.minDimension, outfit) }
}

private fun DrawScope.drawFox(s: Float, outfit: Outfit) {
    val cx = s / 2
    // body
    drawOval(Fur, Offset(cx - s * 0.26f, s * 0.58f), Size(s * 0.52f, s * 0.4f))
    drawOval(Cream, Offset(cx - s * 0.15f, s * 0.64f), Size(s * 0.3f, s * 0.32f))
    // ears
    for (dir in listOf(-1f, 1f)) {
        val ear = Path().apply {
            moveTo(cx + dir * s * 0.12f, s * 0.26f)
            lineTo(cx + dir * s * 0.34f, s * 0.06f)
            lineTo(cx + dir * s * 0.33f, s * 0.36f)
            close()
        }
        drawPath(ear, Fur)
        val inner = Path().apply {
            moveTo(cx + dir * s * 0.17f, s * 0.27f)
            lineTo(cx + dir * s * 0.31f, s * 0.13f)
            lineTo(cx + dir * s * 0.30f, s * 0.32f)
            close()
        }
        drawPath(inner, FurDark)
    }
    // head
    drawOval(Fur, Offset(cx - s * 0.32f, s * 0.2f), Size(s * 0.64f, s * 0.46f))
    val muzzle = Path().apply {
        moveTo(cx - s * 0.3f, s * 0.44f)
        quadraticTo(cx, s * 0.36f, cx + s * 0.3f, s * 0.44f)
        quadraticTo(cx + s * 0.18f, s * 0.68f, cx, s * 0.66f)
        quadraticTo(cx - s * 0.18f, s * 0.68f, cx - s * 0.3f, s * 0.44f)
    }
    drawPath(muzzle, Cream)
    // eyes
    for (dir in listOf(-1f, 1f)) {
        drawCircle(Nose, s * 0.035f, Offset(cx + dir * s * 0.12f, s * 0.4f))
        drawCircle(Color.White, s * 0.012f, Offset(cx + dir * s * 0.12f + s * 0.01f, s * 0.39f))
    }
    drawOval(Nose, Offset(cx - s * 0.035f, s * 0.5f), Size(s * 0.07f, s * 0.05f))
    // small open book held in paws
    drawRect(Color(0xFF5C6BC0), Offset(cx - s * 0.16f, s * 0.78f), Size(s * 0.32f, s * 0.12f))
    drawRect(Color.White, Offset(cx - s * 0.145f, s * 0.785f), Size(s * 0.135f, s * 0.1f))
    drawRect(Color.White, Offset(cx + s * 0.01f, s * 0.785f), Size(s * 0.135f, s * 0.1f))

    when (outfit) {
        Outfit.CLASSIC -> Unit
        Outfit.SCARF -> {
            drawRoundRect(Color(0xFFD64545), Offset(cx - s * 0.24f, s * 0.6f), Size(s * 0.48f, s * 0.08f), androidx.compose.ui.geometry.CornerRadius(s * 0.04f))
            drawRect(Color(0xFFD64545), Offset(cx + s * 0.1f, s * 0.64f), Size(s * 0.07f, s * 0.16f))
            drawRect(Color.White.copy(alpha = 0.6f), Offset(cx + s * 0.1f, s * 0.72f), Size(s * 0.07f, s * 0.02f))
        }
        Outfit.GLASSES -> glasses(cx, s, Color(0xFF3B3B44))
        Outfit.BERET -> {
            drawOval(Color(0xFF8E3B5F), Offset(cx - s * 0.26f, s * 0.12f), Size(s * 0.46f, s * 0.16f))
            drawCircle(Color(0xFF8E3B5F), s * 0.02f, Offset(cx - s * 0.02f, s * 0.11f))
        }
        Outfit.HEADPHONES -> {
            drawArc(Color(0xFF30343F), 190f, 160f, false, Offset(cx - s * 0.34f, s * 0.12f), Size(s * 0.68f, s * 0.5f), style = Stroke(s * 0.05f))
            for (dir in listOf(-1f, 1f)) drawRoundRect(
                Color(0xFF4FC3F7), Offset(cx + dir * s * 0.33f - s * 0.05f, s * 0.34f), Size(s * 0.1f, s * 0.15f),
                androidx.compose.ui.geometry.CornerRadius(s * 0.03f),
            )
        }
        Outfit.WIZARD -> {
            val hat = Path().apply {
                moveTo(cx - s * 0.24f, s * 0.24f)
                lineTo(cx + s * 0.04f, -s * 0.06f)
                lineTo(cx + s * 0.24f, s * 0.24f)
                close()
            }
            drawPath(hat, Color(0xFF4B3C99))
            drawOval(Color(0xFF4B3C99), Offset(cx - s * 0.3f, s * 0.2f), Size(s * 0.6f, s * 0.08f))
            drawCircle(Gold, s * 0.025f, Offset(cx - s * 0.04f, s * 0.12f))
            drawCircle(Gold, s * 0.015f, Offset(cx + s * 0.08f, s * 0.04f))
        }
        Outfit.DETECTIVE -> {
            drawOval(Color(0xFF8D6E4F), Offset(cx - s * 0.3f, s * 0.17f), Size(s * 0.6f, s * 0.1f))
            drawRoundRect(Color(0xFF8D6E4F), Offset(cx - s * 0.18f, s * 0.06f), Size(s * 0.36f, s * 0.16f), androidx.compose.ui.geometry.CornerRadius(s * 0.06f))
            drawRect(Color(0xFF5D4632), Offset(cx - s * 0.18f, s * 0.16f), Size(s * 0.36f, s * 0.03f))
            drawCircle(Color(0xFF3B3B44), s * 0.06f, Offset(cx + s * 0.12f, s * 0.4f), style = Stroke(s * 0.015f))
        }
        Outfit.ASTRONAUT -> {
            drawCircle(Color(0xFFB3E5FC).copy(alpha = 0.35f), s * 0.42f, Offset(cx, s * 0.4f))
            drawCircle(Color.White, s * 0.42f, Offset(cx, s * 0.4f), style = Stroke(s * 0.035f))
            drawCircle(Color.White.copy(alpha = 0.7f), s * 0.04f, Offset(cx - s * 0.22f, s * 0.2f))
        }
        Outfit.CROWN -> {
            val crown = Path().apply {
                moveTo(cx - s * 0.18f, s * 0.22f)
                lineTo(cx - s * 0.2f, s * 0.06f)
                lineTo(cx - s * 0.08f, s * 0.14f)
                lineTo(cx, s * 0.02f)
                lineTo(cx + s * 0.08f, s * 0.14f)
                lineTo(cx + s * 0.2f, s * 0.06f)
                lineTo(cx + s * 0.18f, s * 0.22f)
                close()
            }
            drawPath(crown, Gold)
            drawCircle(Color(0xFFD64545), s * 0.02f, Offset(cx, s * 0.17f))
        }
    }
}

private val Gold = Color(0xFFE5B53B)

private fun DrawScope.glasses(cx: Float, s: Float, c: Color) {
    val stroke = Stroke(s * 0.018f)
    drawCircle(c, s * 0.065f, Offset(cx - s * 0.12f, s * 0.4f), style = stroke)
    drawCircle(c, s * 0.065f, Offset(cx + s * 0.12f, s * 0.4f), style = stroke)
    drawLine(c, Offset(cx - s * 0.055f, s * 0.4f), Offset(cx + s * 0.055f, s * 0.4f), s * 0.018f)
}
