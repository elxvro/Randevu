package com.elxvro.randevu.ui

import com.elxvro.randevu.core.Appointment
import com.elxvro.randevu.core.AppointmentStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ReferenceAppModelTest {
    private val appointments = listOf(
        Appointment("1", "Zeynep Arslan", "+905321234567", "Cilt Bakımı", "Mert Yılmaz", "2026-10-02", "10:00", AppointmentStatus.CONFIRMED, ""),
        Appointment("2", "Zeynep Arslan", "+905321234567", "Saç Kesimi", "Mert Yılmaz", "2026-09-28", "12:00", AppointmentStatus.COMPLETED, ""),
        Appointment("3", "Elif Kaya", "+905324567890", "Manikür", "Deniz Arıcı", "2026-10-02", "13:30", AppointmentStatus.PENDING, ""),
        Appointment("4", "Ahmet Koç", "+905333210987", "Randevu", "Mert Yılmaz", "2026-10-02", "17:00", AppointmentStatus.CANCELLED, "")
    )

    @Test
    fun `dashboard metrics follow appointment statuses`() {
        val metrics = ReferenceAppModel.metrics(appointments)
        assertEquals(4, metrics.total)
        assertEquals(1, metrics.completed)
        assertEquals(1, metrics.pending)
        assertEquals(1, metrics.cancelled)
    }

    @Test
    fun `customers are grouped into one compact record`() {
        val customers = ReferenceAppModel.customers(appointments)
        assertEquals(3, customers.size)
        val zeynep = customers.first { it.phone == "+905321234567" }
        assertEquals(2, zeynep.appointmentCount)
        assertEquals("Zeynep Arslan", zeynep.name)
    }

    @Test
    fun `staff rows include appointment counts`() {
        val staff = ReferenceAppModel.staff(appointments)
        assertEquals(2, staff.size)
        assertEquals(3, staff.first { it.name == "Mert Yılmaz" }.appointmentCount)
    }
}
