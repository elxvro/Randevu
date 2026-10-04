package com.elxvro.randevu.online

import org.json.JSONObject

data class V2HttpMutationRequest(
    val method: String,
    val path: String,
    val bodyJson: String?
)

object V2MutationRequestFactory {
    fun needsResolvedVersion(mutation: OnlineMutation): Boolean =
        mutation.entityType == OnlineEntityType.BUSINESS &&
            mutation.mutationType == OnlineMutationType.UPSERT &&
            mutation.expectedVersion == null

    fun create(mutation: OnlineMutation, resolvedVersion: Int? = null): V2HttpMutationRequest {
        val version = mutation.expectedVersion ?: resolvedVersion
        val collectionPath = when (mutation.entityType) {
            OnlineEntityType.BUSINESS -> "/v2/business"
            OnlineEntityType.SERVICE -> "/v2/services"
            OnlineEntityType.STAFF -> "/v2/staff"
            OnlineEntityType.STAFF_LEAVE -> "/v2/staff-leaves"
            OnlineEntityType.APPOINTMENT -> "/v2/appointments"
        }
        val itemPath = when (mutation.entityType) {
            OnlineEntityType.BUSINESS -> collectionPath
            OnlineEntityType.SERVICE -> V2ApiContract.servicePath(mutation.externalId)
            OnlineEntityType.STAFF -> V2ApiContract.staffPath(mutation.externalId)
            OnlineEntityType.STAFF_LEAVE -> V2ApiContract.staffLeavePath(mutation.externalId)
            OnlineEntityType.APPOINTMENT -> V2ApiContract.appointmentPath(mutation.externalId)
        }

        if (mutation.mutationType == OnlineMutationType.DELETE) {
            require(mutation.entityType != OnlineEntityType.BUSINESS)
            requireNotNull(version)
            return V2HttpMutationRequest(
                method = "DELETE",
                path = itemPath,
                bodyJson = JSONObject().put("expected_version", version).toString()
            )
        }

        val payload = if (mutation.payloadJson.isBlank()) JSONObject() else JSONObject(mutation.payloadJson)
        if (mutation.entityType != OnlineEntityType.BUSINESS && mutation.expectedVersion == null) {
            payload.put("external_id", mutation.externalId)
            return V2HttpMutationRequest("POST", collectionPath, payload.toString())
        }

        requireNotNull(version)
        payload.put("expected_version", version)
        return V2HttpMutationRequest("PUT", itemPath, payload.toString())
    }
}
