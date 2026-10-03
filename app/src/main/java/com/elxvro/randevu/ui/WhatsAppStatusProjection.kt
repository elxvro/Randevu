package com.elxvro.randevu.ui

import com.elxvro.randevu.core.WhatsAppConnectionState

object WhatsAppStatusProjection {
    fun label(state: WhatsAppConnectionState, pendingCount: Int): String {
        if (pendingCount > 0) return "Bekleyen senkronizasyon"
        return when (state) {
            WhatsAppConnectionState.CONNECTED -> "Bağlı"
            WhatsAppConnectionState.INCOMPLETE -> "Eksik kurulum"
            WhatsAppConnectionState.UNREACHABLE -> "Sunucuya ulaşılamıyor"
            WhatsAppConnectionState.DISABLED -> "Kapalı"
        }
    }

    fun canClaimScheduled(backendAcknowledged: Boolean, scheduledCount: Int): Boolean =
        backendAcknowledged && scheduledCount > 0
}
