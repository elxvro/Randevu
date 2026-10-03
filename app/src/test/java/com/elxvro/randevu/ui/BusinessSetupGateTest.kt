package com.elxvro.randevu.ui

import com.elxvro.randevu.business.BusinessProfile
import com.elxvro.randevu.business.ServiceRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class BusinessSetupGateTest {
    @Test fun `unconfigured or incomplete business stays in setup`() {
        val empty = BusinessProfile.unconfigured("Europe/Istanbul")
        assertEquals(SetupDestination.SETUP, BusinessSetupGate.destination(empty, emptyList()))
        val completeFlag = empty.copy(businessName = "Studio", ownerName = "Emre", setupCompleted = true)
        assertEquals(SetupDestination.SETUP, BusinessSetupGate.destination(completeFlag, emptyList()))
    }

    @Test fun `completed business with active service enters app`() {
        val profile = BusinessProfile("Studio", "555", "Adres", "Europe/Istanbul", "Emre", true)
        val service = ServiceRecord("s1", "Bakım", 30, true)
        assertEquals(SetupDestination.APP, BusinessSetupGate.destination(profile, listOf(service)))
    }
}
