package com.leanote.next.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.leanote.next.ui.theme.ColorRole
import com.leanote.next.ui.theme.ColorRoles
import com.leanote.next.ui.theme.LocalCardCorner
import com.leanote.next.ui.theme.LocalUiSpacing
import com.leanote.next.ui.theme.colorForRole
import com.leanote.next.ui.theme.parseHexColor

private val QuickColors = listOf(
    Color(0xFF0F766E),
    Color(0xFF1D4ED8),
    Color(0xFF6D28D9),
    Color(0xFFBE185D),
    Color(0xFFC2410C),
    Color(0xFFA16207),
    Color(0xFF15803D),
    Color(0xFF0E7490),
    Color(0xFF334155),
    Color(0xFF64748B),
    Color(0xFF78716C),
    Color(0xFF111827),
    Color(0xFFE7E1D2),
    Color(0xFFFDFBF4),
    Color(0xFF8A8A8A),
    Color(0xFFFFFFFF),
)

@Composable
fun CustomColorsSection(
    customColors: Map<String, String>,
    onColorChange: (String, String?) -> Unit,
    onClearAll: () -> Unit,
) {
    var editingRole by remember { mutableStateOf<ColorRole?>(null) }
    // 默认收起，避免设置页一进来就被十几个颜色项占满。
    var expanded by rememberSaveable { mutableStateOf(false) }
    val spacing = LocalUiSpacing.current

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(LocalCardCorner.current.dp),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(
                horizontal = spacing.cardHorizontal,
                vertical = spacing.cardVertical,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("自定义配色", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (customColors.isEmpty()) {
                            "在预设或动态取色之上覆盖指定颜色"
                        } else {
                            "已自定义 ${customColors.size} 项颜色"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (expanded) {
                Spacer(Modifier.height(spacing.itemGap))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = onClearAll,
                        enabled = customColors.isNotEmpty(),
                    ) {
                        Text("全部恢复默认")
                    }
                }

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
                            .heightIn(min = 48.dp)
                            .padding(vertical = spacing.itemGap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(role.displayName, style = MaterialTheme.typography.bodyMedium)
                        if (customized) {
                            Spacer(Modifier.width(spacing.inlineGap))
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
    val spacing = LocalUiSpacing.current
    val startColor = parseHexColor(initialValue) ?: fallbackColor
    val startHsv = remember(role.key, initialValue) { startColor.toHsv() }
    var hue by remember(role.key, initialValue) { mutableStateOf(startHsv[0]) }
    var saturation by remember(role.key, initialValue) { mutableStateOf(startHsv[1]) }
    var value by remember(role.key, initialValue) { mutableStateOf(startHsv[2]) }
    var input by remember(role.key, initialValue) { mutableStateOf(initialValue) }
    val parsedColor = parseHexColor(input)
    val invalid = input.isNotBlank() && parsedColor == null
    val wheelColor = Color.hsv(hue, saturation, value)

    fun applyHsv(h: Float, s: Float, v: Float) {
        hue = h
        saturation = s
        value = v
        input = Color.hsv(h, s, v).toHexString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义${role.displayName}") },
        text = {
            Column(
                Modifier
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                // 实时预览 + 当前 HEX
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(parsedColor ?: wheelColor)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    )
                    Spacer(Modifier.width(spacing.inlineGap))
                    Column {
                        Text("实时预览", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            parsedColor?.toHexString() ?: wheelColor.toHexString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(spacing.contentVertical))
                // 颜色盘：外圈选色相，圆心方向表示浓度
                ColorWheel(
                    hue = hue,
                    saturation = saturation,
                    onPick = { h, s -> applyHsv(h, s, value) },
                )
                Spacer(Modifier.height(spacing.itemGap))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("浓度", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${(value * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Slider(
                    value = value,
                    onValueChange = { applyHsv(hue, saturation, it) },
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { text ->
                        input = text
                        parseHexColor(text)?.let { color ->
                            val hsv = color.toHsv()
                            hue = hsv[0]
                            saturation = hsv[1]
                            value = hsv[2]
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("十六进制颜色") },
                    placeholder = { Text("#0F766E") },
                    singleLine = true,
                    isError = invalid,
                    supportingText = {
                        Text(
                            if (invalid) "颜色格式不正确" else "支持 #RGB、#RRGGBB、#AARRGGBB",
                        )
                    },
                )
                Spacer(Modifier.height(spacing.itemGap))
                Text("常用颜色", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(spacing.itemGap))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.inlineGap),
                    verticalArrangement = Arrangement.spacedBy(spacing.itemGap),
                ) {
                    QuickColors.forEach { color ->
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                .clickable {
                                    val hsv = color.toHsv()
                                    applyHsv(hsv[0], hsv[1], hsv[2])
                                },
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
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.inlineGap),
                verticalArrangement = Arrangement.spacedBy(spacing.itemGap),
            ) {
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

@Composable
private fun ColorWheel(
    hue: Float,
    saturation: Float,
    onPick: (hue: Float, saturation: Float) -> Unit,
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset -> pickWheel(offset, size, onPick) }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> pickWheel(offset, size, onPick) },
                    onDrag = { change, _ ->
                        pickWheel(change.position, size, onPick)
                        change.consume()
                    },
                )
            },
    ) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val hueColors = listOf(
            Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red,
        )
        drawCircle(brush = Brush.sweepGradient(hueColors), radius = radius, center = center)
        // 圆心为白，越靠外浓度越高
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color.White.copy(alpha = 0f)),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.12f),
            radius = radius,
            center = center,
            style = Stroke(width = 1.dp.toPx()),
        )
        val angle = Math.toRadians(hue.toDouble())
        val knob = Offset(
            center.x + kotlin.math.cos(angle).toFloat() * saturation * radius,
            center.y + kotlin.math.sin(angle).toFloat() * saturation * radius,
        )
        drawCircle(Color.White, radius = 8.dp.toPx(), center = knob)
        drawCircle(
            Color.Black.copy(alpha = 0.55f),
            radius = 8.dp.toPx(),
            center = knob,
            style = Stroke(width = 1.5.dp.toPx()),
        )
    }
}

private fun pickWheel(
    offset: Offset,
    size: androidx.compose.ui.unit.IntSize,
    onPick: (Float, Float) -> Unit,
) {
    val radius = minOf(size.width, size.height) / 2f
    if (radius <= 0f) return
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val dx = offset.x - centerX
    val dy = offset.y - centerY
    val distance = kotlin.math.hypot(dx, dy)
    val degree = (Math.toDegrees(kotlin.math.atan2(dy, dx).toDouble()) + 360.0) % 360.0
    onPick(degree.toFloat(), (distance / radius).coerceIn(0f, 1f))
}

private fun Color.toHsv(): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)
    return hsv
}

private fun Color.toHexString(): String {
    val argb = toArgb()
    return if ((argb ushr 24) == 0xFF) {
        String.format("#%06X", argb and 0xFFFFFF)
    } else {
        String.format("#%08X", argb)
    }
}
