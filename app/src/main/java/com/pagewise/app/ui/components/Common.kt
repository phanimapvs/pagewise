package com.pagewise.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.pagewise.app.ui.theme.DisplayFont
import com.pagewise.app.data.Repository
import com.pagewise.app.data.db.Book
import com.pagewise.app.ui.theme.StatNumber
import kotlin.math.absoluteValue

val LocalRepo = staticCompositionLocalOf<Repository> { error("Repository not provided") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(title: String, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
        actions = { actions() },
    )
}

private val coverPalette = listOf(
    Color(0xFF2E3A59), Color(0xFF8E3B5F), Color(0xFF3D7068), Color(0xFFB85A1F),
    Color(0xFF5C6BC0), Color(0xFF6D4C41), Color(0xFF455A64), Color(0xFF7B1FA2),
)

/** Book cover image, or a generated typographic cover when there's no image. */
@Composable
fun BookCover(book: Book, modifier: Modifier = Modifier, width: Dp = 72.dp) =
    BookCover(book.title, book.authors, book.coverUrl, modifier, width)

@Composable
fun BookCover(title: String, authors: String, coverUrl: String?, modifier: Modifier = Modifier, width: Dp = 72.dp) {
    val shape = RoundedCornerShape(6.dp)
    val m = modifier.width(width).height(width * 1.5f).clip(shape)
    if (coverUrl.isNullOrBlank()) {
        GeneratedCover(title, authors, m)
    } else {
        SubcomposeAsyncImage(
            model = coverUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = m,
            error = { GeneratedCover(title, authors, Modifier.fillMaxSize()) },
            loading = { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh)) },
        )
    }
}

@Composable
private fun GeneratedCover(title: String, authors: String, modifier: Modifier) {
    val base = coverPalette[title.hashCode().absoluteValue % coverPalette.size]
    Box(
        modifier.background(Brush.verticalGradient(listOf(base, base.copy(alpha = 0.8f)))).padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                title, color = Color.White, fontFamily = DisplayFont, fontWeight = FontWeight.Bold,
                fontSize = 11.sp, lineHeight = 13.sp, textAlign = TextAlign.Center, maxLines = 4, overflow = TextOverflow.Ellipsis,
            )
            if (authors.isNotBlank()) Text(
                authors, color = Color.White.copy(alpha = 0.8f), fontSize = 8.sp, textAlign = TextAlign.Center,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** A row of tiles that all match the tallest one's height. Give each tile `Modifier.weight(1f)`. */
@Composable
fun TileRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp), content = content)
}

/**
 * A labelled number. The note line is always reserved (blank when [sub] is null), so every
 * tile in the app is the same height whether or not it has a note.
 */
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, sub: String? = null) {
    Card(modifier.fillMaxHeight(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, style = StatNumber, maxLines = 1)
            Text(sub.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, outfit: Outfit = Outfit.CLASSIC, action: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Pip(outfit, 96.dp)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(12.dp))
            action()
        }
    }
}

@Composable
fun ProgressRing(fraction: Float, modifier: Modifier = Modifier, stroke: Dp = 10.dp, color: Color = MaterialTheme.colorScheme.primary, content: @Composable () -> Unit = {}) {
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = stroke.toPx()
            val inset = w / 2
            val sz = Size(size.width - w, size.height - w)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), sz, style = Stroke(w))
            drawArc(color, -90f, 360f * fraction.coerceIn(0f, 1f), false, Offset(inset, inset), sz, style = Stroke(w, cap = StrokeCap.Round))
        }
        content()
    }
}

@Composable
fun LinearBar(fraction: Float, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    Canvas(modifier.fillMaxWidth().height(8.dp)) {
        val r = CornerRadius(size.height / 2)
        drawRoundRect(track, cornerRadius = r)
        drawRoundRect(color, size = Size(size.width * fraction.coerceIn(0f, 1f), size.height), cornerRadius = r)
    }
}

/** Simple vertical bar chart with labels underneath. */
@Composable
fun BarChart(values: List<Pair<String, Int>>, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary, highlightLast: Boolean = true) {
    val max = (values.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    val muted = color.copy(alpha = 0.45f)
    Column(modifier) {
        Row(Modifier.fillMaxWidth().height(140.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
            values.forEachIndexed { i, (_, v) ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (v > 0) Text(v.toString(), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    Canvas(Modifier.fillMaxWidth().height((110f * v / max).dp.coerceAtLeast(2.dp))) {
                        drawRoundRect(
                            if (highlightLast && i == values.lastIndex) color else muted,
                            cornerRadius = CornerRadius(4.dp.toPx()),
                        )
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            values.forEach { (label, _) ->
                Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** Star rating in half-star steps; tap a star's left/right half. */
@Composable
fun StarRating(value: Float, onChange: ((Float) -> Unit)?, modifier: Modifier = Modifier, size: Dp = 28.dp) {
    Row(modifier) {
        for (i in 1..5) {
            val icon = when {
                value >= i -> Icons.Filled.Star
                value >= i - 0.5f -> Icons.AutoMirrored.Filled.StarHalf
                else -> Icons.Filled.StarOutline
            }
            Box(Modifier.size(size)) {
                Icon(icon, null, tint = Color(0xFFE5B53B), modifier = Modifier.fillMaxSize())
                if (onChange != null) Row(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxSize().clickable { onChange(if (value == i - 0.5f) 0f else i - 0.5f) })
                    Box(Modifier.weight(1f).fillMaxSize().clickable { onChange(if (value == i.toFloat()) 0f else i.toFloat()) })
                }
            }
        }
    }
}

@Composable
fun TextInputDialog(
    title: String,
    initial: String = "",
    label: String = "",
    singleLine: Boolean = true,
    number: Boolean = false,
    confirm: String = "Save",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                text, { text = it }, label = { Text(label) }, singleLine = singleLine,
                minLines = if (singleLine) 1 else 3,
                keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text.trim()) }, enabled = text.isNotBlank()) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun ConfirmDialog(title: String, body: String, confirm: String = "Delete", onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun Pill(text: String, color: Color = MaterialTheme.colorScheme.secondaryContainer, modifier: Modifier = Modifier) {
    Text(
        text, style = MaterialTheme.typography.labelMedium,
        modifier = modifier.clip(RoundedCornerShape(50)).background(color).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
