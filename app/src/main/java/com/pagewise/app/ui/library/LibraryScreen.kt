package com.pagewise.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.pagewise.app.data.db.Book
import com.pagewise.app.data.db.BookStatus
import com.pagewise.app.data.db.Shelf
import com.pagewise.app.ui.Routes
import com.pagewise.app.ui.components.BookCover
import com.pagewise.app.ui.components.ConfirmDialog
import com.pagewise.app.ui.components.EmptyState
import com.pagewise.app.ui.components.LinearBar
import com.pagewise.app.ui.components.LocalRepo
import com.pagewise.app.ui.components.TextInputDialog
import kotlinx.coroutines.launch

enum class BookSort(val label: String) { ADDED("Recently added"), TITLE("Title"), AUTHOR("Author"), PROGRESS("Progress"), FINISHED("Date finished") }

val statusLabels = mapOf(
    BookStatus.READING to "Reading",
    BookStatus.WANT_TO_READ to "To read",
    BookStatus.FINISHED to "Finished",
    BookStatus.DNF to "Did not finish",
)

val shelfColors = listOf(0xFF5C6BC0, 0xFFE0703A, 0xFF6BA38E, 0xFF8E3B5F, 0xFFE5B53B, 0xFF455A64)

fun List<Book>.sortedFor(sort: BookSort) = when (sort) {
    BookSort.ADDED -> sortedByDescending { it.addedAt }
    BookSort.TITLE -> sortedBy { it.title.lowercase() }
    BookSort.AUTHOR -> sortedBy { it.authors.lowercase() }
    BookSort.PROGRESS -> sortedByDescending { if (it.pageCount > 0) it.currentPage.toFloat() / it.pageCount else 0f }
    BookSort.FINISHED -> sortedByDescending { it.finishedAt ?: 0 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(nav: NavController) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var sortMenu by remember { mutableStateOf(false) }
    var sort by rememberSaveable { mutableStateOf(BookSort.ADDED) }
    var grid by rememberSaveable { mutableStateOf(false) }
    var newShelf by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library") },
                actions = {
                    if (tab == 0) {
                        IconButton(onClick = { grid = !grid }) {
                            Icon(if (grid) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView, "Toggle layout")
                        }
                        Box {
                            IconButton(onClick = { sortMenu = true }) { Icon(Icons.AutoMirrored.Filled.Sort, "Sort") }
                            DropdownMenu(sortMenu, { sortMenu = false }) {
                                BookSort.entries.forEach { s ->
                                    DropdownMenuItem(text = { Text(s.label) }, onClick = { sort = s; sortMenu = false })
                                }
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { if (tab == 0) nav.navigate(Routes.add()) else newShelf = true }) { Icon(Icons.Filled.Add, "Add") }
        },
    ) { pad ->
        Column(Modifier.padding(pad)) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(tab == 0, { tab = 0 }, text = { Text("Books") })
                Tab(tab == 1, { tab = 1 }, text = { Text("Shelves") })
            }
            if (tab == 0) BooksTab(nav, sort, grid) else ShelvesTab(nav)
        }
    }

    if (newShelf) {
        val repo = LocalRepo.current
        val scope = rememberCoroutineScope()
        TextInputDialog("New shelf", label = "Name", onDismiss = { newShelf = false }) { name ->
            scope.launch { repo.saveShelf(Shelf(name = name, color = shelfColors.random(), sortOrder = Int.MAX_VALUE)) }
            newShelf = false
        }
    }
}

@Composable
private fun BooksTab(nav: NavController, sort: BookSort, grid: Boolean) {
    val repo = LocalRepo.current
    val books by repo.books.collectAsStateWithLifecycle(emptyList())
    var filter by rememberSaveable { mutableStateOf<BookStatus?>(null) }
    var query by rememberSaveable { mutableStateOf("") }

    val shown = books
        .filter { filter == null || it.status == filter }
        .filter { query.isBlank() || it.title.contains(query, true) || it.authors.contains(query, true) }
        .sortedFor(sort)

    Column {
        OutlinedTextField(
            query, { query = it }, placeholder = { Text("Search your library") }, singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(filter == null, { filter = null }, label = { Text("All ${books.size}") }) }
            items(BookStatus.entries) { s ->
                FilterChip(filter == s, { filter = if (filter == s) null else s }, label = { Text("${statusLabels[s]} ${books.count { it.status == s }}") })
            }
        }
        if (shown.isEmpty()) {
            EmptyState("No books here", if (books.isEmpty()) "Tap + to add your first book." else "Try a different filter.")
        } else if (grid) {
            LazyVerticalGrid(GridCells.Adaptive(100.dp), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(shown, key = { it.id }) { b ->
                    Column(Modifier.clickable { nav.navigate(Routes.book(b.id)) }) {
                        BookCover(b, width = 100.dp)
                        Text(b.title, style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown, key = { it.id }) { b -> BookRow(b) { nav.navigate(Routes.book(b.id)) } }
            }
        }
    }
}

@Composable
fun BookRow(b: Book, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            BookCover(b, width = 52.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(b.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(b.authors, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val extra = listOfNotNull(
                    statusLabels[b.status],
                    b.lentTo?.let { "Lent to $it" },
                    b.borrowedFrom?.let { "Borrowed from $it" },
                ).joinToString(" · ")
                Text(extra, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                if (b.status == BookStatus.READING && b.pageCount > 0) LinearBar(b.currentPage.toFloat() / b.pageCount, Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun ShelvesTab(nav: NavController) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val shelves by repo.shelves.collectAsStateWithLifecycle(emptyList())
    val links by repo.shelfLinks.collectAsStateWithLifecycle(emptyList())
    var deleting by remember { mutableStateOf<Shelf?>(null) }

    if (shelves.isEmpty()) {
        EmptyState("No shelves yet", "Shelves group your books however you like: favourites, book club, summer reads…")
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(shelves, key = { _, s -> s.id }) { i, s ->
            Card(onClick = { nav.navigate(Routes.shelf(s.id)) }) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(14.dp, 40.dp).clip(RoundedCornerShape(4.dp)).background(Color(s.color)))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.name, style = MaterialTheme.typography.titleMedium)
                        Text("${links.count { it.shelfId == s.id }} books", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(enabled = i > 0, onClick = {
                        scope.launch { repo.reorderShelves(shelves.toMutableList().apply { add(i - 1, removeAt(i)) }) }
                    }) { Icon(Icons.Filled.KeyboardArrowUp, "Move up") }
                    IconButton(enabled = i < shelves.lastIndex, onClick = {
                        scope.launch { repo.reorderShelves(shelves.toMutableList().apply { add(i + 1, removeAt(i)) }) }
                    }) { Icon(Icons.Filled.KeyboardArrowDown, "Move down") }
                    IconButton(onClick = { deleting = s }) { Icon(Icons.Filled.Delete, "Delete") }
                }
            }
        }
    }
    deleting?.let { s ->
        ConfirmDialog("Delete shelf?", "\"${s.name}\" will be removed. The books stay in your library.", onDismiss = { deleting = null }) {
            scope.launch { repo.deleteShelf(s) }
        }
    }
}
