package com.pagewise.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

private val Context.dataStore by preferencesDataStore("pagewise_prefs")

enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark"), NIGHT("Night reading") }

@Serializable
data class UserPrefs(
    val name: String = "",
    val onboarded: Boolean = false,
    val reminderEnabled: Boolean = false,
    /** Minutes after midnight. */
    val reminderMinute: Int = 20 * 60,
    val ink: Int = 0,
    val unlockedOutfits: Set<String> = setOf("classic"),
    val activeOutfit: String = "classic",
    val theme: ThemeMode = ThemeMode.SYSTEM,
    /** Color theme id, see ui.theme.Palette. */
    val palette: String = "ember",
    /** Font id, see ui.theme.AppFont. */
    val font: String = "classic",
    /** Companion animal id, see ui.components.Critter. */
    val critter: String = "fox",
    val defaultSound: String? = null,
    val lastBackupAt: Long = 0,
    /** One-off events that achievements check, e.g. "ambient", "infographic". */
    val flags: Set<String> = emptySet(),
)

class Prefs(private val context: Context) {
    private object K {
        val name = stringPreferencesKey("name")
        val onboarded = booleanPreferencesKey("onboarded")
        val reminderEnabled = booleanPreferencesKey("reminder_enabled")
        val reminderMinute = intPreferencesKey("reminder_minute")
        val ink = intPreferencesKey("ink")
        val unlocked = stringSetPreferencesKey("unlocked_outfits")
        val outfit = stringPreferencesKey("active_outfit")
        val theme = stringPreferencesKey("theme")
        val palette = stringPreferencesKey("palette")
        val font = stringPreferencesKey("font")
        val critter = stringPreferencesKey("critter")
        val sound = stringPreferencesKey("default_sound")
        val lastBackup = stringPreferencesKey("last_backup")
        val flags = stringSetPreferencesKey("flags")
    }

    val flow: Flow<UserPrefs> = context.dataStore.data.map { it.toUserPrefs() }

    suspend fun current(): UserPrefs = flow.first()

    private fun Preferences.toUserPrefs() = UserPrefs(
        name = this[K.name] ?: "",
        onboarded = this[K.onboarded] ?: false,
        reminderEnabled = this[K.reminderEnabled] ?: false,
        reminderMinute = this[K.reminderMinute] ?: (20 * 60),
        ink = this[K.ink] ?: 0,
        unlockedOutfits = this[K.unlocked] ?: setOf("classic"),
        activeOutfit = this[K.outfit] ?: "classic",
        theme = this[K.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
        palette = this[K.palette] ?: "ember",
        font = this[K.font] ?: "classic",
        critter = this[K.critter] ?: "fox",
        defaultSound = this[K.sound],
        lastBackupAt = this[K.lastBackup]?.toLongOrNull() ?: 0,
        flags = this[K.flags] ?: emptySet(),
    )

    suspend fun setName(v: String) = context.dataStore.edit { it[K.name] = v }
    suspend fun setOnboarded() = context.dataStore.edit { it[K.onboarded] = true }
    suspend fun setReminder(enabled: Boolean, minute: Int) = context.dataStore.edit {
        it[K.reminderEnabled] = enabled
        it[K.reminderMinute] = minute
    }
    suspend fun setTheme(t: ThemeMode) = context.dataStore.edit { it[K.theme] = t.name }
    suspend fun setPalette(id: String) = context.dataStore.edit { it[K.palette] = id }
    suspend fun setFont(id: String) = context.dataStore.edit { it[K.font] = id }
    suspend fun setCritter(id: String) = context.dataStore.edit { it[K.critter] = id }
    suspend fun setDefaultSound(id: String?) = context.dataStore.edit {
        if (id == null) it.remove(K.sound) else it[K.sound] = id
    }
    suspend fun setLastBackup(t: Long) = context.dataStore.edit { it[K.lastBackup] = t.toString() }

    suspend fun addInk(amount: Int) = context.dataStore.edit { it[K.ink] = (it[K.ink] ?: 0) + amount }

    /** Spends ink if the balance allows it. Returns true on success. */
    suspend fun spendInk(amount: Int): Boolean {
        var ok = false
        context.dataStore.edit {
            val balance = it[K.ink] ?: 0
            if (balance >= amount) {
                it[K.ink] = balance - amount
                ok = true
            }
        }
        return ok
    }

    suspend fun unlockOutfit(id: String) = context.dataStore.edit {
        it[K.unlocked] = (it[K.unlocked] ?: setOf("classic")) + id
    }

    suspend fun setOutfit(id: String) = context.dataStore.edit { it[K.outfit] = id }

    suspend fun addFlag(flag: String) = context.dataStore.edit { it[K.flags] = (it[K.flags] ?: emptySet()) + flag }

    suspend fun restore(p: UserPrefs) = context.dataStore.edit {
        it[K.name] = p.name
        it[K.onboarded] = p.onboarded
        it[K.reminderEnabled] = p.reminderEnabled
        it[K.reminderMinute] = p.reminderMinute
        it[K.ink] = p.ink
        it[K.unlocked] = p.unlockedOutfits
        it[K.outfit] = p.activeOutfit
        it[K.theme] = p.theme.name
        it[K.palette] = p.palette
        it[K.font] = p.font
        it[K.critter] = p.critter
        p.defaultSound?.let { s -> it[K.sound] = s }
        it[K.flags] = p.flags
    }
}
