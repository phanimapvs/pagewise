package com.pagewise.app.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pagewise.app.data.ThemeMode
import com.pagewise.app.data.UserPrefs
import com.pagewise.app.ui.components.Critter
import com.pagewise.app.ui.components.LocalRepo
import com.pagewise.app.ui.components.Outfit
import com.pagewise.app.ui.components.Pip
import com.pagewise.app.ui.theme.AppFont
import com.pagewise.app.ui.theme.Palette
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearanceSettings(prefs: UserPrefs) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Label("Mode")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { t ->
                FilterChip(prefs.theme == t, { scope.launch { repo.prefs.setTheme(t) } }, label = { Text(t.label) })
            }
        }

        Label(if (prefs.theme == ThemeMode.NIGHT) "Color theme (not used in Night reading)" else "Color theme")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Palette.entries.forEach { p ->
                val selected = prefs.palette == p.id
                Column(
                    Modifier.clip(RoundedCornerShape(12.dp)).clickable { scope.launch { repo.prefs.setPalette(p.id) } }.padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(p.swatch)
                            .border(3.dp, if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { if (selected) Icon(Icons.Filled.Check, null, tint = Color.White) }
                    Text(p.label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        Label("Font")
        AppFont.entries.forEach { f ->
            val selected = prefs.font == f.id
            val family = remember(f) { f.heading() }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
                    .clickable { scope.launch { repo.prefs.setFont(f.id) } }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Aa", fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 26.sp, modifier = Modifier.width(52.dp))
                Column(Modifier.weight(1f)) {
                    Text(f.label, fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    Text(f.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (selected) Icon(Icons.Filled.Check, "Selected", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Label("Reading companion")
        CompanionPicker(prefs)
    }
}

@Composable
fun CompanionPicker(prefs: UserPrefs) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val outfit = Outfit.of(prefs.activeOutfit)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Critter.entries.forEach { c ->
            val selected = prefs.critter == c.id
            Column(
                Modifier.clip(RoundedCornerShape(16.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
                    .clickable { scope.launch { repo.prefs.setCritter(c.id) } }
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Pip(outfit, 72.dp, critter = c)
                Text(c.label, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun Label(text: String) =
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
