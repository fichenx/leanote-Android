package com.leanote.next.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leanote.next.data.local.NoteEntity
import com.leanote.next.data.model.SurfaceEffect
import com.leanote.next.ui.theme.LocalSurfaceEffect
import com.leanote.next.ui.theme.barColor
import com.leanote.next.ui.theme.pageBrush
import com.leanote.next.data.local.NotebookEntity
import com.leanote.next.data.model.SortMode
import com.leanote.next.ui.components.AccountAvatar
import com.leanote.next.ui.components.ConfirmDialog
import com.leanote.next.ui.components.EmptyState
import com.leanote.next.ui.components.LeanoteMark
import com.leanote.next.ui.components.LoadingBox
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenNote: (Long) -> Unit,
    onEditNote: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onNeedLogin: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val effect = LocalSurfaceEffect.current
    val scheme = MaterialTheme.colorScheme

    var showNewSheet by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var showCreateNotebook by remember { mutableStateOf(false) }
    var notebookMenuTarget by remember { mutableStateOf<NotebookEntity?>(null) }
    var renameTarget by remember { mutableStateOf<NotebookEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<NotebookEntity?>(null) }
    var deleteNoteTarget by remember { mutableStateOf<NoteEntity?>(null) }
    var sortMenu by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showBulkDelete by remember { mutableStateOf(false) }
    var exportFormat by remember { mutableStateOf(com.leanote.next.data.transfer.ExportFormat.MARKDOWN) }

    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) viewModel.exportSelected(uri, exportFormat)
    }

    LaunchedEffect(Unit) {
        launch { viewModel.messages.collect { snackbar.showSnackbar(it) } }
    }
    LaunchedEffect(Unit) {
        viewModel.createdNote.collect { onEditNote(it) }
    }
    LaunchedEffect(state.loading, state.account) {
        if (!state.loading && state.account == null) onNeedLogin()
    }
    LaunchedEffect(Unit) {
        viewModel.sync(silent = true)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = effect.barColor(scheme),
                drawerShape = MaterialTheme.shapes.extraLarge,
            ) {
                HomeDrawerContent(
                    account = state.account,
                    notebooks = state.notebooks,
                    tags = state.tags,
                    selectedNotebookId = state.filter.notebookServerId,
                    selectedTag = state.filter.tag,
                    trashSelected = state.filter.trash,
                    onSelectAll = {
                        viewModel.showAll()
                        scope.launch { drawerState.close() }
                    },
                    onShowTrash = {
                        viewModel.showTrash()
                        scope.launch { drawerState.close() }
                    },
                    onSelectNotebook = {
                        viewModel.selectNotebook(it)
                        scope.launch { drawerState.close() }
                    },
                    onSelectTag = { viewModel.selectTag(it) },
                    onAddNotebook = { showCreateNotebook = true },
                    onNotebookMenu = { notebookMenuTarget = it },
                    onSwitchAccount = { showAccountSheet = true },
                    onOpenBlog = {
                        val account = state.account
                        if (account == null) {
                            scope.launch { snackbar.showSnackbar("请先登录账户") }
                        } else {
                            scope.launch { drawerState.close() }
                            runCatching {
                                context.startActivity(
                                    android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse(account.blogUrl()),
                                    ),
                                )
                            }.onFailure {
                                scope.launch { snackbar.showSnackbar("无法打开博客页面") }
                            }
                        }
                    },
                    onSettings = {
                        scope.launch { drawerState.close() }
                        onOpenSettings()
                    },
                )
            }
        },
    ) {
        Scaffold(
            containerColor = if (effect == SurfaceEffect.NONE) scheme.background else Color.Transparent,
            modifier = Modifier.background(effect.pageBrush(scheme)),
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            if (state.selectionMode) {
                                Column {
                                    Text(
                                        "已选 ${state.selectedIds.size} 项",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        "可移动、导出或删除所选笔记",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            } else {
                            Column {
                                Text(
                                    "蚂蚁笔记",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = when {
                                        state.filter.trash -> "回收站"
                                        state.currentNotebook != null -> state.currentNotebook!!.title
                                        state.filter.tag != null -> "#${state.filter.tag}"
                                        else -> "全部笔记 · ${state.notes.size}"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                if (state.selectionMode) viewModel.clearSelection()
                                else scope.launch { drawerState.open() }
                            }) {
                                Icon(
                                    if (state.selectionMode) Icons.Rounded.Close else Icons.Rounded.Menu,
                                    contentDescription = if (state.selectionMode) "退出多选" else "打开侧边栏",
                                )
                            }
                        },
                        actions = {
                            if (state.selectionMode) {
                                IconButton(onClick = { viewModel.selectAllVisible() }) {
                                    Icon(Icons.Outlined.SelectAll, contentDescription = "全选/取消全选")
                                }
                                IconButton(onClick = { showMoveDialog = true }) {
                                    Icon(Icons.Outlined.DriveFileMove, contentDescription = "移动到其他笔记本")
                                }
                                IconButton(onClick = { showExportDialog = true }, enabled = !state.exporting) {
                                    Icon(Icons.Outlined.IosShare, contentDescription = "导出所选笔记")
                                }
                                IconButton(onClick = { showBulkDelete = true }) {
                                    Icon(
                                        Icons.Outlined.DeleteOutline,
                                        contentDescription = "删除所选笔记",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            } else {
                            state.account?.let { account ->
                                IconButton(onClick = { showAccountSheet = true }) {
                                    AccountAvatar(account, size = 30.dp)
                                }
                            }
                            IconButton(onClick = { viewModel.sync() }, enabled = !state.syncing) {
                                if (state.syncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(19.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Icon(Icons.Rounded.Sync, contentDescription = "同步")
                                }
                            }
                            Box {
                                IconButton(onClick = { moreMenu = true }) {
                                    Icon(Icons.Rounded.MoreVert, contentDescription = "更多")
                                }
                                DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                                    listOf(
                                        SortMode.UPDATED_DESC to "最近更新",
                                        SortMode.CREATED_DESC to "最近创建",
                                        SortMode.UPDATED_ASC to "最早更新",
                                        SortMode.TITLE_ASC to "标题排序",
                                    ).forEach { (mode, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            leadingIcon = {
                                                if (state.sortMode == mode) {
                                                    Icon(Icons.Outlined.Check, contentDescription = null)
                                                } else {
                                                    Icon(Icons.Outlined.Sort, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                                                }
                                            },
                                            onClick = {
                                                viewModel.setSortMode(mode)
                                                moreMenu = false
                                            },
                                        )
                                    }
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("设置") },
                                        leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                                        onClick = {
                                            moreMenu = false
                                            onOpenSettings()
                                        },
                                    )
                                }
                            }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = effect.barColor(scheme),
                        ),
                    )
                    SearchField(
                        query = state.filter.query,
                        onQueryChange = viewModel::setQuery,
                    )
                    if (state.syncing) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        )
                    }
                }
            },
            floatingActionButton = {
                if (!state.filter.trash && !state.selectionMode) {
                    ExtendedFloatingActionButton(
                        onClick = { showNewSheet = true },
                        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                        text = { Text("新建") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            },
        ) { padding ->
            Column(Modifier.padding(padding)) {
                AnimatedVisibility(visible = state.tags.isNotEmpty() && !state.filter.trash) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.tags) { tag ->
                            FilterChip(
                                selected = state.filter.tag == tag,
                                onClick = {
                                    viewModel.selectTag(if (state.filter.tag == tag) null else tag)
                                },
                                label = { Text(tag, style = MaterialTheme.typography.labelMedium) },
                                shape = CircleShape,
                                leadingIcon = if (state.filter.tag == tag) {
                                    {
                                        Icon(
                                            Icons.Rounded.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                } else null,
                            )
                        }
                    }
                }

                if (state.loading) {
                    LoadingBox()
                } else {
                    PullToRefreshBox(
                        isRefreshing = state.syncing,
                        onRefresh = { viewModel.sync() },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (state.notes.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                EmptyState(
                                    icon = if (state.filter.trash) Icons.Outlined.DeleteOutline else Icons.Outlined.StickyNote2,
                                    title = when {
                                        state.filter.trash -> "回收站是空的"
                                        state.filter.query.isNotBlank() -> "没有找到匹配的笔记"
                                        else -> "这里还没有笔记"
                                    },
                                    description = when {
                                        state.filter.trash -> "删除的笔记会在这里保留，直到你永久删除"
                                        state.filter.query.isNotBlank() -> "试试其他关键词，搜索会匹配标题、正文和标签"
                                        else -> "点击右下角的新建按钮，写下第一条笔记"
                                    },
                                    action = if (state.filter.trash) null else {
                                        {
                                            Button(onClick = { showNewSheet = true }) {
                                                Icon(Icons.Rounded.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Text("新建笔记")
                                            }
                                        }
                                    },
                                )
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(state.notes, key = { it.localId }) { note ->
                                    NoteCard(
                                        note = note,
                                        notebookName = state.notebookName(note.notebookServerId),
                                        account = state.account,
                                        selected = note.localId in state.selectedIds,
                                        selectionMode = state.selectionMode,
                                        onToggleSelect = { viewModel.toggleSelect(note.localId) },
                                        onClick = { onOpenNote(note.localId) },
                                        onEdit = { onEditNote(note.localId) },
                                        onTrash = { viewModel.moveToTrash(note) },
                                        onRestore = if (state.filter.trash) {
                                            { viewModel.restore(note) }
                                        } else null,
                                        onDeleteForever = if (state.filter.trash) {
                                            { deleteNoteTarget = note }
                                        } else null,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewSheet) {
        ModalBottomSheet(onDismissRequest = { showNewSheet = false }) {
            Column(Modifier.padding(bottom = 28.dp)) {
                Text(
                    "新建笔记",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 24.dp, bottom = 8.dp),
                )
                ListItem(
                    headlineContent = { Text("Markdown 笔记") },
                    supportingContent = { Text("使用 Markdown 语法书写，支持实时预览") },
                    leadingContent = {
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(9.dp),
                        ) {
                            Icon(
                                Icons.Outlined.TextSnippet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    modifier = Modifier.clickable {
                        showNewSheet = false
                        viewModel.createNote(markdown = true)
                    },
                )
                ListItem(
                    headlineContent = { Text("富文本笔记") },
                    supportingContent = { Text("像使用文档软件一样编辑格式丰富的内容") },
                    leadingContent = {
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                                .padding(9.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    modifier = Modifier.clickable {
                        showNewSheet = false
                        viewModel.createNote(markdown = false)
                    },
                )
            }
        }
    }

    if (showAccountSheet) {
        ModalBottomSheet(onDismissRequest = { showAccountSheet = false }) {
            Column(Modifier.padding(bottom = 28.dp)) {
                Text(
                    "切换账户",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 24.dp, bottom = 12.dp),
                )
                state.accounts.forEach { account ->
                    ListItem(
                        headlineContent = { Text(account.displayName) },
                        supportingContent = {
                            Text(account.host.removePrefix("https://").removePrefix("http://"))
                        },
                        leadingContent = { AccountAvatar(account, size = 36.dp) },
                        trailingContent = {
                            if (account.id == state.account?.id) {
                                Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        modifier = Modifier.clickable {
                            showAccountSheet = false
                            viewModel.switchAccount(account.id)
                        },
                    )
                }
                ListItem(
                    headlineContent = { Text("添加账户") },
                    leadingContent = {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    modifier = Modifier.clickable {
                        showAccountSheet = false
                        onNeedLogin()
                    },
                )
            }
        }
    }

    if (showCreateNotebook) {
        NotebookTitleDialog(
            title = "新建笔记本",
            initial = "",
            confirmText = "创建",
            onDismiss = { showCreateNotebook = false },
            onConfirm = { name ->
                showCreateNotebook = false
                if (name.isNotBlank()) viewModel.createNotebook(name)
            },
        )
    }

    notebookMenuTarget?.let { notebook ->
        AlertDialog(
            onDismissRequest = { notebookMenuTarget = null },
            title = { Text(notebook.title) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("重命名") },
                        leadingContent = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        modifier = Modifier.clickable {
                            notebookMenuTarget = null
                            renameTarget = notebook
                        },
                    )
                    ListItem(
                        headlineContent = { Text("删除笔记本", color = MaterialTheme.colorScheme.error) },
                        leadingContent = {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        modifier = Modifier.clickable {
                            notebookMenuTarget = null
                            deleteTarget = notebook
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { notebookMenuTarget = null }) { Text("取消") }
            },
        )
    }

    renameTarget?.let { notebook ->
        NotebookTitleDialog(
            title = "重命名笔记本",
            initial = notebook.title,
            confirmText = "保存",
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                val target = renameTarget
                renameTarget = null
                if (target != null && name.isNotBlank()) viewModel.renameNotebook(target, name)
            },
        )
    }

    deleteTarget?.let { notebook ->
        ConfirmDialog(
            title = "删除笔记本？",
            text = "「${notebook.title}」及其中的笔记会一并删除（服务器端同样生效）。",
            confirmText = "删除",
            destructive = true,
            onConfirm = {
                viewModel.deleteNotebook(notebook)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }

    deleteNoteTarget?.let { note ->
        ConfirmDialog(
            title = "永久删除笔记？",
            text = "「${note.title.ifBlank { "无标题笔记" }}」将被永久删除，无法恢复。",
            confirmText = "永久删除",
            destructive = true,
            onConfirm = {
                viewModel.deleteForever(note)
                deleteNoteTarget = null
            },
            onDismiss = { deleteNoteTarget = null },
        )
    }

    if (showMoveDialog) {
        AlertDialog(
            onDismissRequest = { showMoveDialog = false },
            title = { Text("移动 ${state.selectedIds.size} 条笔记到") },
            text = {
                LazyColumn {
                    items(state.notebooks) { notebook ->
                        ListItem(
                            headlineContent = { Text(notebook.title) },
                            supportingContent = if (notebook.serverId == state.filter.notebookServerId) {
                                { Text("当前笔记本") }
                            } else null,
                            colors = ListItemDefaults.colors(
                                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                            ),
                            modifier = Modifier.clickable {
                                showMoveDialog = false
                                viewModel.moveSelected(notebook.serverId)
                            },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMoveDialog = false }) { Text("取消") }
            },
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("导出 ${state.selectedIds.size} 条笔记") },
            text = {
                Column {
                    com.leanote.next.data.transfer.ExportFormat.entries.forEach { format ->
                        ListItem(
                            headlineContent = { Text(format.label) },
                            supportingContent = {
                                Text(
                                    when (format) {
                                        com.leanote.next.data.transfer.ExportFormat.MARKDOWN -> "每篇笔记一个 .md 文件，富文本导出为 .html"
                                        com.leanote.next.data.transfer.ExportFormat.HTML -> "每篇笔记一个独立网页，可用浏览器打开"
                                        com.leanote.next.data.transfer.ExportFormat.ENEX -> "合并为一个 .enex，可导入 Evernote / 桌面版"
                                    },
                                )
                            },
                            leadingContent = {
                                if (exportFormat == format) {
                                    Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                            modifier = Modifier.clickable { exportFormat = format },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showExportDialog = false
                    exportLauncher.launch(null)
                }) { Text("选择目录并导出") }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) { Text("取消") }
            },
        )
    }

    if (showBulkDelete) {
        ConfirmDialog(
            title = if (state.filter.trash) "永久删除所选笔记？" else "删除所选笔记？",
            text = if (state.filter.trash) {
                "将永久删除 ${state.selectedIds.size} 条笔记，无法恢复。"
            } else {
                "将 ${state.selectedIds.size} 条笔记移至回收站，并同步到服务器。"
            },
            confirmText = if (state.filter.trash) "永久删除" else "移至回收站",
            destructive = true,
            onConfirm = {
                showBulkDelete = false
                viewModel.deleteSelected()
            },
            onDismiss = { showBulkDelete = false },
        )
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        placeholder = { Text("搜索标题、正文或标签") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Rounded.Close, contentDescription = "清空搜索")
                }
            }
        } else null,
        singleLine = true,
        shape = CircleShape,
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        ),
    )
}

@Composable
private fun NotebookTitleDialog(
    title: String,
    initial: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                label = { Text("名称") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
