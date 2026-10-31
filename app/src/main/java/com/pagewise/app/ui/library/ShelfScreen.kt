package com.pagewise.app.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.pagewise.app.ui.Routes
import com.pagewise.app.ui.components.BackTopBar
import com.pagewise.app.ui.components.EmptyState
import com.pagewise.app.ui.components.LocalRepo
import com.pagewise.app.ui.components.TextInputDialog
import kotlinx.coroutines.launch

@Composable
fun ShelfScreen(nav: NavController, shelfId: Long) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val shelves by repo.shelves.collectAsStateWithLifecycle(emptyList())
    val links by repo.shelfLinks.collectAsStateWithLifecycle(emptyList())
    val books by repo.books.collectAsStateWithLifecycle(emptyList())
    val shelf = shelves.firstOrNull { it.id == shelfId } ?: return
    val onShelf = links.filter { it.shelfId == shelfId }.mapTo(HashSet()) { it.bookId }
    var picking by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BackTopBar(shelf.name, { nav.popBackStack() }) {
                IconButton(onClick = { renaming = true }) { Icon(Icons.Filled.Edit, "Rename") }
            }
        },
        floatingActionButton = { FloatingActionButton(onClick = { picking = true }) { Icon(Icons.Filled.Add, "Add books") } },
    ) { pad ->
        val shown = books.filter { it.id in onShelf }
        if (shown.isEmpty()) {
            EmptyState("Empty shelf", "Tap + to put books on this shelf.", Modifier.padding(pad))
        } else {
            LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown, key = { it.id }) { b -> BookRow(b) { nav.navigate(Routes.book(b.id)) } }
            }
        }
    }

    if (picking) AlertDialog(
        onDismissRequest = { picking = false },
        title = { Text("Books on ${shelf.name}") },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(books, key = { it.id }) { b ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(b.id in onShelf, { on -> scope.launch { repo.setOnShelf(shelfId, b.id, on) } })
                        Column {
                            Text(b.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { picking = false }) { Text("Done") } },
    )

    if (renaming) TextInputDialog("Rename shelf", initial = shelf.name, label = "Name", onDismiss = { renaming = false }) { name ->
        scope.launch { repo.saveShelf(shelf.copy(name = name)) }
        renaming = false
    }
}
