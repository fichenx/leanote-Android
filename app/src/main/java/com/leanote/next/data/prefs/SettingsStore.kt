package com.leanote.next.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.leanote.next.data.model.AppSettings
import com.leanote.next.data.model.SortMode
import com.leanote.next.data.model.SurfaceEffect
import com.leanote.next.data.model.ThemeMode
import com.leanote.next.data.model.ThemePalette
import com.leanote.next.data.model.UiDensity
import com.leanote.next.data.model.UiStyle
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
    private val surfaceEffectKey = stringPreferencesKey("surface_effect")
    private val surfaceTranslucencyKey = intPreferencesKey("surface_translucency")
    private val customColorsKey = stringPreferencesKey("custom_colors")
    private val densityKey = stringPreferencesKey("ui_density")
    private val uiScaleKey = intPreferencesKey("ui_scale")
    private val uiStyleKey = stringPreferencesKey("ui_style")
    private val noteCardCornerKey = intPreferencesKey("note_card_corner")
    private val noteCardBorderKey = booleanPreferencesKey("note_card_border")
    private val autoSyncKey = booleanPreferencesKey("auto_sync")
    private val sortKey = stringPreferencesKey("sort_mode")
    private val markdownKey = booleanPreferencesKey("default_markdown")

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        val palette = runCatching { ThemePalette.valueOf(prefs[paletteKey] ?: "") }
            .getOrDefault(ThemePalette.FOREST)
        val legacyCorner = when (palette) {
            ThemePalette.MONOCHROME -> 0
            ThemePalette.CREAM_PAPER, ThemePalette.NEON -> 12
            else -> 22
        }
        AppSettings(
            themeMode = runCatching { ThemeMode.valueOf(prefs[themeKey] ?: "") }.getOrDefault(ThemeMode.SYSTEM),
            palette = palette,
            dynamicColor = prefs[dynamicKey] ?: false,
            surfaceEffect = runCatching { SurfaceEffect.valueOf(prefs[surfaceEffectKey] ?: "") }.getOrDefault(SurfaceEffect.NONE),
            surfaceTranslucency = (prefs[surfaceTranslucencyKey] ?: 20).coerceIn(0, 60),
            customColors = decodeCustomColors(prefs[customColorsKey]),
            density = runCatching { UiDensity.valueOf(prefs[densityKey] ?: "") }.getOrDefault(UiDensity.STANDARD),
            uiScale = (prefs[uiScaleKey] ?: 100).coerceIn(85, 130),
            uiStyle = runCatching { UiStyle.valueOf(prefs[uiStyleKey] ?: "") }.getOrDefault(UiStyle.MATERIAL),
            noteCardCorner = (prefs[noteCardCornerKey] ?: legacyCorner).coerceIn(0, 28),
            noteCardBorder = prefs[noteCardBorderKey] ?: true,
            autoSync = prefs[autoSyncKey] ?: true,
            sortMode = runCatching { SortMode.valueOf(prefs[sortKey] ?: "") }.getOrDefault(SortMode.UPDATED_DESC),
            defaultMarkdown = prefs[markdownKey] ?: true,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = dataStore.edit { it[themeKey] = mode.name }

    suspend fun setPalette(palette: ThemePalette) = dataStore.edit { it[paletteKey] = palette.name }

    suspend fun setDynamicColor(enabled: Boolean) = dataStore.edit { it[dynamicKey] = enabled }

    suspend fun setSurfaceEffect(effect: SurfaceEffect) = dataStore.edit { it[surfaceEffectKey] = effect.name }

    suspend fun setSurfaceTranslucency(value: Int) =
        dataStore.edit { it[surfaceTranslucencyKey] = value.coerceIn(0, 60) }

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

    suspend fun setDensity(density: UiDensity) = dataStore.edit { it[densityKey] = density.name }

    suspend fun setUiScale(value: Int) = dataStore.edit {
        it[uiScaleKey] = (85 + ((value - 85) / 5) * 5).coerceIn(85, 130)
    }

    suspend fun setUiStyle(style: UiStyle) = dataStore.edit { it[uiStyleKey] = style.name }

    suspend fun setNoteCardCorner(value: Int) = dataStore.edit {
        it[noteCardCornerKey] = (value / 2 * 2).coerceIn(0, 28)
    }

    suspend fun setNoteCardBorder(enabled: Boolean) = dataStore.edit { it[noteCardBorderKey] = enabled }

    suspend fun setAutoSync(enabled: Boolean) = dataStore.edit { it[autoSyncKey] = enabled }

    suspend fun setSortMode(mode: SortMode) = dataStore.edit { it[sortKey] = mode.name }

    suspend fun setDefaultMarkdown(enabled: Boolean) = dataStore.edit { it[markdownKey] = enabled }

    private fun decodeCustomColors(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching { json.decodeFromString<Map<String, String>>(raw) }.getOrDefault(emptyMap())
    }
}
