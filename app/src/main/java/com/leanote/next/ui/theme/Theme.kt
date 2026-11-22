package com.leanote.next.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leanote.next.data.model.AppSettings
import com.leanote.next.data.model.SurfaceEffect
import com.leanote.next.data.model.ThemeMode
import com.leanote.next.data.model.UiDensity
import com.leanote.next.data.model.UiStyle

val ReadingFont: FontFamily = FontFamily.Serif
val MonoFont: FontFamily = FontFamily.Monospace

private val BaseTypography = Typography()

val LeanoteTypography = Typography(
    displayLarge = BaseTypography.displayLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp),
    displayMedium = BaseTypography.displayMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineLarge = BaseTypography.headlineLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    headlineMedium = BaseTypography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = BaseTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = BaseTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = BaseTypography.titleMedium.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 26.sp, letterSpacing = 0.2.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp, letterSpacing = 0.2.sp),
    labelLarge = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
)

val LeanoteShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private fun shapesFor(style: UiStyle): Shapes = when (style) {
    UiStyle.MATERIAL -> LeanoteShapes
    UiStyle.MIUIX -> Shapes(
        extraSmall = RoundedCornerShape(10.dp),
        small = RoundedCornerShape(16.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(26.dp),
        extraLarge = RoundedCornerShape(32.dp),
    )
    UiStyle.CUPERTINO -> Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(20.dp),
    )
    UiStyle.FLUENT -> Shapes(
        extraSmall = RoundedCornerShape(2.dp),
        small = RoundedCornerShape(4.dp),
        medium = RoundedCornerShape(6.dp),
        large = RoundedCornerShape(8.dp),
        extraLarge = RoundedCornerShape(10.dp),
    )
}

private fun typographyFor(style: UiStyle): Typography = when (style) {
    UiStyle.MATERIAL -> LeanoteTypography
    UiStyle.MIUIX -> LeanoteTypography.copy(
        headlineLarge = LeanoteTypography.headlineLarge.copy(fontSize = 34.sp, fontWeight = FontWeight.Bold),
        headlineMedium = LeanoteTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = LeanoteTypography.titleLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
        titleMedium = LeanoteTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = LeanoteTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
    UiStyle.CUPERTINO -> LeanoteTypography.copy(
        headlineLarge = LeanoteTypography.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
        titleLarge = LeanoteTypography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = LeanoteTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = LeanoteTypography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 25.sp, letterSpacing = 0.sp),
        bodyMedium = LeanoteTypography.bodyMedium.copy(letterSpacing = 0.sp),
        labelLarge = LeanoteTypography.labelLarge.copy(fontWeight = FontWeight.Normal),
    )
    UiStyle.FLUENT -> LeanoteTypography.copy(
        displayLarge = LeanoteTypography.displayLarge.copy(letterSpacing = 0.sp),
        headlineLarge = LeanoteTypography.headlineLarge.copy(letterSpacing = 0.sp),
        titleLarge = LeanoteTypography.titleLarge.copy(fontSize = 20.sp, letterSpacing = 0.sp),
        titleMedium = LeanoteTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
        bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 24.sp, fontFamily = FontFamily.SansSerif),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp, fontFamily = FontFamily.SansSerif),
        labelLarge = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
    )
}

/** 各界面风格在共享组件上的结构差异。 */
@Immutable
data class UiStyleTokens(
    val cardBorderWidth: Dp,
    val cardShadowElevation: Dp,
    val navigationLeadingIndicator: Boolean,
    val centerTopBarTitle: Boolean,
)

private val MaterialStyleTokens = UiStyleTokens(
    cardBorderWidth = 0.dp,
    cardShadowElevation = 1.dp,
    navigationLeadingIndicator = false,
    centerTopBarTitle = false,
)

private fun styleTokensFor(style: UiStyle): UiStyleTokens = when (style) {
    UiStyle.MATERIAL -> MaterialStyleTokens
    UiStyle.MIUIX -> UiStyleTokens(0.dp, 0.dp, false, false)
    UiStyle.CUPERTINO -> UiStyleTokens(0.5.dp, 0.dp, false, true)
    UiStyle.FLUENT -> UiStyleTokens(1.dp, 0.dp, true, false)
}

/** 应用内结构性间距；字体和图标尺寸由全局缩放单独控制。 */
@Immutable
data class UiSpacing(
    val pageHorizontal: Dp,
    val pageVertical: Dp,
    val cardHorizontal: Dp,
    val cardVertical: Dp,
    val contentVertical: Dp,
    val sectionGap: Dp,
    val itemGap: Dp,
    val inlineGap: Dp,
)

private val StandardSpacing = UiSpacing(
    pageHorizontal = 16.dp,
    pageVertical = 12.dp,
    cardHorizontal = 18.dp,
    cardVertical = 12.dp,
    contentVertical = 16.dp,
    sectionGap = 18.dp,
    itemGap = 10.dp,
    inlineGap = 8.dp,
)

