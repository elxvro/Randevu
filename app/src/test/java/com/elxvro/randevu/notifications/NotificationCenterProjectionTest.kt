package com.elxvro.randevu.notifications

import com.elxvro.randevu.core.Appointment
import com.elxvro.randevu.core.AppointmentStatus
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationCenterProjectionTest {
    private fun appointment(status: AppointmentStatus = AppointmentStatus.CONFIRMED) = Appointment(
        id = "a1",
        customer = "Ayşe Kaya",
        phone = "+905551112233",
        service = "Cilt Bakımı",
        staff = "Zeynep Arslan",
        date = "2026-10-05",
        time = "12:00",
        status = status,
        note = ""
    )

    @Test
    fun futureAppointment_projectsEligibleReminderRows() {
        val rows = NotificationCenterProjection.rows(listOf(appointment()), LocalDateTime.of(2026, 10, 3, 8, 0))
        assertEquals(2, rows.size)
        assertEquals(listOf("24 saat önce", "2 saat önce"), rows.map { it.timingLabel })
    }

    @Test
    fun pastOrCancelledAppointments_projectNoRows() {
        assertTrue(NotificationCenterProjection.rows(listOf(appointment()), LocalDateTime.of(2026, 10, 6, 8, 0)).isEmpty())
        assertTrue(NotificationCenterProjection.rows(listOf(appointment(AppointmentStatus.CANCELLED)), LocalDateTime.of(2026, 10, 3, 8, 0)).isEmpty())
    }
}
