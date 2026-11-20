package com.leanote.next.ui.settings

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.leanote.next.ui.theme.ColorRole
import com.leanote.next.ui.theme.ColorRoles
import com.leanote.next.ui.theme.colorForRole
import com.leanote.next.ui.theme.parseHexColor

private val QuickColors = listOf(
    Color(0xFF0B6B5D),
    Color(0xFF1565C0),
    Color(0xFF6D4FA3),
    Color(0xFFB04E22),
    Color(0xFFC2185B),
    Color(0xFFD32F2F),
    Color(0xFF2E7D32),
    Color(0xFF00838F),
    Color(0xFFF9A825),
    Color(0xFF795548),
    Color(0xFF546E7A),
    Color(0xFF1A1A1A),
    Color(0xFF757575),
    Color(0xFFE0E0E0),
    Color(0xFFFBFAF6),
    Color(0xFFFFFFFF),
)

@Composable
fun CustomColorsSection(
    customColors: Map<String, String>,
    onColorChange: (String, String?) -> Unit,
    onClearAll: () -> Unit,
) {
    var editingRole by remember { mutableStateOf<ColorRole?>(null) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("自定义配色", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "在预设或动态取色之上覆盖指定颜色",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = onClearAll,
                    enabled = customColors.isNotEmpty(),
                ) {
                    Text("全部恢复默认")
                }
            }
            Spacer(Modifier.height(8.dp))

            ColorRoles.all.forEachIndexed { index, role ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
                val customized = customColors.containsKey(role.key)
                val color = customColors[role.key]
                    ?.let(::parseHexColor)
                    ?: MaterialTheme.colorScheme.colorForRole(role.key)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editingRole = role }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(role.displayName, style = MaterialTheme.typography.bodyMedium)
                    if (customized) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = MaterialTheme.shapes.extraSmall,
                        ) {
                            Text(
                                "已自定义",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    )
                }
            }
        }
    }

    editingRole?.let { role ->
        val currentColor = MaterialTheme.colorScheme.colorForRole(role.key)
        val initialValue = customColors[role.key]
            ?.takeIf { parseHexColor(it) != null }
            ?: currentColor.toHexString()
        CustomColorDialog(
            role = role,
            initialValue = initialValue,
            fallbackColor = currentColor,
            onConfirm = { hex ->
                onColorChange(role.key, hex)
                editingRole = null
            },
            onDismiss = { editingRole = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomColorDialog(
    role: ColorRole,
    initialValue: String,
    fallbackColor: Color,
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember(role.key, initialValue) { mutableStateOf(initialValue) }
    val parsedColor = parseHexColor(input)
    val invalid = input.isNotBlank() && parsedColor == null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义${role.displayName}") },
        text = {
            Column {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("十六进制颜色") },
                    placeholder = { Text("#0B6B5D") },
                    singleLine = true,
                    isError = invalid,
                    supportingText = {
                        Text(
                            if (invalid) {
                                "颜色格式不正确"
                            } else {
                                "支持 #RGB、#RRGGBB、#AARRGGBB"
                            },
                        )
                    },
                )
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(parsedColor ?: fallbackColor)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("实时预览", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            parsedColor?.toHexString() ?: "请输入有效颜色",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("常用颜色", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    QuickColors.forEach { color ->
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                .clickable { input = color.toHexString() },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsedColor?.let { onConfirm(it.toHexString()) } },
                enabled = parsedColor != null,
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onConfirm(null) }) {
                    Text("恢复默认")
                }
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        },
    )
}

private fun Color.toHexString(): String {
    val argb = toArgb()
    return if ((argb ushr 24) == 0xFF) {
        String.format("#%06X", argb and 0xFFFFFF)
    } else {
        String.format("#%08X", argb)
    }
}
