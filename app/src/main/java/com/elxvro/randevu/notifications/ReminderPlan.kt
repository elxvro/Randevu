package com.elxvro.randevu.notifications

import com.elxvro.randevu.core.Appointment
import com.elxvro.randevu.core.AppointmentStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class ReminderEntry(
    val appointment: Appointment,
    val offsetHours: Long,
    val triggerAt: LocalDateTime,
    val workName: String
)

object ReminderPlan {
    private val offsets = listOf(24L, 2L)

    fun entries(appointment: Appointment, now: LocalDateTime): List<ReminderEntry> {
        if (appointment.status == AppointmentStatus.CANCELLED || appointment.status == AppointmentStatus.COMPLETED) return emptyList()
        val date = runCatching { LocalDate.parse(appointment.date) }.getOrNull() ?: return emptyList()
        val time = runCatching { LocalTime.parse(appointment.time) }.getOrNull() ?: return emptyList()
        val appointmentAt = LocalDateTime.of(date, time)
        if (!appointmentAt.isAfter(now)) return emptyList()
        return offsets.mapNotNull { offset ->
            val trigger = appointmentAt.minusHours(offset)
            if (!trigger.isAfter(now)) null else ReminderEntry(
                appointment = appointment,
                offsetHours = offset,
                triggerAt = trigger,
                workName = workName(appointment.id, offset)
            )
        }
    }

    fun workName(appointmentId: String, offsetHours: Long): String =
        "appointment-reminder-${appointmentId}-${offsetHours}h"
}
