package com.pagewise.app.ui.book

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.pagewise.app.data.db.RatingAspect
import com.pagewise.app.domain.Stats
import com.pagewise.app.domain.formatDuration
import com.pagewise.app.domain.toLocalDate
import com.pagewise.app.ui.components.BackTopBar
import com.pagewise.app.ui.components.BookCover
import com.pagewise.app.ui.components.LocalRepo
import com.pagewise.app.ui.components.StarRating
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun InfographicScreen(nav: NavController, bookId: Long) {
    val repo = LocalRepo.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val book by repo.book(bookId).collectAsStateWithLifecycle(null)
    val sessions by repo.sessionsFor(bookId).collectAsStateWithLifecycle(emptyList())
    val quotes by repo.quotes(bookId).collectAsStateWithLifecycle(emptyList())
    val ratings by repo.ratings(bookId).collectAsStateWithLifecycle(emptyList())
    val layer = rememberGraphicsLayer()
    val b = book ?: return

    val start = b.startedAt?.toLocalDate() ?: sessions.minOfOrNull { it.startTime }?.toLocalDate()
    val end = b.finishedAt?.toLocalDate()
    val days = if (start != null && end != null) ChronoUnit.DAYS.between(start, end) + 1 else null
    val stars = ratings.firstOrNull { it.aspect == RatingAspect.OVERALL }?.stars
    val quote = quotes.filter { it.text.isNotBlank() }.minByOrNull { it.text.length.coerceAtLeast(40) }
    val fmt = DateTimeFormatter.ofPattern("d MMM yyyy")

    Scaffold(topBar = { BackTopBar("Infographic", { nav.popBackStack() }) }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Column(
                Modifier
                    .drawWithContent {
                        layer.record { this@drawWithContent.drawContent() }
                        drawLayer(layer)
                    }
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF2E3A59), Color(0xFF1B2238))))
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("I FINISHED", color = Color(0xFFFFA676), letterSpacing = 4.sp, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(12.dp))
                BookCover(b, width = 120.dp)
                Spacer(Modifier.height(12.dp))
                Text(b.title, color = Color.White, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 22.sp, textAlign = TextAlign.Center)
                if (b.authors.isNotBlank()) Text(b.authors, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                stars?.let { StarRating(it, null, Modifier.padding(top = 8.dp), size = 22.dp) }
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Big(days?.toString() ?: "–", "days")
                    Big(formatDuration(sessions.sumOf { it.durationSec }), "reading")
                    Big(sessions.size.toString(), "sessions")
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Big(if (b.pageCount > 0) b.pageCount.toString() else "–", "pages")
                    Big(Stats.pagesPerHour(sessions).takeIf { it > 0 }?.let { "%.0f".format(it) } ?: "–", "pages/hour")
                    Big(Stats.readingDays(sessions).size.toString(), "days read")
                }
                quote?.let {
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "“${it.text}”", color = Color.White.copy(alpha = 0.9f), fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center, maxLines = 5, overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    listOfNotNull(start?.format(fmt), end?.format(fmt)).joinToString(" → "),
                    color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium,
                )
                Text("tracked with Pagewise", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                scope.launch {
                    val bmp = layer.toImageBitmap().asAndroidBitmap()
                    share(context, bmp)
                    repo.flag("infographic")
                }
            }) {
                Icon(Icons.Filled.Share, null)
                Spacer(Modifier.width(8.dp))
                Text("Share")
            }
        }
    }
}

@Composable
private fun Big(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(label, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
    }
}

private suspend fun share(context: Context, bmp: Bitmap) {
    val file = withContext(Dispatchers.IO) {
        File(context.cacheDir, "shared").apply { mkdirs() }.let { File(it, "infographic.png") }.also { f ->
            f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val intent = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(intent, "Share infographic"))
}
