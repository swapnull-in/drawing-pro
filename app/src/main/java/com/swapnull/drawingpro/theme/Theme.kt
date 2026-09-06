package com.swapnull.drawingpro.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val StudioDarkBackground = Color(0xFF101216)
val StudioDarkSurface = Color(0xEE1C1E24)
val StudioDarkSurfaceVariant = Color(0xFF282B34)
val StudioDarkBorder = Color(0xFF383C48)

val AccentCobalt = Color(0xFF635BFF)
val AccentCobaltContainer = Color(0xFF1E1B4B)
val AccentCoral = Color(0xFFFF5263)
val AccentCoralContainer = Color(0xFF4C0B12)
val AccentAmber = Color(0xFFFFB800)

val OnStudioDark = Color(0xFFF1F3F9)
val OnStudioDarkSecondary = Color(0xFF9EA3B5)

private val DarkColorScheme = darkColorScheme(
    primary = AccentCobalt,
    onPrimary = Color.White,
    primaryContainer = AccentCobaltContainer,
    onPrimaryContainer = Color(0xFFE0E0FF),
    secondary = AccentCoral,
    onSecondary = Color.White,
    secondaryContainer = AccentCoralContainer,
    onSecondaryContainer = Color(0xFFFFDAD9),
    tertiary = AccentAmber,
    background = StudioDarkBackground,
    onBackground = OnStudioDark,
    surface = StudioDarkSurface,
    onSurface = OnStudioDark,
    surfaceVariant = StudioDarkSurfaceVariant,
    onSurfaceVariant = OnStudioDarkSecondary,
    outline = StudioDarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = AccentCobalt,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEECFF),
    onPrimaryContainer = Color(0xFF120066),
    secondary = AccentCoral,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFEDEC),
    onSecondaryContainer = Color(0xFF410006),
    tertiary = AccentAmber,
    background = Color(0xFFF6F7FA),
    onBackground = Color(0xFF181A20),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF181A20),
    surfaceVariant = Color(0xFFECEFF5),
    onSurfaceVariant = Color(0xFF5B6072),
    outline = Color(0xFFD4D8E2)
)

@Composable
fun StudioDrawingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
