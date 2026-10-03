package com.elxvro.randevu.business

import java.time.ZoneId

data class BusinessProfile(
    val businessName: String,
    val phone: String,
    val address: String,
    val timezoneId: String,
    val ownerName: String,
    val setupCompleted: Boolean,
    val openingTime: String = "09:00",
    val closingTime: String = "18:00"
) {
    companion object {
        fun unconfigured(timezoneId: String = ZoneId.systemDefault().id) = BusinessProfile("", "", "", timezoneId, "", false)
    }
}

data class ServiceRecord(val id: String, val name: String, val durationMinutes: Int, val active: Boolean)
