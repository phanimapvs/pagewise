package com.pagewise.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
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

/** The companion animal, chosen in Settings or the shop. Every animal shares one body layout, so outfits fit them all. */
enum class Critter(val id: String, val label: String) {
    FOX("fox", "Fox"),
    CAT("cat", "Black cat"),
    PUPPY("puppy", "Puppy"),
    ELEPHANT("elephant", "Elephant"),
    LEOPARD("leopard", "Leopard");

    companion object {
        fun of(id: String) = entries.firstOrNull { it.id == id } ?: FOX
    }
}

val LocalCritter = staticCompositionLocalOf { Critter.FOX }

private val Ink = Color(0xFF2B2B33)
private val BookBlue = Color(0xFF5C6BC0)

@Composable
fun Pip(outfit: Outfit, size: Dp = 96.dp, modifier: Modifier = Modifier, animate: Boolean = false, critter: Critter = LocalCritter.current) {
    val bob = if (animate) {
        rememberInfiniteTransition(label = "pip").animateFloat(0f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "bob")
    } else null
    // The bob is read in the layer block, so each frame only moves the layer:
    // no recomposition and no re-drawing of the animal.
    Canvas(
        modifier.size(size).graphicsLayer {
            if (bob != null) translationY = -bob.value * this.size.minDimension * 0.03f
        },
    ) { drawPip(this.size.minDimension, critter, outfit) }
}

private fun DrawScope.eyes(cx: Float, s: Float, iris: Color? = null) {
    for (dir in listOf(-1f, 1f)) {
        val c = Offset(cx + dir * s * 0.12f, s * 0.4f)
        if (iris != null) {
            drawOval(iris, Offset(c.x - s * 0.045f, c.y - s * 0.04f), Size(s * 0.09f, s * 0.08f))
            drawOval(Ink, Offset(c.x - s * 0.016f, c.y - s * 0.035f), Size(s * 0.032f, s * 0.07f))
        } else {
            drawCircle(Ink, s * 0.035f, c)
        }
        drawCircle(Color.White, s * 0.012f, Offset(c.x + s * 0.01f, c.y - s * 0.01f))
    }
}

private fun DrawScope.body(cx: Float, s: Float, fur: Color, belly: Color) {
    drawOval(fur, Offset(cx - s * 0.26f, s * 0.58f), Size(s * 0.52f, s * 0.4f))
    drawOval(belly, Offset(cx - s * 0.15f, s * 0.64f), Size(s * 0.3f, s * 0.32f))
}

private fun DrawScope.pointyEars(cx: Float, s: Float, fur: Color, inner: Color, spread: Float = 0.34f, tip: Float = 0.06f) {
    for (dir in listOf(-1f, 1f)) {
        drawPath(Path().apply {
            moveTo(cx + dir * s * 0.12f, s * 0.26f)
            lineTo(cx + dir * s * spread, s * tip)
            lineTo(cx + dir * s * 0.33f, s * 0.36f)
            close()
        }, fur)
        drawPath(Path().apply {
            moveTo(cx + dir * s * 0.17f, s * 0.27f)
            lineTo(cx + dir * s * (spread - 0.03f), s * (tip + 0.07f))
            lineTo(cx + dir * s * 0.30f, s * 0.32f)
            close()
        }, inner)
    }
}

private fun DrawScope.head(cx: Float, s: Float, fur: Color) =
    drawOval(fur, Offset(cx - s * 0.32f, s * 0.2f), Size(s * 0.64f, s * 0.46f))

private fun DrawScope.muzzle(cx: Float, s: Float, color: Color) = drawPath(Path().apply {
    moveTo(cx - s * 0.3f, s * 0.44f)
    quadraticTo(cx, s * 0.36f, cx + s * 0.3f, s * 0.44f)
    quadraticTo(cx + s * 0.18f, s * 0.68f, cx, s * 0.66f)
    quadraticTo(cx - s * 0.18f, s * 0.68f, cx - s * 0.3f, s * 0.44f)
}, color)

private fun DrawScope.nose(cx: Float, s: Float, color: Color) =
    drawOval(color, Offset(cx - s * 0.035f, s * 0.5f), Size(s * 0.07f, s * 0.05f))

