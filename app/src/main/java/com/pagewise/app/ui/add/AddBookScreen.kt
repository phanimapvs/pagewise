package com.pagewise.app.ui.add

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pagewise.app.data.db.BookStatus
import com.pagewise.app.data.remote.BookCandidate
import com.pagewise.app.ui.camera.CameraPermissionGate
import com.pagewise.app.ui.camera.IsbnScanner
import com.pagewise.app.ui.components.BackTopBar
import com.pagewise.app.ui.components.BookCover
import com.pagewise.app.ui.components.LocalRepo
import kotlinx.coroutines.launch

@Composable
fun AddBookScreen(nav: NavController, startWithScan: Boolean) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableStateOf(if (startWithScan) 1 else 0) }
    var pick by remember { mutableStateOf<BookCandidate?>(null) }
    val added = remember { mutableStateListOf<String>() }

    Scaffold(
        topBar = { BackTopBar(if (added.isEmpty()) "Add books" else "Add books (${added.size} added)", { nav.popBackStack() }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = tab) {
                listOf("Search", "Scan ISBN", "Manual").forEachIndexed { i, t -> Tab(tab == i, { tab = i }, text = { Text(t) }) }
            }
            when (tab) {
                0 -> SearchTab(added) { pick = it }
                1 -> ScanTab { pick = it }
                2 -> ManualTab { pick = it }
            }
        }
    }

    pick?.let { c ->
        AddCandidateDialog(c, onDismiss = { pick = null }) { status ->
            scope.launch {
                repo.addBook(c, status, readInPast = status == BookStatus.FINISHED)
                added += c.title
                pick = null
                snackbar.showSnackbar("Added \"${c.title}\"")
            }
        }
    }
}

@Composable
private fun SearchTab(added: List<String>, onPick: (BookCandidate) -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<BookCandidate>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val search = {
        if (query.isNotBlank()) scope.launch {
            loading = true
            error = null
            results = runCatching { repo.remote.searchBooks(query) }.getOrElse { error = "Search failed. Check your connection."; emptyList() }
            if (results.isEmpty() && error == null) error = "No books found."
            loading = false
        }
    }
    Column {
        OutlinedTextField(
            query, { query = it }, placeholder = { Text("Title, author or ISBN") }, singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { search() }),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        if (loading) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        error?.let { Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error) }
        LazyColumn(contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(results) { c -> CandidateRow(c, c.title in added) { onPick(c) } }
        }
    }
}

@Composable
private fun ScanTab(onPick: (BookCandidate) -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Point the camera at the barcode on the back cover.") }
    CameraPermissionGate("Pagewise needs the camera to scan book barcodes.") {
        Column(Modifier.fillMaxSize()) {
            IsbnScanner(
                onIsbn = { isbn ->
                    status = "Looking up $isbn…"
                    scope.launch {
                        val c = runCatching { repo.remote.lookupIsbn(isbn) }.getOrNull()
                        if (c != null) {
                            status = "Found \"${c.title}\". Scan the next one when you're ready."
                            onPick(c)
                        } else {
                            status = "No match for $isbn. Try search or manual entry."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
            Text(status, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ManualTab(onPick: (BookCandidate) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var authors by rememberSaveable { mutableStateOf("") }
    var pages by rememberSaveable { mutableStateOf("") }
    var isbn by rememberSaveable { mutableStateOf("") }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(title, { title = it }, label = { Text("Title*") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(authors, { authors = it }, label = { Text("Author(s)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            pages, { pages = it.filter(Char::isDigit) }, label = { Text("Number of pages") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(isbn, { isbn = it }, label = { Text("ISBN") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                onPick(BookCandidate(title.trim(), authors.trim(), isbn.ifBlank { null }, null, pages.toIntOrNull() ?: 0, null, null))
                title = ""; authors = ""; pages = ""; isbn = ""
            },
            enabled = title.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Continue") }
    }
}

@Composable
private fun CandidateRow(c: BookCandidate, added: Boolean, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            BookCover(c.title, c.authors, c.coverUrl, width = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(c.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(c.authors, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (c.pageCount > 0) Text("${c.pageCount} pages", style = MaterialTheme.typography.labelSmall)
            }
            if (added) Icon(Icons.Filled.CheckCircle, "Added", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun AddCandidateDialog(c: BookCandidate, onDismiss: () -> Unit, onAdd: (BookStatus) -> Unit) {
    var status by remember { mutableStateOf(BookStatus.READING) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { BookCover(c.title, c.authors, c.coverUrl, width = 64.dp) },
        title = { Text(c.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column {
                Text(c.authors, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                Text("Add as", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(BookStatus.READING to "Reading", BookStatus.WANT_TO_READ to "To read", BookStatus.FINISHED to "Read before").forEach { (s, l) ->
                        FilterChip(status == s, { status = s }, label = { Text(l) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(status) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
