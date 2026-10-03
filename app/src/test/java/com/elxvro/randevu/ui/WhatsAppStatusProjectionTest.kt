package com.elxvro.randevu.ui

import com.elxvro.randevu.core.WhatsAppConnectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppStatusProjectionTest {
    @Test fun `labels match approved Turkish states`() {
        assertEquals("Bağlı", WhatsAppStatusProjection.label(WhatsAppConnectionState.CONNECTED, 0))
        assertEquals("Eksik kurulum", WhatsAppStatusProjection.label(WhatsAppConnectionState.INCOMPLETE, 0))
        assertEquals("Sunucuya ulaşılamıyor", WhatsAppStatusProjection.label(WhatsAppConnectionState.UNREACHABLE, 0))
        assertEquals("Kapalı", WhatsAppStatusProjection.label(WhatsAppConnectionState.DISABLED, 0))
        assertEquals("Bekleyen senkronizasyon", WhatsAppStatusProjection.label(WhatsAppConnectionState.CONNECTED, 2))
    }

    @Test fun `scheduled claim requires backend acknowledgement`() {
        assertTrue(WhatsAppStatusProjection.canClaimScheduled(backendAcknowledged = true, scheduledCount = 1))
        assertFalse(WhatsAppStatusProjection.canClaimScheduled(backendAcknowledged = false, scheduledCount = 1))
        assertFalse(WhatsAppStatusProjection.canClaimScheduled(backendAcknowledged = true, scheduledCount = 0))
    }
}
