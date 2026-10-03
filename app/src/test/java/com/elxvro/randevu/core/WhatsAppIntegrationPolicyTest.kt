package com.elxvro.randevu.core

import com.elxvro.randevu.network.WhatsAppServerConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppIntegrationPolicyTest {
    private val appointment = Appointment("apt-1", "Ayşe", "+905551112233", "Bakım", "Deniz", "2026-10-05", "10:00", AppointmentStatus.CONFIRMED, "")

    @Test fun `settings and server config round trip without setup key`() {
        val settings = WhatsAppSettings(enabled = true, reminder24h = true, reminder2h = false, templateName = "hatirlatma", languageCode = "tr", metaPhoneNumberId = "123")
        val rawSettings = WhatsAppStorageCodec.encodeSettings(settings)
        val decodedSettings = WhatsAppStorageCodec.decodeSettings(rawSettings)
        assertEquals(settings, decodedSettings)
        assertFalse(rawSettings.contains("setup_key"))
        assertFalse(rawSettings.contains("meta_access_token"))

        val config = WhatsAppServerConfig("https://api.example.com", "token-1")
        assertEquals(config, WhatsAppStorageCodec.decodeServerConfig(WhatsAppStorageCodec.encodeServerConfig(config)))
    }

    @Test fun `pending operations round trip with appointment snapshot`() {
        val operations = listOf(
            WhatsAppPendingSync("apt-1", WhatsAppAppointmentMutation.CREATE_OR_UPDATE, appointment),
            WhatsAppPendingSync("apt-2", WhatsAppAppointmentMutation.CANCEL_OR_DELETE, null)
        )
        assertEquals(operations, WhatsAppStorageCodec.decodePending(WhatsAppStorageCodec.encodePending(operations)))
    }

    @Test fun `invalid persistence falls back safely`() {
        assertEquals(WhatsAppSettings(), WhatsAppStorageCodec.decodeSettings("not-json"))
        assertEquals(WhatsAppServerConfig(), WhatsAppStorageCodec.decodeServerConfig("not-json"))
        assertTrue(WhatsAppStorageCodec.decodePending("not-json").isEmpty())
    }

    @Test fun `appointment actions project to correct whatsapp mutation`() {
        assertEquals(WhatsAppAppointmentMutation.CREATE_OR_UPDATE, WhatsAppMutationPolicy.fromAction(AppointmentAction.Add(appointment), emptyList())?.mutation)
        assertEquals(WhatsAppAppointmentMutation.CREATE_OR_UPDATE, WhatsAppMutationPolicy.fromAction(AppointmentAction.Update(appointment), listOf(appointment))?.mutation)
        assertEquals(WhatsAppAppointmentMutation.CANCEL_OR_DELETE, WhatsAppMutationPolicy.fromAction(AppointmentAction.Delete("apt-1"), listOf(appointment))?.mutation)
        assertEquals(WhatsAppAppointmentMutation.CANCEL_OR_DELETE, WhatsAppMutationPolicy.fromAction(AppointmentAction.ChangeStatus("apt-1", AppointmentStatus.CANCELLED), listOf(appointment))?.mutation)
        assertEquals(WhatsAppAppointmentMutation.CANCEL_OR_DELETE, WhatsAppMutationPolicy.fromAction(AppointmentAction.ChangeStatus("apt-1", AppointmentStatus.COMPLETED), listOf(appointment))?.mutation)
        assertEquals(WhatsAppAppointmentMutation.CREATE_OR_UPDATE, WhatsAppMutationPolicy.fromAction(AppointmentAction.ChangeStatus("apt-1", AppointmentStatus.CONFIRMED), listOf(appointment))?.mutation)
    }
}
