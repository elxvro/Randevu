package com.elxvro.randevu.ui

import com.elxvro.randevu.online.OnlineConnectionState

object OnlineConnectionProjection {
    fun label(state: OnlineConnectionState, pendingCount: Int): String = when (state) {
        OnlineConnectionState.ONLINE -> "Online"
        OnlineConnectionState.SYNCING -> "Senkronize ediliyor"
        OnlineConnectionState.OFFLINE -> "Çevrimdışı — $pendingCount işlem bekliyor"
        OnlineConnectionState.AUTH_REQUIRED -> "Oturum gerekli"
        OnlineConnectionState.CONFLICT -> "Çakışma var"
    }
}
