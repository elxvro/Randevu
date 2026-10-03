package com.elxvro.randevu.ui

import com.elxvro.randevu.staff.StaffLeave
import com.elxvro.randevu.staff.StaffRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReferenceBookingRulesTest {
    private val staff = StaffRecord("s1", "Mert Yılmaz", "Uzman", "", true)

    @Test
    fun leaveBlockedStaff_returnsTurkishError() {
        val leave = StaffLeave("l1", "s1", "2026-10-10", "2026-10-10", null, null, "İzin", 1L)
        assertEquals(
            "Bu personel seçilen tarihte izinli.",
            ReferenceBookingRules.validationError(staff, listOf(leave), "2026-10-10", "11:00")
        )
    }

    @Test
    fun inactiveStaff_returnsTurkishError() {
        assertEquals(
            "Seçilen personel aktif değil.",
            ReferenceBookingRules.validationError(staff.copy(active = false), emptyList(), "2026-10-10", "11:00")
        )
    }

    @Test
    fun validBookingOutsideLeave_returnsNoError() {
        val leave = StaffLeave("l1", "s1", "2026-10-11", "2026-10-11", null, null, "İzin", 1L)
        assertNull(ReferenceBookingRules.validationError(staff, listOf(leave), "2026-10-10", "11:00"))
    }
}
