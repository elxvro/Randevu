package com.elxvro.randevu.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReferenceDesignContractTest {
    @Test
    fun `bottom navigation is fixed to five reference tabs`() {
        assertEquals(
            listOf("Ana Sayfa", "Takvim", "Müşteriler", "Personel", "Daha Fazla"),
            ReferenceDesignContract.bottomTabs.map { it.label }
        )
        assertEquals(5, ReferenceDesignContract.bottomTabs.size)
    }

    @Test
    fun `reference system is dark only with cyan blue accent`() {
        assertFalse(ReferenceDesignContract.supportsLightTheme)
        assertEquals(0xFF06131CL, ReferenceDesignContract.backgroundArgb)
        assertEquals(0xFF00E6E6L, ReferenceDesignContract.cyanArgb)
        assertEquals(0xFF159CFCL, ReferenceDesignContract.blueArgb)
    }

    @Test
    fun `spacing and radii stay compact`() {
        assertEquals(16, ReferenceDesignContract.pageInsetDp)
        assertEquals(12, ReferenceDesignContract.cardGapDp)
        assertEquals(18, ReferenceDesignContract.cardRadiusDp)
        assertEquals(14, ReferenceDesignContract.controlRadiusDp)
        assertEquals(72, ReferenceDesignContract.bottomNavHeightDp)
        assertTrue(ReferenceDesignContract.cardRadiusDp < 24)
    }
}
