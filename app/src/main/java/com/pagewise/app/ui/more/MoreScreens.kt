package com.pagewise.app.ui.more

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.pagewise.app.ui.theme.DisplayFont
import com.pagewise.app.data.UserPrefs
import com.pagewise.app.domain.AchievementInput
import com.pagewise.app.domain.AchievementTier
import com.pagewise.app.domain.Achievements
import com.pagewise.app.ui.Routes
import com.pagewise.app.ui.components.BackTopBar
import com.pagewise.app.ui.components.LinearBar
import com.pagewise.app.ui.components.LocalRepo
import com.pagewise.app.ui.components.Outfit
import com.pagewise.app.ui.components.Pip
import com.pagewise.app.ui.theme.Bronze
import com.pagewise.app.ui.theme.Gold
import com.pagewise.app.ui.theme.Silver
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(nav: NavController, prefs: UserPrefs) {
    Scaffold(topBar = { TopAppBar(title = { Text("More") }) }) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 16.dp)) {
            item {
                Card(onClick = { nav.navigate(Routes.STORE) }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Pip(Outfit.of(prefs.activeOutfit), 64.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(if (prefs.name.isBlank()) "Reader" else prefs.name, style = MaterialTheme.typography.titleLarge)
                            Text("${prefs.ink} ink · tap to dress up Pip", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Spacer(Modifier.size(8.dp))
            }
            val entries = listOf(
                Triple("Pip's reading report", Icons.Filled.Psychology, Routes.ASSISTANT),
                Triple("Achievements", Icons.Filled.EmojiEvents, Routes.ACHIEVEMENTS),
                Triple("Readathons", Icons.Filled.Groups, Routes.READATHONS),
                Triple("Quotes & thoughts", Icons.Filled.FormatQuote, Routes.NOTES),
                Triple("Shop", Icons.Filled.Storefront, Routes.STORE),
                Triple("Settings & backup", Icons.Filled.Settings, Routes.SETTINGS),
            )
            items(entries) { (label, icon, route) -> NavRow(label, icon) { nav.navigate(route) } }
        }
    }
}

@Composable
private fun NavRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        ListItem(
            headlineContent = { Text(label) },
            leadingContent = { Icon(icon, null) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        )
    }
}

