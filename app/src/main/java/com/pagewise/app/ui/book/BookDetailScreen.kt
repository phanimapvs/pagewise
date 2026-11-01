package com.pagewise.app.ui.book

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.pagewise.app.data.db.Book
import com.pagewise.app.data.db.BookStatus
import com.pagewise.app.data.db.RatingAspect
import com.pagewise.app.domain.Stats
import com.pagewise.app.domain.formatDuration
import com.pagewise.app.domain.toLocalDate
import com.pagewise.app.service.ReadingTimer
import com.pagewise.app.ui.Routes
import com.pagewise.app.ui.components.BackTopBar
import com.pagewise.app.ui.components.BookCover
import com.pagewise.app.ui.components.ConfirmDialog
import com.pagewise.app.ui.components.LinearBar
import com.pagewise.app.ui.components.LocalRepo
import com.pagewise.app.ui.components.Pill
import com.pagewise.app.ui.components.StarRating
import com.pagewise.app.ui.components.StatTile
import com.pagewise.app.ui.components.TileRow
import com.pagewise.app.ui.components.TextInputDialog
import com.pagewise.app.ui.library.statusLabels
import com.pagewise.app.ui.stats.SessionRow
import com.pagewise.app.ui.timer.LogSessionDialog
import com.pagewise.app.ui.timer.StartSessionDialog
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class Sheet { EDIT, PAGE, LEND, BORROW, DEADLINE, SHELVES, COVER, RATE, DELETE, START, LOG }

