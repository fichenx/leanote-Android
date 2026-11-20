package com.leanote.next.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leanote.next.data.model.AppSettings
import com.leanote.next.data.model.ThemeMode
import com.leanote.next.data.model.ThemePalette
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontSynthesis

private val Ink = Color(0xFF191C1B)
private val Paper = Color(0xFFFBFAF6)
private val Teal = Color(0xFF0B6B5D)
private val TealLight = Color(0xFF98D5C9)

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB4F1E5),
    onPrimaryContainer = Color(0xFF002A24),
    secondary = Color(0xFF4B635C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDE8DE),
    onSecondaryContainer = Color(0xFF082019),
    tertiary = Color(0xFF8A5A00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDF9C),
    onTertiaryContainer = Color(0xFF2C1A00),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Color(0xFFDBE5E1),
    onSurfaceVariant = Color(0xFF3F4946),
    outline = Color(0xFF6F7976),
    outlineVariant = Color(0xFFBFC9C5),
    surfaceTint = Teal,
    inverseSurface = Color(0xFF2E3130),
    inverseOnSurface = Color(0xFFF0F1ED),
    inversePrimary = TealLight,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F4F0),
    surfaceContainer = Color(0xFFEFEEE9),
    surfaceContainerHigh = Color(0xFFE9E8E3),
    surfaceContainerHighest = Color(0xFFE3E2DD),
)

private val DarkColors = darkColorScheme(
    primary = TealLight,
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF005047),
    onPrimaryContainer = Color(0xFFB4F1E5),
    secondary = Color(0xFFB1CCC2),
    onSecondary = Color(0xFF1D352E),
    secondaryContainer = Color(0xFF334B44),
    onSecondaryContainer = Color(0xFFCDE8DE),
    tertiary = Color(0xFFF2BE5C),
    onTertiary = Color(0xFF472A00),
    tertiaryContainer = Color(0xFF663E00),
    onTertiaryContainer = Color(0xFFFFDF9C),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF161A19),
    onBackground = Color(0xFFE3E6E3),
    surface = Color(0xFF161A19),
    onSurface = Color(0xFFE3E6E3),
    surfaceVariant = Color(0xFF3C4542),
    onSurfaceVariant = Color(0xFFC2CCC8),
    outline = Color(0xFF8A9490),
    outlineVariant = Color(0xFF46504D),
    surfaceTint = TealLight,
    inverseSurface = Color(0xFFE1E3E0),
    inverseOnSurface = Color(0xFF2E3130),
    inversePrimary = Teal,
    surfaceContainerLowest = Color(0xFF101413),
    surfaceContainerLow = Color(0xFF1B201F),
    surfaceContainer = Color(0xFF202624),
    surfaceContainerHigh = Color(0xFF282E2C),
    surfaceContainerHighest = Color(0xFF303735),
)

private data class PaletteColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
)

