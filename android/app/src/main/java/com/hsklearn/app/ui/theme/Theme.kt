package com.hsklearn.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val HskDarkColorScheme = darkColorScheme(
    primary = GoldAccent,
    secondary = GoldAccent,
    tertiary = GoldAccent,
    background = DarkNavy,
    surface = Surface,
    onPrimary = DarkNavy,
    onSecondary = DarkNavy,
    onTertiary = DarkNavy,
    onBackground = OnSurface,
    onSurface = OnSurface,
    error = ErrorColor,
    onError = DarkNavy,
    surfaceVariant = Surface,
    onSurfaceVariant = OnSurface.copy(alpha = 0.7f),
)

@Composable
fun HskLearnTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HskDarkColorScheme,
        typography = HskTypography,
        content = content,
    )
}
