package com.elxvro.randevu.core

import org.junit.Assert.assertEquals
import org.junit.Test

class WhatsAppReminderCoreTest {
    private val settings = WhatsAppSettings(enabled = true, reminder24h = true, reminder2h = true)

    @Test fun `connected create and update is upsert`() {
        assertEquals(WhatsAppSyncAction.UPSERT, WhatsAppReminderCore.syncAction(settings, WhatsAppConnectionState.CONNECTED, WhatsAppAppointmentMutation.CREATE_OR_UPDATE))
    }

    @Test fun `connected cancel and delete cancels remote reminders`() {
        assertEquals(WhatsAppSyncAction.CANCEL, WhatsAppReminderCore.syncAction(settings, WhatsAppConnectionState.CONNECTED, WhatsAppAppointmentMutation.CANCEL_OR_DELETE))
    }

    @Test fun `unavailable backend becomes pending local instead of scheduled`() {
        assertEquals(WhatsAppSyncAction.PENDING_LOCAL, WhatsAppReminderCore.syncAction(settings, WhatsAppConnectionState.UNREACHABLE, WhatsAppAppointmentMutation.CREATE_OR_UPDATE))
        assertEquals(WhatsAppSyncAction.PENDING_LOCAL, WhatsAppReminderCore.syncAction(settings, WhatsAppConnectionState.INCOMPLETE, WhatsAppAppointmentMutation.CANCEL_OR_DELETE))
    }

    @Test fun `disabled whatsapp performs no backend action`() {
        assertEquals(WhatsAppSyncAction.NONE, WhatsAppReminderCore.syncAction(settings.copy(enabled = false), WhatsAppConnectionState.CONNECTED, WhatsAppAppointmentMutation.CREATE_OR_UPDATE))
    }

    @Test fun `default settings use approved reminder offsets and template`() {
        val defaults = WhatsAppSettings()
        assertEquals(true, defaults.reminder24h)
        assertEquals(true, defaults.reminder2h)
        assertEquals("appointment_reminder", defaults.templateName)
        assertEquals("tr", defaults.languageCode)
    }
}
