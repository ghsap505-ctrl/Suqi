package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val SouqiLightColors = lightColorScheme(
    primary = SouqiTeal,
    onPrimary = Color.White,
    primaryContainer = SouqiTealContainer,
    onPrimaryContainer = SouqiOnTealContainer,
    secondary = SouqiAmber,
    onSecondary = Color(0xFF231600),
    secondaryContainer = SouqiAmberContainer,
    onSecondaryContainer = SouqiOnAmberContainer,
    tertiary = SouqiEmerald,
    onTertiary = Color.White,
    tertiaryContainer = SouqiEmeraldContainer,
    onTertiaryContainer = SouqiOnEmeraldContainer,
    error = SouqiTerracotta,
    onError = Color.White,
    errorContainer = SouqiTerracottaContainer,
    onErrorContainer = SouqiOnTerracottaContainer,
    background = SandBackground,
    onBackground = InkPrimary,
    surface = SandSurface,
    onSurface = InkPrimary,
    surfaceVariant = SandSurfaceVariant,
    onSurfaceVariant = InkSecondary,
    outline = SandOutline,
)

private val SouqiDarkColors = darkColorScheme(
    primary = Color(0xFF6BC7DC),
    onPrimary = Color(0xFF003642),
    primaryContainer = SouqiTeal,
    onPrimaryContainer = SouqiTealContainer,
    secondary = SouqiAmber,
    onSecondary = Color(0xFF2B1C00),
    secondaryContainer = Color(0xFF563A00),
    onSecondaryContainer = SouqiAmberContainer,
    tertiary = Color(0xFF6DD9AD),
    onTertiary = Color(0xFF003825),
    tertiaryContainer = SouqiEmerald,
    onTertiaryContainer = SouqiEmeraldContainer,
    error = Color(0xFFFFB4A6),
    onError = Color(0xFF690F02),
    background = NightBackground,
    onBackground = NightTextPrimary,
    surface = NightSurface,
    onSurface = NightTextPrimary,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightTextSecondary,
    outline = NightOutline,
)

val SouqiShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun SouqiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) SouqiDarkColors else SouqiLightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = SouqiTypography,
        shapes = SouqiShapes,
        content = content,
    )
}
