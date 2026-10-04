package com.elxvro.randevu.online

import org.json.JSONObject

enum class OnlineEntityType { BUSINESS, SERVICE, STAFF, STAFF_LEAVE, APPOINTMENT }
enum class OnlineMutationType { UPSERT, DELETE }
enum class OnlineConnectionState { ONLINE, SYNCING, OFFLINE, AUTH_REQUIRED, CONFLICT }

data class OnlineMutation(
    val operationId: String,
    val entityType: OnlineEntityType,
    val externalId: String,
    val mutationType: OnlineMutationType,
    val expectedVersion: Int?,
    val payloadJson: String,
    val createdAtEpochMs: Long,
    val retryCount: Int = 0,
    val lastError: String? = null
)

data class SyncConflict(
    val entityType: OnlineEntityType,
    val externalId: String,
    val currentServerPayload: String
)

data class V2SyncOutcome(
    val state: OnlineConnectionState,
    val pendingCount: Int,
    val conflict: SyncConflict? = null,
    val message: String? = null
)

object OnlineSyncQueue {
    fun enqueue(current: List<OnlineMutation>, next: OnlineMutation): List<OnlineMutation> {
        val existing = current.firstOrNull {
            it.entityType == next.entityType && it.externalId == next.externalId
        } ?: return (current + next).sortedBy { it.createdAtEpochMs }

        if (existing.expectedVersion == null && next.mutationType == OnlineMutationType.DELETE) {
            return current.filterNot {
                it.entityType == next.entityType && it.externalId == next.externalId
            }
        }

        val merged = next.copy(
            operationId = existing.operationId,
            expectedVersion = existing.expectedVersion ?: next.expectedVersion,
            createdAtEpochMs = existing.createdAtEpochMs,
            retryCount = maxOf(existing.retryCount, next.retryCount),
            lastError = next.lastError ?: existing.lastError
        )
        return current.map {
            if (it.entityType == next.entityType && it.externalId == next.externalId) merged else it
        }.sortedBy { it.createdAtEpochMs }
    }

    fun remove(current: List<OnlineMutation>, operationId: String): List<OnlineMutation> =
        current.filterNot { it.operationId == operationId }
}

object OnlineSyncCore {
    fun failure(
        httpCode: Int?,
        entityType: OnlineEntityType?,
        currentServerPayload: String?
    ): V2SyncOutcome {
        if (httpCode == 401) return V2SyncOutcome(OnlineConnectionState.AUTH_REQUIRED, 0)
        if (httpCode == 409 && entityType != null && !currentServerPayload.isNullOrBlank()) {
            val id = runCatching { JSONObject(currentServerPayload).optString("external_id").trim() }
                .getOrDefault("")
            return V2SyncOutcome(
                state = OnlineConnectionState.CONFLICT,
                pendingCount = 0,
                conflict = SyncConflict(entityType, id, currentServerPayload)
            )
        }
        return V2SyncOutcome(OnlineConnectionState.OFFLINE, 0)
    }
}
