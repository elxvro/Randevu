package com.elxvro.randevu.staff

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StaffProjectionTest {
    private val staff = StaffRecord("s1", "Mert Yılmaz", "Uzman", "", true)

    @Test
    fun nextLeave_returnsNearestCurrentOrFutureLeave() {
        val leaves = listOf(
            StaffLeave("old", "s1", "2026-09-01", "2026-09-02", null, null, "Eski", 1L),
            StaffLeave("later", "s1", "2026-10-20", "2026-10-21", null, null, "Sonraki", 2L),
            StaffLeave("near", "s1", "2026-10-10", "2026-10-11", null, null, "Yakın", 3L)
        )
        assertEquals("near", StaffProjection.nextLeave(staff.id, leaves, LocalDate.of(2026, 10, 3))?.id)
    }

    @Test
    fun nextLeave_returnsNullWhenNothingUpcoming() {
        val leaves = listOf(StaffLeave("old", "s1", "2026-09-01", "2026-09-02", null, null, "Eski", 1L))
        assertNull(StaffProjection.nextLeave(staff.id, leaves, LocalDate.of(2026, 10, 3)))
    }
}