private fun paletteColors(palette: ThemePalette, dark: Boolean): PaletteColors {
    if (dark) {
        return when (palette) {
            ThemePalette.FOREST -> PaletteColors(
                Color(0xFF98D5C9), Color(0xFF003731), Color(0xFF005047), Color(0xFFB4F1E5),
                Color(0xFFB1CCC2), Color(0xFF1D352E), Color(0xFF334B44), Color(0xFFCDE8DE),
                Color(0xFFF2BE5C), Color(0xFF472A00), Color(0xFF663E00), Color(0xFFFFDF9C),
            )
            ThemePalette.OCEAN -> PaletteColors(
                Color(0xFFA9C7FF), Color(0xFF00325C), Color(0xFF00497F), Color(0xFFD6E3FF),
                Color(0xFFBAC8DA), Color(0xFF243141), Color(0xFF3B4858), Color(0xFFD6E3F7),
                Color(0xFF80D4DA), Color(0xFF00363A), Color(0xFF004F53), Color(0xFF9CF1F5),
            )
            ThemePalette.LAVENDER -> PaletteColors(
                Color(0xFFD4BBFF), Color(0xFF3F1E6F), Color(0xFF563787), Color(0xFFEBDCFF),
                Color(0xFFCDC2DB), Color(0xFF342D42), Color(0xFF4B4358), Color(0xFFE9DEF8),
                Color(0xFFFFB0C8), Color(0xFF4A1526), Color(0xFF633B48), Color(0xFFFFD8E4),
            )
            ThemePalette.SUNSET -> PaletteColors(
                Color(0xFFFFB693), Color(0xFF5C1900), Color(0xFF7E2F08), Color(0xFFFFDBCB),
                Color(0xFFE7BEB0), Color(0xFF442A20), Color(0xFF5D4036), Color(0xFFFFDBCB),
                Color(0xFFDBC64A), Color(0xFF383000), Color(0xFF504600), Color(0xFFF8E364),
            )
            ThemePalette.GRAPHITE -> PaletteColors(
                Color(0xFFB5CCC5), Color(0xFF20342F), Color(0xFF364B45), Color(0xFFD1E7E0),
                Color(0xFFB3CCC4), Color(0xFF25332F), Color(0xFF354B44), Color(0xFFCFE9E1),
                Color(0xFFD8C58A), Color(0xFF3A2F00), Color(0xFF51461B), Color(0xFFF4E3A7),
            )
            ThemePalette.CREAM_PAPER -> PaletteColors(
                Color(0xFFD8C58A), Color(0xFF3A2F00), Color(0xFF51461B), Color(0xFFF4E3A7),
                Color(0xFFBEB095), Color(0xFF31281A), Color(0xFF463B2B), Color(0xFFE9DDC6),
                Color(0xFFCF9E6D), Color(0xFF3B1E00), Color(0xFF5F350A), Color(0xFFFFD8B0),
            )
            ThemePalette.MONOCHROME -> PaletteColors(
                Color(0xFFE0E0E0), Color(0xFF1C1C1C), Color(0xFF3C3C3C), Color(0xFFF4F4F4),
                Color(0xFFCACACA), Color(0xFF2B2B2B), Color(0xFF484848), Color(0xFFDDDDDD),
                Color(0xFFB0B0B0), Color(0xFF1E1E1E), Color(0xFF3F3F3F), Color(0xFFE5E5E5),
            )
            ThemePalette.NEON -> PaletteColors(
                Color(0xFF00FF9D), Color(0xFF00210F), Color(0xFF003A1D), Color(0xFF5DFFC0),
                Color(0xFF4ADE80), Color(0xFF00210F), Color(0xFF003A1D), Color(0xFFA7F3D0),
                Color(0xFFFF4DC4), Color(0xFF3C0029), Color(0xFF5C003D), Color(0xFFFFB4DE),
            )
        }
    }
    return when (palette) {
        ThemePalette.FOREST -> PaletteColors(
            Teal, Color.White, Color(0xFFB4F1E5), Color(0xFF002A24),
            Color(0xFF4B635C), Color.White, Color(0xFFCDE8DE), Color(0xFF082019),
            Color(0xFF8A5A00), Color.White, Color(0xFFFFDF9C), Color(0xFF2C1A00),
        )
        ThemePalette.OCEAN -> PaletteColors(
            Color(0xFF1565C0), Color.White, Color(0xFFD6E3FF), Color(0xFF001B3D),
            Color(0xFF4F6072), Color.White, Color(0xFFD6E3F7), Color(0xFF0B1B29),
            Color(0xFF00696E), Color.White, Color(0xFF9CF1F5), Color(0xFF002021),
        )
        ThemePalette.LAVENDER -> PaletteColors(
            Color(0xFF6D4FA3), Color.White, Color(0xFFEBDCFF), Color(0xFF290055),
            Color(0xFF635B71), Color.White, Color(0xFFE9DEF8), Color(0xFF1F182C),
            Color(0xFF7D5260), Color.White, Color(0xFFFFD8E4), Color(0xFF31111D),
        )
        ThemePalette.SUNSET -> PaletteColors(
            Color(0xFFB04E22), Color.White, Color(0xFFFFDBCB), Color(0xFF3B0900),
            Color(0xFF77574A), Color.White, Color(0xFFFFDBCB), Color(0xFF2C160D),
            Color(0xFF6A5F00), Color.White, Color(0xFFF8E364), Color(0xFF201C00),
        )
        ThemePalette.GRAPHITE -> PaletteColors(
            Color(0xFF4E635E), Color.White, Color(0xFFD1E7E0), Color(0xFF0A1F1B),
            Color(0xFF4B635C), Color.White, Color(0xFFCFE9E1), Color(0xFF082019),
            Color(0xFF6B5D2F), Color.White, Color(0xFFF4E3A7), Color(0xFF211B00),
        )
        ThemePalette.CREAM_PAPER -> PaletteColors(
            Color(0xFF8A5A00), Color.White, Color(0xFFFFDF9C), Color(0xFF2C1A00),
            Color(0xFF7A6A47), Color.White, Color(0xFFF5E7C7), Color(0xFF251E0F),
            Color(0xFF5E3A14), Color.White, Color(0xFFE8C8A0), Color(0xFF1D0F00),
        )
        ThemePalette.MONOCHROME -> PaletteColors(
            Color(0xFF1A1A1A), Color.White, Color(0xFFE0E0E0), Color(0xFF000000),
            Color(0xFF4A4A4A), Color.White, Color(0xFFEAEAEA), Color(0xFF1A1A1A),
            Color(0xFF666666), Color.White, Color(0xFFF0F0F0), Color(0xFF222222),
        )
        ThemePalette.NEON -> PaletteColors(
            Color(0xFF00B56B), Color.White, Color(0xFF00FF9D), Color(0xFF00210F),
            Color(0xFF1F6B4A), Color.White, Color(0xFFA7F3D0), Color(0xFF00210F),
            Color(0xFFC2007E), Color.White, Color(0xFFFF4DC4), Color(0xFF3C0029),
        )
    }
}

