package com.elxvro.randevu.network

import com.elxvro.randevu.core.Appointment
import com.elxvro.randevu.core.AppointmentStatus
import com.elxvro.randevu.core.BackendSession
import com.elxvro.randevu.core.LiveSyncApiContract
import com.elxvro.randevu.core.PendingSyncOperation
import com.elxvro.randevu.core.SyncOperationType
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class LiveApiResult<T>(
    val ok: Boolean,
    val value: T? = null,
    val message: String = ""
)

object LiveApiClient {
    suspend fun login(baseUrl: String, name: String, phone: String): LiveApiResult<BackendSession> {
        val body = JSONObject()
            .put("name", name.trim())
            .put("phone", phone.trim())
            .put("role", "business")
        return runCatching {
            val response = request("POST", baseUrl, LiveSyncApiContract.loginPath(), body = body)
            if (response.code !in 200..299 || !response.json.optBoolean("ok")) {
                return LiveApiResult(false, message = response.message())
            }
            val user = response.json.optJSONObject("user") ?: JSONObject()
            val token = response.json.optString("token")
            if (token.isBlank()) return LiveApiResult(false, message = "Sunucu oturum anahtarı döndürmedi")
            LiveApiResult(
                ok = true,
                value = BackendSession(
                    authenticated = true,
                    userId = user.optLong("id").takeIf { it > 0 },
                    token = token,
                    displayName = user.optString("name", name).trim(),
                    phone = user.optString("phone", phone).trim()
                ),
                message = "Oturum açıldı"
            )
        }.getOrElse { LiveApiResult(false, message = readableError(it)) }
    }

    suspend fun listAppointments(baseUrl: String, token: String): LiveApiResult<List<Appointment>> =
        runCatching {
            val response = request(
                method = "GET",
                baseUrl = baseUrl,
                path = LiveSyncApiContract.appointmentsPath(),
                token = token
            )
            if (response.code !in 200..299 || !response.json.optBoolean("ok")) {
                return LiveApiResult(false, message = response.message())
            }
            val array = response.json.optJSONArray("appointments") ?: JSONArray()
            val items = buildList {
                for (index in 0 until array.length()) {
                    array.optJSONObject(index)?.let { add(parseAppointment(it)) }
                }
            }
            LiveApiResult(true, items, "${items.size} randevu alındı")
        }.getOrElse { LiveApiResult(false, message = readableError(it)) }

    suspend fun createAppointment(
        baseUrl: String,
        token: String,
        appointment: Appointment
    ): LiveApiResult<Appointment> = runCatching {
        val response = request(
            method = "POST",
            baseUrl = baseUrl,
            path = LiveSyncApiContract.appointmentsPath(),
            token = token,
            body = appointmentBody(appointment)
        )
        if (response.code !in 200..299 || !response.json.optBoolean("ok")) {
            return LiveApiResult(false, message = response.message())
        }
        val json = response.json.optJSONObject("appointment")
            ?: return LiveApiResult(false, message = "Randevu verisi alınamadı")
        LiveApiResult(true, parseAppointment(json), "Randevu sunucuya kaydedildi")
    }.getOrElse { LiveApiResult(false, message = readableError(it)) }

    suspend fun updateAppointment(
        baseUrl: String,
        token: String,
        appointment: Appointment
    ): LiveApiResult<Appointment> = runCatching {
        val response = request(
            method = "POST",
            baseUrl = baseUrl,
            path = LiveSyncApiContract.appointmentPath(appointment.id),
            token = token,
            body = appointmentBody(appointment)
        )
        if (response.code !in 200..299 || !response.json.optBoolean("ok")) {
            return LiveApiResult(false, message = response.message())
        }
        val json = response.json.optJSONObject("appointment")
            ?: return LiveApiResult(false, message = "Güncel randevu alınamadı")
        LiveApiResult(true, parseAppointment(json), "Randevu güncellendi")
    }.getOrElse { LiveApiResult(false, message = readableError(it)) }

    suspend fun updateStatus(
        baseUrl: String,
        token: String,
        appointmentId: String,
        status: AppointmentStatus
    ): LiveApiResult<Appointment> = runCatching {
        val response = request(
            method = "POST",
            baseUrl = baseUrl,
            path = LiveSyncApiContract.statusPath(appointmentId),
            token = token,
            body = JSONObject().put("status", LiveSyncApiContract.toWireStatus(status))
        )
        if (response.code !in 200..299 || !response.json.optBoolean("ok")) {
            return LiveApiResult(false, message = response.message())
        }
        val json = response.json.optJSONObject("appointment")
            ?: return LiveApiResult(false, message = "Durum yanıtı alınamadı")
        LiveApiResult(true, parseAppointment(json), "Randevu durumu güncellendi")
    }.getOrElse { LiveApiResult(false, message = readableError(it)) }

