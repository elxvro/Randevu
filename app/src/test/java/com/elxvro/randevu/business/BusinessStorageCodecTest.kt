package com.elxvro.randevu.business

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessStorageCodecTest {
    @Test fun `missing and corrupt profile decode as unconfigured`() {
        assertFalse(BusinessStorageCodec.decodeProfile(null).setupCompleted)
        assertFalse(BusinessStorageCodec.decodeProfile("{broken").setupCompleted)
    }

    @Test fun `empty services stay empty and malformed records are skipped`() {
        assertTrue(BusinessStorageCodec.decodeServices("[]").isEmpty())
        val decoded = BusinessStorageCodec.decodeServices("[{\"id\":\"ok\",\"name\":\"Bakım\",\"duration_minutes\":45,\"active\":true},{\"id\":\"bad\"}]")
        assertEquals(listOf("Bakım"), decoded.map { it.name })
    }

    @Test fun `profile and services round trip`() {
        val profile = BusinessProfile("Studio", "555", "Adres", "Europe/Istanbul", "Emre", true)
        val services = listOf(ServiceRecord("s1", "Bakım", 45, true))
        assertEquals(profile, BusinessStorageCodec.decodeProfile(BusinessStorageCodec.encodeProfile(profile)))
        assertEquals(services, BusinessStorageCodec.decodeServices(BusinessStorageCodec.encodeServices(services)))
    }
}
