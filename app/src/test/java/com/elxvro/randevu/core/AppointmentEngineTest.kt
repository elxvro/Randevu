package com.elxvro.randevu.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppointmentEngineTest {

    private val base = Appointment(
        id = "a1",
        customer = "Ayşe Yılmaz",
        phone = "05550000000",
        service = "Saç Kesimi",
        staff = "Ahmet Demir",
        date = "2026-10-01",
        time = "10:30",
        status = AppointmentStatus.CONFIRMED,
        note = ""
    )

    @Test
    fun addUpdateDeleteFlowKeepsIdsStable() {
        val added = AppointmentEngine.reduce(emptyList(), AppointmentAction.Add(base))
        assertEquals(1, added.size)

        val changed = base.copy(service = "Saç Boyama", time = "11:30")
        val updated = AppointmentEngine.reduce(added, AppointmentAction.Update(changed))
        assertEquals(1, updated.size)
        assertEquals("Saç Boyama", updated.single().service)
        assertEquals("11:30", updated.single().time)

        val deleted = AppointmentEngine.reduce(updated, AppointmentAction.Delete("a1"))
        assertTrue(deleted.isEmpty())
    }

    @Test
    fun statusAndSearchFiltersWorkTogether() {
        val other = base.copy(
            id = "a2",
            customer = "Mehmet Kaya",
            service = "Cilt Bakımı",
            status = AppointmentStatus.PENDING,
            time = "14:00"
        )
        val all = listOf(base, other)

        val byCustomer = AppointmentEngine.filter(all, "ayşe", null)
        assertEquals(listOf("a1"), byCustomer.map { it.id })

        val pending = AppointmentEngine.filter(all, "", AppointmentStatus.PENDING)
        assertEquals(listOf("a2"), pending.map { it.id })

        val serviceAndStatus = AppointmentEngine.filter(all, "cilt", AppointmentStatus.PENDING)
        assertEquals(listOf("a2"), serviceAndStatus.map { it.id })
    }

    @Test
    fun dayAndWeekViewsUseAnchorDate() {
        val sameDay = base.copy(id = "same", time = "09:00")
        val withinWeek = base.copy(id = "week", date = "2026-10-04")
        val outsideWeek = base.copy(id = "outside", date = "2026-10-10")
        val all = listOf(sameDay, withinWeek, outsideWeek)

        val day = AppointmentEngine.forView(all, CalendarView.DAY, "2026-10-01")
        assertEquals(listOf("same"), day.map { it.id })

        val week = AppointmentEngine.forView(all, CalendarView.WEEK, "2026-10-01")
        assertEquals(listOf("same", "week"), week.map { it.id })
    }

    @Test
    fun changeStatusUpdatesOnlyTargetAppointment() {
        val other = base.copy(id = "a2", status = AppointmentStatus.PENDING)
        val changed = AppointmentEngine.reduce(
            listOf(base, other),
            AppointmentAction.ChangeStatus("a2", AppointmentStatus.COMPLETED)
        )

        assertEquals(AppointmentStatus.CONFIRMED, changed.first { it.id == "a1" }.status)
        assertEquals(AppointmentStatus.COMPLETED, changed.first { it.id == "a2" }.status)
    }
}
