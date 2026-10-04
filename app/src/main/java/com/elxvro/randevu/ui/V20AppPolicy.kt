package com.elxvro.randevu.ui

import com.elxvro.randevu.business.BusinessProfile
import com.elxvro.randevu.business.ServiceRecord
import com.elxvro.randevu.online.OnlineConnectionState
import com.elxvro.randevu.online.V2EntryDestination
import com.elxvro.randevu.online.V2EntryGate
import com.elxvro.randevu.online.V2SyncOutcome

object V20AppPolicy {
    const val importCopy = "Bu cihazdaki mevcut verileri online hesabına aktar."
    const val keepLegacyDataOnDismiss = true

    fun destination(hasToken: Boolean, profile: BusinessProfile, services: List<ServiceRecord>): V2EntryDestination =
        V2EntryGate.destination(hasToken, profile.setupCompleted, services.any { it.active })

    fun shouldOfferImport(authenticated: Boolean, hasLegacyData: Boolean, alreadyHandled: Boolean): Boolean =
        authenticated && hasLegacyData && !alreadyHandled

    fun canShowCloudSuccess(outcome: V2SyncOutcome): Boolean =
        outcome.state == OnlineConnectionState.ONLINE && outcome.pendingCount == 0
}
