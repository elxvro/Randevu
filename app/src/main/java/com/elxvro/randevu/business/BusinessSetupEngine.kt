package com.elxvro.randevu.business

import java.time.ZoneId

object BusinessSetupEngine {
    fun canComplete(profile: BusinessProfile, services: List<ServiceRecord>): Boolean =
        profile.businessName.trim().isNotEmpty() &&
            isValidTimezone(profile.timezoneId) &&
            activeServices(services).isNotEmpty()

    fun activeServices(services: List<ServiceRecord>): List<ServiceRecord> =
        services.filter { it.active && it.name.trim().isNotEmpty() && it.durationMinutes > 0 }

    fun isValidTimezone(value: String): Boolean = runCatching { ZoneId.of(value.trim()) }.isSuccess

    fun validatedTimezone(value: String, fallback: String): String {
        val candidate = value.trim()
        if (isValidTimezone(candidate)) return candidate
        val safeFallback = fallback.trim()
        return if (isValidTimezone(safeFallback)) safeFallback else "UTC"
    }
}
