package com.elxvro.randevu.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReferenceThemeColorTest {
    @Test
    fun referenceThemeColors_areValidOpaqueSrgbColors() {
        val colors = listOf(
            RefBackground,
            RefSurface,
            RefSurfaceRaised,
            RefCyan,
            RefBlue,
            RefText,
            RefTextMuted,
            RefBorder,
            RefSuccess,
            RefWarning,
            RefDanger
        )

        colors.forEach { color ->
            assertTrue("Reference color must use a valid sRGB color space", color.colorSpace.isSrgb)
            assertEquals("Reference color must be opaque", 1f, color.alpha, 0.001f)
        }
    }
}
