package com.elxvro.randevu.online

import org.junit.Assert.*
import org.junit.Test

class V2ApiContractTest {
    @Test fun `base url requires https and normalizes trailing slash`() {
        assertEquals("https://api.example.com", V2ApiContract.normalizeBaseUrl(" https://api.example.com/ "))
        assertEquals("", V2ApiContract.normalizeBaseUrl("http://api.example.com"))
        assertEquals("", V2ApiContract.normalizeBaseUrl("not a url"))
    }

    @Test fun `v2 paths are exact`() {
        assertEquals("/v2/auth/register-owner", V2ApiContract.registerPath)
        assertEquals("/v2/auth/login", V2ApiContract.loginPath)
        assertEquals("/v2/auth/logout", V2ApiContract.logoutPath)
        assertEquals("/v2/me", V2ApiContract.mePath)
        assertEquals("/v2/sync/bootstrap", V2ApiContract.bootstrapPath)
        assertEquals("/v2/appointments/apt%201", V2ApiContract.appointmentPath("apt 1"))
    }

    @Test fun `registration payload uses owner fields and never setup key`() {
        val json = V2ApiContract.registerJson(
            ownerName = "Emre",
            businessName = "Salon",
            email = "owner@example.com",
            password = "12345678",
            phone = "+905551112233",
            address = "Bursa",
            timezone = "Europe/Istanbul"
        )
        assertEquals("Emre", json.getString("owner_name"))
        assertEquals("Salon", json.getString("business_name"))
        assertEquals("owner@example.com", json.getString("email"))
        assertEquals("12345678", json.getString("password"))
        assertEquals("Europe/Istanbul", json.getString("timezone"))
        assertFalse(json.has("setup_key"))
    }

    @Test fun `login payload contains only email and password`() {
        val json = V2ApiContract.loginJson(" Owner@Example.com ", "secret123")
        assertEquals(setOf("email","password"), json.keys().asSequence().toSet())
        assertEquals("owner@example.com", json.getString("email"))
    }
}
