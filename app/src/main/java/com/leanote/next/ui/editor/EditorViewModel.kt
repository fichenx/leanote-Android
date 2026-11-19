package com.leanote.next.ui.editor

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leanote.next.data.local.NoteEntity
import com.leanote.next.data.local.NotebookEntity
import com.leanote.next.data.model.Account
import com.leanote.next.data.prefs.AccountStore
import com.leanote.next.data.repo.ImageRepository
import com.leanote.next.data.repo.NoteRepository
import com.leanote.next.data.repo.NotebookRepository
import com.leanote.next.data.repo.PushResult
import com.leanote.next.ui.components.notePreview
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class EditorSyncStatus { IDLE, SYNCING, SYNCED, OFFLINE }

data class EditorState(
    val loading: Boolean = true,
    val account: Account? = null,
    val note: NoteEntity? = null,
    val notebooks: List<NotebookEntity> = emptyList(),
    val title: String = "",
    val content: String = "",
    val tags: String = "",
    val isMarkdown: Boolean = true,
    val isBlog: Boolean = false,
    val notebookServerId: String = "",
    val editorHtml: String = "",
    val saving: Boolean = false,
    val savedAt: Long = 0L,
    val dirty: Boolean = false,
    val syncStatus: EditorSyncStatus = EditorSyncStatus.IDLE,
) {
    val notebookName: String?
        get() = notebooks.firstOrNull { it.serverId == notebookServerId }?.title

    val preview: String
        get() = if (note != null) notePreview(note) else ""
}

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val notebookRepository: NotebookRepository,
    private val imageRepository: ImageRepository,
    private val accountStore: AccountStore,
) : ViewModel() {

    private val _state = MutableStateFlow(EditorState())
    val state = _state.asStateFlow()

    val messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val insertImageRequests = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val closed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private var startded = false
    private var saveJob: Job? = null

    fun load(localId: Long) {
        if (startded) return
        startded = true
        viewModelScope.launch {
            val account = accountStore.current()
            val note = noteRepository.get(localId)
            if (note == null) {
                closed.emit(Unit)
                return@launch
            }
            val notebooks = account?.let { notebookRepository.getAll(it.id) }.orEmpty()
            val editorHtml = if (!note.isMarkdown && account != null) {
                imageRepository.prepareForWeb(note.content, account)
            } else {
                ""
            }
            _state.value = EditorState(
                loading = false,
                account = account,
                note = note,
                notebooks = notebooks,
                title = note.title,
                content = note.content,
                tags = note.tags,
                isMarkdown = note.isMarkdown,
                isBlog = note.isBlog,
                notebookServerId = note.notebookServerId,
                editorHtml = editorHtml,
            )
        }
    }

    fun updateTitle(value: String) {
        _state.update { it.copy(title = value, dirty = true) }
        scheduleSave()
    }

    fun updateContent(value: String) {
        if (value == _state.value.content) return
        _state.update { it.copy(content = value, dirty = true) }
        scheduleSave()
    }

    fun updateTags(value: String) {
        _state.update { it.copy(tags = value, dirty = true) }
        scheduleSave()
    }

    /** 富文本编辑器的 HTML 变更：还原成本地/服务器引用后保存。 */
    fun updateHtml(rawHtml: String) {
        val account = _state.value.account ?: return
        updateContent(imageRepository.restoreFromWeb(rawHtml, account))
    }

    fun setNotebook(serverId: String) {
        _state.update { it.copy(notebookServerId = serverId, dirty = true) }
        scheduleSave()
    }

    fun setBlog(isBlog: Boolean) {
        _state.update { it.copy(isBlog = isBlog, dirty = true) }
        scheduleSave()
    }

    fun addImage(uri: Uri) {
        viewModelScope.launch {
            val note = _state.value.note ?: return@launch
            val image = imageRepository.importImage(uri, note.localId)
            if (image == null) {
                messages.emit("无法读取所选图片")
                return@launch
            }
            insertImageRequests.emit("file:/getImage?id=${image.localId}")
        }
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(700)
            persistLocal()
        }
    }

    fun saveNow() {
        saveJob?.cancel()
        viewModelScope.launch { persistLocal() }
    }

    suspend fun persistLocal() {
        val current = _state.value
        val note = current.note ?: return
        if (!current.dirty) return
        _state.update { it.copy(saving = true) }
        runCatching {
            noteRepository.saveLocal(
                localId = note.localId,
                title = current.title,
                content = current.content,
                tags = current.tags,
                isMarkdown = current.isMarkdown,
                isBlog = current.isBlog,
                notebookServerId = current.notebookServerId,
            )
        }.onSuccess {
            _state.update {
                it.copy(saving = false, dirty = false, savedAt = System.currentTimeMillis())
            }
        }.onFailure {
            _state.update { it.copy(saving = false) }
            messages.emit("保存到本地失败：${it.message}")
        }
    }

    fun syncNow() {
        saveJob?.cancel()
        viewModelScope.launch {
            persistLocal()
            val snapshot = _state.value
            val note = snapshot.note ?: return@launch
            val account = snapshot.account ?: return@launch
            _state.update { it.copy(syncStatus = EditorSyncStatus.SYNCING) }
            val result = runCatching { noteRepository.push(note.localId, account) }
            result.onSuccess { pushResult ->
                when (pushResult) {
                    PushResult.SUCCESS -> {
                        val fresh = noteRepository.get(note.localId)
                        _state.update { current ->
                            if (fresh != null && current.content == snapshot.content) {
                                current.copy(
                                    note = fresh,
                                    content = fresh.content,
                                    title = fresh.title,
                                    tags = fresh.tags,
                                    syncStatus = EditorSyncStatus.SYNCED,
                                    dirty = false,
                                )
                            } else {
                                current.copy(syncStatus = EditorSyncStatus.SYNCED)
                            }
                        }
                    }
                    PushResult.CONFLICT -> {
                        val fresh = noteRepository.get(note.localId)
                        _state.update {
                            it.copy(
                                syncStatus = EditorSyncStatus.IDLE,
                                note = fresh ?: it.note,
                            )
                        }
                        messages.emit("服务器上有更新版本，本地修改已另存为冲突副本")
                    }
                    PushResult.SKIPPED -> {
                        _state.update { it.copy(syncStatus = EditorSyncStatus.IDLE) }
                        messages.emit("请先选择一个笔记本再同步")
                    }
                }
            }.onFailure {
                _state.update { it.copy(syncStatus = EditorSyncStatus.IDLE) }
                messages.emit(it.message ?: "同步失败，已保存到本地")
            }
        }
    }

    fun deleteNote() {
        viewModelScope.launch {
            val note = _state.value.note ?: return@launch
            saveJob?.cancel()
            noteRepository.setTrash(note.localId, true)
            runCatching { accountStore.current()?.let { noteRepository.push(note.localId, it) } }
            messages.emit("已移至回收站")
            closed.emit(Unit)
        }
    }

    suspend fun renderPreview(): String {
        val current = _state.value
        val account = current.account ?: return current.content
        return imageRepository.prepareForRender(current.content, account)
    }

    fun normalizeHtml(html: String): String {
        val account = _state.value.account ?: return html
        return imageRepository.restoreFromWeb(html, account)
    }
}