    suspend fun deleteAppointment(
        baseUrl: String,
        token: String,
        appointmentId: String
    ): LiveApiResult<Unit> = runCatching {
        val response = request(
            method = "DELETE",
            baseUrl = baseUrl,
            path = LiveSyncApiContract.appointmentPath(appointmentId),
            token = token
        )
        if (response.code !in 200..299 || !response.json.optBoolean("ok")) {
            return LiveApiResult(false, message = response.message())
        }
        LiveApiResult(true, Unit, "Randevu silindi")
    }.getOrElse { LiveApiResult(false, message = readableError(it)) }

    suspend fun syncOperation(
        baseUrl: String,
        token: String,
        operation: PendingSyncOperation
    ): LiveApiResult<Unit> {
        return when (operation.type) {
            SyncOperationType.CREATE -> {
                val item = operation.appointment
                    ?: return LiveApiResult(false, message = "Bekleyen kayıt verisi eksik")
                createAppointment(baseUrl, token, item).asUnit()
            }
            SyncOperationType.UPDATE -> {
                val item = operation.appointment
                    ?: return LiveApiResult(false, message = "Bekleyen güncelleme verisi eksik")
                updateAppointment(baseUrl, token, item).asUnit()
            }
            SyncOperationType.DELETE -> deleteAppointment(baseUrl, token, operation.appointmentId)
            SyncOperationType.STATUS -> {
                val status = operation.status
                    ?: operation.appointment?.status
                    ?: return LiveApiResult(false, message = "Bekleyen durum verisi eksik")
                updateStatus(baseUrl, token, operation.appointmentId, status).asUnit()
            }
        }
    }

    private fun appointmentBody(appointment: Appointment): JSONObject = JSONObject()
        .put("client_uid", appointment.id)
        .put("customer_name", appointment.customer)
        .put("customer_phone", appointment.phone)
        .put("service", appointment.service)
        .put("staff", appointment.staff)
        .put("starts_at", LiveSyncApiContract.startsAt(appointment.date, appointment.time))
        .put("status", LiveSyncApiContract.toWireStatus(appointment.status))
        .put("note", appointment.note)

    private fun parseAppointment(json: JSONObject): Appointment {
        val status = runCatching {
            AppointmentStatus.valueOf(json.optString("status", "pending").uppercase())
        }.getOrDefault(AppointmentStatus.PENDING)
        val startsAt = json.optString("starts_at")
        val date = json.optString("date").ifBlank {
            startsAt.substringBefore(' ').takeIf { it.length == 10 }.orEmpty()
        }
        val time = json.optString("time").ifBlank {
            startsAt.substringAfter(' ', "").take(5)
        }
        return Appointment(
            id = json.optString("id").ifBlank { json.optLong("id").toString() },
            customer = json.optString("customer_name", json.optString("customer")),
            phone = json.optString("customer_phone", json.optString("phone")),
            service = json.optString("service"),
            staff = json.optString("staff"),
            date = date,
            time = time,
            status = status,
            note = json.optString("note")
        )
    }

    private data class HttpResponse(val code: Int, val json: JSONObject) {
        fun message(): String = json.optString("message")
            .ifBlank { json.optString("error") }
            .ifBlank { "Sunucu HTTP $code döndürdü" }
    }

    private suspend fun request(
        method: String,
        baseUrl: String,
        path: String,
        token: String = "",
        body: JSONObject? = null
    ): HttpResponse = withContext(Dispatchers.IO) {
        val normalized = baseUrl.trim().trimEnd('/')
        require(normalized.startsWith("https://")) { "Sunucu adresi HTTPS olmalı" }
        var connection: HttpURLConnection? = null
        try {
            connection = (URL("$normalized$path").openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 8000
                readTimeout = 10000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Randevu-Android/0.6.0")
                if (token.isNotBlank()) setRequestProperty("Authorization", "Bearer $token")
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
            }
            if (body != null) {
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(body.toString())
                }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val json = if (text.isBlank()) JSONObject().put("ok", code in 200..299) else JSONObject(text)
            HttpResponse(code, json)
        } finally {
            connection?.disconnect()
        }
    }

    private fun <T> LiveApiResult<T>.asUnit(): LiveApiResult<Unit> =
        LiveApiResult(ok = ok, value = if (ok) Unit else null, message = message)

    private fun readableError(error: Throwable): String =
        error.message?.take(180)?.ifBlank { null } ?: "Sunucuya bağlanılamadı"
}