private fun colorSchemeFor(palette: ThemePalette, dark: Boolean): ColorScheme {
    val base = if (dark) DarkColors else LightColors
    val colors = paletteColors(palette, dark)
    return base.copy(
        primary = colors.primary,
        onPrimary = colors.onPrimary,
        primaryContainer = colors.primaryContainer,
        onPrimaryContainer = colors.onPrimaryContainer,
        secondary = colors.secondary,
        onSecondary = colors.onSecondary,
        secondaryContainer = colors.secondaryContainer,
        onSecondaryContainer = colors.onSecondaryContainer,
        tertiary = colors.tertiary,
        onTertiary = colors.onTertiary,
        tertiaryContainer = colors.tertiaryContainer,
        onTertiaryContainer = colors.onTertiaryContainer,
        surfaceTint = colors.primary,
        inversePrimary = colors.primary,
    )
}

val ReadingFont: FontFamily = FontFamily.Serif
val MonoFont: FontFamily = FontFamily.Monospace

/** 每种配色自带的视觉风格（形状 + 字体预设）。 */
private fun shapesFor(palette: ThemePalette): Shapes = when (palette) {
    ThemePalette.CREAM_PAPER -> Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(10.dp),
        large = RoundedCornerShape(12.dp),
        extraLarge = RoundedCornerShape(16.dp),
    )
    ThemePalette.MONOCHROME -> Shapes(
        extraSmall = RoundedCornerShape(0.dp),
        small = RoundedCornerShape(0.dp),
        medium = RoundedCornerShape(0.dp),
        large = RoundedCornerShape(0.dp),
        extraLarge = RoundedCornerShape(0.dp),
    )
    ThemePalette.NEON -> Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(6.dp),
        medium = RoundedCornerShape(8.dp),
        large = RoundedCornerShape(12.dp),
        extraLarge = RoundedCornerShape(16.dp),
    )
    else -> LeanoteShapes
}

private fun typographyFor(palette: ThemePalette): Typography = when (palette) {
    ThemePalette.CREAM_PAPER -> {
        val serif = FontFamily.Serif
        LeanoteTypography.copy(
            displayLarge = LeanoteTypography.displayLarge.withFamily(serif),
            displayMedium = LeanoteTypography.displayMedium.withFamily(serif),
            headlineLarge = LeanoteTypography.headlineLarge.withFamily(serif).copy(fontWeight = FontWeight.Bold),
            headlineMedium = LeanoteTypography.headlineMedium.withFamily(serif).copy(fontWeight = FontWeight.Bold),
            headlineSmall = LeanoteTypography.headlineSmall.withFamily(serif),
            titleLarge = LeanoteTypography.titleLarge.withFamily(serif),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 26.sp, fontFamily = serif),
            bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp, fontFamily = serif),
        )
    }
    ThemePalette.MONOCHROME -> LeanoteTypography.copy(
        titleLarge = LeanoteTypography.titleLarge.copy(fontWeight = FontWeight.Black, letterSpacing = (-1.5).sp),
        titleMedium = LeanoteTypography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
        bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 24.sp, letterSpacing = 0.3.sp),
        bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 20.sp, letterSpacing = 0.3.sp),
    )
    ThemePalette.NEON -> LeanoteTypography.copy(
        titleLarge = LeanoteTypography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp),
        bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 24.sp, fontFamily = FontFamily.Monospace),
    )
    else -> LeanoteTypography
}

private fun TextStyle.withFamily(family: FontFamily): TextStyle = copy(
    fontFamily = family,
    fontSynthesis = FontSynthesis.All,
)

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

@Composable
fun LeanoteTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val scheme = when {
        settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> colorSchemeFor(settings.palette, dark)
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = typographyFor(settings.palette),
        shapes = shapesFor(settings.palette),
        content = content,
    )
}
