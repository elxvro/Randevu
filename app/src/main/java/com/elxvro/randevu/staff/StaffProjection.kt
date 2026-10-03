package com.elxvro.randevu.staff

import java.time.LocalDate

object StaffProjection {
    fun nextLeave(staffId: String, leaves: List<StaffLeave>, today: LocalDate): StaffLeave? = leaves
        .asSequence()
        .filter { it.staffId == staffId }
        .mapNotNull { leave ->
            val start = runCatching { LocalDate.parse(leave.startDate) }.getOrNull() ?: return@mapNotNull null
            val end = runCatching { LocalDate.parse(leave.endDate) }.getOrNull() ?: return@mapNotNull null
            if (end.isBefore(today)) null else Triple(leave, start, end)
        }
        .sortedWith(compareBy<Triple<StaffLeave, LocalDate, LocalDate>> { if (it.second.isBefore(today)) today else it.second }
            .thenBy { it.first.createdAt })
        .map { it.first }
        .firstOrNull()

    fun leavesFor(staffId: String, leaves: List<StaffLeave>): List<StaffLeave> = leaves
        .filter { it.staffId == staffId }
        .sortedWith(compareBy<StaffLeave> { it.startDate }.thenBy { it.startTime.orEmpty() })
}
