package com.leanote.next.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.leanote.next.data.model.AppSettings
import com.leanote.next.data.model.SortMode
import com.leanote.next.data.model.ThemeMode
import com.leanote.next.data.model.ThemePalette
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("leanote_settings")

@Singleton
class SettingsStore @Inject constructor(@ApplicationContext context: Context) {

    private val dataStore = context.settingsDataStore
    private val json = Json { ignoreUnknownKeys = true }

    private val themeKey = stringPreferencesKey("theme_mode")
    private val paletteKey = stringPreferencesKey("theme_palette")
    private val dynamicKey = booleanPreferencesKey("dynamic_color")
    private val customColorsKey = stringPreferencesKey("custom_colors")
    private val autoSyncKey = booleanPreferencesKey("auto_sync")
    private val sortKey = stringPreferencesKey("sort_mode")
    private val markdownKey = booleanPreferencesKey("default_markdown")

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            themeMode = runCatching { ThemeMode.valueOf(prefs[themeKey] ?: "") }.getOrDefault(ThemeMode.SYSTEM),
            palette = runCatching { ThemePalette.valueOf(prefs[paletteKey] ?: "") }.getOrDefault(ThemePalette.FOREST),
            dynamicColor = prefs[dynamicKey] ?: false,
            customColors = decodeCustomColors(prefs[customColorsKey]),
            autoSync = prefs[autoSyncKey] ?: true,
            sortMode = runCatching { SortMode.valueOf(prefs[sortKey] ?: "") }.getOrDefault(SortMode.UPDATED_DESC),
            defaultMarkdown = prefs[markdownKey] ?: true,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = dataStore.edit { it[themeKey] = mode.name }

    suspend fun setPalette(palette: ThemePalette) = dataStore.edit { it[paletteKey] = palette.name }

    suspend fun setDynamicColor(enabled: Boolean) = dataStore.edit { it[dynamicKey] = enabled }

    suspend fun setCustomColor(role: String, hex: String?) {
        dataStore.edit { prefs ->
            val colors = decodeCustomColors(prefs[customColorsKey]).toMutableMap()
            val value = hex?.trim().orEmpty()
            if (value.isEmpty()) colors.remove(role) else colors[role] = value
            if (colors.isEmpty()) {
                prefs.remove(customColorsKey)
            } else {
                prefs[customColorsKey] = json.encodeToString(colors)
            }
        }
    }

    suspend fun clearCustomColors() = dataStore.edit { it.remove(customColorsKey) }

    suspend fun setAutoSync(enabled: Boolean) = dataStore.edit { it[autoSyncKey] = enabled }

    suspend fun setSortMode(mode: SortMode) = dataStore.edit { it[sortKey] = mode.name }

    suspend fun setDefaultMarkdown(enabled: Boolean) = dataStore.edit { it[markdownKey] = enabled }

    private fun decodeCustomColors(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching { json.decodeFromString<Map<String, String>>(raw) }.getOrDefault(emptyMap())
    }
}
