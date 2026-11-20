package com.leanote.next.ui.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leanote.next.data.local.NoteEntity
import com.leanote.next.data.repo.ImageRepository
import com.leanote.next.ui.components.ConfirmDialog
import com.leanote.next.ui.components.EmptyState
import com.leanote.next.ui.components.LoadingBox
import com.leanote.next.ui.components.TagRow
import com.leanote.next.ui.theme.MonoFont
import com.leanote.next.ui.web.HtmlContent
import com.leanote.next.ui.web.MarkdownContent
import com.leanote.next.util.TimeUtils
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    localId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: NoteDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var moreMenu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var fitToWidth by rememberSaveable { mutableStateOf(true) }

    val imageRepository = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            DetailEntryPoint::class.java,
        ).imageRepository()
    }

    LaunchedEffect(localId) { viewModel.load(localId) }
    LaunchedEffect(Unit) {
        launch { viewModel.messages.collect { snackbar.showSnackbar(it) } }
    }
    LaunchedEffect(Unit) {
        viewModel.closed.collect { onBack() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                title = {
                    Text(
                        state.notebookName ?: "笔记",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                actions = {
                    IconButton(onClick = { fitToWidth = !fitToWidth }) {
                        Icon(
                            imageVector = if (fitToWidth) Icons.Rounded.CropFree else Icons.Rounded.FitScreen,
                            contentDescription = if (fitToWidth) "切换为原文模式" else "切换为移动端适配",
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, contentDescription = "编辑")
                    }
                    IconButton(onClick = {
                        val text = viewModel.shareText()
                        if (text.isNotBlank()) {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(intent, "分享笔记"))
                        }
                    }) {
                        Icon(Icons.Rounded.Share, contentDescription = "分享")
                    }
                    Box {
                        IconButton(onClick = { moreMenu = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                            viewModel.blogUrl()?.let { url ->
                                DropdownMenuItem(
                                    text = { Text("在博客中查看") },
                                    leadingIcon = { Icon(Icons.Outlined.Public, contentDescription = null) },
                                    onClick = {
                                        moreMenu = false
                                        context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("复制笔记链接") },
                                leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = null) },
                                onClick = {
                                    moreMenu = false
                                    val link = viewModel.blogUrl()
                                    if (link == null) {
                                        scope.launch { snackbar.showSnackbar("这条笔记尚未发布为博客") }
                                    } else {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("笔记链接", link))
                                        scope.launch { snackbar.showSnackbar("链接已复制") }
                                    }
                                },
                            )
                            if (state.note?.isTrash == true) {
                                DropdownMenuItem(
                                    text = { Text("恢复到笔记本") },
                                    leadingIcon = { Icon(Icons.Outlined.Restore, contentDescription = null) },
                                    onClick = {
                                        moreMenu = false
                                        viewModel.setTrash(false)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("永久删除", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    },
                                    onClick = {
                                        moreMenu = false
                                        confirmDelete = true
                                    },
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("移至回收站") },
                                    leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
                                    onClick = {
                                        moreMenu = false
                                        viewModel.setTrash(true)
                                    },
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        val note = state.note
        when {
            state.loading -> LoadingBox(Modifier.padding(padding))
            note == null -> EmptyState(
                icon = Icons.Outlined.DeleteOutline,
                title = "笔记不存在",
                description = "它可能已被删除或尚未同步",
                modifier = Modifier.padding(padding),
            )
            note.isMarkdown -> MarkdownDetail(
                note = note,
                rendered = state.renderedContent,
                fitToWidth = fitToWidth,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
            )
            else -> HtmlDetail(
                note = note,
                rendered = state.renderedContent,
                imageRepository = imageRepository,
                account = state.account,
                fitToWidth = fitToWidth,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
            )
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "永久删除笔记？",
            text = "删除后将无法恢复。",
            confirmText = "永久删除",
            destructive = true,
            onConfirm = {
                confirmDelete = false
                viewModel.deleteForever()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun MarkdownDetail(
    note: NoteEntity,
    rendered: String,
    fitToWidth: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
    ) {
        NoteHeader(note, originalMode = !fitToWidth)
        if (fitToWidth) {
            MarkdownContent(
                markdown = rendered,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 56.dp),
            )
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = MonoFont,
                        fontSize = 14.sp,
                        lineHeight = 23.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 56.dp),
                )
            }
        }
    }
}

@Composable
private fun HtmlDetail(
    note: NoteEntity,
    rendered: String,
    imageRepository: ImageRepository,
    account: com.leanote.next.data.model.Account?,
    fitToWidth: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Box(Modifier.padding(horizontal = 22.dp)) { NoteHeader(note, originalMode = !fitToWidth) }
        HtmlContent(
            html = rendered,
            account = account,
            images = imageRepository,
            fitToWidth = fitToWidth,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}

@Composable
private fun NoteHeader(note: NoteEntity, originalMode: Boolean = false) {
    Column(Modifier.padding(top = 4.dp, bottom = 12.dp)) {
        Text(
            text = if (note.title.isBlank()) "无标题笔记" else note.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            lineHeight = MaterialTheme.typography.headlineMedium.lineHeight,
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = buildString {
                    append(TimeUtils.formatFull(note.updatedTime))
                    if (note.isLocalOnly || note.isDirty) append(" · 未同步")
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (note.isBlog) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Outlined.Public,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(13.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                shape = MaterialTheme.shapes.extraSmall,
            ) {
                Text(
                    if (note.isMarkdown) "Markdown" else "富文本",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                )
            }
            if (originalMode) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Outlined.Public,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
        if (originalMode) {
            Spacer(Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                shape = MaterialTheme.shapes.extraSmall,
            ) {
                Text(
                    "原文模式 · 可左右滑动",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
        if (note.tagList.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            TagRow(note.tagList, max = 6)
        }
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(Modifier.height(18.dp))
    }
}

private fun launchMessage(scope: kotlinx.coroutines.CoroutineScope, host: SnackbarHostState, message: String) {
    scope.launch { host.showSnackbar(message) }
}


@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface DetailEntryPoint {
    fun imageRepository(): ImageRepository
}