private fun spacingFor(density: UiDensity): UiSpacing = when (density) {
    UiDensity.COMPACT -> UiSpacing(12.dp, 8.dp, 14.dp, 8.dp, 12.dp, 12.dp, 6.dp, 6.dp)
    UiDensity.STANDARD -> StandardSpacing
    UiDensity.COMFORTABLE -> UiSpacing(20.dp, 16.dp, 22.dp, 16.dp, 20.dp, 24.dp, 14.dp, 12.dp)
}

data class ColorRole(
    val key: String,
    val displayName: String,
)

object ColorRoles {
    const val PRIMARY = "primary"
    const val ON_PRIMARY = "onPrimary"
    const val BACKGROUND = "background"
    const val ON_BACKGROUND = "onBackground"
    const val SURFACE = "surface"
    const val ON_SURFACE = "onSurface"
    const val NOTE_CARD = "noteCard"
    const val SURFACE_VARIANT = "surfaceVariant"
    const val ON_SURFACE_VARIANT = "onSurfaceVariant"
    const val SECONDARY_CONTAINER = "secondaryContainer"
    const val OUTLINE = "outline"

    val all = listOf(
        ColorRole(PRIMARY, "主题色"),
        ColorRole(ON_PRIMARY, "主题色文字"),
        ColorRole(BACKGROUND, "背景色"),
        ColorRole(ON_BACKGROUND, "正文文字"),
        ColorRole(SURFACE, "页面表面"),
        ColorRole(NOTE_CARD, "笔记卡片"),
        ColorRole(ON_SURFACE, "卡片文字"),
        ColorRole(SURFACE_VARIANT, "辅助表面"),
        ColorRole(ON_SURFACE_VARIANT, "次要文字"),
        ColorRole(SECONDARY_CONTAINER, "次级容器"),
        ColorRole(OUTLINE, "边框"),
    )
}

fun ColorScheme.colorForRole(roleKey: String): Color = when (roleKey) {
    ColorRoles.PRIMARY -> primary
    ColorRoles.ON_PRIMARY -> onPrimary
    ColorRoles.BACKGROUND -> background
    ColorRoles.ON_BACKGROUND -> onBackground
    ColorRoles.SURFACE -> surface
    ColorRoles.NOTE_CARD -> surfaceContainerLowest
    ColorRoles.ON_SURFACE -> onSurface
    ColorRoles.SURFACE_VARIANT -> surfaceVariant
    ColorRoles.ON_SURFACE_VARIANT -> onSurfaceVariant
    ColorRoles.SECONDARY_CONTAINER -> secondaryContainer
    ColorRoles.OUTLINE -> outline
    else -> primary
}

fun parseHexColor(value: String): Color? {
    val hex = value.trim().removePrefix("#")
    if (hex.length != 3 && hex.length != 6 && hex.length != 8) return null
    if (hex.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) return null

    val argb = when (hex.length) {
        3 -> "FF" + hex.map { "$it$it" }.joinToString("")
        6 -> "FF$hex"
        else -> hex
    }
    return Color(
        alpha = argb.substring(0, 2).toInt(16),
        red = argb.substring(2, 4).toInt(16),
        green = argb.substring(4, 6).toInt(16),
        blue = argb.substring(6, 8).toInt(16),
    )
}

private fun ColorScheme.withCustomColors(customColors: Map<String, String>): ColorScheme {
    if (customColors.isEmpty()) return this

    val primary = customColors[ColorRoles.PRIMARY]?.let(::parseHexColor)
    val onPrimary = customColors[ColorRoles.ON_PRIMARY]?.let(::parseHexColor)
    val background = customColors[ColorRoles.BACKGROUND]?.let(::parseHexColor)
    val onBackground = customColors[ColorRoles.ON_BACKGROUND]?.let(::parseHexColor)
    val surface = customColors[ColorRoles.SURFACE]?.let(::parseHexColor)
    val noteCard = customColors[ColorRoles.NOTE_CARD]?.let(::parseHexColor)
    val onSurface = customColors[ColorRoles.ON_SURFACE]?.let(::parseHexColor)
    val surfaceVariant = customColors[ColorRoles.SURFACE_VARIANT]?.let(::parseHexColor)
    val onSurfaceVariant = customColors[ColorRoles.ON_SURFACE_VARIANT]?.let(::parseHexColor)
    val secondaryContainer = customColors[ColorRoles.SECONDARY_CONTAINER]?.let(::parseHexColor)
    val outline = customColors[ColorRoles.OUTLINE]?.let(::parseHexColor)

    return copy(
        primary = primary ?: this.primary,
        onPrimary = onPrimary ?: this.onPrimary,
        background = background ?: this.background,
        onBackground = onBackground ?: this.onBackground,
        surface = surface ?: this.surface,
        surfaceContainerLowest = noteCard ?: this.surfaceContainerLowest,
        onSurface = onSurface ?: this.onSurface,
        surfaceVariant = surfaceVariant ?: this.surfaceVariant,
        onSurfaceVariant = onSurfaceVariant ?: this.onSurfaceVariant,
        secondaryContainer = secondaryContainer ?: this.secondaryContainer,
        outline = outline ?: this.outline,
        surfaceTint = primary ?: this.surfaceTint,
    )
}

