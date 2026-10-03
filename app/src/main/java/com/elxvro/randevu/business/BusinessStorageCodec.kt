package com.elxvro.randevu.business

import java.time.ZoneId
import org.json.JSONArray
import org.json.JSONObject

object BusinessStorageCodec {
    fun encodeProfile(profile: BusinessProfile): String = JSONObject()
        .put("business_name", profile.businessName)
        .put("phone", profile.phone)
        .put("address", profile.address)
        .put("timezone_id", profile.timezoneId)
        .put("owner_name", profile.ownerName)
        .put("setup_completed", profile.setupCompleted)
        .put("opening_time", profile.openingTime)
        .put("closing_time", profile.closingTime)
        .toString()

    fun decodeProfile(raw: String?, fallbackTimezone: String = ZoneId.systemDefault().id): BusinessProfile {
        if (raw.isNullOrBlank()) return BusinessProfile.unconfigured(fallbackTimezone)
        return runCatching {
            val json = JSONObject(raw)
            val zone = BusinessSetupEngine.validatedTimezone(json.optString("timezone_id"), fallbackTimezone)
            BusinessProfile(
                businessName = json.optString("business_name").trim(),
                phone = json.optString("phone").trim(),
                address = json.optString("address").trim(),
                timezoneId = zone,
                ownerName = json.optString("owner_name").trim(),
                setupCompleted = json.optBoolean("setup_completed", false),
                openingTime = json.optString("opening_time", "09:00").ifBlank { "09:00" },
                closingTime = json.optString("closing_time", "18:00").ifBlank { "18:00" }
            ).let { profile ->
                if (BusinessSetupEngine.isValidTimezone(json.optString("timezone_id"))) profile
                else profile.copy(setupCompleted = false)
            }
        }.getOrElse { BusinessProfile.unconfigured(fallbackTimezone) }
    }

    fun encodeServices(services: List<ServiceRecord>): String {
        val array = JSONArray()
        services.forEach { service ->
            array.put(JSONObject()
                .put("id", service.id)
                .put("name", service.name)
                .put("duration_minutes", service.durationMinutes)
                .put("active", service.active))
        }
        return array.toString()
    }

    fun decodeServices(raw: String?): List<ServiceRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val json = array.optJSONObject(index) ?: continue
                    val id = json.optString("id").trim()
                    val name = json.optString("name").trim()
                    val duration = json.optInt("duration_minutes", 0)
                    if (id.isBlank() || name.isBlank() || duration <= 0) continue
                    add(ServiceRecord(id, name, duration, json.optBoolean("active", true)))
                }
            }
        }.getOrDefault(emptyList())
    }
}
