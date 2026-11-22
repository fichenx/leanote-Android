package com.leanote.next.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.leanote.next.data.local.NoteEntity
import com.leanote.next.data.model.Account
import com.leanote.next.data.model.UiDensity
import com.leanote.next.data.repo.ImageRepository
import com.leanote.next.ui.components.NoteMetaLine
import com.leanote.next.ui.components.TagRow
import com.leanote.next.ui.components.notePreview
import com.leanote.next.ui.theme.LocalCardCorner
import com.leanote.next.ui.theme.LocalNoteCardBorder
import com.leanote.next.ui.theme.LocalSurfaceEffect
import com.leanote.next.ui.theme.LocalSurfaceTranslucency
import com.leanote.next.ui.theme.LocalUiDensity
import com.leanote.next.ui.theme.LocalUiSpacing
import com.leanote.next.ui.theme.cardColor
import com.leanote.next.ui.theme.sheenBrush
import dagger.hilt.android.EntryPointAccessors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: NoteEntity,
    notebookName: String?,
    account: Account?,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onTrash: () -> Unit,
    onRestore: (() -> Unit)? = null,
    onDeleteForever: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val images = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            ThumbnailEntryPoint::class.java,
        ).imageRepository()
    }
    val thumbnailGeneration by images.thumbnailGeneration.collectAsStateWithLifecycle()
    var thumbnails by remember(note.localId, note.accountId, thumbnailGeneration, account?.token) {
        mutableStateOf(images.peekThumbnailModels(note, account).orEmpty())
    }
    var thumbIndex by remember(note.localId) { mutableStateOf(0) }

    LaunchedEffect(note.localId, note.accountId, thumbnailGeneration, account?.token) {
        if (images.peekThumbnailModels(note, account) == null) {
            thumbnails = runCatching { images.thumbnailModels(note, account) }
                .getOrDefault(emptyList())
        }
        thumbIndex = 0
    }

    val effect = LocalSurfaceEffect.current
    val translucency = LocalSurfaceTranslucency.current
    val density = LocalUiDensity.current
    val spacing = LocalUiSpacing.current
    val cardShape = RoundedCornerShape(LocalCardCorner.current.dp)
    val showBorder = LocalNoteCardBorder.current
    val container = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        // 跟随「笔记卡片」自定义颜色，并叠加材质效果的半透明。
        effect.cardColor(MaterialTheme.colorScheme, translucency = translucency)
    }
    // 边框颜色跟随自定义配色里的「边框」，可在设置中关闭。
    val border = when {
        selected -> androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
        )
        showBorder -> androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
        )
        else -> null
    }
    val previewLines = when (density) {
        UiDensity.COMPACT -> 1
        UiDensity.STANDARD -> 2
        UiDensity.COMFORTABLE -> 3
    }
    val thumbnailSize = when (density) {
        UiDensity.COMPACT -> 46.dp
        UiDensity.STANDARD -> 54.dp
        UiDensity.COMFORTABLE -> 62.dp
    }
    val thumbnailModel = thumbnails.getOrNull(thumbIndex)
    val thumbnailRequest = remember(thumbnailModel, note.accountId, note.localId, thumbnailGeneration) {
        thumbnailModel?.let { model ->
            val cacheKey = "${note.accountId}:${note.localId}:$thumbnailGeneration:$model"
            ImageRequest.Builder(context)
                .data(model)
                .memoryCacheKey(cacheKey)
                .diskCacheKey(cacheKey)
                .build()
        }
    }

    Box(modifier) {
        Surface(
            color = container,
            shape = cardShape,
            shadowElevation = if (selected) 0.dp else 1.dp,
            border = border,
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .combinedClickable(
                    onClick = { if (selectionMode) onToggleSelect() else onClick() },
                    onLongClick = onToggleSelect,
                ),
        ) {
            Box {
                Column(
                    Modifier.padding(
                        start = spacing.cardHorizontal,
                        end = spacing.inlineGap,
                        top = spacing.cardVertical,
                        bottom = spacing.cardVertical + 2.dp,
                    ),
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = if (note.title.isBlank()) "无标题笔记" else note.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(spacing.itemGap / 2))
                            Text(
                                text = notePreview(note),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = previewLines,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            if (selectionMode) {
                                androidx.compose.material3.Checkbox(
                                    checked = selected,
                                    onCheckedChange = { onToggleSelect() },
                                )
                            } else {
                            Box {
                                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(30.dp)) {
                                    Icon(
                                        Icons.Rounded.MoreHoriz,
                                        contentDescription = "更多",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                    DropdownMenuItem(text = { Text("编辑") }, onClick = {
                                        menuOpen = false
                                        onEdit()
                                    })
                                    if (onRestore != null) {
                                        DropdownMenuItem(text = { Text("恢复到笔记本") }, onClick = {
                                            menuOpen = false
                                            onRestore()
                                        })
                                    }
                                    if (onDeleteForever != null) {
                                        DropdownMenuItem(text = { Text("永久删除") }, onClick = {
                                            menuOpen = false
                                            onDeleteForever()
                                        })
                                    } else {
                                        DropdownMenuItem(text = { Text("移至回收站") }, onClick = {
                                            menuOpen = false
                                            onTrash()
                                        })
                                    }
                                }
                            }
                            }
                            if (thumbnailModel != null) {
                                Spacer(Modifier.height(2.dp))
                                AsyncImage(
                                    model = thumbnailRequest,
                                    contentDescription = null,
                                    onError = {
                                        // 图片损坏或地址失效时自动尝试下一张。
                                        thumbIndex += 1
                                    },
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                        .size(thumbnailSize)
                                        .clip(MaterialTheme.shapes.medium)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(spacing.itemGap))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NoteMetaLine(note, notebookName)
                        Spacer(Modifier.weight(1f))
                        if (note.isBlog) {
                            Icon(
                                Icons.Outlined.Public,
                                contentDescription = "已发布到博客",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier
                                    .size(13.dp)
                                    .padding(end = 2.dp),
                            )
                        }
                        if (note.isLocalOnly || note.isDirty) {
                            Icon(
                                Icons.Rounded.CloudOff,
                                contentDescription = "未同步",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .size(13.dp),
                            )
                        }
                        if (note.isMarkdown) {
                            Box(
                                modifier = Modifier
                                    .padding(start = spacing.inlineGap)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                            ) {
                                Text(
                                    "MD",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }

                    if (note.tagList.isNotEmpty()) {
                        Spacer(Modifier.height(spacing.itemGap + 2.dp))
                        TagRow(note.tagList.reversed(), max = 3)
                    }
                }
                // 亚克力材质：叠加一层很淡的高光，让卡片有玻璃质感。
                effect.sheenBrush()?.let { brush ->
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(brush),
                    )
                }
                }
        }
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface ThumbnailEntryPoint {
    fun imageRepository(): ImageRepository
}
