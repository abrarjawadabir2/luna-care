package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = MutedRoseLight,
    secondary = LavenderLight,
    tertiary = WarmCoralLight,
    background = DarkPlumBackground,
    surface = DarkCardSurface,
    onPrimary = DarkPlumText,
    onSecondary = DarkPlumText,
    onTertiary = DarkPlumText,
    onBackground = SoftCreamBackground,
    onSurface = SoftCreamBackground,
    error = AlertRed
)

private val LightColorScheme = lightColorScheme(
    primary = MutedRosePrimary,
    secondary = LavenderSecondary,
    tertiary = WarmCoralTertiary,
    background = SoftCreamBackground,
    surface = SoftCardSurface,
    onPrimary = SoftCardSurface,
    onSecondary = SoftCardSurface,
    onTertiary = SoftCardSurface,
    onBackground = DarkPlumText,
    onSurface = DarkPlumText,
    error = AlertRed
)

@Composable
fun LunaCareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    LunaCareTheme(darkTheme = darkTheme, content = content)
}
