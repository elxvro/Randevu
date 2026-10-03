package com.elxvro.randevu.ui

import com.elxvro.randevu.business.BusinessProfile
import com.elxvro.randevu.business.BusinessSetupEngine
import com.elxvro.randevu.business.ServiceRecord

enum class SetupDestination { SETUP, APP }

object BusinessSetupGate {
    fun destination(profile: BusinessProfile, services: List<ServiceRecord>): SetupDestination =
        if (profile.setupCompleted && BusinessSetupEngine.canComplete(profile, services)) SetupDestination.APP else SetupDestination.SETUP
}
