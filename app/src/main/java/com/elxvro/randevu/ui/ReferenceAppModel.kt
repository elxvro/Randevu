package com.elxvro.randevu.ui

import com.elxvro.randevu.core.Appointment
import com.elxvro.randevu.core.AppointmentStatus

data class ReferenceMetrics(
    val total: Int,
    val completed: Int,
    val pending: Int,
    val cancelled: Int
)

data class ReferenceCustomer(
    val name: String,
    val phone: String,
    val appointmentCount: Int,
    val lastDate: String,
    val active: Boolean
)

data class ReferenceStaffRow(
    val name: String,
    val appointmentCount: Int,
    val completedCount: Int
)

object ReferenceAppModel {
    fun metrics(appointments: List<Appointment>): ReferenceMetrics = ReferenceMetrics(
        total = appointments.size,
        completed = appointments.count { it.status == AppointmentStatus.COMPLETED },
        pending = appointments.count { it.status == AppointmentStatus.PENDING },
        cancelled = appointments.count { it.status == AppointmentStatus.CANCELLED }
    )

    fun customers(appointments: List<Appointment>): List<ReferenceCustomer> = appointments
        .groupBy { it.phone.trim().ifBlank { it.customer.trim().lowercase() } }
        .map { (_, items) ->
            val newest = items.maxWithOrNull(compareBy<Appointment> { it.date }.thenBy { it.time }) ?: items.first()
            ReferenceCustomer(
                name = newest.customer.ifBlank { "İsimsiz Müşteri" },
                phone = newest.phone,
                appointmentCount = items.size,
                lastDate = newest.date,
                active = items.any { it.status != AppointmentStatus.CANCELLED }
            )
        }
        .sortedBy { it.name.lowercase() }

    fun staff(appointments: List<Appointment>): List<ReferenceStaffRow> = appointments
        .filter { it.staff.isNotBlank() }
        .groupBy { it.staff.trim() }
        .map { (name, items) ->
            ReferenceStaffRow(
                name = name,
                appointmentCount = items.size,
                completedCount = items.count { it.status == AppointmentStatus.COMPLETED }
            )
        }
        .sortedBy { it.name.lowercase() }
}
