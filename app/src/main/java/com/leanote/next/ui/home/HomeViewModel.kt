package com.leanote.next.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leanote.next.data.local.NoteEntity
import com.leanote.next.data.local.NotebookEntity
import com.leanote.next.data.model.Account
import com.leanote.next.data.model.AppSettings
import com.leanote.next.data.model.NoteFilter
import com.leanote.next.data.model.SortMode
import com.leanote.next.data.prefs.AccountStore
import com.leanote.next.data.prefs.SettingsStore
import com.leanote.next.data.repo.NoteRepository
import com.leanote.next.data.repo.NotebookRepository
import com.leanote.next.data.sync.SyncEngine
import com.leanote.next.util.HtmlUtils
import com.leanote.next.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val loading: Boolean = true,
    val account: Account? = null,
    val accounts: List<Account> = emptyList(),
    val notebooks: List<NotebookEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val tags: List<String> = emptyList(),
    val filter: NoteFilter = NoteFilter(),
    val sortMode: SortMode = SortMode.UPDATED_DESC,
    val defaultMarkdown: Boolean = true,
    val syncing: Boolean = false,
) {
    val currentNotebook: NotebookEntity?
        get() = notebooks.firstOrNull { it.serverId == filter.notebookServerId }

    fun notebookName(serverId: String): String? =
        notebooks.firstOrNull { it.serverId == serverId }?.title
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val accountStore: AccountStore,
    private val notebookRepository: NotebookRepository,
    private val noteRepository: NoteRepository,
    private val settingsStore: SettingsStore,
    private val syncEngine: SyncEngine,
    @ApplicationContext private val appContext: android.content.Context,
) : ViewModel() {

    /** resolved=false 表示账户存储还没读出首个结果，此时 account=null 是「未知」而非「未登录」。 */
    private data class AccountSession(val account: Account? = null, val resolved: Boolean = false)

    private val session = accountStore.activeAccount
        .map { AccountSession(it, resolved = true) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AccountSession())

    private val accountsFlow = accountStore.accounts
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val settingsFlow = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val filter = MutableStateFlow(NoteFilter())
    private val syncing = MutableStateFlow(false)

    val messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val createdNote = MutableSharedFlow<Long>(extraBufferCapacity = 1)

    private val notebooksFlow = session.flatMapLatest { s ->
        val account = s.account
        if (account == null) flowOf(emptyList()) else notebookRepository.observe(account.id)
    }

    private val rawNotesFlow = combine(session, filter) { s, f -> s.account to f }
        .flatMapLatest { (account, f) ->
            if (account == null) flowOf(emptyList())
            else noteRepository.observeNotes(account.id, f.notebookServerId ?: "", f.trash)
        }

    private val notesFlow = combine(rawNotesFlow, filter) { notes, f ->
        notes.filter { note ->
            val tagOk = f.tag.isNullOrBlank() || note.tagList.any { it.equals(f.tag, ignoreCase = true) }
            val queryOk = f.query.isBlank() || run {
                val q = f.query.trim()
                note.title.contains(q, ignoreCase = true) ||
                    note.content.contains(q, ignoreCase = true) ||
                    note.tagList.any { it.contains(q, ignoreCase = true) } ||
                    (if (note.isMarkdown) "" else HtmlUtils.toPlainText(note.content)).contains(q, ignoreCase = true)
            }
            tagOk && queryOk
        }
    }

    private val tagsFlow = session.flatMapLatest { s ->
        val account = s.account
        if (account == null) flowOf(emptyList())
        else noteRepository.observeTagRows(account.id).map { rows ->
            rows.flatMap { it.split(',') }
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()
        }
    }

    private val baseState = combine(
        session,
        accountsFlow,
        notebooksFlow,
        notesFlow,
        tagsFlow,
    ) { s, accounts, notebooks, notes, tags ->
        HomeUiState(
            loading = !s.resolved,
            account = s.account,
            accounts = accounts,
            notebooks = notebooks,
            notes = notes,
            tags = tags,
            filter = filter.value,
        )
    }

    private val withFilter = combine(baseState, filter) { state, f ->
        state.copy(filter = f)
    }

    private val withSettings = combine(withFilter, settingsFlow) { state, settings ->
        state.copy(
            sortMode = settings.sortMode,
            defaultMarkdown = settings.defaultMarkdown,
            notes = sortNotes(state.notes, settings.sortMode),
        )
    }

    val uiState = combine(withSettings, syncing) { state, isSyncing ->
        state.copy(syncing = isSyncing)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun selectNotebook(serverId: String?) = filter.update {
        it.copy(notebookServerId = serverId, trash = false)
    }

    fun selectTag(tag: String?) = filter.update { it.copy(tag = tag) }

    fun setQuery(query: String) = filter.update { it.copy(query = query) }

    fun showTrash() = filter.update { NoteFilter(trash = true) }

    fun showAll() = filter.update { NoteFilter() }

    fun setSortMode(mode: SortMode) {
        viewModelScope.launch { settingsStore.setSortMode(mode) }
    }

    fun sync(silent: Boolean = false) {
        if (syncing.value) return
        viewModelScope.launch {
            syncing.value = true
            val summary = syncEngine.sync()
            syncing.value = false
            WidgetUpdater.refresh(appContext)
            if (!silent || !summary.success) {
                val message = if (summary.success) {
                    when {
                        summary.pushed + summary.downloaded == 0 -> "已是最新状态"
                        else -> buildString {
                            append("同步完成")
                            if (summary.downloaded > 0) append(" · 下载 ${summary.downloaded}")
                            if (summary.pushed > 0) append(" · 上传 ${summary.pushed}")
                            if (summary.conflicts > 0) append(" · ${summary.conflicts} 个冲突副本")
                        }
                    }
                } else {
                    summary.message
                }
                messages.emit(message)
            }
        }
    }

    fun createNote(markdown: Boolean) {
        viewModelScope.launch {
            val account = session.value.account
            if (account == null) {
                messages.emit("请先登录")
                return@launch
            }
            var notebookId = filter.value.notebookServerId
            if (notebookId.isNullOrBlank()) {
                notebookId = notebookRepository.getAll(account.id)
                    .firstOrNull { it.parentServerId.isBlank() }
                    ?.serverId
            }
            if (notebookId.isNullOrBlank()) {
                notebookId = notebookRepository.ensureDefault(account)?.serverId
            }
            if (notebookId.isNullOrBlank()) {
                messages.emit("请先在侧边栏创建一个笔记本")
                return@launch
            }
            val localId = noteRepository.create(
                accountId = account.id,
                notebookServerId = notebookId,
                markdown = markdown,
            )
            createdNote.emit(localId)
        }
    }

    fun createNotebook(title: String) {
        viewModelScope.launch {
            val account = session.value.account ?: return@launch
            runCatching { notebookRepository.create(account, title.trim()) }
                .onSuccess { messages.emit("已创建笔记本「${it.title}」") }
                .onFailure { messages.emit(it.message ?: "创建笔记本失败") }
        }
    }

    fun renameNotebook(notebook: NotebookEntity, newTitle: String) {
        viewModelScope.launch {
            val account = session.value.account ?: return@launch
            runCatching { notebookRepository.rename(account, notebook, newTitle.trim()) }
                .onSuccess { messages.emit("已重命名") }
                .onFailure { messages.emit(it.message ?: "重命名失败") }
        }
    }

    fun deleteNotebook(notebook: NotebookEntity) {
        viewModelScope.launch {
            val account = session.value.account ?: return@launch
            runCatching { notebookRepository.delete(account, notebook) }
                .onSuccess {
                    if (filter.value.notebookServerId == notebook.serverId) showAll()
                    messages.emit("已删除笔记本")
                }
                .onFailure { messages.emit(it.message ?: "删除失败") }
        }
    }

    fun moveToTrash(note: NoteEntity) {
        viewModelScope.launch {
            noteRepository.setTrash(note.localId, true)
            messages.emit("已移至回收站")
            runCatching { noteRepository.push(note.localId, session.value.account ?: return@launch) }
        }
    }

    fun restore(note: NoteEntity) {
        viewModelScope.launch {
            noteRepository.setTrash(note.localId, false)
            messages.emit("已恢复笔记")
            runCatching { noteRepository.push(note.localId, session.value.account ?: return@launch) }
        }
    }

    fun deleteForever(note: NoteEntity) {
        viewModelScope.launch {
            val account = session.value.account ?: return@launch
            runCatching { noteRepository.deleteForever(account, note) }
                .onSuccess { messages.emit("已永久删除") }
                .onFailure { messages.emit(it.message ?: "删除失败") }
        }
    }

    fun switchAccount(accountId: String) {
        viewModelScope.launch {
            accountStore.setActive(accountId)
            filter.value = NoteFilter()
        }
    }

    private fun sortNotes(notes: List<NoteEntity>, mode: SortMode): List<NoteEntity> = when (mode) {
        SortMode.UPDATED_DESC -> notes.sortedByDescending { it.updatedTime }
        SortMode.UPDATED_ASC -> notes.sortedBy { it.updatedTime }
        SortMode.CREATED_DESC -> notes.sortedByDescending { it.createdTime }
        SortMode.TITLE_ASC -> notes.sortedWith(compareBy { it.title.lowercase() })
    }
}
