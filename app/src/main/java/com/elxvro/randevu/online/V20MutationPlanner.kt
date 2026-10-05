package com.elxvro.randevu.online

import com.elxvro.randevu.business.BusinessProfile
import com.elxvro.randevu.business.ServiceRecord
import com.elxvro.randevu.core.Appointment
import com.elxvro.randevu.core.AppointmentAction
import com.elxvro.randevu.core.AppointmentStatus
import com.elxvro.randevu.staff.StaffLeave
import com.elxvro.randevu.staff.StaffRecord
import org.json.JSONObject

object V20MutationPlanner {
    fun business(
        profile: BusinessProfile,
        versions: Map<String, Int>,
        now: Long = System.currentTimeMillis()
    ): OnlineMutation = OnlineMutation(
        operationId = operationId(OnlineEntityType.BUSINESS, "business", now),
        entityType = OnlineEntityType.BUSINESS,
        externalId = "business",
        mutationType = OnlineMutationType.UPSERT,
        expectedVersion = versions[versionKey(OnlineEntityType.BUSINESS, "business")],
        payloadJson = JSONObject()
            .put("business_name", profile.businessName)
            .put("phone", profile.phone)
            .put("address", profile.address)
            .put("timezone", profile.timezoneId)
            .put("owner_name", profile.ownerName)
            .put("opening_time", profile.openingTime)
            .put("closing_time", profile.closingTime)
            .toString(),
        createdAtEpochMs = now
    )

    fun serviceChanges(
        before: List<ServiceRecord>,
        after: List<ServiceRecord>,
        versions: Map<String, Int>,
        now: Long = System.currentTimeMillis()
    ): List<OnlineMutation> {
        val old = before.associateBy { it.id }
        val next = after.associateBy { it.id }
        val operations = mutableListOf<OnlineMutation>()
        var tick = 0L

        after.forEach { item ->
            val previous = old[item.id]
            if (previous == null || previous != item) {
                operations += OnlineMutation(
                    operationId = operationId(OnlineEntityType.SERVICE, item.id, now + tick++),
                    entityType = OnlineEntityType.SERVICE,
                    externalId = item.id,
                    mutationType = OnlineMutationType.UPSERT,
                    expectedVersion = versions[versionKey(OnlineEntityType.SERVICE, item.id)],
                    payloadJson = JSONObject()
                        .put("external_id", item.id)
                        .put("name", item.name)
                        .put("duration_minutes", item.durationMinutes)
                        .put("active", item.active)
                        .toString(),
                    createdAtEpochMs = now + tick
                )
            }
        }

        before.filter { it.id !in next }.forEach { item ->
            operations += OnlineMutation(
                operationId = operationId(OnlineEntityType.SERVICE, item.id, now + tick++),
                entityType = OnlineEntityType.SERVICE,
                externalId = item.id,
                mutationType = OnlineMutationType.DELETE,
                expectedVersion = versions[versionKey(OnlineEntityType.SERVICE, item.id)],
                payloadJson = "{}",
                createdAtEpochMs = now + tick
            )
        }

        return operations
    }

    fun staffChanges(
        before: List<StaffRecord>,
        after: List<StaffRecord>,
        versions: Map<String, Int>,
        now: Long = System.currentTimeMillis()
    ): List<OnlineMutation> {
        val old = before.associateBy { it.id }
        val next = after.associateBy { it.id }
        val operations = mutableListOf<OnlineMutation>()
        var tick = 0L

        after.forEach { item ->
            if (old[item.id] != item) {
                operations += OnlineMutation(
                    operationId = operationId(OnlineEntityType.STAFF, item.id, now + tick++),
                    entityType = OnlineEntityType.STAFF,
                    externalId = item.id,
                    mutationType = OnlineMutationType.UPSERT,
                    expectedVersion = versions[versionKey(OnlineEntityType.STAFF, item.id)],
                    payloadJson = JSONObject()
                        .put("external_id", item.id)
                        .put("name", item.name)
                        .put("title", item.title)
                        .put("phone", item.phone)
                        .put("active", item.active)
                        .put("public_booking", true)
                        .toString(),
                    createdAtEpochMs = now + tick
                )
            }
        }
        before.filter { it.id !in next }.forEach { item ->
            operations += OnlineMutation(
                operationId = operationId(OnlineEntityType.STAFF, item.id, now + tick++),
                entityType = OnlineEntityType.STAFF,
                externalId = item.id,
                mutationType = OnlineMutationType.DELETE,
                expectedVersion = versions[versionKey(OnlineEntityType.STAFF, item.id)],
                payloadJson = "{}",
                createdAtEpochMs = now + tick
            )
        }
        return operations
    }

