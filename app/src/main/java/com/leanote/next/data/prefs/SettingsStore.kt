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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("leanote_settings")

@Singleton
class SettingsStore @Inject constructor(@ApplicationContext context: Context) {

    private val dataStore = context.settingsDataStore

    private val themeKey = stringPreferencesKey("theme_mode")
    private val dynamicKey = booleanPreferencesKey("dynamic_color")
    private val autoSyncKey = booleanPreferencesKey("auto_sync")
    private val sortKey = stringPreferencesKey("sort_mode")
    private val markdownKey = booleanPreferencesKey("default_markdown")

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            themeMode = runCatching { ThemeMode.valueOf(prefs[themeKey] ?: "") }.getOrDefault(ThemeMode.SYSTEM),
            dynamicColor = prefs[dynamicKey] ?: false,
            autoSync = prefs[autoSyncKey] ?: true,
            sortMode = runCatching { SortMode.valueOf(prefs[sortKey] ?: "") }.getOrDefault(SortMode.UPDATED_DESC),
            defaultMarkdown = prefs[markdownKey] ?: true,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = dataStore.edit { it[themeKey] = mode.name }

    suspend fun setDynamicColor(enabled: Boolean) = dataStore.edit { it[dynamicKey] = enabled }

    suspend fun setAutoSync(enabled: Boolean) = dataStore.edit { it[autoSyncKey] = enabled }

    suspend fun setSortMode(mode: SortMode) = dataStore.edit { it[sortKey] = mode.name }

    suspend fun setDefaultMarkdown(enabled: Boolean) = dataStore.edit { it[markdownKey] = enabled }
}
