package com.elxvro.randevu.core

import com.elxvro.randevu.network.WhatsAppServerConfig
import org.json.JSONArray
import org.json.JSONObject

object WhatsAppStorageCodec {
    fun encodeSettings(settings: WhatsAppSettings): String = JSONObject()
        .put("enabled", settings.enabled)
        .put("reminder_24h", settings.reminder24h)
        .put("reminder_2h", settings.reminder2h)
        .put("template_name", settings.templateName)
        .put("language_code", settings.languageCode)
        .put("meta_phone_number_id", settings.metaPhoneNumberId)
        .toString()

    fun decodeSettings(raw: String?): WhatsAppSettings {
        if (raw.isNullOrBlank()) return WhatsAppSettings()
        return runCatching {
            val json = JSONObject(raw)
            WhatsAppSettings(
                enabled = json.optBoolean("enabled", false),
                reminder24h = json.optBoolean("reminder_24h", true),
                reminder2h = json.optBoolean("reminder_2h", true),
                templateName = json.optString("template_name", "appointment_reminder").ifBlank { "appointment_reminder" },
                languageCode = json.optString("language_code", "tr").ifBlank { "tr" },
                metaPhoneNumberId = json.optString("meta_phone_number_id")
            )
        }.getOrDefault(WhatsAppSettings())
    }

    fun encodeServerConfig(config: WhatsAppServerConfig): String = JSONObject()
        .put("base_url", config.baseUrl)
        .put("token", config.token)
        .toString()

    fun decodeServerConfig(raw: String?): WhatsAppServerConfig {
        if (raw.isNullOrBlank()) return WhatsAppServerConfig()
        return runCatching {
            val json = JSONObject(raw)
            WhatsAppServerConfig(
                baseUrl = json.optString("base_url"),
                token = json.optString("token")
            )
        }.getOrDefault(WhatsAppServerConfig())
    }

    fun encodePending(operations: List<WhatsAppPendingSync>): String {
        val array = JSONArray()
        operations.forEach { operation ->
            array.put(JSONObject()
                .put("appointment_id", operation.appointmentId)
                .put("mutation", operation.mutation.name)
                .also { json -> operation.appointment?.let { json.put("appointment", encodeAppointment(it)) } })
        }
        return array.toString()
    }

    fun decodePending(raw: String?): List<WhatsAppPendingSync> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val json = array.optJSONObject(index) ?: continue
                    val appointmentId = json.optString("appointment_id").trim()
                    val mutation = runCatching { WhatsAppAppointmentMutation.valueOf(json.optString("mutation")) }.getOrNull() ?: continue
                    if (appointmentId.isBlank()) continue
                    add(WhatsAppPendingSync(
                        appointmentId = appointmentId,
                        mutation = mutation,
                        appointment = json.optJSONObject("appointment")?.let(::decodeAppointment)
                    ))
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeAppointment(appointment: Appointment): JSONObject = JSONObject()
        .put("id", appointment.id)
        .put("customer", appointment.customer)
        .put("phone", appointment.phone)
        .put("service", appointment.service)
        .put("staff", appointment.staff)
        .put("date", appointment.date)
        .put("time", appointment.time)
        .put("status", appointment.status.name)
        .put("note", appointment.note)

    private fun decodeAppointment(json: JSONObject): Appointment? = runCatching {
        Appointment(
            id = json.getString("id"),
            customer = json.optString("customer"),
            phone = json.optString("phone"),
            service = json.optString("service"),
            staff = json.optString("staff"),
            date = json.optString("date"),
            time = json.optString("time"),
            status = AppointmentStatus.valueOf(json.optString("status", AppointmentStatus.PENDING.name)),
            note = json.optString("note")
        )
    }.getOrNull()
}
