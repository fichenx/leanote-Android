package com.leanote.next.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.leanote.next.BuildConfig
import com.leanote.next.data.local.NotebookEntity
import com.leanote.next.data.model.Account
import com.leanote.next.data.model.UiDensity
import com.leanote.next.ui.components.AccountAvatar
import com.leanote.next.ui.components.SectionHeader
import com.leanote.next.ui.theme.LocalUiDensity
import com.leanote.next.ui.theme.LocalUiSpacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeDrawerContent(
    account: Account?,
    notebooks: List<NotebookEntity>,
    tags: List<String>,
    selectedNotebookId: String?,
    selectedTag: String?,
    trashSelected: Boolean,
    onSelectAll: () -> Unit,
    onShowTrash: () -> Unit,
    onSelectNotebook: (String) -> Unit,
    onSelectTag: (String?) -> Unit,
    onAddNotebook: () -> Unit,
    onNotebookMenu: (NotebookEntity) -> Unit,
    onSwitchAccount: () -> Unit,
    onOpenBlog: () -> Unit,
    onSettings: () -> Unit,
) {
    val spacing = LocalUiSpacing.current
    val rowMinHeight = drawerRowHeight()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = spacing.pageVertical),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(enabled = account != null, onClick = onSwitchAccount)
                    .padding(
                        start = spacing.pageHorizontal,
                        end = spacing.inlineGap,
                        top = spacing.sectionGap,
                        bottom = spacing.contentVertical,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (account != null) {
                    AccountAvatar(account, size = 46.dp)
                    Spacer(Modifier.width(spacing.itemGap))
                    Column(Modifier.weight(1f)) {
                        Text(
                            account.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = account.host.removePrefix("https://").removePrefix("http://"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = onSwitchAccount) {
                        Icon(Icons.Rounded.SwapHoriz, contentDescription = "切换账户")
                    }
                }
            }
        }

        DrawerNavigationItem(
            label = { Text("全部笔记") },
            icon = { Icon(Icons.Rounded.Notes, contentDescription = null) },
            selected = !trashSelected && selectedNotebookId == null && selectedTag == null,
            onClick = onSelectAll,
        )
        DrawerNavigationItem(
            label = { Text("回收站") },
            icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
            selected = trashSelected,
            onClick = onShowTrash,
        )
        DrawerNavigationItem(
            label = {
                Column {
                    Text("我的博客")
                    if (account != null) {
                        Text(
                            text = account.blogUrl().removePrefix("https://").removePrefix("http://"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            },
            icon = { Icon(Icons.Outlined.Public, contentDescription = null) },
            selected = false,
            onClick = onOpenBlog,
        )

        Spacer(Modifier.height(spacing.sectionGap))
        SectionHeader("笔记本") {
            IconButton(onClick = onAddNotebook, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Rounded.Add, contentDescription = "新建笔记本", modifier = Modifier.size(18.dp))
            }
        }

        val collapsedIds = remember { mutableStateOf(emptySet<String>()) }
        val tree = flattenNotebooks(notebooks, collapsedIds.value)
        val parentIds = remember(notebooks) {
            notebooks.map { it.parentServerId }.filter { it.isNotBlank() }.toSet()
        }
        if (tree.isEmpty()) {
            Text(
                "还没有笔记本",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    horizontal = spacing.pageHorizontal + spacing.inlineGap,
                    vertical = spacing.itemGap / 2,
                ),
            )
        }
        tree.forEach { (notebook, depth) ->
            val hasChildren = notebook.serverId in parentIds
            val expanded = notebook.serverId !in collapsedIds.value
            NotebookRow(
                notebook = notebook,
                depth = depth,
                selected = !trashSelected && selectedNotebookId == notebook.serverId,
                hasChildren = hasChildren,
                expanded = expanded,
                onToggleExpand = {
                    collapsedIds.value = if (expanded) {
                        collapsedIds.value + notebook.serverId
                    } else {
                        collapsedIds.value - notebook.serverId
                    }
                },
                onClick = { onSelectNotebook(notebook.serverId) },
                onMenu = { onNotebookMenu(notebook) },
            )
        }

        if (tags.isNotEmpty()) {
            Spacer(Modifier.height(spacing.sectionGap))
            SectionHeader("标签")
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.pageHorizontal),
                horizontalArrangement = Arrangement.spacedBy(spacing.inlineGap),
                verticalArrangement = Arrangement.spacedBy(spacing.inlineGap),
            ) {
                tags.forEach { tag ->
                    val active = selectedTag == tag
                    AssistChip(
                        onClick = { onSelectTag(if (active) null else tag) },
                        label = { Text(tag, style = MaterialTheme.typography.labelMedium) },
                        shape = MaterialTheme.shapes.extraLarge,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                        ),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (active) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerLow,
                            labelColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        }

        Spacer(Modifier.height(spacing.sectionGap))
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = spacing.pageHorizontal),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = rowMinHeight)
                .clickable(onClick = onSettings)
                .padding(horizontal = spacing.pageHorizontal, vertical = spacing.itemGap / 2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(spacing.itemGap))
            Text("设置", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            Text(
                "v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun drawerRowHeight(): Dp = when (LocalUiDensity.current) {
    UiDensity.COMPACT -> 34.dp
    UiDensity.STANDARD -> 40.dp
    UiDensity.COMFORTABLE -> 46.dp
}

@Composable
private fun DrawerNavigationItem(
    label: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val spacing = LocalUiSpacing.current
    val rowMinHeight = drawerRowHeight()
    val shape = MaterialTheme.shapes.medium
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Surface(
            color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else Color.Transparent,
            shape = shape,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.pageHorizontal, vertical = spacing.itemGap / 4)
                .clip(shape)
                .clickable(onClick = onClick),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = rowMinHeight)
                    .padding(horizontal = spacing.inlineGap, vertical = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) { icon() }
                Spacer(Modifier.width(spacing.itemGap))
                Box(Modifier.weight(1f)) { label() }
            }
        }
    }
}