@Composable
fun AchievementsScreen(nav: NavController) {
    val repo = LocalRepo.current
    val earned by repo.earned.collectAsStateWithLifecycle(emptyList())
    var input by remember { mutableStateOf<AchievementInput?>(null) }
    LaunchedEffect(earned) { input = repo.achievementInput() }
    val earnedAt = earned.associate { it.key to it.earnedAt }
    val fmt = DateTimeFormatter.ofPattern("d MMM yyyy")

    Scaffold(topBar = { BackTopBar("Achievements ${earned.size}/${Achievements.all.size}", { nav.popBackStack() }) }) { pad ->
        LazyVerticalGrid(GridCells.Adaptive(150.dp), Modifier.padding(pad), contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(Achievements.all.sortedBy { it.key !in earnedAt }, key = { it.key }) { a ->
                val got = earnedAt[a.key]
                val (v, t) = input?.let { a.progress(it) } ?: (0 to 1)
                val tint = when (a.tier) { AchievementTier.BRONZE -> Bronze; AchievementTier.SILVER -> Silver; AchievementTier.GOLD -> Gold }
                Card(
                    Modifier.border(2.dp, if (got != null) tint else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(if (got != null) Icons.Filled.EmojiEvents else Icons.Filled.Lock, null, tint = if (got != null) tint else MaterialTheme.colorScheme.outline, modifier = Modifier.size(40.dp))
                        Text(a.title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        // Two lines reserved and a bar on every card, so the grid's cards all match.
                        Text(a.description, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (got != null) {
                            LinearBar(1f, Modifier.padding(top = 6.dp))
                            Text("Earned ${java.time.Instant.ofEpochMilli(got).atZone(java.time.ZoneId.systemDefault()).format(fmt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        } else {
                            LinearBar(v.toFloat() / t, Modifier.padding(top = 6.dp))
                            Text("$v / $t · +${a.reward} ink", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StoreScreen(nav: NavController, prefs: UserPrefs) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf<Outfit?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val active = Outfit.of(prefs.activeOutfit)

    Scaffold(topBar = { BackTopBar("Shop", { nav.popBackStack() }) }) { pad ->
        LazyVerticalGrid(GridCells.Adaptive(150.dp), Modifier.padding(pad), contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Pip(active, 140.dp)
                    Text("${prefs.ink} ink", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Earn 1 ink for every ${com.pagewise.app.domain.Ink.PER_MINUTES} minutes with the timer (up to ${com.pagewise.app.domain.Ink.SESSION_CAP} a session), " +
                            "${com.pagewise.app.domain.Ink.BOOK_FINISHED_BONUS} for each finished book, and more from achievements.",
                        style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                    )
                    Text("Your companion", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                    CompanionPicker(prefs)
                    Text("Outfits", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                }
            }
            items(Outfit.entries) { o ->
                val owned = o.id in prefs.unlockedOutfits
                Card(
                    onClick = {
                        if (owned) scope.launch { repo.prefs.setOutfit(o.id) } else confirm = o
                    },
                    modifier = Modifier.border(2.dp, if (o == active) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Pip(o, 90.dp, animate = false)
                        Text(o.label, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
                        Text(
                            when {
                                o == active -> "Wearing"
                                owned -> "Owned · tap to wear"
                                else -> "${o.price} ink"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (!owned && prefs.ink < o.price) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
    confirm?.let { o ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(o.label) },
            icon = { Pip(o, 96.dp, animate = false) },
            text = { Text(if (prefs.ink >= o.price) "Buy for ${o.price} ink?" else "You need ${o.price - prefs.ink} more ink. Keep reading!") },
            confirmButton = {
                TextButton(enabled = prefs.ink >= o.price, onClick = {
                    scope.launch { if (!repo.buyOutfit(o.id, o.price)) message = "Purchase failed." }
                    confirm = null
                }) { Text("Buy") }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } },
        )
    }
    message?.let { m -> AlertDialog(onDismissRequest = { message = null }, text = { Text(m) }, confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } }) }
}

@Composable
fun NotesScreen(nav: NavController) {
    val repo = LocalRepo.current
    val quotes by repo.allQuotes.collectAsStateWithLifecycle(emptyList())
    val thoughts by repo.allThoughts.collectAsStateWithLifecycle(emptyList())
    val books by repo.books.collectAsStateWithLifecycle(emptyList())
    val titles = books.associate { it.id to it.title }
    var tab by rememberSaveable { mutableStateOf(0) }

    Scaffold(topBar = { BackTopBar("Quotes & thoughts", { nav.popBackStack() }) }) { pad ->
        Column(Modifier.padding(pad)) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(tab == 0, { tab = 0 }, text = { Text("Quotes ${quotes.size}") })
                Tab(tab == 1, { tab = 1 }, text = { Text("Thoughts ${thoughts.size}") })
            }
            val rows = if (tab == 0) quotes.map { Triple(it.bookId, "“${it.text}”", it.page) } else thoughts.map { Triple(it.bookId, it.text, it.page) }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (rows.isEmpty()) item { Text("Nothing saved yet. Add them from a book's page.") }
                items(rows) { (bookId, text, page) ->
                    Card(onClick = { nav.navigate(Routes.book(bookId)) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                        Column(Modifier.padding(14.dp)) {
                            Text(text, fontFamily = if (tab == 0) DisplayFont else null, fontStyle = if (tab == 0) FontStyle.Italic else null)
                            Text(listOfNotNull(titles[bookId], page?.let { "p. $it" }).joinToString(" · "), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
