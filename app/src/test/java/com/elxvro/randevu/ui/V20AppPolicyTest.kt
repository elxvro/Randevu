package com.elxvro.randevu.ui

import com.elxvro.randevu.business.BusinessProfile
import com.elxvro.randevu.business.ServiceRecord
import com.elxvro.randevu.online.OnlineConnectionState
import com.elxvro.randevu.online.V2EntryDestination
import com.elxvro.randevu.online.V2SyncOutcome
import org.junit.Assert.*
import org.junit.Test

class V20AppPolicyTest {
    @Test fun `routing uses online session and cloud setup completeness`() {
        val empty=BusinessProfile.unconfigured("Europe/Istanbul")
        val complete=BusinessProfile("Salon","","","Europe/Istanbul","Emre",true)
        assertEquals(V2EntryDestination.AUTH,V20AppPolicy.destination(false,empty,emptyList()))
        assertEquals(V2EntryDestination.SETUP,V20AppPolicy.destination(true,empty,emptyList()))
        assertEquals(V2EntryDestination.SETUP,V20AppPolicy.destination(true,complete,emptyList()))
        assertEquals(V2EntryDestination.APP,V20AppPolicy.destination(true,complete,listOf(ServiceRecord("s","Kesim",30,true))))
    }

    @Test fun `legacy import offer is one time and exact copy`() {
        assertEquals("Bu cihazdaki mevcut verileri online hesabına aktar.",V20AppPolicy.importCopy)
        assertTrue(V20AppPolicy.shouldOfferImport(true,true,false))
        assertFalse(V20AppPolicy.shouldOfferImport(true,true,true))
        assertFalse(V20AppPolicy.shouldOfferImport(false,true,false))
    }

    @Test fun `dismiss import preserves local data and cloud success requires acknowledgement`() {
        assertTrue(V20AppPolicy.keepLegacyDataOnDismiss)
        assertFalse(V20AppPolicy.canShowCloudSuccess(V2SyncOutcome(OnlineConnectionState.OFFLINE,1)))
        assertFalse(V20AppPolicy.canShowCloudSuccess(V2SyncOutcome(OnlineConnectionState.SYNCING,1)))
        assertTrue(V20AppPolicy.canShowCloudSuccess(V2SyncOutcome(OnlineConnectionState.ONLINE,0)))
    }
}
