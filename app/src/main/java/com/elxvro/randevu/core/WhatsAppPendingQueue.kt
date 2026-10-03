package com.elxvro.randevu.core

data class WhatsAppPendingSync(
    val appointmentId: String,
    val mutation: WhatsAppAppointmentMutation,
    val appointment: Appointment?
)

object WhatsAppPendingQueue {
    fun enqueue(
        current: List<WhatsAppPendingSync>,
        operation: WhatsAppPendingSync
    ): List<WhatsAppPendingSync> {
        val withoutSame = current.filterNot { it.appointmentId == operation.appointmentId }
        return withoutSame + operation
    }

    fun remove(current: List<WhatsAppPendingSync>, appointmentId: String): List<WhatsAppPendingSync> =
        current.filterNot { it.appointmentId == appointmentId }
}