private fun DrawScope.drawCritter(s: Float, critter: Critter) {
    val cx = s / 2
    when (critter) {
        Critter.FOX -> {
            val fur = Color(0xFFE8833A)
            body(cx, s, fur, Color(0xFFFFF3E2))
            pointyEars(cx, s, fur, Color(0xFFB85A1F))
            head(cx, s, fur)
            muzzle(cx, s, Color(0xFFFFF3E2))
            eyes(cx, s)
            nose(cx, s, Ink)
        }
        Critter.CAT -> {
            val fur = Color(0xFF26262E)
            body(cx, s, fur, Color(0xFF34343E))
            pointyEars(cx, s, fur, Color(0xFFE79AAE), spread = 0.31f, tip = 0.1f)
            head(cx, s, fur)
            eyes(cx, s, iris = Color(0xFFD9E36A))
            // tiny pink nose and whiskers
            drawPath(Path().apply {
                moveTo(cx - s * 0.03f, s * 0.5f)
                lineTo(cx + s * 0.03f, s * 0.5f)
                lineTo(cx, s * 0.535f)
                close()
            }, Color(0xFFE79AAE))
            for (dir in listOf(-1f, 1f)) for (dy in listOf(-0.015f, 0.02f)) {
                drawLine(
                    Color.White.copy(alpha = 0.55f),
                    Offset(cx + dir * s * 0.08f, s * (0.53f + dy / 2)),
                    Offset(cx + dir * s * 0.27f, s * (0.51f + dy)),
                    s * 0.008f,
                )
            }
        }
        Critter.PUPPY -> {
            val fur = Color(0xFFD9A066)
            val ear = Color(0xFF8B5A2B)
            body(cx, s, fur, Color(0xFFFFF1DD))
            head(cx, s, fur)
            // floppy ears hanging at the sides
            for (dir in listOf(-1f, 1f)) {
                drawOval(ear, Offset(cx + dir * s * 0.3f - s * 0.09f, s * 0.22f), Size(s * 0.18f, s * 0.34f))
            }
            drawOval(Color(0xFFFFF1DD), Offset(cx - s * 0.17f, s * 0.43f), Size(s * 0.34f, s * 0.22f))
            // a patch around one eye
            drawOval(Color(0xFFB07A45), Offset(cx + s * 0.05f, s * 0.33f), Size(s * 0.14f, s * 0.13f))
            eyes(cx, s)
            nose(cx, s, Ink)
            drawOval(Color(0xFFE5737D), Offset(cx - s * 0.025f, s * 0.575f), Size(s * 0.05f, s * 0.06f))
        }
        Critter.ELEPHANT -> {
            val skin = Color(0xFF9AA5B4)
            body(cx, s, skin, Color(0xFFB4BDC9))
            // big round ears behind the head
            for (dir in listOf(-1f, 1f)) {
                drawOval(Color(0xFF8794A5), Offset(cx + dir * s * 0.31f - s * 0.17f, s * 0.18f), Size(s * 0.34f, s * 0.38f))
                drawOval(Color(0xFFE3B7C4), Offset(cx + dir * s * 0.33f - s * 0.1f, s * 0.25f), Size(s * 0.2f, s * 0.24f))
            }
            head(cx, s, skin)
            eyes(cx, s)
            // curled trunk
            drawPath(Path().apply {
                moveTo(cx - s * 0.05f, s * 0.46f)
                quadraticTo(cx - s * 0.06f, s * 0.66f, cx + s * 0.05f, s * 0.7f)
                quadraticTo(cx + s * 0.12f, s * 0.72f, cx + s * 0.11f, s * 0.64f)
                lineTo(cx + s * 0.07f, s * 0.64f)
                quadraticTo(cx + s * 0.07f, s * 0.67f, cx + s * 0.04f, s * 0.66f)
                quadraticTo(cx + s * 0.03f, s * 0.6f, cx + s * 0.05f, s * 0.46f)
                close()
            }, skin)
            for (dir in listOf(-1f, 1f)) drawCircle(Color(0xFFF2B8C6).copy(alpha = 0.7f), s * 0.03f, Offset(cx + dir * s * 0.2f, s * 0.5f))
        }
        Critter.LEOPARD -> {
            val fur = Color(0xFFE8B04B)
            val spot = Color(0xFF6B4A2B)
            body(cx, s, fur, Color(0xFFFFF4DC))
            for (dir in listOf(-1f, 1f)) {
                drawCircle(fur, s * 0.09f, Offset(cx + dir * s * 0.24f, s * 0.22f))
                drawCircle(spot, s * 0.045f, Offset(cx + dir * s * 0.24f, s * 0.22f))
            }
            head(cx, s, fur)
            muzzle(cx, s, Color(0xFFFFF4DC))
            // rosettes on the head and body
            val spots = listOf(
                -0.2f to 0.28f, 0.18f to 0.27f, -0.26f to 0.4f, 0.27f to 0.42f, -0.05f to 0.24f,
                -0.2f to 0.7f, 0.21f to 0.74f, 0.17f to 0.88f, -0.17f to 0.9f,
            )
            for ((dx, y) in spots) drawCircle(spot, s * 0.022f, Offset(cx + dx * s, y * s), style = Stroke(s * 0.012f))
            eyes(cx, s, iris = Color(0xFF8DB86B))
            nose(cx, s, Color(0xFF9C5A4A))
        }
    }
    // small open book held in paws
    drawRect(BookBlue, Offset(cx - s * 0.16f, s * 0.78f), Size(s * 0.32f, s * 0.12f))
    drawRect(Color.White, Offset(cx - s * 0.145f, s * 0.785f), Size(s * 0.135f, s * 0.1f))
    drawRect(Color.White, Offset(cx + s * 0.01f, s * 0.785f), Size(s * 0.135f, s * 0.1f))
}

private fun DrawScope.drawPip(s: Float, critter: Critter, outfit: Outfit) {
    val cx = s / 2
    drawCritter(s, critter)

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
