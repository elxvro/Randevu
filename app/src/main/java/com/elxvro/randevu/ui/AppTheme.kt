package com.elxvro.randevu.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BrandBlue = Color(0xFF1769FF)
val BrandBlueDark = Color(0xFF0D47B5)
val AppBackground = Color(0xFFF6F8FC)
val TextPrimary = Color(0xFF111827)
val TextSecondary = Color(0xFF657085)
val DividerColor = Color(0xFFE7EBF2)
val SuccessGreen = Color(0xFF23B26D)
val WarningOrange = Color(0xFFFFA51F)
val DangerRed = Color(0xFFFF4D4F)

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F0FF),
    onPrimaryContainer = BrandBlueDark,
    secondary = Color(0xFF6C5CE7),
    background = AppBackground,
    surface = Color.White,
    onSurface = TextPrimary,
    onBackground = TextPrimary,
    outline = DividerColor
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF80AFFF),
    onPrimary = Color(0xFF002F6C),
    primaryContainer = Color(0xFF0E3E85),
    onPrimaryContainer = Color(0xFFD8E6FF),
    secondary = Color(0xFFB9AEFF),
    background = Color(0xFF0B1220),
    surface = Color(0xFF121B2C),
    onSurface = Color(0xFFF0F4FA),
    onBackground = Color(0xFFF0F4FA),
    outline = Color(0xFF344258)
)

@Composable
fun RandevuTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        content = content
    )
}
