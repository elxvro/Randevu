package com.elxvro.randevu.business

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessSetupEngineTest {
    private val profile = BusinessProfile("ELXVRO Studio", "+905551112233", "İstanbul", "Europe/Istanbul", "Emre", false)

    @Test fun `setup requires business name and active service`() {
        val service = ServiceRecord("s1", "Danışmanlık", 30, true)
        assertFalse(BusinessSetupEngine.canComplete(profile.copy(businessName = " "), listOf(service)))
        assertFalse(BusinessSetupEngine.canComplete(profile, emptyList()))
        assertFalse(BusinessSetupEngine.canComplete(profile, listOf(service.copy(active = false))))
        assertTrue(BusinessSetupEngine.canComplete(profile, listOf(service)))
    }

    @Test fun `only active services are exposed`() {
        val items = listOf(ServiceRecord("1", "A", 30, true), ServiceRecord("2", "B", 45, false))
        assertEquals(listOf("A"), BusinessSetupEngine.activeServices(items).map { it.name })
    }

    @Test fun `invalid timezone falls back safely`() {
        assertEquals("Europe/Istanbul", BusinessSetupEngine.validatedTimezone("Bad/Zone", "Europe/Istanbul"))
        assertEquals("UTC", BusinessSetupEngine.validatedTimezone("UTC", "Europe/Istanbul"))
    }
}
