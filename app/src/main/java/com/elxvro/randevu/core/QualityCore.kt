package com.elxvro.randevu.core

import java.time.LocalDate
import java.time.LocalTime

data class QualityIssue(val code: String, val message: String)

data class AppointmentConflict(
    val staff: String,
    val date: String,
    val time: String,
    val appointmentIds: List<String>
)

data class QualitySummary(
    val total: Int,
    val invalidRecords: Int,
    val conflicts: Int,
    val ready: Boolean
)

object QualityEngine {
    fun validate(appointment: Appointment): List<QualityIssue> = buildList {
        if (appointment.id.isBlank()) add(QualityIssue("id", "Kayıt kimliği boş"))
        if (appointment.customer.trim().length < 2) add(QualityIssue("customer", "Müşteri adı çok kısa"))
        val phoneDigits = appointment.phone.filter(Char::isDigit)
        if (phoneDigits.length !in 10..15) add(QualityIssue("phone", "Telefon numarası geçersiz"))
        if (appointment.service.isBlank()) add(QualityIssue("service", "Hizmet seçilmemiş"))
        if (appointment.staff.isBlank()) add(QualityIssue("staff", "Personel seçilmemiş"))
        if (runCatching { LocalDate.parse(appointment.date) }.isFailure) add(QualityIssue("date", "Tarih biçimi geçersiz"))
        val time = runCatching { LocalTime.parse(appointment.time) }.getOrNull()
        if (time == null) add(QualityIssue("time", "Saat biçimi geçersiz"))
    }

    fun conflicts(appointments: List<Appointment>): List<AppointmentConflict> = appointments
        .asSequence()
        .filter { it.status != AppointmentStatus.CANCELLED }
        .filter { it.staff.isNotBlank() && it.date.isNotBlank() && it.time.isNotBlank() }
        .groupBy { Triple(it.staff.trim().lowercase(), it.date, it.time) }
        .filterValues { it.size > 1 }
        .map { (_, items) ->
            AppointmentConflict(
                staff = items.first().staff,
                date = items.first().date,
                time = items.first().time,
                appointmentIds = items.map { it.id }
            )
        }
        .sortedWith(compareBy<AppointmentConflict> { it.date }.thenBy { it.time }.thenBy { it.staff })
        .toList()

    fun summary(appointments: List<Appointment>): QualitySummary {
        val invalid = appointments.count { validate(it).isNotEmpty() }
        val conflictCount = conflicts(appointments).size
        return QualitySummary(
            total = appointments.size,
            invalidRecords = invalid,
            conflicts = conflictCount,
            ready = invalid == 0 && conflictCount == 0
        )
    }
}
