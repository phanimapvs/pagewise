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

enum class BackupFrequency(val label: String, val days: Long) { DAILY("Daily", 1), WEEKLY("Weekly", 7) }

/**
 * Automatic backup settings and status. Kept out of [UserPrefs] so they are never written
 * into a backup: they are tied to this device (folder permissions, last run).
 */
data class BackupSettings(
    val enabled: Boolean = false,
    val frequency: BackupFrequency = BackupFrequency.DAILY,
    val chargingOnly: Boolean = false,
    /** Optional SAF tree that also receives a copy of every automatic backup. */
    val folderUri: String? = null,
    val lastRunAt: Long = 0,
    val lastError: String? = null,
    /** Fingerprint of the data in the last automatic backup, to skip runs when nothing changed. */
    val lastHash: String? = null,
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
        val autoBackup = booleanPreferencesKey("auto_backup")
        val backupFrequency = stringPreferencesKey("backup_frequency")
        val backupCharging = booleanPreferencesKey("backup_charging")
        val backupFolder = stringPreferencesKey("backup_folder")
        val backupLastRun = stringPreferencesKey("backup_last_run")
        val backupError = stringPreferencesKey("backup_error")
        val backupHash = stringPreferencesKey("backup_hash")
    }

    val flow: Flow<UserPrefs> = context.dataStore.data.map { it.toUserPrefs() }

    suspend fun current(): UserPrefs = flow.first()

    val backupFlow: Flow<BackupSettings> = context.dataStore.data.map {
        BackupSettings(
            enabled = it[K.autoBackup] ?: false,
            frequency = it[K.backupFrequency]?.let { f -> runCatching { BackupFrequency.valueOf(f) }.getOrNull() } ?: BackupFrequency.DAILY,
            chargingOnly = it[K.backupCharging] ?: false,
            folderUri = it[K.backupFolder],
            lastRunAt = it[K.backupLastRun]?.toLongOrNull() ?: 0,
            lastError = it[K.backupError],
            lastHash = it[K.backupHash],
        )
    }

    suspend fun backupSettings(): BackupSettings = backupFlow.first()

    suspend fun setBackupSchedule(enabled: Boolean, frequency: BackupFrequency, chargingOnly: Boolean) = context.dataStore.edit {
        it[K.autoBackup] = enabled
        it[K.backupFrequency] = frequency.name
        it[K.backupCharging] = chargingOnly
    }
    suspend fun setBackupFolder(uri: String?) = context.dataStore.edit {
        if (uri == null) it.remove(K.backupFolder) else it[K.backupFolder] = uri
    }
    /** Records an automatic backup run; [hash] is null when the run failed before writing. */
    suspend fun setBackupResult(at: Long, error: String?, hash: String?) = context.dataStore.edit {
        it[K.backupLastRun] = at.toString()
        if (error == null) it.remove(K.backupError) else it[K.backupError] = error
        if (hash != null) it[K.backupHash] = hash
    }

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
