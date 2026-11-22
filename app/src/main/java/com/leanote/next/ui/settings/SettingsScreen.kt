package com.leanote.next.ui.settings

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leanote.next.BuildConfig
import com.leanote.next.data.model.Account
import com.leanote.next.data.model.SortMode
import com.leanote.next.data.model.SurfaceEffect
import com.leanote.next.data.model.ThemeMode
import com.leanote.next.data.model.ThemePalette
import com.leanote.next.data.model.UiDensity
import com.leanote.next.ui.components.AccountAvatar
import com.leanote.next.ui.components.ConfirmDialog
import com.leanote.next.ui.components.LeanoteTopAppBar
import com.leanote.next.ui.theme.CardHorizontalInset
import com.leanote.next.ui.theme.LocalCardCorner
import com.leanote.next.ui.theme.LocalSurfaceEffect
import com.leanote.next.ui.theme.LocalSurfaceTranslucency
import com.leanote.next.ui.theme.LocalUiSpacing
import com.leanote.next.ui.theme.cardColor
import com.leanote.next.ui.theme.pageBrush
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    onOpenDataTransfer: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val spacing = LocalUiSpacing.current
    val account by viewModel.account.collectAsStateWithLifecycle()
    val syncing by viewModel.syncing.collectAsStateWithLifecycle()
    val uploadingAvatar by viewModel.uploadingAvatar.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmLogout by remember { mutableStateOf(false) }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.uploadAvatar(it) }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }
    LaunchedEffect(Unit) {
        viewModel.loggedOut.collect { onLoggedOut() }
    }

    Scaffold(
        containerColor = if (LocalSurfaceEffect.current == SurfaceEffect.NONE) {
            MaterialTheme.colorScheme.background
        } else {
            Color.Transparent
        },
        modifier = Modifier.background(LocalSurfaceEffect.current.pageBrush(MaterialTheme.colorScheme)),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            LeanoteTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                title = { Text("设置") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (LocalSurfaceEffect.current == SurfaceEffect.NONE) {
                        MaterialTheme.colorScheme.background
                    } else {
                        Color.Transparent
                    },
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = CardHorizontalInset,
                vertical = spacing.pageVertical,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.itemGap),
        ) {
            account?.let { current ->
                item {
                    AccountCard(
                        account = current,
                        syncing = syncing,
                        uploadingAvatar = uploadingAvatar,
                        onSync = viewModel::syncNow,
                        onPickAvatar = { avatarPicker.launch("image/*") },
                    )
                }
            }

            item { SectionTitle("外观") }
            item {
                SettingsCard {
                    SettingLabel("主题模式", "深色模式可减少夜间阅读的眩光")
                    Spacer(Modifier.height(spacing.itemGap))
                    SettingChoiceChips(
                        options = ThemeMode.entries.map { mode ->
                            mode to when (mode) {
                                ThemeMode.SYSTEM -> "跟随系统"
                                ThemeMode.LIGHT -> "浅色"
                                ThemeMode.DARK -> "深色"
                            }
                        },
                        selected = settings.themeMode,
                        onSelect = viewModel::setThemeMode,
                    )
                    Spacer(Modifier.height(spacing.contentVertical))
                    SettingLabel("配色方案", "深色模式使用柔和的深灰背景，不再是纯黑")
                    Spacer(Modifier.height(spacing.itemGap))
                    ThemePaletteChips(
                        selected = settings.palette,
                        enabled = !settings.dynamicColor,
                        onSelect = viewModel::setPalette,
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Spacer(Modifier.height(spacing.itemGap))
                        ToggleRow(
                            title = "动态取色",
                            subtitle = "从系统壁纸中提取主题色",
                            checked = settings.dynamicColor,
                            onCheckedChange = viewModel::setDynamicColor,
                        )
                    }
                    Spacer(Modifier.height(spacing.contentVertical))
                    SettingLabel("界面材质", "磨砂与亚克力会让顶栏、卡片呈现半透明质感")
                    Spacer(Modifier.height(spacing.itemGap))
                    SettingChoiceChips(
                        options = listOf(
                            SurfaceEffect.NONE to "无",
                            SurfaceEffect.FROSTED to "磨砂",
                            SurfaceEffect.ACRYLIC to "亚克力",
                        ),
                        selected = settings.surfaceEffect,
                        onSelect = viewModel::setSurfaceEffect,
                    )
                    if (settings.surfaceEffect != SurfaceEffect.NONE) {
                        Spacer(Modifier.height(spacing.contentVertical))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 40.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("透明强度", style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.weight(1f))
                            Text(
                                "${settings.surfaceTranslucency}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Slider(
                            value = settings.surfaceTranslucency.toFloat(),
                            onValueChange = { viewModel.setSurfaceTranslucency(it.roundToInt()) },
                            valueRange = 0f..60f,
                            steps = 11,
                        )
                        Text(
                            "数值越大越通透；抽屉与顶栏会垫一层页面底色，不会透出背后的笔记列表",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(spacing.contentVertical))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            SettingLabel("全局缩放", "同步调整应用内界面和字体大小")
                        }
                        Spacer(Modifier.width(spacing.itemGap))
                        Text(
                            "${settings.uiScale}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Slider(
                        value = settings.uiScale.toFloat(),
                        onValueChange = { viewModel.setUiScale(it.roundToInt()) },
                        valueRange = 85f..130f,
                        steps = 8,
                    )
                    Spacer(Modifier.height(spacing.contentVertical))
                    SettingLabel("界面紧凑度", "同步调整笔记列表、侧边栏、设置及其他页面的间距")
                    Spacer(Modifier.height(spacing.itemGap))
                    SettingChoiceChips(
                        options = listOf(
                            UiDensity.COMPACT to "紧凑",
                            UiDensity.STANDARD to "标准",
                            UiDensity.COMFORTABLE to "宽松",
                        ),
                        selected = settings.density,
                        onSelect = viewModel::setDensity,
                    )
                    Spacer(Modifier.height(spacing.contentVertical))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            SettingLabel("卡片圆角", "统一调整笔记卡片与各页面卡片的圆角")
                        }
                        Spacer(Modifier.width(spacing.itemGap))
                        Text(
                            "${settings.noteCardCorner} dp",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Slider(
                        value = settings.noteCardCorner.toFloat(),
                        onValueChange = { viewModel.setNoteCardCorner(it.roundToInt()) },
                        valueRange = 0f..28f,
                        steps = 13,
                    )
                    Spacer(Modifier.height(spacing.itemGap))
                    ToggleRow(
                        title = "显示卡片边框",
                        subtitle = "颜色跟随自定义配色中的「边框」",
                        checked = settings.noteCardBorder,
                        onCheckedChange = viewModel::setNoteCardBorder,
                    )
                }
            }

            item {
                CustomColorsSection(
                    customColors = settings.customColors,
                    onColorChange = viewModel::setCustomColor,
                    onClearAll = viewModel::clearCustomColors,
                )
            }

            item { SectionTitle("笔记") }
            item {
                SettingsCard {
                    ToggleRow(
                        title = "新建默认使用 Markdown",
                        subtitle = "关闭后新建笔记为富文本格式",
                        checked = settings.defaultMarkdown,
                        onCheckedChange = viewModel::setDefaultMarkdown,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = spacing.itemGap),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                    SettingLabel("默认排序", "笔记列表的排列方式")
                    Spacer(Modifier.height(spacing.itemGap))
                    SettingChoiceChips(
                        options = listOf(
                            SortMode.UPDATED_DESC to "最近更新",
                            SortMode.CREATED_DESC to "最近创建",
                            SortMode.TITLE_ASC to "标题",
                        ),
                        selected = settings.sortMode,
                        onSelect = viewModel::setSortMode,
                    )
                }
            }

            item { SectionTitle("数据") }
            item {
                SettingsCard {
                    SettingLabel("备份与导入导出", "本地备份/恢复全部笔记与设置，支持 Markdown、HTML、ENEX 格式")
                    Spacer(Modifier.height(spacing.contentVertical))
                    OutlinedButton(
                        onClick = onOpenDataTransfer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp),
                    ) {
                        Text("打开数据管理")
                    }
                }
            }

            item { SectionTitle("同步") }
            item {
                SettingsCard {
                    ToggleRow(
                        title = "自动同步",
                        subtitle = "每 6 小时在后台同步一次，仅在连接网络时进行",
                        checked = settings.autoSync,
                        onCheckedChange = viewModel::setAutoSync,
                    )
                }
            }

            item { SectionTitle("关于") }
            item {
                SettingsCard {
                    SettingLabel("Leanote Next", "Material 3 风格的蚂蚁笔记客户端")
                    Spacer(Modifier.height(spacing.inlineGap))
                    Text(
                        text = "版本 ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(spacing.contentVertical))
                    OutlinedButton(
                        onClick = { confirmLogout = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp),
                    ) {
                        Icon(Icons.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(spacing.inlineGap))
                        Text("退出当前账户", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (confirmLogout) {
        ConfirmDialog(
            title = "退出登录？",
            text = "本地缓存的笔记将从设备中移除，服务器上的笔记不受影响。",
            confirmText = "退出登录",
            destructive = true,
            onConfirm = {
                confirmLogout = false
                viewModel.logout()
            },
            onDismiss = { confirmLogout = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> SettingChoiceChips(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    val spacing = LocalUiSpacing.current
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.inlineGap),
        verticalArrangement = Arrangement.spacedBy(spacing.inlineGap),
    ) {
        options.forEach { (value, label) ->
            AssistChip(
                onClick = { onSelect(value) },
                label = { Text(label) },
                shape = CircleShape,
                border = null,
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (selected == value) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                ),
                modifier = Modifier.heightIn(min = 40.dp),
            )
        }
    }
}

@Composable
private fun ThemePaletteChips(
    selected: ThemePalette,
    enabled: Boolean,
    onSelect: (ThemePalette) -> Unit,
) {
    val spacing = LocalUiSpacing.current
    val items = listOf(
        ThemePalette.FOREST to ("森野" to Color(0xFF0F766E)),
        ThemePalette.OCEAN to ("海洋" to Color(0xFF1D4ED8)),
        ThemePalette.LAVENDER to ("薰衣草" to Color(0xFF6D28D9)),
        ThemePalette.SUNSET to ("陶土" to Color(0xFFC2410C)),
        ThemePalette.GRAPHITE to ("石墨" to Color(0xFF334155)),
        ThemePalette.CREAM_PAPER to ("暖砂" to Color(0xFFA16207)),
        ThemePalette.MONOCHROME to ("黑白" to Color(0xFF111827)),
        ThemePalette.NEON to ("莓果" to Color(0xFFBE185D)),
    )
    // 固定 4 列 × 2 行；每行以最高项为准，让四张配色卡等高。
    Column(verticalArrangement = Arrangement.spacedBy(spacing.inlineGap)) {
        items.chunked(4).forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.inlineGap),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
            ) {
                rowItems.forEach { (palette, pair) ->
                    val (label, color) = pair
                    val containerColor = if (selected == palette) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                    Surface(
                        color = containerColor.copy(alpha = if (enabled) 1f else 0.38f),
                        contentColor = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = if (enabled) 1f else 0.38f,
                        ),
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .heightIn(min = 40.dp)
                            .clip(CircleShape)
                            .clickable(enabled = enabled) { onSelect(palette) },
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(color.copy(alpha = if (enabled) 1f else 0.38f)),
                            )
                            Spacer(Modifier.height(spacing.inlineGap / 2))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center,
                                maxLines = 3,
                                overflow = TextOverflow.Clip,
                                softWrap = true,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountCard(
    account: Account,
    syncing: Boolean,
    uploadingAvatar: Boolean,
    onSync: () -> Unit,
    onPickAvatar: () -> Unit,
) {
    val spacing = LocalUiSpacing.current
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        shape = RoundedCornerShape(LocalCardCorner.current.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = spacing.cardHorizontal,
                vertical = spacing.cardVertical,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(enabled = !uploadingAvatar, onClick = onPickAvatar),
            ) {
                AccountAvatar(account, size = 44.dp)
                if (uploadingAvatar) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier.size(18.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(4.dp),
                            strokeWidth = 1.5.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier.size(16.dp),
                    ) {
                        Icon(
                            Icons.Rounded.PhotoCamera,
                            contentDescription = "更换头像",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(3.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.width(spacing.itemGap))
            Column(Modifier.weight(1f)) {
                Text(
                    account.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    account.email,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(spacing.inlineGap))
            OutlinedButton(
                onClick = onSync,
                enabled = !syncing,
                modifier = Modifier.heightIn(min = 40.dp),
                contentPadding = PaddingValues(
                    horizontal = spacing.itemGap,
                    vertical = spacing.inlineGap,
                ),
            ) {
                if (syncing) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(spacing.inlineGap))
                Text(if (syncing) "同步中" else "立即同步", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    val effect = LocalSurfaceEffect.current
    val spacing = LocalUiSpacing.current
    val shape = RoundedCornerShape(LocalCardCorner.current.dp)
    Surface(
        color = effect.cardColor(
            MaterialTheme.colorScheme,
            MaterialTheme.colorScheme.surfaceContainerLow,
            LocalSurfaceTranslucency.current,
        ),
        shape = shape,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(
                horizontal = spacing.cardHorizontal,
                vertical = spacing.contentVertical,
            ),
            content = content,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    val spacing = LocalUiSpacing.current
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            start = spacing.inlineGap,
            top = spacing.sectionGap - spacing.itemGap,
        ),
    )
}

@Composable
private fun SettingLabel(title: String, subtitle: String) {
    val spacing = LocalUiSpacing.current
    Text(title, style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(spacing.inlineGap / 4))
    Text(
        subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val spacing = LocalUiSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            SettingLabel(title, subtitle)
        }
        Spacer(Modifier.width(spacing.itemGap))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