@Composable
fun LeanoteTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val baseScheme = when {
        settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> colorSchemeFor(settings.palette, dark)
    }
    val uiScale = settings.uiScale.coerceIn(85, 130)
    val scale = uiScale / 100f
    val baseDensity = LocalDensity.current
    // dp 与 sp 都会使用缩放后的 density；保留系统 fontScale，避免字体重复缩放。
    val scaledDensity = Density(
        density = baseDensity.density * scale,
        fontScale = baseDensity.fontScale,
    )
    CompositionLocalProvider(
        LocalDensity provides scaledDensity,
        LocalUiScale provides uiScale,
        LocalSurfaceEffect provides settings.surfaceEffect,
        LocalSurfaceTranslucency provides settings.surfaceTranslucency,
        LocalUiDensity provides settings.density,
        LocalUiSpacing provides spacingFor(settings.density),
        LocalUiStyle provides settings.uiStyle,
        LocalUiStyleTokens provides styleTokensFor(settings.uiStyle),
        LocalNoteCardCorner provides settings.noteCardCorner.coerceIn(0, 28),
        LocalNoteCardBorder provides settings.noteCardBorder,
    ) {
        MaterialTheme(
            colorScheme = baseScheme.withCustomColors(settings.customColors),
            typography = typographyFor(settings.uiStyle),
            shapes = shapesFor(settings.uiStyle),
            content = content,
        )
    }
}

/** 当前应用内缩放百分比，供 AndroidView 与 WebView 使用。 */
val LocalUiScale = staticCompositionLocalOf { 100 }

/** 当前界面材质效果，由主题统一提供。 */
val LocalSurfaceEffect = staticCompositionLocalOf { SurfaceEffect.NONE }

/** 当前材质透明强度（0–60），由主题统一提供。 */
val LocalSurfaceTranslucency = staticCompositionLocalOf { 20 }

/** 当前界面紧凑度，保留给预览行数等密度专属行为。 */
val LocalUiDensity = staticCompositionLocalOf { UiDensity.STANDARD }

/** 当前应用内语义化间距。 */
val LocalUiSpacing = staticCompositionLocalOf { StandardSpacing }

val LocalUiStyle = staticCompositionLocalOf { UiStyle.MATERIAL }
val LocalUiStyleTokens = staticCompositionLocalOf { MaterialStyleTokens }

/** 笔记卡片独立圆角（dp），由主题统一提供。 */
val LocalNoteCardCorner = staticCompositionLocalOf { 22 }

/** 笔记卡片是否显示边框，由主题统一提供。 */
val LocalNoteCardBorder = staticCompositionLocalOf { true }

/** 把透明强度换算成前景色 alpha：磨砂克制一些，亚克力更通透。 */
private fun SurfaceEffect.materialAlpha(translucency: Int): Float {
    val ratio = translucency.coerceIn(0, 60) / 100f
    return when (this) {
        SurfaceEffect.NONE -> 1f
        SurfaceEffect.FROSTED -> 1f - ratio * 0.5f
        SurfaceEffect.ACRYLIC -> 1f - ratio
    }
}

/** 顶栏、侧边栏等大面积容器的底色。 */
fun SurfaceEffect.barColor(scheme: ColorScheme, translucency: Int = 20): Color = when (this) {
    SurfaceEffect.NONE -> scheme.background
    else -> scheme.surface.copy(alpha = materialAlpha(translucency))
}

/** 卡片底色，半透明后可以透出页面渐变，形成磨砂/亚克力观感。 */
fun SurfaceEffect.cardColor(
    scheme: ColorScheme,
    base: Color = scheme.surfaceContainerLowest,
    translucency: Int = 20,
): Color = when (this) {
    SurfaceEffect.NONE -> base
    else -> base.copy(alpha = materialAlpha(translucency))
}

/** 页面背景渐变，材质效果关闭时保持纯色。 */
fun SurfaceEffect.pageBrush(scheme: ColorScheme): Brush = when (this) {
    SurfaceEffect.NONE -> Brush.verticalGradient(listOf(scheme.background, scheme.background))
    SurfaceEffect.FROSTED -> Brush.verticalGradient(
        listOf(
            scheme.background,
            scheme.primary.copy(alpha = 0.08f),
            scheme.secondaryContainer.copy(alpha = 0.14f),
            scheme.background,
        ),
    )
    SurfaceEffect.ACRYLIC -> Brush.linearGradient(
        listOf(
            scheme.primary.copy(alpha = 0.16f),
            scheme.background,
            scheme.secondaryContainer.copy(alpha = 0.22f),
            scheme.background,
        ),
    )
}

/** 亚克力材质叠加的高光渐变。 */
fun SurfaceEffect.sheenBrush(): Brush? = if (this == SurfaceEffect.ACRYLIC) {
    Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = 0.10f),
            Color.Transparent,
            Color.White.copy(alpha = 0.04f),
        ),
    )
} else {
    null
}
