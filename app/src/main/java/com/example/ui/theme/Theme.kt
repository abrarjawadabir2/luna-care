package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MutedRoseLight,
    onPrimary = Color(0xFF552D2E),
    primaryContainer = Color(0xFF6F383A),
    onPrimaryContainer = Color(0xFFFFDADB),
    secondary = LavenderLight,
    onSecondary = Color(0xFF353255),
    secondaryContainer = Color(0xFF444164),
    onSecondaryContainer = Color(0xFFE6E1FF),
    tertiary = WarmCoralLight,
    onTertiary = Color(0xFF543021),
    tertiaryContainer = Color(0xFF6F3928),
    onTertiaryContainer = Color(0xFFFFDACC),
    background = DarkPlumBackground,
    onBackground = Color(0xFFF4EDEE),
    surface = DarkCardSurface,
    onSurface = Color(0xFFF4EDEE),
    surfaceVariant = Color(0xFF4F4143),
    onSurfaceVariant = Color(0xFFE0C8CA),
    outline = Color(0xFFAD9293),
    outlineVariant = Color(0xFF665052),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = MutedRosePrimary,
    onPrimary = SoftCardSurface,
    primaryContainer = Color(0xFFF3D5D4),
    onPrimaryContainer = Color(0xFF472425),
    secondary = LavenderSecondary,
    onSecondary = SoftCardSurface,
    secondaryContainer = Color(0xFFE5E1FF),
    onSecondaryContainer = Color(0xFF29264D),
    tertiary = WarmCoralTertiary,
    onTertiary = SoftCardSurface,
    tertiaryContainer = Color(0xFFFFDACC),
    onTertiaryContainer = Color(0xFF482719),
    background = SoftCreamBackground,
    onBackground = DarkPlumText,
    surface = SoftCardSurface,
    onSurface = DarkPlumText,
    surfaceVariant = Color(0xFFF9E9E8),
    onSurfaceVariant = TextMuted,
    outline = Color(0xFF8A7374),
    outlineVariant = Color(0xFFDDC6C5),
    error = AlertRed,
    onError = SoftCardSurface,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

@Composable
fun LunaCareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}

// Retained for existing activities that call the legacy theme name.
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    LunaCareTheme(darkTheme = darkTheme, content = content)
}
