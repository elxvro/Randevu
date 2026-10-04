package com.elxvro.randevu.online

import com.elxvro.randevu.business.BusinessProfile
import com.elxvro.randevu.business.ServiceRecord
import com.elxvro.randevu.core.Appointment
import com.elxvro.randevu.core.AppointmentStatus
import com.elxvro.randevu.staff.StaffLeave
import com.elxvro.randevu.staff.StaffRecord
import org.json.JSONObject

data class V2ProjectedSnapshot(
    val profile: BusinessProfile?,
    val services: List<ServiceRecord>,
    val staff: List<StaffRecord>,
    val leaves: List<StaffLeave>,
    val appointments: List<Appointment>,
    val versions: Map<String, Int>
)

object V2SnapshotProjector {
    fun project(snapshot: OnlineSnapshot, ownerName: String): V2ProjectedSnapshot {
        val versions = linkedMapOf<String, Int>()

        val services = snapshot.servicesJson.mapNotNull { raw ->
            runCatching {
                val json = JSONObject(raw)
                val id = json.getString("external_id").trim()
                val name = json.getString("name").trim()
                if (id.isBlank() || name.isBlank()) return@runCatching null
                versions["SERVICE:$id"] = json.optInt("version", 1)
                ServiceRecord(
                    id = id,
                    name = name,
                    durationMinutes = json.optInt("duration_minutes", 30).coerceAtLeast(5),
                    active = json.optBoolean("active", true)
                )
            }.getOrNull()
        }

        val staff = snapshot.staffJson.mapNotNull { raw ->
            runCatching {
                val json = JSONObject(raw)
                val id = json.getString("external_id").trim()
                val name = json.getString("name").trim()
                if (id.isBlank() || name.isBlank()) return@runCatching null
                versions["STAFF:$id"] = json.optInt("version", 1)
                StaffRecord(
                    id = id,
                    name = name,
                    title = json.optString("title").trim(),
                    phone = json.optString("phone").trim(),
                    active = json.optBoolean("active", true)
                )
            }.getOrNull()
        }

        val leaves = snapshot.staffLeavesJson.mapNotNull { raw ->
            runCatching {
                val json = JSONObject(raw)
                val id = json.getString("external_id").trim()
                val staffId = json.getString("staff_external_id").trim()
                if (id.isBlank() || staffId.isBlank()) return@runCatching null
                versions["STAFF_LEAVE:$id"] = json.optInt("version", 1)
                StaffLeave(
                    id = id,
                    staffId = staffId,
                    startDate = json.getString("start_date"),
                    endDate = json.getString("end_date"),
                    startTime = nullableString(json, "start_time"),
                    endTime = nullableString(json, "end_time"),
                    reason = json.optString("reason").trim(),
                    createdAt = 0L
                )
            }.getOrNull()
        }

        val appointments = snapshot.appointmentsJson.mapNotNull { raw ->
            runCatching {
                val json = JSONObject(raw)
                val id = json.getString("external_id").trim()
                if (id.isBlank()) return@runCatching null
                versions["APPOINTMENT:$id"] = json.optInt("version", 1)
                Appointment(
                    id = id,
                    customer = json.optString("customer_name").trim(),
                    phone = json.optString("customer_phone").trim(),
                    service = json.optString("service").trim(),
                    staff = json.optString("staff").trim(),
                    date = json.getString("date"),
                    time = json.getString("time"),
                    status = runCatching {
                        AppointmentStatus.valueOf(json.optString("status", "pending").uppercase())
                    }.getOrDefault(AppointmentStatus.PENDING),
                    note = json.optString("note").trim()
                )
            }.getOrNull()
        }

        val baseProfile = snapshot.businessJson?.let { raw ->
            runCatching {
                val json = JSONObject(raw)
                versions["BUSINESS:business"] = json.optInt("version", 1)
                BusinessProfile(
                    businessName = json.optString("business_name").trim(),
                    phone = json.optString("phone").trim(),
                    address = json.optString("address").trim(),
                    timezoneId = json.optString("timezone", "Europe/Istanbul").trim().ifBlank { "Europe/Istanbul" },
                    ownerName = ownerName.trim(),
                    setupCompleted = false,
                    openingTime = json.optString("opening_time", "09:00").take(5),
                    closingTime = json.optString("closing_time", "18:00").take(5)
                )
            }.getOrNull()
        }
        val profile = baseProfile?.copy(
            setupCompleted = baseProfile.businessName.isNotBlank() && services.any { it.active }
        )

        return V2ProjectedSnapshot(profile, services, staff, leaves, appointments, versions)
    }

    private fun nullableString(json: JSONObject, key: String): String? =
        if (json.isNull(key)) null else json.optString(key).trim().takeIf { it.isNotBlank() }
}
