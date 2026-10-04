package com.elxvro.randevu.storage

import com.elxvro.randevu.online.OnlineEntityType
import com.elxvro.randevu.online.OnlineMutation
import com.elxvro.randevu.online.OnlineMutationType
import com.elxvro.randevu.online.OnlineSnapshot
import com.elxvro.randevu.online.SyncConflict
import org.json.JSONArray
import org.json.JSONObject

object V2OnlineCodec {
    fun encodePending(value: List<OnlineMutation>): String {
        val array = JSONArray()
        value.forEach { item ->
            array.put(JSONObject()
                .put("operation_id", item.operationId)
                .put("entity_type", item.entityType.name)
                .put("external_id", item.externalId)
                .put("mutation_type", item.mutationType.name)
                .put("expected_version", item.expectedVersion)
                .put("payload_json", item.payloadJson)
                .put("created_at", item.createdAtEpochMs)
                .put("retry_count", item.retryCount)
                .put("last_error", item.lastError))
        }
        return array.toString()
    }

    fun decodePending(raw: String?): List<OnlineMutation> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val json = array.optJSONObject(index) ?: continue
                    add(OnlineMutation(
                        operationId = json.getString("operation_id"),
                        entityType = OnlineEntityType.valueOf(json.getString("entity_type")),
                        externalId = json.getString("external_id"),
                        mutationType = OnlineMutationType.valueOf(json.getString("mutation_type")),
                        expectedVersion = if (json.isNull("expected_version")) null else json.getInt("expected_version"),
                        payloadJson = json.optString("payload_json", "{}"),
                        createdAtEpochMs = json.optLong("created_at"),
                        retryCount = json.optInt("retry_count"),
                        lastError = json.optString("last_error").takeIf { it.isNotBlank() && it != "null" }
                    ))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun encodeVersions(value: Map<String, Int>): String {
        val json = JSONObject()
        value.forEach { (key, version) -> json.put(key, version) }
        return json.toString()
    }

    fun decodeVersions(raw: String?): Map<String, Int> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            buildMap {
                json.keys().forEach { key -> put(key, json.getInt(key)) }
            }
        }.getOrDefault(emptyMap())
    }

    fun encodeConflict(value: SyncConflict?): String? = value?.let {
        JSONObject()
            .put("entity_type", it.entityType.name)
            .put("external_id", it.externalId)
            .put("current_server_payload", it.currentServerPayload)
            .toString()
    }

    fun decodeConflict(raw: String?): SyncConflict? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val json = JSONObject(raw)
            SyncConflict(
                entityType = OnlineEntityType.valueOf(json.getString("entity_type")),
                externalId = json.getString("external_id"),
                currentServerPayload = json.getString("current_server_payload")
            )
        }.getOrNull()
    }

    fun encodeImported(value: Set<String>): String = JSONArray(value.toList().sorted()).toString()

    fun decodeImported(raw: String?): Set<String> {
        if (raw.isNullOrBlank()) return emptySet()
        return runCatching {
            val array = JSONArray(raw)
            buildSet { for (index in 0 until array.length()) add(array.getString(index)) }
        }.getOrDefault(emptySet())
    }

    fun encodeSnapshot(value: OnlineSnapshot): String = JSONObject()
        .put("server_time", value.serverTime)
        .put("business_json", value.businessJson)
        .put("services_json", JSONArray(value.servicesJson))
        .put("staff_json", JSONArray(value.staffJson))
        .put("staff_leaves_json", JSONArray(value.staffLeavesJson))
        .put("appointments_json", JSONArray(value.appointmentsJson))
        .toString()

    fun decodeSnapshot(raw: String?): OnlineSnapshot? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val json = JSONObject(raw)
            OnlineSnapshot(
                serverTime = json.optString("server_time"),
                businessJson = json.optString("business_json").takeIf { it.isNotBlank() && it != "null" },
                servicesJson = strings(json.optJSONArray("services_json")),
                staffJson = strings(json.optJSONArray("staff_json")),
                staffLeavesJson = strings(json.optJSONArray("staff_leaves_json")),
                appointmentsJson = strings(json.optJSONArray("appointments_json"))
            )
        }.getOrNull()
    }

    private fun strings(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) add(array.optString(index))
        }
    }
}
