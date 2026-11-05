package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = PureBlack,
    primaryContainer = DarkSurfaceElevated,
    onPrimaryContainer = PureWhite,
    secondary = PureWhite,
    onSecondary = PureBlack,
    secondaryContainer = DarkSurface,
    onSecondaryContainer = PureWhite,
    background = PureBlack,
    onBackground = PureWhite,
    surface = DarkSurface,
    onSurface = PureWhite,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextMutedDark,
    outline = DarkBorder,
    outlineVariant = DarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = PureBlack,
    onPrimary = PureWhite,
    primaryContainer = LightSurfaceElevated,
    onPrimaryContainer = PureBlack,
    secondary = PureBlack,
    onSecondary = PureWhite,
    secondaryContainer = LightSurface,
    onSecondaryContainer = PureBlack,
    background = PureWhite,
    onBackground = PureBlack,
    surface = LightSurface,
    onSurface = PureBlack,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = TextMutedLight,
    outline = LightBorder,
    outlineVariant = LightBorder
)

@Composable
fun MonovoxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // We intentionally enforce the high-contrast monochrome design system
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
