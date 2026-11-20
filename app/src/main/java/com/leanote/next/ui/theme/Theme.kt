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
        typography = LeanoteTypography,
        shapes = LeanoteShapes,
        content = content,
    )
}
