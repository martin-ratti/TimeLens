package com.timelens.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Dark theme - Primary palette (tonos azul/púrpura con acentos neón)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650A4)
val PurpleGrey40 = Color(0xFF625B71)
val Pink40 = Color(0xFF7D5260)

// Paleta modo oscuro (neón vibrante sobre fondo oscuro)
val DarkNeonBlue = Color(0xFF00D4FF)
val DarkNeonPurple = Color(0xFFBB86FC)
val DarkNeonGreen = Color(0xFF00E676)
val DarkNeonOrange = Color(0xFFFF9100)
val DarkNeonRed = Color(0xFFFF5252)
val DarkNeonCyan = Color(0xFF00E5FF)

// Paleta modo claro (alto contraste WCAG AA sobre fondos claros)
val LightNeonBlue = Color(0xFF0284C7)
val LightNeonPurple = Color(0xFF7C3AED)
val LightNeonGreen = Color(0xFF059669)
val LightNeonOrange = Color(0xFFD97706)
val LightNeonRed = Color(0xFFDC2626)
val LightNeonCyan = Color(0xFF0E7490)

val DarkBackground = Color(0xFF0D1117)
val DarkSurface = Color(0xFF161B22)
val DarkCard = Color(0xFF21262D)

@Immutable
data class TimeLensAccentPalette(
    val isDark: Boolean,
    val blue: Color,
    val purple: Color,
    val green: Color,
    val orange: Color,
    val red: Color,
    val cyan: Color
)

val DarkAccentPalette = TimeLensAccentPalette(
    isDark = true,
    blue = DarkNeonBlue,
    purple = DarkNeonPurple,
    green = DarkNeonGreen,
    orange = DarkNeonOrange,
    red = DarkNeonRed,
    cyan = DarkNeonCyan
)

val LightAccentPalette = TimeLensAccentPalette(
    isDark = false,
    blue = LightNeonBlue,
    purple = LightNeonPurple,
    green = LightNeonGreen,
    orange = LightNeonOrange,
    red = LightNeonRed,
    cyan = LightNeonCyan
)

val LocalTimeLensAccents = staticCompositionLocalOf<TimeLensAccentPalette?> { null }

@Composable
private fun currentAccents(): TimeLensAccentPalette =
    LocalTimeLensAccents.current ?: if (isSystemInDarkTheme()) DarkAccentPalette else LightAccentPalette

// Tokens adaptativos para modo claro y oscuro
val NeonBlue: Color
    @Composable get() = currentAccents().blue

val NeonPurple: Color
    @Composable get() = currentAccents().purple

val NeonGreen: Color
    @Composable get() = currentAccents().green

val NeonOrange: Color
    @Composable get() = currentAccents().orange

val NeonRed: Color
    @Composable get() = currentAccents().red

val NeonCyan: Color
    @Composable get() = currentAccents().cyan
