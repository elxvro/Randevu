package com.elxvro.randevu.notifications

import com.elxvro.randevu.core.Appointment
import com.elxvro.randevu.core.AppointmentStatus
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPlanTest {
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
    fun sufficientlyFutureAppointment_generates24hAnd2hEntries() {
        val rows = ReminderPlan.entries(appointment(), LocalDateTime.of(2026, 10, 3, 8, 0))
        assertEquals(listOf(24L, 2L), rows.map { it.offsetHours })
        assertEquals("appointment-reminder-a1-24h", rows[0].workName)
        assertEquals("appointment-reminder-a1-2h", rows[1].workName)
    }

    @Test
    fun pastOffsets_areSkippedIndependently() {
        val rows = ReminderPlan.entries(appointment(), LocalDateTime.of(2026, 10, 5, 9, 0))
        assertEquals(listOf(2L), rows.map { it.offsetHours })
    }

    @Test
    fun cancelledAppointment_generatesNoEntries() {
        assertTrue(ReminderPlan.entries(appointment(AppointmentStatus.CANCELLED), LocalDateTime.of(2026, 10, 3, 8, 0)).isEmpty())
    }
}
