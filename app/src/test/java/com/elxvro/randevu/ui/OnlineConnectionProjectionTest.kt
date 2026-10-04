package com.elxvro.randevu.ui

import com.elxvro.randevu.online.OnlineConnectionState
import org.junit.Assert.assertEquals
import org.junit.Test

class OnlineConnectionProjectionTest {
    @Test fun `connection labels are exact turkish copy`() {
        assertEquals("Online",OnlineConnectionProjection.label(OnlineConnectionState.ONLINE,0))
        assertEquals("Senkronize ediliyor",OnlineConnectionProjection.label(OnlineConnectionState.SYNCING,2))
        assertEquals("Çevrimdışı — 3 işlem bekliyor",OnlineConnectionProjection.label(OnlineConnectionState.OFFLINE,3))
        assertEquals("Oturum gerekli",OnlineConnectionProjection.label(OnlineConnectionState.AUTH_REQUIRED,1))
        assertEquals("Çakışma var",OnlineConnectionProjection.label(OnlineConnectionState.CONFLICT,1))
    }
}