    fun leaveChanges(
        before: List<StaffLeave>,
        after: List<StaffLeave>,
        versions: Map<String, Int>,
        now: Long = System.currentTimeMillis()
    ): List<OnlineMutation> {
        val old = before.associateBy { it.id }
        val next = after.associateBy { it.id }
        val operations = mutableListOf<OnlineMutation>()
        var tick = 0L

        after.forEach { item ->
            if (old[item.id] != item) {
                operations += OnlineMutation(
                    operationId = operationId(OnlineEntityType.STAFF_LEAVE, item.id, now + tick++),
                    entityType = OnlineEntityType.STAFF_LEAVE,
                    externalId = item.id,
                    mutationType = OnlineMutationType.UPSERT,
                    expectedVersion = versions[versionKey(OnlineEntityType.STAFF_LEAVE, item.id)],
                    payloadJson = JSONObject()
                        .put("external_id", item.id)
                        .put("staff_external_id", item.staffId)
                        .put("start_date", item.startDate)
                        .put("end_date", item.endDate)
                        .put("start_time", item.startTime)
                        .put("end_time", item.endTime)
                        .put("reason", item.reason)
                        .toString(),
                    createdAtEpochMs = now + tick
                )
            }
        }
        before.filter { it.id !in next }.forEach { item ->
            operations += OnlineMutation(
                operationId = operationId(OnlineEntityType.STAFF_LEAVE, item.id, now + tick++),
                entityType = OnlineEntityType.STAFF_LEAVE,
                externalId = item.id,
                mutationType = OnlineMutationType.DELETE,
                expectedVersion = versions[versionKey(OnlineEntityType.STAFF_LEAVE, item.id)],
                payloadJson = "{}",
                createdAtEpochMs = now + tick
            )
        }
        return operations
    }

    fun appointment(
        action: AppointmentAction,
        before: List<Appointment>,
        services: List<ServiceRecord>,
        staff: List<StaffRecord>,
        versions: Map<String, Int>,
        now: Long = System.currentTimeMillis()
    ): OnlineMutation? {
        val item: Appointment
        val mutationType: OnlineMutationType

        when (action) {
            is AppointmentAction.Add -> {
                item = action.appointment
                mutationType = OnlineMutationType.UPSERT
            }
            is AppointmentAction.Update -> {
                item = action.appointment
                mutationType = OnlineMutationType.UPSERT
            }
            is AppointmentAction.Delete -> {
                return OnlineMutation(
                    operationId = operationId(OnlineEntityType.APPOINTMENT, action.id, now),
                    entityType = OnlineEntityType.APPOINTMENT,
                    externalId = action.id,
                    mutationType = OnlineMutationType.DELETE,
                    expectedVersion = versions[versionKey(OnlineEntityType.APPOINTMENT, action.id)],
                    payloadJson = "{}",
                    createdAtEpochMs = now
                )
            }
            is AppointmentAction.ChangeStatus -> {
                val current = before.firstOrNull { it.id == action.id } ?: return null
                item = current.copy(status = action.status)
                mutationType = OnlineMutationType.UPSERT
            }
        }

        val serviceId = services.firstOrNull { it.name == item.service }?.id ?: return null
        val staffId = staff.firstOrNull { it.name == item.staff }?.id ?: return null

        return OnlineMutation(
            operationId = operationId(OnlineEntityType.APPOINTMENT, item.id, now),
            entityType = OnlineEntityType.APPOINTMENT,
            externalId = item.id,
            mutationType = mutationType,
            expectedVersion = versions[versionKey(OnlineEntityType.APPOINTMENT, item.id)],
            payloadJson = JSONObject()
                .put("external_id", item.id)
                .put("customer_name", item.customer)
                .put("customer_phone", item.phone)
                .put("service_external_id", serviceId)
                .put("staff_external_id", staffId)
                .put("date", item.date)
                .put("time", item.time)
                .put("status", statusValue(item.status))
                .put("note", item.note)
                .toString(),
            createdAtEpochMs = now
        )
    }

    private fun statusValue(status: AppointmentStatus): String = when (status) {
        AppointmentStatus.PENDING -> "pending"
        AppointmentStatus.CONFIRMED -> "confirmed"
        AppointmentStatus.COMPLETED -> "completed"
        AppointmentStatus.CANCELLED -> "cancelled"
    }

    private fun versionKey(entity: OnlineEntityType, id: String): String = "${entity.name}:$id"

    private fun operationId(entity: OnlineEntityType, id: String, now: Long): String =
        "v2-${entity.name.lowercase()}-$id-$now"
}
