package com.leanote.next.ui.detail

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.leanote.next.data.local.NoteAttachmentEntity
import com.leanote.next.data.local.NoteEntity
import com.leanote.next.data.model.Account
import com.leanote.next.data.repo.AttachmentRepository
import com.leanote.next.ui.components.ConfirmDialog
import com.leanote.next.ui.theme.LocalUiSpacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class AttachmentsState(
    val items: List<NoteAttachmentEntity> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val uploading: Boolean = false,
    val workingId: Long? = null,
    val workingLabel: String? = null,
)

@HiltViewModel
class AttachmentsViewModel @Inject constructor(
    private val attachmentRepository: AttachmentRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AttachmentsState())
    val state = _state.asStateFlow()
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 4)

    private var note: NoteEntity? = null
    private var account: Account? = null
    private var observeJob: Job? = null

    fun start(note: NoteEntity, account: Account?) {
        this.note = note
        this.account = account
        if (observeJob != null) return
        observeJob = viewModelScope.launch {
            attachmentRepository.observe(note.localId).collect { items ->
                _state.update { it.copy(items = items, loading = false) }
            }
        }
    }

    fun refresh() {
        val note = note ?: return
        val account = account ?: return
        if (note.serverId.isBlank() || _state.value.refreshing) return
        viewModelScope.launch {
            _state.update { it.copy(refreshing = true) }
            runCatching { attachmentRepository.refresh(account, note) }
                .onFailure { messages.emit(it.message ?: "获取附件列表失败") }
            _state.update { it.copy(refreshing = false, loading = false) }
        }
    }

    fun upload(context: Context, uri: Uri) {
        val note = note ?: return
        val account = account ?: return
        if (_state.value.uploading || _state.value.workingId != null) return
        viewModelScope.launch {
            _state.update { it.copy(uploading = true) }
            runCatching { attachmentRepository.upload(context, account, note, uri) }
                .onSuccess { messages.emit("附件已上传") }
                .onFailure { messages.emit(it.message ?: "上传附件失败") }
            _state.update { it.copy(uploading = false) }
        }
    }

    fun open(context: Context, attachment: NoteAttachmentEntity) {
        val account = account ?: return
        if (_state.value.uploading || _state.value.workingId != null) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    workingId = attachment.localId,
                    workingLabel = if (attachment.localPath.isNullOrBlank()) "正在下载…" else "正在打开…",
                )
            }
            runCatching { attachmentRepository.open(context, account, attachment) }
                .onFailure { messages.emit(it.message ?: "打开附件失败") }
            _state.update { it.copy(workingId = null, workingLabel = null) }
        }
    }

    fun delete(attachment: NoteAttachmentEntity) {
        val account = account ?: return
        if (_state.value.uploading || _state.value.workingId != null) return
        viewModelScope.launch {
            _state.update { it.copy(workingId = attachment.localId, workingLabel = "正在删除…") }
            runCatching { attachmentRepository.delete(account, attachment) }
                .onSuccess { messages.emit("附件已删除") }
                .onFailure { messages.emit(it.message ?: "删除附件失败") }
            _state.update { it.copy(workingId = null, workingLabel = null) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentsSheet(
    note: NoteEntity,
    account: Account?,
    onDismiss: () -> Unit,
    viewModel: AttachmentsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var menuFor by remember { mutableStateOf<NoteAttachmentEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<NoteAttachmentEntity?>(null) }
    val spacing = LocalUiSpacing.current

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.upload(context, uri)
    }

    LaunchedEffect(note.localId, note.serverId, account?.token) {
        viewModel.start(note, account)
        viewModel.refresh()
    }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = spacing.pageVertical),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.pageHorizontal),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("附件", style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (state.items.isEmpty()) "管理笔记中的非图片文件" else "${state.items.size} 个文件",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = { picker.launch(arrayOf("*/*")) },
                    enabled = !state.uploading && state.workingId == null,
                ) {
                    if (state.uploading) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(spacing.inlineGap))
                    Text(if (state.uploading) "正在上传…" else "添加附件")
                }
            }
            if (state.refreshing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = spacing.itemGap),
                )
            } else {
                Spacer(Modifier.height(spacing.itemGap))
            }

            when {
                state.loading -> Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
                state.items.isEmpty() -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.pageHorizontal * 2, vertical = spacing.contentVertical * 2),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.Outlined.AttachFile,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(spacing.itemGap))
                    Text("暂无附件", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(spacing.inlineGap))
                    Text(
                        "点击“添加附件”选择要上传的文件",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp),
                    contentPadding = PaddingValues(vertical = spacing.itemGap),
                    verticalArrangement = Arrangement.spacedBy(spacing.itemGap),
                ) {
                    items(state.items, key = { it.localId }) { attachment ->
                        val working = state.workingId == attachment.localId
                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !state.uploading && state.workingId == null) {
                                    viewModel.open(context, attachment)
                                },
                            leadingContent = {
                                Icon(
                                    imageVector = attachmentIcon(attachment),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            },
                            headlineContent = {
                                Text(
                                    attachment.fileName,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            supportingContent = {
                                Text(
                                    if (working) {
                                        state.workingLabel ?: "正在处理…"
                                    } else {
                                        "${formatFileSize(attachment.size)} · ${attachment.mimeType}"
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            trailingContent = {
                                if (working) {
                                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                                } else {
                                    Box {
                                        IconButton(
                                            onClick = { menuFor = attachment },
                                            enabled = !state.uploading && state.workingId == null,
                                        ) {
                                            Icon(Icons.Outlined.MoreVert, contentDescription = "附件操作")
                                        }
                                        DropdownMenu(
                                            expanded = menuFor?.localId == attachment.localId,
                                            onDismissRequest = { menuFor = null },
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("删除附件", color = MaterialTheme.colorScheme.error) },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Outlined.DeleteOutline,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.error,
                                                    )
                                                },
                                                onClick = {
                                                    menuFor = null
                                                    pendingDelete = attachment
                                                },
                                            )
                                        }
                                    }
                                }
                            },
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = spacing.pageHorizontal),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }
                }
            }
            SnackbarHost(snackbar)
        }
    }

    pendingDelete?.let { attachment ->
        ConfirmDialog(
            title = "删除附件？",
            text = "将删除“${attachment.fileName}”及其本地缓存，此操作无法撤销。",
            confirmText = "删除",
            destructive = true,
            onConfirm = {
                pendingDelete = null
                viewModel.delete(attachment)
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

private fun attachmentIcon(attachment: NoteAttachmentEntity): ImageVector {
    val extension = attachment.fileName.substringAfterLast('.', "").lowercase()
    return when {
        attachment.mimeType == "application/pdf" || extension == "pdf" -> Icons.Outlined.PictureAsPdf
        attachment.mimeType.startsWith("audio/") -> Icons.Outlined.AudioFile
        attachment.mimeType.startsWith("video/") -> Icons.Outlined.VideoFile
        extension in setOf("zip", "rar", "7z", "tar", "gz") -> Icons.Outlined.FolderZip
        attachment.mimeType.startsWith("text/") ||
            extension in setOf("doc", "docx", "xls", "xlsx", "ppt", "pptx", "md") -> Icons.Outlined.Description
        else -> Icons.Outlined.InsertDriveFile
    }
}

private fun formatFileSize(size: Long): String = when {
    size < 1024L -> "$size B"
    size < 1024L * 1024L -> String.format(Locale.CHINA, "%.1f KB", size / 1024.0)
    size < 1024L * 1024L * 1024L -> String.format(Locale.CHINA, "%.1f MB", size / (1024.0 * 1024.0))
    else -> String.format(Locale.CHINA, "%.1f GB", size / (1024.0 * 1024.0 * 1024.0))
}
