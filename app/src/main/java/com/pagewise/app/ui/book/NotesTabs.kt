package com.pagewise.app.ui.book

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pagewise.app.data.db.Book
import com.pagewise.app.data.db.Quote
import com.pagewise.app.data.db.Thought
import com.pagewise.app.data.db.WordEntry
import com.pagewise.app.data.remote.Definition
import com.pagewise.app.ui.camera.CameraPermissionGate
import com.pagewise.app.ui.camera.TextCapture
import com.pagewise.app.ui.components.ConfirmDialog
import com.pagewise.app.ui.components.LocalRepo
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun QuotesTab(book: Book) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val quotes by repo.quotes(book.id).collectAsStateWithLifecycle(emptyList())
    var editing by remember { mutableStateOf<Quote?>(null) }
    var camera by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Quote?>(null) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { editing = Quote(bookId = book.id, text = "", page = book.currentPage) }, label = { Text("Type quote") }, leadingIcon = { Icon(Icons.Filled.Add, null) })
                AssistChip(onClick = { camera = true }, label = { Text("From photo") }, leadingIcon = { Icon(Icons.Filled.CameraAlt, null) })
            }
        }
        if (quotes.isEmpty()) item { Text("Save passages you love. Snap a photo of the page and Pagewise reads the text for you.") }
        items(quotes, key = { it.id }) { q ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(14.dp)) {
                    q.imagePath?.let { path ->
                        AsyncImage(File(path), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(8.dp)))
                    }
                    if (q.text.isNotBlank()) Text("“${q.text}”", fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(q.page?.let { "p. $it" } ?: "", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = { shareQuote(context, book, q) }) { Icon(Icons.Filled.Share, "Share") }
                        IconButton(onClick = { editing = q }) { Icon(Icons.Filled.Edit, "Edit") }
                        IconButton(onClick = { deleting = q }) { Icon(Icons.Filled.Delete, "Delete") }
                    }
                }
            }
        }
    }

    editing?.let { q -> QuoteDialog(q, onDismiss = { editing = null }) { scope.launch { repo.saveQuote(it) }; editing = null } }
    deleting?.let { q -> ConfirmDialog("Delete quote?", "This can't be undone.", onDismiss = { deleting = null }) { scope.launch { repo.deleteQuote(q) } } }
    if (camera) Dialog(onDismissRequest = { camera = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize()) {
            CameraPermissionGate("Allow the camera to capture quotes from pages.") {
                TextCapture(onCaptured = { file, text ->
                    camera = false
                    editing = Quote(bookId = book.id, text = text.replace('\n', ' ').trim(), page = book.currentPage, imagePath = file.absolutePath)
                }, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

private fun shareQuote(context: android.content.Context, book: Book, q: Quote) {
    val text = buildString {
        if (q.text.isNotBlank()) append("“${q.text}”\n\n")
        append("— ${book.title}")
        if (book.authors.isNotBlank()) append(", ${book.authors}")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, text)
        val path = q.imagePath
        if (path != null && File(path).exists()) {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, "${context.packageName}.files", File(path)))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            type = "text/plain"
        }
    }
    context.startActivity(Intent.createChooser(intent, "Share quote"))
}

@Composable
private fun QuoteDialog(q: Quote, onDismiss: () -> Unit, onSave: (Quote) -> Unit) {
    var text by rememberSaveable { mutableStateOf(q.text) }
    var page by rememberSaveable { mutableStateOf(q.page?.toString().orEmpty()) }
    var keepPhoto by rememberSaveable { mutableStateOf(q.imagePath != null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (q.id == 0L) "New quote" else "Edit quote") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(text, { text = it }, label = { Text("Quote") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(page, { page = it.filter(Char::isDigit) }, label = { Text("Page") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                if (q.imagePath != null) Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(keepPhoto, { keepPhoto = it })
                    Text("Keep the photo with the quote")
                }
            }
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank() || keepPhoto, onClick = {
                onSave(q.copy(text = text.trim(), page = page.toIntOrNull(), imagePath = q.imagePath.takeIf { keepPhoto }))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun ThoughtsTab(book: Book) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val thoughts by repo.thoughts(book.id).collectAsStateWithLifecycle(emptyList())
    var editing by remember { mutableStateOf<Thought?>(null) }
    var deleting by remember { mutableStateOf<Thought?>(null) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            AssistChip(onClick = { editing = Thought(bookId = book.id, text = "", page = book.currentPage) }, label = { Text("Add thought") }, leadingIcon = { Icon(Icons.Filled.Add, null) })
        }
        if (thoughts.isEmpty()) item { Text("Jot down predictions, reactions and ideas as you read.") }
        items(thoughts, key = { it.id }) { t ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(14.dp)) {
                    Text(t.text, style = MaterialTheme.typography.bodyLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(t.page?.let { "p. $it" } ?: "", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = { editing = t }) { Icon(Icons.Filled.Edit, "Edit") }
                        IconButton(onClick = { deleting = t }) { Icon(Icons.Filled.Delete, "Delete") }
                    }
                }
            }
        }
    }

    editing?.let { t ->
        var text by rememberSaveable(t.id) { mutableStateOf(t.text) }
        var page by rememberSaveable(t.id) { mutableStateOf(t.page?.toString().orEmpty()) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(if (t.id == 0L) "New thought" else "Edit thought") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(text, { text = it }, label = { Text("Thought") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(page, { page = it.filter(Char::isDigit) }, label = { Text("Page") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            },
            confirmButton = {
                TextButton(enabled = text.isNotBlank(), onClick = {
                    scope.launch { repo.saveThought(t.copy(text = text.trim(), page = page.toIntOrNull())) }
                    editing = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
        )
    }
    deleting?.let { t -> ConfirmDialog("Delete thought?", "This can't be undone.", onDismiss = { deleting = null }) { scope.launch { repo.deleteThought(t) } } }
}

@Composable
fun WordsTab(book: Book) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val words by repo.words(book.id).collectAsStateWithLifecycle(emptyList())
    var adding by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { AssistChip(onClick = { adding = true }, label = { Text("Look up a word") }, leadingIcon = { Icon(Icons.Filled.Add, null) }) }
        if (words.isEmpty()) item { Text("Build a vocabulary list from the new words you meet in this book.") }
        items(words, key = { it.id }) { w ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(w.word, style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Serif)
                        Text(w.definition, style = MaterialTheme.typography.bodyMedium)
                        w.page?.let { Text("p. $it", style = MaterialTheme.typography.labelSmall) }
                    }
                    IconButton(onClick = { scope.launch { repo.deleteWord(w) } }) { Icon(Icons.Filled.Delete, "Delete") }
                }
            }
        }
    }
    if (adding) WordLookupDialog(book, onDismiss = { adding = false })
}

@Composable
private fun WordLookupDialog(book: Book, onDismiss: () -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    var word by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Definition>?>(null) }
    var loading by remember { mutableStateOf(false) }
    var custom by rememberSaveable { mutableStateOf("") }
    val save = { def: String ->
        scope.launch { repo.saveWord(WordEntry(bookId = book.id, word = word.trim(), definition = def, page = book.currentPage)) }
        onDismiss()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New word") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(word, { word = it; results = null }, label = { Text("Word") }, singleLine = true, modifier = Modifier.weight(1f))
                    TextButton(enabled = word.isNotBlank(), onClick = {
                        scope.launch {
                            loading = true
                            results = repo.remote.define(word)
                            loading = false
                        }
                    }) { Text("Define") }
                }
                if (loading) CircularProgressIndicator()
                results?.let { list ->
                    if (list.isEmpty()) Text("No definition found. Write your own below.")
                    LazyColumn(Modifier.height(220.dp)) {
                        items(list) { d ->
                            TextButton(onClick = { save("(${d.partOfSpeech}) ${d.meaning}") }) {
                                Text("(${d.partOfSpeech}) ${d.meaning}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                OutlinedTextField(custom, { custom = it }, label = { Text("Your own definition") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(enabled = word.isNotBlank() && custom.isNotBlank(), onClick = { save(custom.trim()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
