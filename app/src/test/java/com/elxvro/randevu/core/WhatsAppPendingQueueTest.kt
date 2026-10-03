package com.elxvro.randevu.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WhatsAppPendingQueueTest {
    private val appointment = Appointment(
        id = "apt-1",
        customer = "Ayşe",
        phone = "+905551112233",
        service = "Bakım",
        staff = "Deniz",
        date = "2026-10-05",
        time = "10:00",
        status = AppointmentStatus.CONFIRMED,
        note = ""
    )

    @Test fun `latest operation replaces older operation for same appointment`() {
        val first = WhatsAppPendingSync("apt-1", WhatsAppAppointmentMutation.CREATE_OR_UPDATE, appointment)
        val cancelled = WhatsAppPendingSync("apt-1", WhatsAppAppointmentMutation.CANCEL_OR_DELETE, null)
        val queued = WhatsAppPendingQueue.enqueue(listOf(first), cancelled)

        assertEquals(1, queued.size)
        assertEquals(WhatsAppAppointmentMutation.CANCEL_OR_DELETE, queued.single().mutation)
        assertNull(queued.single().appointment)
    }

    @Test fun `different appointments remain ordered`() {
        val one = WhatsAppPendingSync("apt-1", WhatsAppAppointmentMutation.CREATE_OR_UPDATE, appointment)
        val twoAppointment = appointment.copy(id = "apt-2")
        val two = WhatsAppPendingSync("apt-2", WhatsAppAppointmentMutation.CREATE_OR_UPDATE, twoAppointment)
        val queued = WhatsAppPendingQueue.enqueue(WhatsAppPendingQueue.enqueue(emptyList(), one), two)

        assertEquals(listOf("apt-1", "apt-2"), queued.map { it.appointmentId })
    }

    @Test fun `acknowledgement removes only matching appointment`() {
        val one = WhatsAppPendingSync("apt-1", WhatsAppAppointmentMutation.CREATE_OR_UPDATE, appointment)
        val two = WhatsAppPendingSync("apt-2", WhatsAppAppointmentMutation.CANCEL_OR_DELETE, null)
        val queued = WhatsAppPendingQueue.remove(listOf(one, two), "apt-1")

        assertEquals(listOf("apt-2"), queued.map { it.appointmentId })
    }
}
