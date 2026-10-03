package com.elxvro.randevu.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ReferenceLayoutContractTest {
    @Test
    fun compactSystemInsetContract_isStable() {
        assertEquals(56, ReferenceDesignContract.headerHeightDp)
        assertEquals(64, ReferenceDesignContract.bottomNavContentHeightDp)
    }
}
