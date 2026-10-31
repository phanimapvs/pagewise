package com.pagewise.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pagewise.app.data.AppEvent
import com.pagewise.app.data.UserPrefs
import com.pagewise.app.service.ReadingTimer
import com.pagewise.app.ui.add.AddBookScreen
import com.pagewise.app.ui.book.BookDetailScreen
import com.pagewise.app.ui.book.InfographicScreen
import com.pagewise.app.ui.components.LocalRepo
import com.pagewise.app.ui.components.Outfit
import com.pagewise.app.ui.components.Pip
import com.pagewise.app.ui.goals.GoalsScreen
import com.pagewise.app.ui.home.HomeScreen
import com.pagewise.app.ui.library.LibraryScreen
import com.pagewise.app.ui.library.ShelfScreen
import com.pagewise.app.ui.more.AchievementsScreen
import com.pagewise.app.ui.more.AssistantScreen
import com.pagewise.app.ui.more.MoreScreen
import com.pagewise.app.ui.more.NotesScreen
import com.pagewise.app.ui.more.OnboardingScreen
import com.pagewise.app.ui.more.ReadathonDetailScreen
import com.pagewise.app.ui.more.ReadathonsScreen
import com.pagewise.app.ui.more.SettingsScreen
import com.pagewise.app.ui.more.StoreScreen
import com.pagewise.app.ui.stats.SessionsScreen
import com.pagewise.app.ui.stats.StatsScreen
import com.pagewise.app.ui.timer.EndSessionScreen
import com.pagewise.app.ui.timer.TimerScreen

object Routes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val GOALS = "goals"
    const val STATS = "stats"
    const val MORE = "more"
    const val ADD = "add?scan={scan}"
    const val TIMER = "timer"
    const val END = "end"
    const val ACHIEVEMENTS = "achievements"
    const val STORE = "store"
    const val READATHONS = "readathons"
    const val ASSISTANT = "assistant"
    const val SETTINGS = "settings"
    const val SESSIONS = "sessions"
    const val NOTES = "notes"

    fun add(scan: Boolean = false) = "add?scan=$scan"
    fun book(id: Long) = "book/$id"
    fun shelf(id: Long) = "shelf/$id"
    fun readathon(id: Long) = "readathon/$id"
    fun infographic(id: Long) = "infographic/$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Filled.Home),
    Tab(Routes.LIBRARY, "Library", Icons.AutoMirrored.Filled.MenuBook),
    Tab(Routes.GOALS, "Goals", Icons.Filled.Flag),
    Tab(Routes.STATS, "Stats", Icons.Filled.BarChart),
    Tab(Routes.MORE, "More", Icons.Filled.MoreHoriz),
)

@Composable
fun AppRoot(prefs: UserPrefs) {
    if (!prefs.onboarded) {
        OnboardingScreen()
        return
    }
    val nav = rememberNavController()
    val repo = LocalRepo.current
    val outfit = Outfit.of(prefs.activeOutfit)

    // Celebrations queue up so several unlocks in a row are each shown.
    val celebrations = remember { mutableStateListOf<AppEvent>() }
    LaunchedEffect(Unit) { repo.events.collect { celebrations += it } }

    // Stopping the timer from the notification lands the user in the end-of-session flow.
    val timer by ReadingTimer.state.collectAsStateWithLifecycle()
    LaunchedEffect(timer?.stopRequested) {
        if (timer?.stopRequested == true && nav.currentDestination?.route != Routes.END) nav.navigate(Routes.END)
    }

    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    Scaffold(
        bottomBar = {
            if (tabs.any { it.route == current }) NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = current == tab.route,
                        onClick = { nav.switchTab(tab.route) },
                        icon = { Icon(tab.icon, null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { pad ->
        NavHost(nav, Routes.HOME, Modifier.padding(bottom = pad.calculateBottomPadding()).consumeWindowInsets(pad)) {
            composable(Routes.HOME) { HomeScreen(nav, prefs) }
            composable(Routes.LIBRARY) { LibraryScreen(nav) }
            composable(Routes.GOALS) { GoalsScreen(prefs) }
            composable(Routes.STATS) { StatsScreen(nav) }
            composable(Routes.MORE) { MoreScreen(nav, prefs) }
            composable(Routes.ADD, arguments = listOf(navArgument("scan") { type = NavType.BoolType; defaultValue = false })) {
                AddBookScreen(nav, startWithScan = it.arguments?.getBoolean("scan") == true)
            }
            composable("book/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                BookDetailScreen(nav, it.arguments!!.getLong("id"))
            }
            composable("shelf/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                ShelfScreen(nav, it.arguments!!.getLong("id"))
            }
            composable("infographic/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                InfographicScreen(nav, it.arguments!!.getLong("id"))
            }
            composable("readathon/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                ReadathonDetailScreen(nav, it.arguments!!.getLong("id"))
            }
            composable(Routes.TIMER) { TimerScreen(nav, prefs) }
            composable(Routes.END) { EndSessionScreen(nav, prefs) }
            composable(Routes.ACHIEVEMENTS) { AchievementsScreen(nav) }
            composable(Routes.STORE) { StoreScreen(nav, prefs) }
            composable(Routes.READATHONS) { ReadathonsScreen(nav) }
            composable(Routes.ASSISTANT) { AssistantScreen(nav, prefs) }
            composable(Routes.SETTINGS) { SettingsScreen(nav, prefs) }
            composable(Routes.SESSIONS) { SessionsScreen(nav) }
            composable(Routes.NOTES) { NotesScreen(nav) }
        }
    }

    celebrations.firstOrNull()?.let { event ->
        CelebrationDialog(event, outfit) { celebrations.removeAt(0) }
    }
}

fun NavController.switchTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
private fun CelebrationDialog(event: AppEvent, outfit: Outfit, onDismiss: () -> Unit) {
    val (title, body) = when (event) {
        is AppEvent.AchievementUnlocked -> "Achievement unlocked!" to
            "${event.achievement.title}\n${event.achievement.description}\n+${event.achievement.reward} ink"
        is AppEvent.LevelUp -> "Level ${event.level}!" to "You're reading your way up. Keep going!"
        is AppEvent.GoalCompleted -> "Goal complete!" to
            "You hit your ${event.goal.period.name.lowercase()} goal of ${event.goal.target} ${event.goal.metric.unit}."
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Pip(outfit, 88.dp, animate = true) },
        title = { Text(title, textAlign = TextAlign.Center) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(body, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Nice!") } },
    )
}
