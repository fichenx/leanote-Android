package com.leanote.next.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leanote.next.data.model.AppSettings
import com.leanote.next.data.model.SortMode
import com.leanote.next.data.model.ThemeMode
import com.leanote.next.data.prefs.AccountStore
import com.leanote.next.data.prefs.SettingsStore
import com.leanote.next.data.repo.AuthRepository
import com.leanote.next.data.sync.SyncEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val accountStore: AccountStore,
    private val authRepository: AuthRepository,
    private val syncEngine: SyncEngine,
) : ViewModel() {

    val settings = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val account = accountStore.activeAccount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _syncing = MutableStateFlow(false)
    val syncing = _syncing.asStateFlow()

    val messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val loggedOut = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        settingsStore.setThemeMode(mode)
    }

    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch {
        settingsStore.setDynamicColor(enabled)
    }

    fun setAutoSync(enabled: Boolean) = viewModelScope.launch {
        settingsStore.setAutoSync(enabled)
    }

    fun setDefaultMarkdown(enabled: Boolean) = viewModelScope.launch {
        settingsStore.setDefaultMarkdown(enabled)
    }

    fun setSortMode(mode: SortMode) = viewModelScope.launch {
        settingsStore.setSortMode(mode)
    }

    fun syncNow() {
        if (_syncing.value) return
        viewModelScope.launch {
            _syncing.value = true
            val summary = syncEngine.sync()
            _syncing.value = false
            messages.emit(
                if (summary.success) {
                    "同步完成 · 上传 ${summary.pushed} 条，下载 ${summary.downloaded} 条"
                } else {
                    summary.message.ifBlank { "同步失败" }
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            val current = accountStore.current()
            if (current != null) authRepository.logout(current)
            loggedOut.emit(Unit)
        }
    }
}
