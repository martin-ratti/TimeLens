package com.timelens.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.timelens.app.domain.model.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = NeonBlue,
    secondary = NeonPurple,
    tertiary = NeonGreen,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkCard,
    onPrimary = DarkBackground,
    onSecondary = DarkBackground,
    onTertiary = DarkBackground,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF94A3B8)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    secondary = Color(0xFF7C3AED),
    tertiary = Color(0xFF059669),
    background = Color(0xFFF1F5F9),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF64748B)
)

@Composable
fun TimeLensTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun TimeLensTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val mode = if (darkTheme) ThemeMode.DARK else ThemeMode.LIGHT
    TimeLensTheme(themeMode = mode, content = content)
}
