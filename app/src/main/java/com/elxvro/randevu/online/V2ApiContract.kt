package com.elxvro.randevu.online

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.json.JSONObject

object V2ApiContract {
    const val registerPath = "/v2/auth/register-owner"
    const val loginPath = "/v2/auth/login"
    const val logoutPath = "/v2/auth/logout"
    const val mePath = "/v2/me"
    const val bootstrapPath = "/v2/sync/bootstrap"

    fun normalizeBaseUrl(value: String): String {
        val trimmed = value.trim().trimEnd('/')
        if (trimmed.isBlank()) return ""
        return try {
            val uri = URI(trimmed)
            if (uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()) trimmed else ""
        } catch (_: Throwable) {
            ""
        }
    }

    fun appointmentPath(externalId: String): String =
        "/v2/appointments/${encodePath(externalId)}"

    fun servicePath(externalId: String): String =
        "/v2/services/${encodePath(externalId)}"

    fun staffPath(externalId: String): String =
        "/v2/staff/${encodePath(externalId)}"

    fun staffLeavePath(externalId: String): String =
        "/v2/staff-leaves/${encodePath(externalId)}"

    fun registerJson(
        ownerName: String,
        businessName: String,
        email: String,
        password: String,
        phone: String = "",
        address: String = "",
        timezone: String
    ): JSONObject = JSONObject()
        .put("owner_name", ownerName.trim())
        .put("business_name", businessName.trim())
        .put("email", email.trim().lowercase())
        .put("password", password)
        .put("phone", phone.trim())
        .put("address", address.trim())
        .put("timezone", timezone.trim())

    fun loginJson(email: String, password: String): JSONObject = JSONObject()
        .put("email", email.trim().lowercase())
        .put("password", password)

    private fun encodePath(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString()).replace("+", "%20")
}
