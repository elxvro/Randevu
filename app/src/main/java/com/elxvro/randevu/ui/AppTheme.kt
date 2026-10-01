package com.elxvro.randevu.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
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

private val RandevuColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F0FF),
    onPrimaryContainer = BrandBlueDark,
    secondary = Color(0xFF6C5CE7),
    background = AppBackground,
    surface = Color.White,
    onSurface = TextPrimary,
    outline = DividerColor
)

@Composable
fun RandevuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RandevuColors,
        typography = Typography(),
        content = content
    )
}
