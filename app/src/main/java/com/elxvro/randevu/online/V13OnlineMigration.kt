package com.elxvro.randevu.online

import com.elxvro.randevu.business.BusinessProfile
import com.elxvro.randevu.business.ServiceRecord
import com.elxvro.randevu.core.Appointment
import com.elxvro.randevu.staff.StaffLeave
import com.elxvro.randevu.staff.StaffRecord
import org.json.JSONObject

object V13OnlineMigration {
    fun plan(
        profile: BusinessProfile,
        services: List<ServiceRecord>,
        staff: List<StaffRecord>,
        leaves: List<StaffLeave>,
        appointments: List<Appointment>,
        importedIds: Set<String>,
        now: Long = System.currentTimeMillis()
    ): List<OnlineMutation> {
        val result = mutableListOf<OnlineMutation>()
        var sequence = 0L

        fun add(entity: OnlineEntityType, id: String, payload: JSONObject) {
            val marker = "${entity.name}:$id"
            if (marker in importedIds) return
            result += OnlineMutation(
                operationId = "import-${entity.name.lowercase()}-$id",
                entityType = entity,
                externalId = id,
                mutationType = OnlineMutationType.UPSERT,
                expectedVersion = null,
                payloadJson = payload.toString(),
                createdAtEpochMs = now + sequence++
            )
        }

        add(OnlineEntityType.BUSINESS, "business", JSONObject()
            .put("business_name", profile.businessName)
            .put("phone", profile.phone)
            .put("address", profile.address)
            .put("timezone", profile.timezoneId)
            .put("owner_name", profile.ownerName)
            .put("opening_time", profile.openingTime)
            .put("closing_time", profile.closingTime))

        services.forEach { item ->
            add(OnlineEntityType.SERVICE, item.id, JSONObject()
                .put("external_id", item.id)
                .put("name", item.name)
                .put("duration_minutes", item.durationMinutes)
                .put("active", item.active))
        }

        staff.forEach { item ->
            add(OnlineEntityType.STAFF, item.id, JSONObject()
                .put("external_id", item.id)
                .put("name", item.name)
                .put("title", item.title)
                .put("phone", item.phone)
                .put("active", item.active)
                .put("public_booking", true))
        }

        leaves.forEach { item ->
            add(OnlineEntityType.STAFF_LEAVE, item.id, JSONObject()
                .put("external_id", item.id)
                .put("staff_external_id", item.staffId)
                .put("start_date", item.startDate)
                .put("end_date", item.endDate)
                .put("start_time", item.startTime)
                .put("end_time", item.endTime)
                .put("reason", item.reason))
        }

        val serviceByName = services.associateBy { it.name.trim().lowercase() }
        val staffByName = staff.associateBy { it.name.trim().lowercase() }
        appointments.forEach { item ->
            val serviceId = serviceByName[item.service.trim().lowercase()]?.id.orEmpty()
            val staffId = staffByName[item.staff.trim().lowercase()]?.id.orEmpty()
            add(OnlineEntityType.APPOINTMENT, item.id, JSONObject()
                .put("external_id", item.id)
                .put("customer_name", item.customer)
                .put("customer_phone", item.phone)
                .put("service_external_id", serviceId)
                .put("staff_external_id", staffId)
                .put("date", item.date)
                .put("time", item.time)
                .put("status", item.status.name.lowercase())
                .put("note", item.note))
        }
        return result
    }
}
