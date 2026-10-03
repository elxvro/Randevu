package com.elxvro.randevu.staff

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StaffLeaveEngineTest {
    private val activeStaff = StaffRecord("s1", "Mert Yılmaz", "Uzman", "", true)

    @Test
    fun fullDayLeave_blocksInclusiveDateRange() {
        val leave = StaffLeave("l1", "s1", "2026-10-10", "2026-10-12", null, null, "Yıllık izin", 1L)
        assertTrue(StaffLeaveEngine.blocks(activeStaff, leave, "2026-10-10", "09:00"))
        assertTrue(StaffLeaveEngine.blocks(activeStaff, leave, "2026-10-12", "18:00"))
        assertFalse(StaffLeaveEngine.blocks(activeStaff, leave, "2026-10-13", "09:00"))
    }

    @Test
    fun partialDayLeave_blocksOnlyInsideTimeWindow() {
        val leave = StaffLeave("l2", "s1", "2026-10-10", "2026-10-10", "13:00", "16:00", "Doktor", 1L)
        assertFalse(StaffLeaveEngine.blocks(activeStaff, leave, "2026-10-10", "12:59"))
        assertTrue(StaffLeaveEngine.blocks(activeStaff, leave, "2026-10-10", "13:00"))
        assertTrue(StaffLeaveEngine.blocks(activeStaff, leave, "2026-10-10", "15:59"))
        assertFalse(StaffLeaveEngine.blocks(activeStaff, leave, "2026-10-10", "16:00"))
    }

    @Test
    fun inactiveStaff_cannotBeBookedEvenWithoutLeave() {
        val inactive = activeStaff.copy(active = false)
        assertFalse(StaffLeaveEngine.canBook(inactive, emptyList(), "2026-10-10", "10:00"))
    }

    @Test
    fun invalidLeaveDates_doNotCrashOrBlock() {
        val invalid = StaffLeave("bad", "s1", "not-a-date", "2026-10-10", null, null, "", 1L)
        assertFalse(StaffLeaveEngine.blocks(activeStaff, invalid, "2026-10-10", "10:00"))
    }
}
