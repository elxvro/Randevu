package com.elxvro.randevu.ui

import com.elxvro.randevu.core.Appointment
import com.elxvro.randevu.staff.StaffLeave
import com.elxvro.randevu.staff.StaffLeaveEngine
import com.elxvro.randevu.staff.StaffRecord
import java.time.LocalDate
import java.time.LocalTime

object ReferenceBookingRules {
    fun validationError(
        staff: StaffRecord?,
        leaves: List<StaffLeave>,
        date: String,
        time: String,
        appointments: List<Appointment> = emptyList(),
        editingAppointmentId: String? = null
    ): String? {
        if (staff == null) return "Personel seçin."
        if (!staff.active) return "Seçilen personel aktif değil."
        if (runCatching { LocalDate.parse(date) }.isFailure || runCatching { LocalTime.parse(time) }.isFailure) {
            return "Tarih ve saat alanlarını kontrol edin."
        }
        if (!StaffLeaveEngine.canBook(staff, leaves, date, time)) {
            return "Bu personel seçilen tarihte izinli."
        }
        if (appointments.any {
                it.id != editingAppointmentId &&
                    it.staff.equals(staff.name, ignoreCase = true) &&
                    it.date == date &&
                    it.time == time &&
                    it.status != com.elxvro.randevu.core.AppointmentStatus.CANCELLED
            }) {
            return "Bu personelin aynı saatte başka randevusu var."
        }
        return null
    }
}
