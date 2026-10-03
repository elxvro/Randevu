package com.elxvro.randevu.core

object WhatsAppMutationPolicy {
    fun fromAction(action: AppointmentAction, before: List<Appointment>): WhatsAppPendingSync? = when (action) {
        is AppointmentAction.Add -> WhatsAppPendingSync(
            action.appointment.id,
            mutationForStatus(action.appointment.status),
            action.appointment.takeUnless { mutationForStatus(it.status) == WhatsAppAppointmentMutation.CANCEL_OR_DELETE }
        )

        is AppointmentAction.Update -> WhatsAppPendingSync(
            action.appointment.id,
            mutationForStatus(action.appointment.status),
            action.appointment.takeUnless { mutationForStatus(it.status) == WhatsAppAppointmentMutation.CANCEL_OR_DELETE }
        )

        is AppointmentAction.Delete -> WhatsAppPendingSync(
            action.id,
            WhatsAppAppointmentMutation.CANCEL_OR_DELETE,
            null
        )

        is AppointmentAction.ChangeStatus -> {
            val current = before.firstOrNull { it.id == action.id } ?: return null
            val updated = current.copy(status = action.status)
            val mutation = mutationForStatus(action.status)
            WhatsAppPendingSync(
                action.id,
                mutation,
                updated.takeUnless { mutation == WhatsAppAppointmentMutation.CANCEL_OR_DELETE }
            )
        }
    }

    private fun mutationForStatus(status: AppointmentStatus): WhatsAppAppointmentMutation = when (status) {
        AppointmentStatus.CANCELLED, AppointmentStatus.COMPLETED -> WhatsAppAppointmentMutation.CANCEL_OR_DELETE
        AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED -> WhatsAppAppointmentMutation.CREATE_OR_UPDATE
    }
}
