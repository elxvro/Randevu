package com.elxvro.randevu.notifications

import com.elxvro.randevu.core.Appointment
import java.time.LocalDateTime

data class NotificationCenterRow(
    val appointmentId: String,
    val customer: String,
    val service: String,
    val staff: String,
    val appointmentDate: String,
    val appointmentTime: String,
    val timingLabel: String,
    val triggerAt: LocalDateTime
)

object NotificationCenterProjection {
    fun rows(appointments: List<Appointment>, now: LocalDateTime): List<NotificationCenterRow> = appointments
        .flatMap { appointment ->
            ReminderPlan.entries(appointment, now).map { entry ->
                NotificationCenterRow(
                    appointmentId = appointment.id,
                    customer = appointment.customer,
                    service = appointment.service,
                    staff = appointment.staff,
                    appointmentDate = appointment.date,
                    appointmentTime = appointment.time,
                    timingLabel = if (entry.offsetHours == 24L) "24 saat önce" else "2 saat önce",
                    triggerAt = entry.triggerAt
                )
            }
        }
        .sortedBy { it.triggerAt }
}
