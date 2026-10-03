package com.elxvro.randevu.network

import com.elxvro.randevu.business.BusinessProfile
import com.elxvro.randevu.core.Appointment
import com.elxvro.randevu.core.AppointmentStatus
import com.elxvro.randevu.core.WhatsAppConnectionState
import com.elxvro.randevu.core.WhatsAppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets


data class WhatsAppServerConfig(
    val baseUrl: String = "",
    val token: String = ""
) {
    val configured: Boolean get() = baseUrl.isNotBlank() && token.isNotBlank()
}

data class WhatsAppStatusSnapshot(
    val state: WhatsAppConnectionState,
    val settings: WhatsAppSettings,
    val error: String? = null
)

data class WhatsAppBootstrapResult(
    val token: String,
    val expiresAt: String
)

data class WhatsAppScheduleResult(
    val acknowledged: Boolean,
    val scheduledCount: Int,
    val error: String? = null
)

data class WhatsAppCallResult<T>(
    val ok: Boolean,
    val value: T? = null,
    val error: String? = null,
    val httpCode: Int? = null
)

object WhatsAppApiContract {
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

    fun reminderPath(appointmentId: String): String =
        "/appointments/${URLEncoder.encode(appointmentId, StandardCharsets.UTF_8.toString()).replace("+", "%20")}/reminders"

    fun appointmentJson(appointment: Appointment): JSONObject = JSONObject().apply {
        put("customer_name", appointment.customer)
        put("customer_phone", appointment.phone)
        put("date", appointment.date)
        put("time", appointment.time)
        put("service", appointment.service)
        put("staff", appointment.staff)
        put("status", when (appointment.status) {
            AppointmentStatus.PENDING -> "pending"
            AppointmentStatus.CONFIRMED -> "confirmed"
            AppointmentStatus.COMPLETED -> "completed"
            AppointmentStatus.CANCELLED -> "cancelled"
        })
    }

    fun bootstrapJson(setupKey: String, profile: BusinessProfile): JSONObject = JSONObject().apply {
        put("setup_key", setupKey)
        put("business_name", profile.businessName)
        put("owner_name", profile.ownerName)
        put("phone", profile.phone)
        put("address", profile.address)
        put("timezone", profile.timezoneId)
    }

    fun profileJson(profile: BusinessProfile): JSONObject = JSONObject().apply {
        put("business_name", profile.businessName)
        put("phone", profile.phone)
        put("address", profile.address)
        put("timezone", profile.timezoneId)
    }

    fun settingsJson(settings: WhatsAppSettings): JSONObject = JSONObject().apply {
        put("enabled", settings.enabled)
        put("reminder_24h", settings.reminder24h)
        put("reminder_2h", settings.reminder2h)
        put("template_name", settings.templateName.trim())
        put("template_language", settings.languageCode.trim())
        put("meta_phone_number_id", settings.metaPhoneNumberId.trim())
    }

    fun parseConnectionState(value: String?): WhatsAppConnectionState = when (value?.lowercase()) {
        "connected" -> WhatsAppConnectionState.CONNECTED
        "incomplete" -> WhatsAppConnectionState.INCOMPLETE
        "disabled" -> WhatsAppConnectionState.DISABLED
        else -> WhatsAppConnectionState.UNREACHABLE
    }

    fun statusFromJson(root: JSONObject, fallback: WhatsAppSettings): WhatsAppStatusSnapshot {
        val json = root.optJSONObject("whatsapp") ?: root
        val state = parseConnectionState(json.optString("state", "unreachable"))
        return WhatsAppStatusSnapshot(
            state = state,
            settings = fallback.copy(
                enabled = json.optBoolean("enabled", fallback.enabled),
                reminder24h = json.optBoolean("reminder_24h", fallback.reminder24h),
                reminder2h = json.optBoolean("reminder_2h", fallback.reminder2h),
                templateName = json.optString("template_name", fallback.templateName),
                languageCode = json.optString("template_language", fallback.languageCode)
            ),
            error = json.optString("error").takeIf { it.isNotBlank() && it != "null" }
        )
    }
}

