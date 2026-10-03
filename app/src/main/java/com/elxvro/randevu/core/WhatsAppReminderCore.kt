package com.elxvro.randevu.core

data class WhatsAppSettings(
    val enabled: Boolean = false,
    val reminder24h: Boolean = true,
    val reminder2h: Boolean = true,
    val templateName: String = "appointment_reminder",
    val languageCode: String = "tr",
    val metaPhoneNumberId: String = ""
)

enum class WhatsAppConnectionState {
    CONNECTED,
    INCOMPLETE,
    UNREACHABLE,
    DISABLED
}

enum class WhatsAppAppointmentMutation {
    CREATE_OR_UPDATE,
    CANCEL_OR_DELETE
}

enum class WhatsAppSyncAction {
    UPSERT,
    CANCEL,
    PENDING_LOCAL,
    NONE
}

object WhatsAppReminderCore {
    fun syncAction(
        settings: WhatsAppSettings,
        connectionState: WhatsAppConnectionState,
        mutation: WhatsAppAppointmentMutation
    ): WhatsAppSyncAction {
        if (!settings.enabled) return WhatsAppSyncAction.NONE
        if (connectionState != WhatsAppConnectionState.CONNECTED) return WhatsAppSyncAction.PENDING_LOCAL
        return when (mutation) {
            WhatsAppAppointmentMutation.CREATE_OR_UPDATE -> WhatsAppSyncAction.UPSERT
            WhatsAppAppointmentMutation.CANCEL_OR_DELETE -> WhatsAppSyncAction.CANCEL
        }
    }
}
