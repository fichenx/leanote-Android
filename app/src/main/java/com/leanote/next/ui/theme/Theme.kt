package com.leanote.next.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
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
    background = Color(0xFF0F1413),
    onBackground = Color(0xFFE1E3E0),
    surface = Color(0xFF0F1413),
    onSurface = Color(0xFFE1E3E0),
    surfaceVariant = Color(0xFF3F4946),
    onSurfaceVariant = Color(0xFFBFC9C5),
    outline = Color(0xFF889390),
    outlineVariant = Color(0xFF3F4946),
    surfaceTint = TealLight,
    inverseSurface = Color(0xFFE1E3E0),
    inverseOnSurface = Color(0xFF2E3130),
    inversePrimary = Teal,
    surfaceContainerLowest = Color(0xFF0A0F0E),
    surfaceContainerLow = Color(0xFF171C1B),
    surfaceContainer = Color(0xFF1B2120),
    surfaceContainerHigh = Color(0xFF262B2A),
    surfaceContainerHighest = Color(0xFF303635),
)

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
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = LeanoteTypography,
        shapes = LeanoteShapes,
        content = content,
    )
}
