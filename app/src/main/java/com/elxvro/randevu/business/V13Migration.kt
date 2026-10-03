package com.elxvro.randevu.business

import com.elxvro.randevu.core.Appointment
import com.elxvro.randevu.core.AppointmentStatus
import com.elxvro.randevu.staff.StaffRecord
import java.time.LocalDate

object V13Migration {
    fun cleanAppointments(records: List<Appointment>): List<Appointment> = records.filterNot(::isShippedSeedAppointment)

    fun cleanStaff(records: List<StaffRecord>): List<StaffRecord> = records.filterNot(::isShippedDefaultStaff)

    private fun isShippedSeedAppointment(item: Appointment): Boolean {
        if (runCatching { LocalDate.parse(item.date) }.isFailure) return false
        return when (item.id) {
            "seed-1" -> item.matchesSeed("Ayşe Kaya", "+905339876543", "Saç Kesimi", "Mert Yılmaz", "10:30", AppointmentStatus.PENDING)
            "seed-2" -> item.matchesSeed("Elif Arslan", "+905321234567", "Cilt Bakımı", "Zeynep Arslan", "11:15", AppointmentStatus.CONFIRMED)
            "seed-3" -> item.matchesSeed("Can Demir", "+905327894561", "Saç Boyama", "Deniz Arıcı", "13:00", AppointmentStatus.PENDING)
            "seed-4" -> item.matchesSeed("Burcu Kaya", "+905324567890", "Manikür", "Zeynep Arslan", "14:30", AppointmentStatus.CONFIRMED)
            else -> false
        }
    }

    private fun Appointment.matchesSeed(
        customerValue: String,
        phoneValue: String,
        serviceValue: String,
        staffValue: String,
        timeValue: String,
        statusValue: AppointmentStatus
    ): Boolean = customer == customerValue && phone == phoneValue && service == serviceValue && staff == staffValue &&
        time == timeValue && status == statusValue && note.isBlank()

    private fun isShippedDefaultStaff(item: StaffRecord): Boolean = when (item.id) {
        "staff-mert" -> item == StaffRecord("staff-mert", "Mert Yılmaz", "Uzman", "+905339876543", true)
        "staff-zeynep" -> item == StaffRecord("staff-zeynep", "Zeynep Arslan", "Uzman", "+905321234567", true)
        "staff-deniz" -> item == StaffRecord("staff-deniz", "Deniz Arıcı", "Uzman", "+905327894561", true)
        else -> false
    }
}