@Composable
private fun NotebookRow(
    notebook: NotebookEntity,
    depth: Int,
    selected: Boolean,
    hasChildren: Boolean = false,
    expanded: Boolean = true,
    onToggleExpand: () -> Unit = {},
    onClick: () -> Unit,
    onMenu: () -> Unit,
) {
    val spacing = LocalUiSpacing.current
    val rowMinHeight = drawerRowHeight()
    val shape = MaterialTheme.shapes.medium

    Surface(
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        else Color.Transparent,
        shape = shape,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.pageHorizontal, vertical = spacing.itemGap / 4)
            .clip(shape)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = rowMinHeight)
                .padding(
                    start = spacing.inlineGap + spacing.itemGap * depth,
                    end = 2.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (hasChildren) {
                IconButton(onClick = onToggleExpand, modifier = Modifier.size(22.dp)) {
                    Icon(
                        if (expanded) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.KeyboardArrowRight,
                        contentDescription = if (expanded) "收起子分类" else "展开子分类",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Spacer(Modifier.width(22.dp))
            }
            Icon(
                if (selected) Icons.Outlined.FolderOpen else Icons.Outlined.Folder,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(spacing.inlineGap))
            Text(
                notebook.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onMenu, modifier = Modifier.size(26.dp)) {
                Icon(
                    Icons.Rounded.MoreHoriz,
                    contentDescription = "笔记本操作",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 把 notebook 列表按父子关系展开成一维列表，孤儿节点兜底放到顶层。 */
fun flattenNotebooks(
    notebooks: List<NotebookEntity>,
    collapsed: Set<String> = emptySet(),
): List<Pair<NotebookEntity, Int>> {
    val byParent = notebooks.groupBy { it.parentServerId }
    val result = mutableListOf<Pair<NotebookEntity, Int>>()
    val visited = mutableSetOf<String>()

    fun visit(parentId: String, depth: Int) {
        byParent[parentId]?.sortedWith(compareBy({ it.seq }, { it.title }))?.forEach { nb ->
            if (!visited.add(nb.serverId)) return@forEach
            result.add(nb to depth)
            if (nb.serverId !in collapsed) visit(nb.serverId, depth + 1)
        }
    }
    visit("", 0)
    // 收起父级时子级仍归属父级，不能作为孤儿提升到顶层；只兜底真正的孤儿节点。
    val knownIds = notebooks.map { it.serverId }.toSet()
    notebooks.filter { it.serverId !in visited && it.parentServerId !in knownIds }
        .sortedWith(compareBy({ it.seq }, { it.title }))
        .forEach { result.add(it to 0) }
    return result
}
