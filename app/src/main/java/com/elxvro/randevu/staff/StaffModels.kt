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

fun defaultStaffRecords(): List<StaffRecord> = listOf(
    StaffRecord("staff-mert", "Mert Yılmaz", "Uzman", "+905339876543", true),
    StaffRecord("staff-zeynep", "Zeynep Arslan", "Uzman", "+905321234567", true),
    StaffRecord("staff-deniz", "Deniz Arıcı", "Uzman", "+905327894561", true)
)
