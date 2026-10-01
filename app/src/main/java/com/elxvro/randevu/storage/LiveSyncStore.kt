package com.elxvro.randevu.storage

import android.content.Context
import com.elxvro.randevu.core.ApiMode
import com.elxvro.randevu.core.Appointment
import com.elxvro.randevu.core.AppointmentStatus
import com.elxvro.randevu.core.BackendConfig
import com.elxvro.randevu.core.BackendSession
import com.elxvro.randevu.core.BackendState
import com.elxvro.randevu.core.ConnectionState
import com.elxvro.randevu.core.LiveSyncState
import com.elxvro.randevu.core.PendingSyncOperation
import com.elxvro.randevu.core.SyncOperationType
import org.json.JSONArray
import org.json.JSONObject

class LiveSyncStore(context: Context) {
    private val prefs = context.getSharedPreferences("randevu_live_sync_v1", Context.MODE_PRIVATE)

    fun loadSyncState(): LiveSyncState {
        val pending = mutableListOf<PendingSyncOperation>()
        val raw = prefs.getString(KEY_PENDING, null)
        if (!raw.isNullOrBlank()) {
            runCatching {
                val array = JSONArray(raw)
                for (index in 0 until array.length()) {
                    array.optJSONObject(index)?.let(::decodeOperation)?.let(pending::add)
                }
            }
        }
        return LiveSyncState(
            pending = pending,
            syncing = false,
            lastSyncAt = prefs.getString(KEY_LAST_SYNC, null),
            errorMessage = null,
            nextSequence = prefs.getLong(KEY_SEQUENCE, 1L).coerceAtLeast(1L)
        )
    }

    fun saveSyncState(state: LiveSyncState) {
        val array = JSONArray()
        state.pending.forEach { array.put(encodeOperation(it)) }
        prefs.edit()
            .putString(KEY_PENDING, array.toString())
            .putString(KEY_LAST_SYNC, state.lastSyncAt)
            .putLong(KEY_SEQUENCE, state.nextSequence)
            .apply()
    }

    fun loadAppointments(): List<Appointment> {
        val raw = prefs.getString(KEY_APPOINTMENTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    array.optJSONObject(index)?.let(::decodeAppointment)?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveAppointments(appointments: List<Appointment>) {
        val array = JSONArray()
        appointments.forEach { array.put(encodeAppointment(it)) }
        prefs.edit().putString(KEY_APPOINTMENTS, array.toString()).apply()
    }

    fun loadBackendState(): BackendState {
        val mode = runCatching {
            ApiMode.valueOf(prefs.getString(KEY_MODE, ApiMode.DEMO.name) ?: ApiMode.DEMO.name)
        }.getOrDefault(ApiMode.DEMO)
        val token = prefs.getString(KEY_TOKEN, "").orEmpty()
        val userId = prefs.getLong(KEY_USER_ID, -1L).takeIf { it > 0 }
        val session = BackendSession(
            authenticated = token.isNotBlank(),
            userId = userId,
            token = token,
            displayName = prefs.getString(KEY_DISPLAY_NAME, "").orEmpty(),
            phone = prefs.getString(KEY_PHONE, "").orEmpty()
        )
        return BackendState(
            config = BackendConfig(
                baseUrl = prefs.getString(KEY_BASE_URL, "").orEmpty(),
                mode = mode
            ),
            connectionState = if (mode == ApiMode.DEMO) ConnectionState.CONNECTED else ConnectionState.IDLE,
            serverName = if (mode == ApiMode.DEMO) "Yerel Demo" else "",
            session = session
        )
    }

    fun saveBackendState(state: BackendState) {
        prefs.edit()
            .putString(KEY_MODE, state.config.mode.name)
            .putString(KEY_BASE_URL, state.config.baseUrl)
            .putString(KEY_TOKEN, state.session.token)
            .putLong(KEY_USER_ID, state.session.userId ?: -1L)
            .putString(KEY_DISPLAY_NAME, state.session.displayName)
            .putString(KEY_PHONE, state.session.phone)
            .apply()
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_DISPLAY_NAME)
            .remove(KEY_PHONE)
            .apply()
    }

    private fun encodeOperation(operation: PendingSyncOperation): JSONObject = JSONObject()
        .put("operation_id", operation.operationId)
        .put("type", operation.type.name)
        .put("appointment_id", operation.appointmentId)
        .put("status", operation.status?.name)
        .also { json -> operation.appointment?.let { json.put("appointment", encodeAppointment(it)) } }

    private fun decodeOperation(json: JSONObject): PendingSyncOperation? = runCatching {
        PendingSyncOperation(
            operationId = json.getString("operation_id"),
            type = SyncOperationType.valueOf(json.getString("type")),
            appointmentId = json.getString("appointment_id"),
            appointment = json.optJSONObject("appointment")?.let(::decodeAppointment),
            status = json.optString("status").takeIf { it.isNotBlank() && it != "null" }?.let {
                AppointmentStatus.valueOf(it)
            }
        )
    }.getOrNull()

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

    private companion object {
        const val KEY_PENDING = "pending"
        const val KEY_LAST_SYNC = "last_sync"
        const val KEY_SEQUENCE = "sequence"
        const val KEY_APPOINTMENTS = "appointments"
        const val KEY_MODE = "mode"
        const val KEY_BASE_URL = "base_url"
        const val KEY_TOKEN = "token"
        const val KEY_USER_ID = "user_id"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_PHONE = "phone"
    }
}
