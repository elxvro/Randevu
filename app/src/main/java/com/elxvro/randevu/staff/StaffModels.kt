package com.elxvro.randevu.staff

data class StaffRecord(
    val id: String,
    val name: String,
    val title: String,
    val phone: String,
    val active: Boolean
)

data class StaffLeave(
    val id: String,
    val staffId: String,
    val startDate: String,
    val endDate: String,
    val startTime: String?,
    val endTime: String?,
    val reason: String,
    val createdAt: Long
)

@Deprecated("v1.3.0 no longer seeds staff", level = DeprecationLevel.WARNING)
fun defaultStaffRecords(): List<StaffRecord> = emptyList()
