package com.elxvro.randevu.online

import org.json.JSONArray
import org.json.JSONObject

data class V2AuthenticatedSession(
    val token: String,
    val session: V2Session
)

object V2ApiParser {
    fun authSession(root: JSONObject): V2AuthenticatedSession {
        val json = root.optJSONObject("session") ?: root
        val token = json.getString("token").trim()
        require(token.isNotBlank())
        return V2AuthenticatedSession(
            token = token,
            session = V2Session(
                ownerName = json.optString("owner_name").trim(),
                email = json.getString("email").trim().lowercase(),
                businessSlug = json.getString("business_slug").trim(),
                expiresAt = json.optString("expires_at").trim()
            )
        )
    }

    fun bootstrap(root: JSONObject): OnlineSnapshot = OnlineSnapshot(
        serverTime = root.optString("server_time"),
        businessJson = root.optJSONObject("business")?.toString(),
        servicesJson = strings(root.optJSONArray("services")),
        staffJson = strings(root.optJSONArray("staff")),
        staffLeavesJson = strings(root.optJSONArray("staff_leaves")),
        appointmentsJson = strings(root.optJSONArray("appointments"))
    )

    private fun strings(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                array.optJSONObject(index)?.let { add(it.toString()) }
            }
        }
    }
}
