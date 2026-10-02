package com.elxvro.randevu.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QualityCoreTest {
    @Test
    fun `valid appointment has no validation issues`() {
        val appointment = Appointment(
            id = "1",
            customer = "Ayşe Yılmaz",
            phone = "05551234567",
            service = "Saç Kesimi",
            staff = "Elif",
            date = "2026-10-05",
            time = "14:30",
            status = AppointmentStatus.CONFIRMED,
            note = ""
        )
        assertTrue(QualityEngine.validate(appointment).isEmpty())
    }

    @Test
    fun `invalid fields are reported without throwing`() {
        val appointment = Appointment("", "A", "123", "", "", "x", "99:99", AppointmentStatus.PENDING, "")
        val issues = QualityEngine.validate(appointment)
        assertTrue(issues.size >= 6)
        assertTrue(issues.any { it.code == "customer" })
        assertTrue(issues.any { it.code == "phone" })
        assertTrue(issues.any { it.code == "date" })
        assertTrue(issues.any { it.code == "time" })
    }

    @Test
    fun `conflict detector only flags active overlapping staff slots`() {
        val list = listOf(
            Appointment("1", "Ayşe", "05551234567", "Saç", "Elif", "2026-10-05", "10:00", AppointmentStatus.CONFIRMED, ""),
            Appointment("2", "Mehmet", "05321234567", "Sakal", "Elif", "2026-10-05", "10:00", AppointmentStatus.PENDING, ""),
            Appointment("3", "Zeynep", "05441234567", "Saç", "Mert", "2026-10-05", "10:00", AppointmentStatus.CONFIRMED, ""),
            Appointment("4", "Can", "05051234567", "Saç", "Elif", "2026-10-05", "10:00", AppointmentStatus.CANCELLED, "")
        )
        val conflicts = QualityEngine.conflicts(list)
        assertEquals(1, conflicts.size)
        assertTrue(conflicts.first().appointmentIds.containsAll(listOf("1", "2")))
        assertFalse(conflicts.first().appointmentIds.contains("4"))
    }

    @Test
    fun `quality summary counts invalid and duplicate records`() {
        val list = listOf(
            Appointment("1", "Ayşe", "05551234567", "Saç", "Elif", "2026-10-05", "10:00", AppointmentStatus.CONFIRMED, ""),
            Appointment("2", "Mehmet", "05321234567", "Saç", "Elif", "2026-10-05", "10:00", AppointmentStatus.PENDING, ""),
            Appointment("3", "A", "1", "", "", "bad", "bad", AppointmentStatus.PENDING, "")
        )
        val summary = QualityEngine.summary(list)
        assertEquals(3, summary.total)
        assertEquals(1, summary.invalidRecords)
        assertEquals(1, summary.conflicts)
        assertFalse(summary.ready)
    }
}