private val dateFmt = DateTimeFormatter.ofPattern("d MMM yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(nav: NavController, bookId: Long) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val book by repo.book(bookId).collectAsStateWithLifecycle(null)
    val sessions by repo.sessionsFor(bookId).collectAsStateWithLifecycle(emptyList())
    val allSessions by repo.sessions.collectAsStateWithLifecycle(emptyList())
    val ratings by repo.ratings(bookId).collectAsStateWithLifecycle(emptyList())
    val timer by ReadingTimer.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(0) }
    var menu by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    val b = book ?: return

    Scaffold(
        topBar = {
            BackTopBar("", { nav.popBackStack() }) {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "More") }
                DropdownMenu(menu, { menu = false }) {
                    val item: @Composable (String, () -> Unit) -> Unit = { label, action ->
                        DropdownMenuItem(text = { Text(label) }, onClick = { menu = false; action() })
                    }
                    item("Edit details") { sheet = Sheet.EDIT }
                    item("Change cover") { sheet = Sheet.COVER }
                    item("Shelves") { sheet = Sheet.SHELVES }
                    item("Rate") { sheet = Sheet.RATE }
                    HorizontalDivider()
                    BookStatus.entries.filter { it != b.status }.forEach { s ->
                        item("Mark as ${statusLabels[s]!!.lowercase()}") { scope.launch { repo.setStatus(b, s) } }
                    }
                    if (b.status == BookStatus.FINISHED || b.status == BookStatus.DNF) item("Read again") { scope.launch { repo.restart(b) } }
                    if (b.status == BookStatus.READING) item(if (b.isPinned) "Pinned to top" else "Pin to top") { scope.launch { repo.pin(b) } }
                    HorizontalDivider()
                    item(if (b.lentTo == null) "Lend to someone" else "Mark as returned") {
                        if (b.lentTo == null) sheet = Sheet.LEND else scope.launch { repo.saveBook(b.copy(lentTo = null)) }
                    }
                    item(if (b.borrowedFrom == null) "Borrowed from…" else "Mark as given back") {
                        if (b.borrowedFrom == null) sheet = Sheet.BORROW else scope.launch { repo.saveBook(b.copy(borrowedFrom = null)) }
                    }
                    item(if (b.targetDate == null) "Set finish-by date" else "Clear finish-by date") {
                        if (b.targetDate == null) sheet = Sheet.DEADLINE else scope.launch { repo.saveBook(b.copy(targetDate = null)) }
                    }
                    if (b.status == BookStatus.FINISHED) item("Infographic") { nav.navigate(Routes.infographic(b.id)) }
                    HorizontalDivider()
                    item("Delete book") { sheet = Sheet.DELETE }
                }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Header(b, ratings.firstOrNull { it.aspect == RatingAspect.OVERALL }?.stars, timerBusy = timer != null,
                onStart = { sheet = Sheet.START }, onLog = { sheet = Sheet.LOG }, onPage = { sheet = Sheet.PAGE })
            PrimaryScrollableTabRow(selectedTabIndex = tab, edgePadding = 12.dp) {
                listOf("Overview", "Sessions", "Quotes", "Thoughts", "Words").forEachIndexed { i, t ->
                    Tab(tab == i, { tab = i }, text = { Text(t) })
                }
            }
            when (tab) {
                0 -> Overview(b, sessions, allSessions, ratings.associate { it.aspect to it.stars })
                1 -> LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (sessions.isEmpty()) item { Text("No sessions yet. Use the timer or log one manually.") }
                    items(sessions, key = { it.id }) { s -> SessionRow(s, null) { scope.launch { repo.deleteSession(s) } } }
                }
                2 -> QuotesTab(b)
                3 -> ThoughtsTab(b)
                4 -> WordsTab(b)
            }
        }
    }

    when (sheet) {
        Sheet.START -> StartSessionDialog(b, nav) { sheet = null }
        Sheet.LOG -> LogSessionDialog(b) { sheet = null }
        Sheet.EDIT -> EditBookDialog(b) { sheet = null }
        Sheet.PAGE -> TextInputDialog("Current page", b.currentPage.toString(), "Page", number = true, onDismiss = { sheet = null }) {
            scope.launch { repo.saveBook(b.copy(currentPage = it.toIntOrNull()?.coerceAtLeast(0) ?: b.currentPage)) }
            sheet = null
        }
        Sheet.LEND -> TextInputDialog("Lend to", label = "Name", onDismiss = { sheet = null }) {
            scope.launch { repo.saveBook(b.copy(lentTo = it)) }; sheet = null
        }
        Sheet.BORROW -> TextInputDialog("Borrowed from", label = "Name or library", onDismiss = { sheet = null }) {
            scope.launch { repo.saveBook(b.copy(borrowedFrom = it)) }; sheet = null
        }
        Sheet.DEADLINE -> {
            val state = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { sheet = null },
                confirmButton = {
                    TextButton(onClick = {
                        state.selectedDateMillis?.let { utc ->
                            val d = Instant.ofEpochMilli(utc).atZone(ZoneId.of("UTC")).toLocalDate()
                            scope.launch { repo.saveBook(b.copy(targetDate = d.atTime(23, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())) }
                        }
                        sheet = null
                    }) { Text("Set") }
                },
            ) { DatePicker(state) }
        }
        Sheet.SHELVES -> ShelvesDialog(b) { sheet = null }
        Sheet.COVER -> CoverPickerDialog(b) { sheet = null }
        Sheet.RATE -> RateDialog(b, ratings.associate { it.aspect to it.stars }) { sheet = null }
        Sheet.DELETE -> ConfirmDialog("Delete \"${b.title}\"?", "Its sessions, quotes, thoughts and words are deleted too.", onDismiss = { sheet = null }) {
            scope.launch { repo.deleteBook(b); nav.popBackStack() }
        }
        null -> Unit
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Header(b: Book, stars: Float?, timerBusy: Boolean, onStart: () -> Unit, onLog: () -> Unit, onPage: () -> Unit) {
    Row(Modifier.padding(horizontal = 16.dp)) {
        BookCover(b, width = 100.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(b.title, style = MaterialTheme.typography.headlineSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(b.authors, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Pill(statusLabels[b.status]!!)
                b.lentTo?.let { Pill("Lent: $it", MaterialTheme.colorScheme.tertiaryContainer) }
                b.borrowedFrom?.let { Pill("Borrowed: $it", MaterialTheme.colorScheme.tertiaryContainer) }
                b.targetDate?.let { Pill("Finish by ${it.toLocalDate().format(dateFmt)}", MaterialTheme.colorScheme.primaryContainer) }
            }
            stars?.let { StarRating(it, null, Modifier.padding(top = 4.dp), size = 18.dp) }
            if (b.pageCount > 0) {
                Spacer(Modifier.height(8.dp))
                LinearBar(b.currentPage.toFloat() / b.pageCount)
                TextButton(onClick = onPage, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    Text("Page ${b.currentPage} of ${b.pageCount}", style = MaterialTheme.typography.labelMedium)
                }
            }
            if (b.status == BookStatus.READING || b.status == BookStatus.WANT_TO_READ) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStart, enabled = !timerBusy) { Icon(Icons.Filled.PlayArrow, null); Text("Read") }
                FilledTonalButton(onClick = onLog) { Icon(Icons.Filled.EditNote, null); Text(" Log") }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun Overview(b: Book, sessions: List<com.pagewise.app.data.db.ReadingSession>, all: List<com.pagewise.app.data.db.ReadingSession>, ratings: Map<RatingAspect, Float>) {
    val total = sessions.sumOf { it.durationSec }
    val pph = Stats.pagesPerHour(sessions)
    val left = Stats.timeRemaining(b, sessions, all)
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TileRow {
            StatTile("Time read", formatDuration(total), Modifier.weight(1f))
            StatTile("Sessions", sessions.size.toString(), Modifier.weight(1f))
        }
        TileRow {
            StatTile("Pages / hour", if (pph > 0) "%.0f".format(pph) else "–", Modifier.weight(1f))
            StatTile("Time left", left?.let { formatDuration(it) } ?: "–", Modifier.weight(1f))
        }
        TileRow {
            StatTile("Avg session", if (sessions.isEmpty()) "–" else formatDuration(total / sessions.size), Modifier.weight(1f))
            StatTile("Days read", Stats.readingDays(sessions).size.toString(), Modifier.weight(1f))
        }
        b.startedAt?.let { Text("Started ${it.toLocalDate().format(dateFmt)}", style = MaterialTheme.typography.bodyMedium) }
        b.finishedAt?.let { Text("${if (b.status == BookStatus.DNF) "Stopped" else "Finished"} ${it.toLocalDate().format(dateFmt)}", style = MaterialTheme.typography.bodyMedium) }
        if (ratings.isNotEmpty()) {
            Text("Ratings", style = MaterialTheme.typography.titleMedium)
            RatingAspect.entries.filter { it in ratings }.forEach { a ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(a.label, Modifier.weight(1f))
                    StarRating(ratings.getValue(a), null, size = 20.dp)
                }
            }
        }
        b.publisher?.let { Text("Publisher: $it", style = MaterialTheme.typography.bodySmall) }
        b.isbn?.let { Text("ISBN: $it", style = MaterialTheme.typography.bodySmall) }
        b.description?.let {
            Text("About", style = MaterialTheme.typography.titleMedium)
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EditBookDialog(b: Book, onDismiss: () -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    var title by rememberSaveable { mutableStateOf(b.title) }
    var authors by rememberSaveable { mutableStateOf(b.authors) }
    var pages by rememberSaveable { mutableStateOf(b.pageCount.toString()) }
    var current by rememberSaveable { mutableStateOf(b.currentPage.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit book") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(authors, { authors = it }, label = { Text("Author(s)") }, singleLine = true)
                OutlinedTextField(pages, { pages = it.filter(Char::isDigit) }, label = { Text("Pages") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(current, { current = it.filter(Char::isDigit) }, label = { Text("Current page") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = {
                scope.launch {
                    repo.saveBook(b.copy(title = title.trim(), authors = authors.trim(), pageCount = pages.toIntOrNull() ?: 0, currentPage = current.toIntOrNull() ?: 0))
                    onDismiss()
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ShelvesDialog(b: Book, onDismiss: () -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val shelves by repo.shelves.collectAsStateWithLifecycle(emptyList())
    val links by repo.shelfLinks.collectAsStateWithLifecycle(emptyList())
    var creating by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Shelves") },
        text = {
            LazyColumn(Modifier.heightIn(max = 400.dp)) {
                items(shelves, key = { it.id }) { s ->
                    val on = links.any { it.shelfId == s.id && it.bookId == b.id }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(on, { scope.launch { repo.setOnShelf(s.id, b.id, it) } })
                        Text(s.name)
                    }
                }
                item { TextButton(onClick = { creating = true }) { Text("+ New shelf") } }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
    if (creating) TextInputDialog("New shelf", label = "Name", onDismiss = { creating = false }) { name ->
        scope.launch { repo.saveShelf(com.pagewise.app.data.db.Shelf(name = name, color = com.pagewise.app.ui.library.shelfColors.random(), sortOrder = Int.MAX_VALUE)) }
        creating = false
    }
}

@Composable
private fun RateDialog(b: Book, current: Map<RatingAspect, Float>, onDismiss: () -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val values = remember { mutableStateMapOf<RatingAspect, Float>().apply { putAll(current) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate this book") },
        text = {
            Column {
                RatingAspect.entries.forEach { a ->
                    Text(a.label, style = MaterialTheme.typography.labelLarge)
                    StarRating(values[a] ?: 0f, { values[a] = it })
                }
            }
        },
        confirmButton = { TextButton(onClick = { scope.launch { repo.saveRatings(b.id, values.toMap()); onDismiss() } }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun CoverPickerDialog(b: Book, onDismiss: () -> Unit) {
    val repo = LocalRepo.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var covers by remember { mutableStateOf<List<String>?>(null) }
    LaunchedEffect(b.id) { covers = runCatching { repo.remote.searchCovers(b.title, b.authors) }.getOrDefault(emptyList()) }
    val gallery = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) scope.launch {
            val file = com.pagewise.app.ui.camera.newImageFile(context)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
            }
            repo.saveBook(b.copy(coverUrl = android.net.Uri.fromFile(file).toString()))
            onDismiss()
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a cover") },
        text = {
            Column {
                TextButton(onClick = {
                    gallery.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text("Pick from gallery") }
                when (val c = covers) {
                    null -> Text("Searching covers…")
                    else -> if (c.isEmpty()) Text("No online covers found.") else androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        androidx.compose.foundation.lazy.grid.GridCells.Fixed(3), Modifier.heightIn(max = 380.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(c.size) { i ->
                            BookCover(b.title, b.authors, c[i], Modifier.clickable { scope.launch { repo.saveBook(b.copy(coverUrl = c[i])); onDismiss() } })
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
