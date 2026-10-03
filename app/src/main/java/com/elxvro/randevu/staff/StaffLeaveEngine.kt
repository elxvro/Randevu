package com.elxvro.randevu.staff

import java.time.LocalDate
import java.time.LocalTime

object StaffLeaveEngine {
    fun blocks(staff: StaffRecord, leave: StaffLeave, date: String, time: String): Boolean {
        if (leave.staffId != staff.id) return false
        val targetDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return false
        val startDate = runCatching { LocalDate.parse(leave.startDate) }.getOrNull() ?: return false
        val endDate = runCatching { LocalDate.parse(leave.endDate) }.getOrNull() ?: return false
        if (endDate.isBefore(startDate)) return false
        if (targetDate.isBefore(startDate) || targetDate.isAfter(endDate)) return false

        val startRaw = leave.startTime?.takeIf { it.isNotBlank() }
        val endRaw = leave.endTime?.takeIf { it.isNotBlank() }
        if (startRaw == null && endRaw == null) return true

        val targetTime = runCatching { LocalTime.parse(time) }.getOrNull() ?: return false
        val startTime = startRaw?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
        val endTime = endRaw?.let { runCatching { LocalTime.parse(it) }.getOrNull() }

        if (startDate == endDate) {
            if (startTime == null || endTime == null || !endTime.isAfter(startTime)) return false
            return !targetTime.isBefore(startTime) && targetTime.isBefore(endTime)
        }

        return when {
            targetDate == startDate && startTime != null -> !targetTime.isBefore(startTime)
            targetDate == endDate && endTime != null -> targetTime.isBefore(endTime)
            targetDate.isAfter(startDate) && targetDate.isBefore(endDate) -> true
            else -> startTime == null || endTime == null
        }
    }

    fun canBook(staff: StaffRecord, leaves: List<StaffLeave>, date: String, time: String): Boolean =
        staff.active && leaves.none { blocks(staff, it, date, time) }

    fun isValid(leave: StaffLeave): Boolean {
        val startDate = runCatching { LocalDate.parse(leave.startDate) }.getOrNull() ?: return false
        val endDate = runCatching { LocalDate.parse(leave.endDate) }.getOrNull() ?: return false
        if (endDate.isBefore(startDate)) return false
        val startRaw = leave.startTime?.takeIf { it.isNotBlank() }
        val endRaw = leave.endTime?.takeIf { it.isNotBlank() }
        if (startRaw == null && endRaw == null) return true
        if (startRaw == null || endRaw == null) return false
        val startTime = runCatching { LocalTime.parse(startRaw) }.getOrNull() ?: return false
        val endTime = runCatching { LocalTime.parse(endRaw) }.getOrNull() ?: return false
        return startDate != endDate || endTime.isAfter(startTime)
    }
}
