package com.elxvro.randevu.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessEngineTest {
    private val baseState = BusinessState(
        profile = BusinessProfile(
            name = "Elite Kuaför",
            phone = "+90 555 111 22 33",
            address = "Kadıköy, İstanbul",
            description = "Bakım ve kuaför hizmetleri"
        ),
        services = listOf(ServiceConfig("hair", "Saç Kesimi", 30, 250, true)),
        staff = listOf(
            StaffMember(
                id = "staff-1",
                name = "Ahmet Demir",
                title = "Uzman Kuaför",
                active = true,
                serviceIds = setOf("hair"),
                branchId = "main"
            )
        ),
        workingDays = listOf(
            WorkingDay(
                dayOfWeek = 4,
                open = "09:00",
                close = "19:00",
                enabled = true,
                breaks = listOf(BreakWindow("13:00", "13:30"))
            )
        ),
        closedDays = emptyList(),
        branches = listOf(Branch("main", "Merkez", "Kadıköy, İstanbul", "+90 555 111 22 33", true))
    )

    @Test
    fun serviceAndStaffCanBeAddedAndToggled() {
        val withService = BusinessEngine.reduce(
            baseState,
            BusinessAction.UpsertService(ServiceConfig("beard", "Sakal Tıraşı", 20, 150, true))
        )
        assertTrue(withService.services.any { it.id == "beard" })

        val disabled = BusinessEngine.reduce(withService, BusinessAction.ToggleService("beard"))
        assertFalse(disabled.services.first { it.id == "beard" }.active)

        val withStaff = BusinessEngine.reduce(
            disabled,
            BusinessAction.UpsertStaff(
                StaffMember("staff-2", "Mehmet Kaya", "Kıdemli Kuaför", true, setOf("hair"), "main")
            )
        )
        assertTrue(withStaff.staff.any { it.id == "staff-2" })
    }

    @Test
    fun closedDayAndBreakBlockBooking() {
        val closed = baseState.copy(closedDays = listOf(ClosedDay("2026-10-01", "Tadilat")))
        assertFalse(
            BusinessEngine.isAvailable(
                closed,
                date = "2026-10-01",
                time = "10:00",
                durationMinutes = 30,
                staffId = "staff-1",
                appointments = emptyList()
            )
        )

        assertFalse(
            BusinessEngine.isAvailable(
                baseState,
                date = "2026-10-01",
                time = "13:10",
                durationMinutes = 20,
                staffId = "staff-1",
                appointments = emptyList()
            )
        )
    }

    @Test
    fun overlappingAppointmentAndInactiveStaffBlockBooking() {
        val appointments = listOf(
            Appointment(
                id = "a1",
                customer = "Müşteri",
                phone = "555",
                service = "Saç Kesimi",
                staff = "Ahmet Demir",
                date = "2026-10-01",
                time = "10:00",
                status = AppointmentStatus.CONFIRMED,
                note = ""
            )
        )

        assertFalse(
            BusinessEngine.isAvailable(
                baseState,
                date = "2026-10-01",
                time = "10:15",
                durationMinutes = 30,
                staffId = "staff-1",
                appointments = appointments
            )
        )

        val inactive = baseState.copy(staff = baseState.staff.map { it.copy(active = false) })
        assertFalse(
            BusinessEngine.isAvailable(
                inactive,
                date = "2026-10-01",
                time = "11:00",
                durationMinutes = 30,
                staffId = "staff-1",
                appointments = emptyList()
            )
        )
    }

    @Test
    fun freeWorkingSlotIsAvailable() {
        assertTrue(
            BusinessEngine.isAvailable(
                baseState,
                date = "2026-10-01",
                time = "11:00",
                durationMinutes = 30,
                staffId = "staff-1",
                appointments = emptyList()
            )
        )
    }
}
