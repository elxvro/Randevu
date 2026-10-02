package com.elxvro.randevu.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val RefBackground = Color(ReferenceDesignContract.backgroundArgb.toULong())
val RefSurface = Color(ReferenceDesignContract.surfaceArgb.toULong())
val RefSurfaceRaised = Color(ReferenceDesignContract.surfaceRaisedArgb.toULong())
val RefCyan = Color(ReferenceDesignContract.cyanArgb.toULong())
val RefBlue = Color(ReferenceDesignContract.blueArgb.toULong())
val RefText = Color(ReferenceDesignContract.textPrimaryArgb.toULong())
val RefTextMuted = Color(ReferenceDesignContract.textSecondaryArgb.toULong())
val RefBorder = Color(ReferenceDesignContract.borderArgb.toULong())
val RefSuccess = Color(ReferenceDesignContract.successArgb.toULong())
val RefWarning = Color(ReferenceDesignContract.warningArgb.toULong())
val RefDanger = Color(ReferenceDesignContract.dangerArgb.toULong())
val RefDisabled = Color(0xFF607784)

private val ReferenceColors = darkColorScheme(
    primary = RefCyan,
    onPrimary = Color(0xFF001D22),
    primaryContainer = Color(0xFF0B3542),
    onPrimaryContainer = RefText,
    secondary = RefBlue,
    onSecondary = Color(0xFF001B2A),
    secondaryContainer = Color(0xFF0D2B3D),
    onSecondaryContainer = RefText,
    background = RefBackground,
    onBackground = RefText,
    surface = RefSurface,
    onSurface = RefText,
    surfaceVariant = RefSurfaceRaised,
    onSurfaceVariant = RefTextMuted,
    outline = RefBorder,
    error = RefDanger,
    onError = Color.White
)

private val ReferenceTypography = Typography(
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 28.sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp),
    bodyLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp),
    bodySmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal, lineHeight = 15.sp),
    labelLarge = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp),
    labelMedium = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, lineHeight = 14.sp),
    labelSmall = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, lineHeight = 12.sp)
)

@Composable
fun ReferenceRandevuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ReferenceColors,
        typography = ReferenceTypography,
        content = content
    )
}
