package com.pagewise.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pagewise.app.data.ThemeMode
import com.pagewise.app.data.UserPrefs
import com.pagewise.app.ui.AppRoot
import com.pagewise.app.ui.components.LocalRepo
import com.pagewise.app.ui.theme.PagewiseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = (application as PagewiseApp).repository
        setContent {
            val prefs by repo.prefs.flow.collectAsStateWithLifecycle(initialValue = null as UserPrefs?)
            val p = prefs ?: return@setContent
            val dark = when (p.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK, ThemeMode.NIGHT -> true
            }
            // Status and navigation bar icons follow the app's theme, not just the system's.
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            CompositionLocalProvider(LocalRepo provides repo) {
                PagewiseTheme(p.theme) {
                    Surface(color = MaterialTheme.colorScheme.background) { AppRoot(p) }
                }
            }
        }
    }
}
