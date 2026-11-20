package com.leanote.next.ui.editor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.FormatAlignCenter
import androidx.compose.material.icons.rounded.FormatAlignLeft
import androidx.compose.material.icons.rounded.FormatAlignRight
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatColorText
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.FormatStrikethrough
import androidx.compose.material.icons.rounded.FormatUnderlined
import androidx.compose.material.icons.rounded.Highlight
import androidx.compose.material.icons.rounded.HorizontalRule
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Redo
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leanote.next.data.repo.ImageRepository
import com.leanote.next.ui.components.ConfirmDialog
import com.leanote.next.ui.components.LoadingBox
import com.leanote.next.ui.theme.MonoFont
import com.leanote.next.ui.web.MarkdownContent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

private const val LOCAL_IMAGE_PREFIX = "file:/getImage?id="
private const val WEB_IMAGE_PREFIX = "https://leanote.local/getImage?localId="

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    localId: Long,
    onBack: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val richController = remember { RichTextController() }

    var showPreview by remember { mutableStateOf(false) }
    var previewHtml by remember { mutableStateOf("") }
    var showInfo by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var colorMode by remember { mutableStateOf<String?>(null) }
    var tableSize by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var markdownValue by remember(localId) { mutableStateOf(TextFieldValue("")) }

    val undoStack = remember(localId) { mutableStateListOf<TextFieldValue>() }
    val redoStack = remember(localId) { mutableStateListOf<TextFieldValue>() }

    fun applyMarkdown(value: TextFieldValue) {
        if (value.text == markdownValue.text && value.selection == markdownValue.selection) return
        if (undoStack.size >= 120) undoStack.removeAt(0)
        undoStack.add(markdownValue)
        redoStack.clear()
        markdownValue = value
        viewModel.updateContent(value.text)
    }

    fun undoMarkdown() {
        if (undoStack.isEmpty()) return
        redoStack.add(markdownValue)
        markdownValue = undoStack.removeAt(undoStack.lastIndex)
        viewModel.updateContent(markdownValue.text)
    }

    fun redoMarkdown() {
        if (redoStack.isEmpty()) return
        undoStack.add(markdownValue)
        markdownValue = redoStack.removeAt(redoStack.lastIndex)
        viewModel.updateContent(markdownValue.text)
    }

    fun runMarkdown(command: String, value: String?) {
        when (command) {
            "undo" -> {
                undoMarkdown()
                return
            }
            "redo" -> {
                redoMarkdown()
                return
            }
        }
        val next = when (command) {
            "bold" -> markdownValue.wrapSelection("**", "**", "粗体")
            "italic" -> markdownValue.wrapSelection("_", "_", "斜体")
            "underline" -> markdownValue.wrapSelection("<u>", "</u>", "下划线")
            "strike" -> markdownValue.wrapSelection("~~", "~~", "删除线")
            "code" -> markdownValue.wrapSelection("`", "`", "代码")
            "color" -> markdownValue.wrapSelection(
                "<font color=\"${value ?: "#E53935"}\">",
                "</font>",
                "彩色文字",
            )
            "highlight" -> markdownValue.wrapSelection(
                "<mark style=\"background-color:${value ?: "#FFF59D"}\">",
                "</mark>",
                "高亮文字",
            )
            "heading" -> markdownValue.setHeading(value ?: "p")
            "ul" -> markdownValue.toggleLinePrefix("- ")
            "ol" -> markdownValue.toggleLinePrefix("1. ")
            "task" -> markdownValue.toggleLinePrefix("- [ ] ")
            "quote" -> markdownValue.toggleLinePrefix("> ")
            "codeblock" -> markdownValue.insertWrappedBlock("\n```\n", "\n```\n")
            "hr" -> markdownValue.insertAtCursor("\n\n---\n\n")
            "table" -> {
                val (r, c) = (value ?: "3x3").split("x").let {
                    (it.getOrNull(0)?.toIntOrNull()?.coerceIn(1, 10) ?: 3) to
                        (it.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 10) ?: 3)
                }
                markdownValue.insertAtCursor(buildMarkdownTable(r, c))
            }
            "link" -> {
                showLinkDialog = true
                return
            }
            else -> markdownValue
        }
        applyMarkdown(next)
    }

    fun runRich(command: String, value: String?) {
        when (command) {
            "table" -> {
                val (r, c) = (value ?: "3x3").split("x").let {
                    (it.getOrNull(0)?.toIntOrNull()?.coerceIn(1, 10) ?: 3) to
                        (it.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 10) ?: 3)
                }
                richController.insertHtml(buildRichTableHtml(r, c))
            }
            "codeblock" -> richController.insertHtml("<pre><br></pre><p><br></p>")
            "task" -> richController.exec("insertUnorderedList")
            "link" -> {
                richController.exec("createLink", "https://")
                return
            }
            else -> {
                val js = when (command) {
                    "bold" -> "bold"
                    "italic" -> "italic"
                    "underline" -> "underline"
                    "strike" -> "strikeThrough"
                    "heading" -> "formatBlock"
                    "ul" -> "insertUnorderedList"
                    "ol" -> "insertOrderedList"
                    "quote" -> "formatBlock"
                    "code" -> "formatBlock"
                    "color" -> "foreColor"
                    "highlight" -> "hiliteColor"
                    "alignLeft" -> "justifyLeft"
                    "alignCenter" -> "justifyCenter"
                    "alignRight" -> "justifyRight"
                    "hr" -> "insertHorizontalRule"
                    "undo" -> "undo"
                    "redo" -> "redo"
                    else -> null
                }
                if (js != null) {
                    val arg = when (command) {
                        "quote" -> "<blockquote>"
                        "code" -> "<pre>"
                        else -> value
                    }
                    richController.exec(js, arg)
                }
            }
        }
    }

    val images = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            EditorEntryPoint::class.java,
        ).imageRepository()
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.addImage(uri)
    }

    LaunchedEffect(localId) { viewModel.load(localId) }
    LaunchedEffect(state.loading, state.note?.localId) {
        if (!state.loading && state.note != null) {
            markdownValue = TextFieldValue(state.content, TextRange(state.content.length))
            undoStack.clear()
            redoStack.clear()
        }
    }
    LaunchedEffect(Unit) {
        launch { viewModel.messages.collect { snackbar.showSnackbar(it) } }
    }
    LaunchedEffect(Unit) {
        viewModel.closed.collect { onBack() }
    }
    LaunchedEffect(Unit) {
        viewModel.insertImageRequests.collect { ref ->
            if (viewModel.state.value.isMarkdown) {
                applyMarkdown(markdownValue.insertAtCursor("![]($ref)"))
            } else {
                richController.insertImage(ref.replace(LOCAL_IMAGE_PREFIX, WEB_IMAGE_PREFIX))
            }
        }
    }
    LaunchedEffect(showPreview, state.content) {
        if (showPreview) previewHtml = viewModel.renderPreview()
    }

    val handleBack: () -> Unit = {
        viewModel.saveNow()
        onBack()
    }
    BackHandler { handleBack() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = state.notebookName ?: "编辑笔记",
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = statusText(state),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::syncNow) {
                        Icon(syncIcon(state.syncStatus), contentDescription = "同步")
                    }
                    IconButton(onClick = { showInfo = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "更多")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            EditorToolbar(
                isMarkdown = state.isMarkdown,
                previewMode = showPreview,
                onCommand = { command, value ->
                    if (state.isMarkdown) runMarkdown(command, value) else runRich(command, value)
                },
                onImage = { pickImage.launch("image/*") },
                onColor = { colorMode = it },
                onTable = { tableSize = Pair(3, 3) },
                onTogglePreview = { showPreview = !showPreview },
            )
        },
    ) { padding ->
        when {
            state.loading -> LoadingBox(Modifier.padding(padding))
            state.note == null -> LoadingBox(Modifier.padding(padding))
            else -> Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                TitleField(
                    title = state.title,
                    onTitleChange = viewModel::updateTitle,
                )
                MetaChips(
                    state = state,
                    onOpenInfo = { showInfo = true },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(top = 10.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
                when {
                    showPreview -> Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 22.dp, vertical = 16.dp),
                    ) {
                        MarkdownContent(markdown = previewHtml, modifier = Modifier.fillMaxWidth())
                    }

                    state.isMarkdown -> MarkdownEditor(
                        value = markdownValue,
                        onValueChange = { applyMarkdown(it) },
                        modifier = Modifier.fillMaxSize(),
                    )

                    else -> RichTextEditorView(
                        initialHtml = state.editorHtml,
                        account = state.account,
                        images = images,
                        controller = richController,
                        onHtmlChanged = viewModel::updateHtml,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp),
                    )
                }
            }
        }
    }

    if (showInfo) {
        NoteInfoSheet(
            state = state,
            onDismiss = { showInfo = false },
            onSelectNotebook = viewModel::setNotebook,
            onTagsChange = viewModel::updateTags,
            onBlogChange = viewModel::setBlog,
            onDelete = {
                showInfo = false
                confirmDelete = true
            },
        )
    }

    if (showLinkDialog) {
        var url by remember { mutableStateOf("https://") }
        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = { Text("插入链接") },
            text = {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("链接地址") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showLinkDialog = false
                    applyMarkdown(markdownValue.wrapSelection("[", "]($url)", "链接"))
                }) { Text("插入") }
            },
            dismissButton = {
                TextButton(onClick = { showLinkDialog = false }) { Text("取消") }
            },
        )
    }

    colorMode?.let { mode ->
        EditorColorDialog(
            mode = mode,
            onDismiss = { colorMode = null },
            onPick = { hex ->
                colorMode = null
                if (state.isMarkdown) runMarkdown(mode, hex) else runRich(mode, hex)
            },
        )
    }

    tableSize?.let { _ ->
        TableSizeDialog(
            onDismiss = { tableSize = null },
        ) { rows, cols ->
            tableSize = null
            val value = "${rows}x${cols}"
            if (state.isMarkdown) runMarkdown("table", value) else runRich("table", value)
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "删除这条笔记？",
            text = "笔记将移入回收站，可稍后恢复。",
            confirmText = "删除",
            destructive = true,
            onConfirm = {
                confirmDelete = false
                scope.launch { viewModel.deleteNote() }
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun TitleField(title: String, onTitleChange: (String) -> Unit) {
    BasicTextField(
        value = title,
        onValueChange = onTitleChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 4.dp),
        textStyle = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { inner ->
            Box {
                if (title.isEmpty()) {
                    Text(
                        text = "无标题笔记",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    )
                }
                inner()
            }
        },
    )
}

@Composable
private fun MetaChips(state: EditorState, onOpenInfo: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AssistChip(
            onClick = onOpenInfo,
            label = {
                Text(
                    state.notebookName ?: "选择笔记本",
                    style = MaterialTheme.typography.labelMedium,
                )
            },
            leadingIcon = { Icon(Icons.Rounded.Notes, contentDescription = null, modifier = Modifier.size(16.dp)) },
            shape = CircleShape,
            border = null,
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        )
        AssistChip(
            onClick = onOpenInfo,
            label = {
                Text(
                    if (state.tags.isBlank()) "添加标签" else state.tags,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingIcon = { Icon(Icons.Outlined.Label, contentDescription = null, modifier = Modifier.size(16.dp)) },
            shape = CircleShape,
            border = null,
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        )
        if (state.isBlog) {
            Text(
                text = "已发布博客",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun MarkdownEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxSize(),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = MonoFont,
                fontSize = 15.sp,
                lineHeight = 24.sp,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { inner ->
                Box {
                    if (value.text.isEmpty()) {
                        Text(
                            text = "开始记录今天的想法…\n支持 Markdown 语法",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = MonoFont,
                                fontSize = 15.sp,
                                lineHeight = 24.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
private fun EditorToolbar(
    isMarkdown: Boolean,
    previewMode: Boolean,
    onCommand: (String, String?) -> Unit,
    onImage: () -> Unit,
    onColor: (String) -> Unit,
    onTable: () -> Unit,
    onTogglePreview: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
    ) {
        if (previewMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ToolbarAction(Icons.Rounded.VisibilityOff, "退出预览", onTogglePreview)
                Spacer(Modifier.width(6.dp))
                Text(
                    "Markdown 预览",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(Modifier.navigationBarsPadding()) {
                ToolbarRow {
                    ToolbarAction(Icons.Rounded.Undo, "撤销") { onCommand("undo", null) }
                    ToolbarAction(Icons.Rounded.Redo, "重做") { onCommand("redo", null) }
                    ToolbarDivider()
                    HeadingMenu(onPick = { onCommand("heading", it) })
                    ToolbarAction(Icons.Rounded.FormatBold, "加粗") { onCommand("bold", null) }
                    ToolbarAction(Icons.Rounded.FormatItalic, "斜体") { onCommand("italic", null) }
                    ToolbarAction(Icons.Rounded.FormatUnderlined, "下划线") { onCommand("underline", null) }
                    ToolbarAction(Icons.Rounded.FormatStrikethrough, "删除线") { onCommand("strike", null) }
                    ToolbarAction(Icons.Rounded.Code, "行内代码") { onCommand("code", null) }
                    ToolbarAction(Icons.Rounded.FormatColorText, "文字颜色") { onColor("color") }
                    ToolbarAction(Icons.Rounded.Highlight, "高亮") { onColor("highlight") }
                    if (!isMarkdown) {
                        ToolbarDivider()
                        ToolbarAction(Icons.Rounded.FormatAlignLeft, "左对齐") { onCommand("alignLeft", null) }
                        ToolbarAction(Icons.Rounded.FormatAlignCenter, "居中") { onCommand("alignCenter", null) }
                        ToolbarAction(Icons.Rounded.FormatAlignRight, "右对齐") { onCommand("alignRight", null) }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ToolbarRow {
                    ToolbarAction(Icons.Rounded.FormatListBulleted, "无序列表") { onCommand("ul", null) }
                    ToolbarAction(Icons.Rounded.FormatListNumbered, "有序列表") { onCommand("ol", null) }
                    ToolbarAction(Icons.Rounded.Checklist, "任务列表") { onCommand("task", null) }
                    ToolbarAction(Icons.Rounded.FormatQuote, "引用") { onCommand("quote", null) }
                    ToolbarAction(Icons.Rounded.Code, "代码块") { onCommand("codeblock", null) }
                    ToolbarDivider()
                    ToolbarAction(Icons.Rounded.Link, "链接") { onCommand("link", null) }
                    ToolbarAction(Icons.Rounded.Image, "插入图片", onImage)
                    ToolbarAction(Icons.Rounded.TableChart, "表格", onTable)
                    ToolbarAction(Icons.Rounded.HorizontalRule, "分割线") { onCommand("hr", null) }
                    ToolbarDivider()
                    ToolbarAction(Icons.Rounded.Visibility, "预览", onTogglePreview)
                }
            }
        }
    }
}

@Composable
private fun ToolbarRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

@Composable
private fun ToolbarDivider() {
    Box(
        Modifier
            .padding(horizontal = 2.dp)
            .size(width = 1.dp, height = 20.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    )
}

@Composable
private fun HeadingMenu(onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.size(42.dp)) {
            Icon(
                imageVector = Icons.Rounded.Title,
                contentDescription = "标题级别",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(21.dp),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            listOf(
                "h1" to "一级标题",
                "h2" to "二级标题",
                "h3" to "三级标题",
                "p" to "正文",
            ).forEach { (level, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        open = false
                        onPick(level)
                    },
                )
            }
        }
    }
}

@Composable
private fun ToolbarAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(42.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(21.dp),
        )
    }
}

private val TextColors = listOf(
    "#E53935", "#FB8C00", "#FDD835", "#43A047",
    "#00897B", "#1E88E5", "#8E24AA", "#546E7A",
)

private val HighlightColors = listOf(
    "#FFF59D", "#C8E6C9", "#BBDEFB", "#F8BBD0", "#FFE0B2", "#D1C4E9",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditorColorDialog(
    mode: String,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    val colors = if (mode == "color") TextColors else HighlightColors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (mode == "color") "文字颜色" else "高亮颜色") },
        text = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                colors.forEach { hex ->
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex)))
                            .clickable { onPick(hex) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

private fun buildMarkdownTable(rows: Int, cols: Int): String {
    val c = cols.coerceIn(1, 10)
    val r = rows.coerceIn(1, 10)
    val header = (1..c).joinToString(" | ") { "列 $it" }
    val sep = (1..c).joinToString(" | ") { "---" }
    val body = (1..r).joinToString("\n") { (1..c).joinToString(" | ") { "内容" } }
    return "\n\n| $header |\n| $sep |\n| $body |\n\n"
}

private fun buildRichTableHtml(rows: Int, cols: Int): String {
    val c = cols.coerceIn(1, 10)
    val r = rows.coerceIn(1, 10)
    val head = (1..c).joinToString("") { "<th>列 $it</th>" }
    val tbody = (1..r).joinToString("") {
        "<tr>" + (1..c).joinToString("") { "<td>内容</td>" } + "</tr>"
    }
    return "<table><thead><tr>$head</tr></thead><tbody>$tbody</tbody></table><p><br></p>"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TableSizeDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    var rows by remember { mutableStateOf(3) }
    var cols by remember { mutableStateOf(3) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("插入表格") },
        text = {
            Column {
                Text(
                    "选择要插入的行数和列数（1~10）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "预览：${rows} 行 × ${cols} 列",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(12.dp))
                Text("行数")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { rows = rows.coerceAtLeast(1) - 1 }, enabled = rows > 1) {
                        Text("−", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(rows.toString(), style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { rows = rows.coerceAtMost(10) + 1 }, enabled = rows < 10) {
                        Text("+", style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("列数")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { cols = cols.coerceAtLeast(1) - 1 }, enabled = cols > 1) {
                        Text("−", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(cols.toString(), style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { cols = cols.coerceAtMost(10) + 1 }, enabled = cols < 10) {
                        Text("+", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(rows, cols) }) { Text("插入") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteInfoSheet(
    state: EditorState,
    onDismiss: () -> Unit,
    onSelectNotebook: (String) -> Unit,
    onTagsChange: (String) -> Unit,
    onBlogChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
        ) {
            Text("笔记信息", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(18.dp))
            Text("笔记本", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            state.notebooks.forEach { notebook ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectNotebook(notebook.serverId) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = state.notebookServerId == notebook.serverId,
                        onClick = { onSelectNotebook(notebook.serverId) },
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = notebook.title.ifBlank { "未命名笔记本" },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = state.tags,
                onValueChange = onTagsChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("标签（用逗号分隔）") },
                singleLine = true,
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("发布到博客", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "公开笔记，可通过链接分享",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.isBlog, onCheckedChange = onBlogChange)
            }
            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("移至回收站", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun statusText(state: EditorState): String = when {
    state.syncStatus == EditorSyncStatus.SYNCING -> "同步中…"
    state.saving -> "保存中…"
    state.dirty -> "有未同步的修改"
    state.syncStatus == EditorSyncStatus.SYNCED -> "已同步到服务器"
    state.note?.isLocalOnly == true -> "仅本地"
    else -> "已保存"
}

private fun syncIcon(status: EditorSyncStatus) = when (status) {
    EditorSyncStatus.SYNCING -> Icons.Rounded.CloudSync
    EditorSyncStatus.SYNCED -> Icons.Rounded.CloudDone
    EditorSyncStatus.IDLE -> Icons.Rounded.CloudUpload
    EditorSyncStatus.OFFLINE -> Icons.Rounded.CloudUpload
}

private fun TextFieldValue.insertAtCursor(snippet: String): TextFieldValue {
    val start = selection.start.coerceIn(0, text.length)
    val end = selection.end.coerceIn(start, text.length)
    val newText = text.replaceRange(start, end, snippet)
    return TextFieldValue(newText, TextRange(start + snippet.length))
}

private fun TextFieldValue.wrapSelection(prefix: String, suffix: String, placeholder: String): TextFieldValue {
    val start = selection.min.coerceIn(0, text.length)
    val end = selection.max.coerceIn(start, text.length)
    val selected = text.substring(start, end).ifBlank { placeholder }
    val newText = text.replaceRange(start, end, prefix + selected + suffix)
    return TextFieldValue(newText, TextRange(start + prefix.length + selected.length))
}

private fun TextFieldValue.toggleLinePrefix(prefix: String): TextFieldValue {
    val cursor = selection.start.coerceIn(0, text.length)
    val lineStart = (text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 })
    return if (text.startsWith(prefix, lineStart)) {
        TextFieldValue(
            text = text.removeRange(lineStart, lineStart + prefix.length),
            selection = TextRange((cursor - prefix.length).coerceAtLeast(lineStart)),
        )
    } else {
        TextFieldValue(
            text = text.substring(0, lineStart) + prefix + text.substring(lineStart),
            selection = TextRange(cursor + prefix.length),
        )
    }
}

private fun TextFieldValue.insertWrappedBlock(prefix: String, suffix: String): TextFieldValue {
    val start = selection.min.coerceIn(0, text.length)
    val end = selection.max.coerceIn(start, text.length)
    val selected = text.substring(start, end)
    val newText = text.replaceRange(start, end, prefix + selected + suffix)
    return TextFieldValue(newText, TextRange(start + prefix.length + selected.length))
}

private fun TextFieldValue.setHeading(level: String): TextFieldValue {
    val prefix = when (level) {
        "h1" -> "# "
        "h2" -> "## "
        "h3" -> "### "
        else -> ""
    }
    val cursor = selection.start.coerceIn(0, text.length)
    val lineStart = (text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 })
    val lineEnd = text.indexOf('\n', cursor).let { if (it < 0) text.length else it }
    val line = text.substring(lineStart, lineEnd)
    val stripped = line.replaceFirst(Regex("^#{1,6}\\s+"), "")
    val newLine = prefix + stripped
    val newText = text.replaceRange(lineStart, lineEnd, newLine)
    val newCursor = (cursor + newLine.length - line.length).coerceIn(lineStart, lineStart + newLine.length)
    return TextFieldValue(newText, TextRange(newCursor))
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface EditorEntryPoint {
    fun imageRepository(): ImageRepository
}
