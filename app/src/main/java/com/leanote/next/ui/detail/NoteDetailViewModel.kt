package com.leanote.next.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leanote.next.data.local.NoteEntity
import com.leanote.next.data.model.Account
import com.leanote.next.data.prefs.AccountStore
import com.leanote.next.data.repo.ImageRepository
import com.leanote.next.data.repo.NoteRepository
import com.leanote.next.data.repo.NotebookRepository
import com.leanote.next.util.HtmlUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NoteDetailState(
    val loading: Boolean = true,
    val note: NoteEntity? = null,
    val account: Account? = null,
    val notebookName: String? = null,
    val renderedContent: String = "",
)

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val notebookRepository: NotebookRepository,
    private val imageRepository: ImageRepository,
    private val accountStore: AccountStore,
) : ViewModel() {

    private val _state = MutableStateFlow(NoteDetailState())
    val state = _state.asStateFlow()

    val messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val closed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private var started = false

    fun load(localId: Long) {
        if (started) return
        started = true
        viewModelScope.launch {
            val account = accountStore.current()
            val notebooks = account?.let { notebookRepository.getAll(it.id) }.orEmpty()
            noteRepository.observeNote(localId).collect { note ->
                if (note == null) {
                    _state.update { it.copy(loading = false, note = null) }
                    closed.emit(Unit)
                    return@collect
                }
                val rendered = when {
                    account == null -> note.content
                    note.isMarkdown -> imageRepository.prepareForRender(note.content, account)
                    else -> imageRepository.prepareForWeb(note.content, account)
                }
                _state.update {
                    NoteDetailState(
                        loading = false,
                        note = note,
                        account = account,
                        notebookName = notebooks.firstOrNull { nb -> nb.serverId == note.notebookServerId }?.title,
                        renderedContent = rendered,
                    )
                }
            }
        }
    }

    fun shareText(): String {
        val note = _state.value.note ?: return ""
        val plain = if (note.isMarkdown) note.content else HtmlUtils.toPlainText(note.content)
        val title = note.title.ifBlank { "无标题笔记" }
        val link = blogUrl()
        return buildString {
            append(title)
            append("\n\n")
            append(plain)
            if (link != null) append("\n\n$link")
        }
    }

    fun blogUrl(): String? {
        val note = _state.value.note ?: return null
        val account = _state.value.account ?: return null
        if (!note.isBlog) return null
        val slug = note.urlTitle.ifBlank { note.serverId }
        return "${account.host}/blog/post/$slug"
    }

    fun setTrash(trash: Boolean) {
        val note = _state.value.note ?: return
        viewModelScope.launch {
            noteRepository.setTrash(note.localId, trash)
            messages.emit(if (trash) "已移至回收站" else "已恢复笔记")
            if (trash) closed.emit(Unit)
            val account = _state.value.account
            if (account != null) {
                runCatching { noteRepository.push(note.localId, account) }
            }
        }
    }

    fun deleteForever() {
        val note = _state.value.note ?: return
        val account = _state.value.account ?: return
        viewModelScope.launch {
            runCatching { noteRepository.deleteForever(account, note) }
                .onSuccess {
                    messages.emit("已永久删除")
                    closed.emit(Unit)
                }
                .onFailure { messages.emit(it.message ?: "删除失败") }
        }
    }
}
