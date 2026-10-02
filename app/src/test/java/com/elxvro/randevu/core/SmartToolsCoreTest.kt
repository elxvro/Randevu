package com.elxvro.randevu.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartToolsCoreTest {
    private val appointments = listOf(
        Appointment("1", "Ayşe", "05551234567", "Saç Kesimi", "Elif", "2026-10-02", "10:00", AppointmentStatus.COMPLETED, "VIP"),
        Appointment("2", "Ayşe", "05551234567", "Sakal", "Elif", "2026-10-03", "11:00", AppointmentStatus.CONFIRMED, ""),
        Appointment("3", "Mehmet", "05320000000", "Cilt Bakımı", "Mert", "2026-10-02", "14:00", AppointmentStatus.CANCELLED, ""),
        Appointment("4", "Zeynep", "05440000000", "Saç Boyama", "Elif", "2026-10-02", "15:00", AppointmentStatus.PENDING, "")
    )

    @Test
    fun `customer summary groups history and tags`() {
        val summary = SmartToolsEngine.customerSummary(appointments, "05551234567")
        assertEquals("Ayşe", summary.customer)
        assertEquals(2, summary.totalAppointments)
        assertEquals(1, summary.completedAppointments)
        assertTrue(summary.tags.contains("VIP"))
    }

    @Test
    fun `message builders produce usable whatsapp and sms text`() {
        val appointment = appointments[1]
        val whatsapp = SmartToolsEngine.whatsappMessage(appointment, "ELXVRO Randevu")
        val sms = SmartToolsEngine.smsMessage(appointment, "ELXVRO Randevu")
        assertTrue(whatsapp.contains("Ayşe"))
        assertTrue(whatsapp.contains("2026-10-03"))
        assertEquals(whatsapp, sms)
    }

    @Test
    fun `report ignores cancelled appointments and calculates expected revenue`() {
        val prices = mapOf("Saç Kesimi" to 250.0, "Sakal" to 150.0, "Cilt Bakımı" to 450.0, "Saç Boyama" to 600.0)
        val report = SmartToolsEngine.report(appointments, prices, "2026-10-02", "2026-10-03")
        assertEquals(3, report.activeAppointments)
        assertEquals(1, report.completedAppointments)
        assertEquals(1000.0, report.expectedRevenue, 0.01)
        assertEquals(250.0, report.realizedRevenue, 0.01)
    }

    @Test
    fun `quick action validation blocks missing phone`() {
        assertTrue(SmartToolsEngine.canContact("05551234567"))
        assertFalse(SmartToolsEngine.canContact("123"))
    }
}
