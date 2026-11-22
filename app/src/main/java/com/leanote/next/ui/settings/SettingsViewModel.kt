package com.leanote.next.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leanote.next.data.model.AppSettings
import com.leanote.next.data.model.SortMode
import com.leanote.next.data.model.SurfaceEffect
import com.leanote.next.data.model.ThemeMode
import com.leanote.next.data.model.ThemePalette
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

    private val _uploadingAvatar = MutableStateFlow(false)
    val uploadingAvatar = _uploadingAvatar.asStateFlow()

    val messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val loggedOut = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        viewModelScope.launch {
            accountStore.current()?.let { runCatching { authRepository.refreshProfile(it) } }
        }
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        settingsStore.setThemeMode(mode)
    }

    fun setPalette(palette: ThemePalette) = viewModelScope.launch {
        settingsStore.setPalette(palette)
    }

    fun uploadAvatar(uri: Uri) {
        if (_uploadingAvatar.value) return
        viewModelScope.launch {
            val current = accountStore.current()
            if (current == null) {
                messages.emit("请先登录账户")
                return@launch
            }
            _uploadingAvatar.value = true
            runCatching { authRepository.uploadAvatar(current, uri) }
                .onSuccess { messages.emit("头像已更新") }
                .onFailure { messages.emit(it.message ?: "头像上传失败") }
            _uploadingAvatar.value = false
        }
    }

    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch {
        settingsStore.setDynamicColor(enabled)
    }

    fun setSurfaceEffect(effect: SurfaceEffect) = viewModelScope.launch {
        settingsStore.setSurfaceEffect(effect)
    }

    fun setSurfaceTranslucency(value: Int) = viewModelScope.launch {
        settingsStore.setSurfaceTranslucency(value)
    }

    fun setCustomColor(role: String, hex: String?) = viewModelScope.launch {
        settingsStore.setCustomColor(role, hex)
    }

    fun clearCustomColors() = viewModelScope.launch {
        settingsStore.clearCustomColors()
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
            val current = accountStore.current()
            // 服务器不可达时不能把异常抛到主线程
            val summary = runCatching { syncEngine.sync() }.getOrElse { error ->
                _syncing.value = false
                messages.emit("同步失败：${error.message ?: "无法连接服务器"}")
                return@launch
            }
            // 同步笔记时一并同步头像（含在别处修改过的头像）
            if (current != null) {
                runCatching { authRepository.refreshProfile(current) }
            }
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

    fun setDensity(density: com.leanote.next.data.model.UiDensity) = viewModelScope.launch {
        settingsStore.setDensity(density)
    }

    fun setUiScale(value: Int) = viewModelScope.launch {
        settingsStore.setUiScale(value)
    }

    fun setNoteCardCorner(value: Int) = viewModelScope.launch {
        settingsStore.setNoteCardCorner(value)
    }

    fun setNoteCardBorder(enabled: Boolean) = viewModelScope.launch {
        settingsStore.setNoteCardBorder(enabled)
    }

    fun logout() {
        viewModelScope.launch {
            val current = accountStore.current()
            if (current != null) authRepository.logout(current)
            loggedOut.emit(Unit)
        }
    }
}