class WhatsAppApiClient {
    suspend fun bootstrap(baseUrl: String, setupKey: String, profile: BusinessProfile): WhatsAppCallResult<WhatsAppBootstrapResult> =
        request(baseUrl, "/auth/bootstrap", "POST", null, WhatsAppApiContract.bootstrapJson(setupKey, profile)) { json ->
            WhatsAppBootstrapResult(json.getString("token"), json.optString("expires_at"))
        }

    suspend fun status(config: WhatsAppServerConfig, fallback: WhatsAppSettings): WhatsAppCallResult<WhatsAppStatusSnapshot> =
        request(config.baseUrl, "/whatsapp/status", "GET", config.token, null) { json ->
            WhatsAppApiContract.statusFromJson(json, fallback)
        }

    suspend fun saveProfile(config: WhatsAppServerConfig, profile: BusinessProfile): WhatsAppCallResult<Unit> =
        request(config.baseUrl, "/business/profile", "PUT", config.token, WhatsAppApiContract.profileJson(profile)) { Unit }

    suspend fun saveSettings(config: WhatsAppServerConfig, settings: WhatsAppSettings): WhatsAppCallResult<WhatsAppStatusSnapshot> =
        request(config.baseUrl, "/business/whatsapp-settings", "PUT", config.token, WhatsAppApiContract.settingsJson(settings)) { json ->
            WhatsAppApiContract.statusFromJson(json, settings)
        }

    suspend fun testConnection(config: WhatsAppServerConfig, fallback: WhatsAppSettings): WhatsAppCallResult<WhatsAppStatusSnapshot> =
        request(config.baseUrl, "/whatsapp/test-connection", "POST", config.token, JSONObject()) { json ->
            WhatsAppApiContract.statusFromJson(json, fallback)
        }

    suspend fun upsertReminder(config: WhatsAppServerConfig, appointment: Appointment): WhatsAppCallResult<WhatsAppScheduleResult> =
        request(config.baseUrl, WhatsAppApiContract.reminderPath(appointment.id), "PUT", config.token, WhatsAppApiContract.appointmentJson(appointment)) { json ->
            WhatsAppScheduleResult(
                acknowledged = json.optBoolean("ok", false),
                scheduledCount = json.optInt("scheduled", 0)
            )
        }

    suspend fun cancelReminder(config: WhatsAppServerConfig, appointmentId: String): WhatsAppCallResult<WhatsAppScheduleResult> =
        request(config.baseUrl, WhatsAppApiContract.reminderPath(appointmentId), "DELETE", config.token, null) { json ->
            WhatsAppScheduleResult(
                acknowledged = json.optBoolean("ok", false),
                scheduledCount = json.optInt("scheduled", 0)
            )
        }

    private suspend fun <T> request(
        baseUrl: String,
        path: String,
        method: String,
        token: String?,
        body: JSONObject?,
        parser: (JSONObject) -> T
    ): WhatsAppCallResult<T> = withContext(Dispatchers.IO) {
        val normalized = WhatsAppApiContract.normalizeBaseUrl(baseUrl)
        if (normalized.isBlank()) return@withContext WhatsAppCallResult(false, error = "https_required")
        var connection: HttpURLConnection? = null
        try {
            connection = URL(normalized + path).openConnection() as HttpURLConnection
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (!token.isNullOrBlank()) connection.setRequestProperty("Authorization", "Bearer ${token.trim()}")
            if (body != null && method != "GET") {
                connection.doOutput = true
                connection.outputStream.use { it.write(body.toString().toByteArray(StandardCharsets.UTF_8)) }
            }
            val code = connection.responseCode
            val raw = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            val json = if (raw.isBlank()) JSONObject() else JSONObject(raw)
            if (code !in 200..299 || !json.optBoolean("ok", code in 200..299)) {
                val error = json.optString("error", "http_$code").ifBlank { "http_$code" }
                return@withContext WhatsAppCallResult(false, error = error, httpCode = code)
            }
            WhatsAppCallResult(true, parser(json), httpCode = code)
        } catch (_: Throwable) {
            WhatsAppCallResult(false, error = "unreachable")
        } finally {
            connection?.disconnect()
        }
    }
}
