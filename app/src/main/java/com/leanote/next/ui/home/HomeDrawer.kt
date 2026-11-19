package com.leanote.next.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.leanote.next.BuildConfig
import com.leanote.next.data.local.NotebookEntity
import com.leanote.next.data.model.Account
import com.leanote.next.ui.components.AccountAvatar
import com.leanote.next.ui.components.SectionHeader

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
    onSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 22.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (account != null) {
                AccountAvatar(account, size = 46.dp)
                Spacer(Modifier.width(12.dp))
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

        NavigationDrawerItem(
            label = { Text("全部笔记") },
            icon = { Icon(Icons.Rounded.Notes, contentDescription = null) },
            selected = !trashSelected && selectedNotebookId == null && selectedTag == null,
            onClick = onSelectAll,
            modifier = Modifier.padding(horizontal = 12.dp),
            shape = MaterialTheme.shapes.medium,
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            ),
        )
        NavigationDrawerItem(
            label = { Text("回收站") },
            icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
            selected = trashSelected,
            onClick = onShowTrash,
            modifier = Modifier.padding(horizontal = 12.dp),
            shape = MaterialTheme.shapes.medium,
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            ),
        )

        Spacer(Modifier.height(10.dp))
        SectionHeader("笔记本") {
            IconButton(onClick = onAddNotebook, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Rounded.Add, contentDescription = "新建笔记本", modifier = Modifier.size(18.dp))
            }
        }

        val tree = flattenNotebooks(notebooks)
        if (tree.isEmpty()) {
            Text(
                "还没有笔记本",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp),
            )
        }
        tree.forEach { (notebook, depth) ->
            NotebookRow(
                notebook = notebook,
                depth = depth,
                selected = !trashSelected && selectedNotebookId == notebook.serverId,
                onClick = { onSelectNotebook(notebook.serverId) },
                onMenu = { onNotebookMenu(notebook) },
            )
        }

        if (tags.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            SectionHeader("标签")
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tags.forEach { tag ->
                    val active = selectedTag == tag
                    AssistChip(
                        onClick = { onSelectTag(if (active) null else tag) },
                        label = { Text(tag, style = MaterialTheme.typography.labelMedium) },
                        shape = CircleShape,
                        border = AssistChipDefaults.assistChipBorder(
                            enabled = true,
                            borderColor = if (active) MaterialTheme.colorScheme.primary
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

        Spacer(Modifier.height(18.dp))
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSettings)
                .padding(horizontal = 22.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(14.dp))
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
private fun NotebookRow(
    notebook: NotebookEntity,
    depth: Int,
    selected: Boolean,
    onClick: () -> Unit,
    onMenu: () -> Unit,
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        else MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 1.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(
                start = 12.dp + (depth * 16).dp,
                end = 4.dp,
                top = 9.dp,
                bottom = 9.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (selected) Icons.Outlined.FolderOpen else Icons.Outlined.Folder,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                notebook.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onMenu, modifier = Modifier.size(30.dp)) {
                Icon(
                    Icons.Rounded.MoreHoriz,
                    contentDescription = "笔记本操作",
                    modifier = Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 把 notebook 列表按父子关系展开成一维列表，孤儿节点兜底放到顶层。 */
fun flattenNotebooks(notebooks: List<NotebookEntity>): List<Pair<NotebookEntity, Int>> {
    val byParent = notebooks.groupBy { it.parentServerId }
    val result = mutableListOf<Pair<NotebookEntity, Int>>()
    val visited = mutableSetOf<String>()

    fun visit(parentId: String, depth: Int) {
        byParent[parentId]?.sortedWith(compareBy({ it.seq }, { it.title }))?.forEach { nb ->
            if (!visited.add(nb.serverId)) return@forEach
            result.add(nb to depth)
            visit(nb.serverId, depth + 1)
        }
    }
    visit("", 0)
    notebooks.filter { it.serverId !in visited }
        .sortedWith(compareBy({ it.seq }, { it.title }))
        .forEach { result.add(it to 0) }
    return result
}
