package com.elxvro.randevu.core

import java.time.LocalDate

enum class AppointmentStatus(val label: String) {
    PENDING("Bekliyor"),
    CONFIRMED("Onaylandı"),
    COMPLETED("Tamamlandı"),
    CANCELLED("İptal")
}

enum class CalendarView {
    DAY,
    WEEK
}

data class Appointment(
    val id: String,
    val customer: String,
    val phone: String,
    val service: String,
    val staff: String,
    val date: String,
    val time: String,
    val status: AppointmentStatus,
    val note: String
)

sealed interface AppointmentAction {
    data class Add(val appointment: Appointment) : AppointmentAction
    data class Update(val appointment: Appointment) : AppointmentAction
    data class Delete(val id: String) : AppointmentAction
    data class ChangeStatus(val id: String, val status: AppointmentStatus) : AppointmentAction
}

object AppointmentEngine {
    fun reduce(state: List<Appointment>, action: AppointmentAction): List<Appointment> = when (action) {
        is AppointmentAction.Add -> {
            if (state.any { it.id == action.appointment.id }) state
            else (state + action.appointment).sortedByDateTime()
        }

        is AppointmentAction.Update -> state.map {
            if (it.id == action.appointment.id) action.appointment else it
        }.sortedByDateTime()

        is AppointmentAction.Delete -> state.filterNot { it.id == action.id }

        is AppointmentAction.ChangeStatus -> state.map {
            if (it.id == action.id) it.copy(status = action.status) else it
        }.sortedByDateTime()
    }

    fun filter(
        appointments: List<Appointment>,
        query: String,
        status: AppointmentStatus?
    ): List<Appointment> {
        val normalized = query.trim().lowercase()
        return appointments.filter { appointment ->
            val statusMatches = status == null || appointment.status == status
            val textMatches = normalized.isBlank() || listOf(
                appointment.customer,
                appointment.phone,
                appointment.service,
                appointment.staff,
                appointment.date,
                appointment.time,
                appointment.note
            ).any { it.lowercase().contains(normalized) }
            statusMatches && textMatches
        }.sortedByDateTime()
    }

    fun forView(
        appointments: List<Appointment>,
        view: CalendarView,
        anchorDate: String
    ): List<Appointment> {
        val anchor = LocalDate.parse(anchorDate)
        return when (view) {
            CalendarView.DAY -> appointments.filter { it.date == anchor.toString() }
            CalendarView.WEEK -> {
                val weekStart = anchor.minusDays((anchor.dayOfWeek.value - 1).toLong())
                val weekEnd = weekStart.plusDays(6)
                appointments.filter { appointment ->
                    val date = LocalDate.parse(appointment.date)
                    !date.isBefore(weekStart) && !date.isAfter(weekEnd)
                }
            }
        }.sortedByDateTime()
    }

    private fun List<Appointment>.sortedByDateTime(): List<Appointment> =
        sortedWith(compareBy<Appointment> { it.date }.thenBy { it.time })
}
