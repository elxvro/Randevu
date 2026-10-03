package com.elxvro.randevu.network

import com.elxvro.randevu.business.BusinessProfile
import com.elxvro.randevu.core.Appointment
import com.elxvro.randevu.core.AppointmentStatus
import com.elxvro.randevu.core.WhatsAppConnectionState
import com.elxvro.randevu.core.WhatsAppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppApiContractTest {
    private val appointment = Appointment("apt 1", "Ayşe", "+905551112233", "Bakım", "Deniz", "2026-10-05", "10:00", AppointmentStatus.CONFIRMED, "")
    private val profile = BusinessProfile("Örnek İşletme", "+905551234567", "İstanbul", "Europe/Istanbul", "Emre", true)

    @Test fun `base url must be https and trailing slash is removed`() {
        assertEquals("https://api.example.com", WhatsAppApiContract.normalizeBaseUrl("https://api.example.com/"))
        assertEquals("", WhatsAppApiContract.normalizeBaseUrl("http://api.example.com"))
    }

    @Test fun `appointment endpoint encodes id`() {
        assertEquals("/appointments/apt%201/reminders", WhatsAppApiContract.reminderPath(appointment.id))
    }

    @Test fun `appointment payload uses backend field names and machine status`() {
        val json = WhatsAppApiContract.appointmentJson(appointment)
        assertEquals("Ayşe", json.getString("customer_name"))
        assertEquals("+905551112233", json.getString("customer_phone"))
        assertEquals("confirmed", json.getString("status"))
    }

    @Test fun `bootstrap payload includes business profile but not meta secrets`() {
        val json = WhatsAppApiContract.bootstrapJson("setup-key", profile)
        assertEquals("setup-key", json.getString("setup_key"))
        assertEquals("Örnek İşletme", json.getString("business_name"))
        assertEquals("Europe/Istanbul", json.getString("timezone"))
        assertFalse(json.has("meta_access_token"))
    }

    @Test fun `settings payload never contains meta access token`() {
        val json = WhatsAppApiContract.settingsJson(WhatsAppSettings(enabled = true, metaPhoneNumberId = "123"))
        assertTrue(json.getBoolean("enabled"))
        assertEquals("123", json.getString("meta_phone_number_id"))
        assertFalse(json.has("meta_access_token"))
    }

    @Test fun `status parser maps server states`() {
        assertEquals(WhatsAppConnectionState.CONNECTED, WhatsAppApiContract.parseConnectionState("connected"))
        assertEquals(WhatsAppConnectionState.INCOMPLETE, WhatsAppApiContract.parseConnectionState("incomplete"))
        assertEquals(WhatsAppConnectionState.UNREACHABLE, WhatsAppApiContract.parseConnectionState("unreachable"))
        assertEquals(WhatsAppConnectionState.DISABLED, WhatsAppApiContract.parseConnectionState("disabled"))
    }
}
