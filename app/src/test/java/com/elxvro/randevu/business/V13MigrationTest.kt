package com.elxvro.randevu.business

import com.elxvro.randevu.core.Appointment
import com.elxvro.randevu.core.AppointmentStatus
import com.elxvro.randevu.staff.StaffRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V13MigrationTest {
    @Test fun `exact shipped demo appointment is removed but edited copy survives`() {
        val exact = Appointment("seed-1", "Ayşe Kaya", "+905339876543", "Saç Kesimi", "Mert Yılmaz", "2026-10-03", "10:30", AppointmentStatus.PENDING, "")
        val edited = exact.copy(customer = "Gerçek Müşteri")
        val result = V13Migration.cleanAppointments(listOf(exact, edited))
        assertEquals(listOf(edited), result)
    }

    @Test fun `exact default staff is removed but edited copy survives`() {
        val exact = StaffRecord("staff-mert", "Mert Yılmaz", "Uzman", "+905339876543", true)
        val edited = exact.copy(title = "Kıdemli Uzman")
        assertEquals(listOf(edited), V13Migration.cleanStaff(listOf(exact, edited)))
    }

    @Test fun `empty inputs remain empty`() {
        assertTrue(V13Migration.cleanAppointments(emptyList()).isEmpty())
        assertTrue(V13Migration.cleanStaff(emptyList()).isEmpty())
    }
}
