package com.elxvro.randevu.storage

import android.content.Context
import com.elxvro.randevu.business.BusinessProfile
import com.elxvro.randevu.business.BusinessStorageCodec
import com.elxvro.randevu.business.ServiceRecord
import com.elxvro.randevu.business.V13Migration
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
import com.elxvro.randevu.staff.StaffLeave
import com.elxvro.randevu.staff.StaffLeaveEngine
import com.elxvro.randevu.staff.StaffRecord
import java.time.ZoneId
import org.json.JSONArray
import org.json.JSONObject

class LiveSyncStore(context: Context) {
    private val prefs = context.getSharedPreferences("randevu_live_sync_v1", Context.MODE_PRIVATE)

    fun migrateV13IfNeeded() {
        if (prefs.getBoolean(KEY_MIGRATION_V13, false)) return
        saveAppointments(V13Migration.cleanAppointments(loadAppointments()))
        saveStaff(V13Migration.cleanStaff(loadStaff()))
        prefs.edit().putBoolean(KEY_MIGRATION_V13, true).apply()
    }

    fun loadBusinessProfile(): BusinessProfile = BusinessStorageCodec.decodeProfile(
        prefs.getString(KEY_BUSINESS_PROFILE, null),
        ZoneId.systemDefault().id
    )

    fun saveBusinessProfile(profile: BusinessProfile) {
        prefs.edit().putString(KEY_BUSINESS_PROFILE, BusinessStorageCodec.encodeProfile(profile)).apply()
    }

    fun loadServices(): List<ServiceRecord> = BusinessStorageCodec.decodeServices(prefs.getString(KEY_SERVICES, null))

    fun saveServices(services: List<ServiceRecord>) {
        prefs.edit().putString(KEY_SERVICES, BusinessStorageCodec.encodeServices(services)).apply()
    }

    fun loadSetupStep(): Int = prefs.getInt(KEY_SETUP_STEP, 0).coerceIn(0, 4)

    fun saveSetupStep(step: Int) {
        prefs.edit().putInt(KEY_SETUP_STEP, step.coerceIn(0, 4)).apply()
    }

    fun clearBusinessSetup(clearOperationalData: Boolean) {
        val editor = prefs.edit()
            .remove(KEY_BUSINESS_PROFILE)
            .remove(KEY_SERVICES)
            .remove(KEY_SETUP_STEP)
        if (clearOperationalData) {
            editor.remove(KEY_APPOINTMENTS)
                .remove(KEY_STAFF)
                .remove(KEY_STAFF_LEAVES)
                .remove(KEY_PENDING)
                .remove(KEY_LAST_SYNC)
        }
        editor.apply()
    }

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

    fun loadStaff(): List<StaffRecord> {
        val raw = prefs.getString(KEY_STAFF, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    array.optJSONObject(index)?.let(::decodeStaff)?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveStaff(staff: List<StaffRecord>) {
        val array = JSONArray()
        staff.forEach { array.put(encodeStaff(it)) }
        prefs.edit().putString(KEY_STAFF, array.toString()).apply()
    }

    fun loadStaffLeaves(): List<StaffLeave> {
        val raw = prefs.getString(KEY_STAFF_LEAVES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    array.optJSONObject(index)?.let(::decodeStaffLeave)?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveStaffLeaves(leaves: List<StaffLeave>) {
        val array = JSONArray()
        leaves.filter(StaffLeaveEngine::isValid).forEach { array.put(encodeStaffLeave(it)) }
        prefs.edit().putString(KEY_STAFF_LEAVES, array.toString()).apply()
    }

    fun loadReminderEnabled(): Boolean = prefs.getBoolean(KEY_REMINDER_ENABLED, false)

    fun saveReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REMINDER_ENABLED, enabled).apply()
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
            serverName = if (mode == ApiMode.DEMO) "Yerel" else "",
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
            status = json.optString("status").takeIf { it.isNotBlank() && it != "null" }?.let { AppointmentStatus.valueOf(it) }
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

    private fun encodeStaff(staff: StaffRecord): JSONObject = JSONObject()
        .put("id", staff.id)
        .put("name", staff.name)
        .put("title", staff.title)
        .put("phone", staff.phone)
        .put("active", staff.active)

    private fun decodeStaff(json: JSONObject): StaffRecord? = runCatching {
        val id = json.getString("id").trim()
        val name = json.getString("name").trim()
        if (id.isBlank() || name.isBlank()) return@runCatching null
        StaffRecord(
            id = id,
            name = name,
            title = json.optString("title", "Personel").ifBlank { "Personel" },
            phone = json.optString("phone"),
            active = json.optBoolean("active", true)
        )
    }.getOrNull()

    private fun encodeStaffLeave(leave: StaffLeave): JSONObject = JSONObject()
        .put("id", leave.id)
        .put("staff_id", leave.staffId)
        .put("start_date", leave.startDate)
        .put("end_date", leave.endDate)
        .put("start_time", leave.startTime)
        .put("end_time", leave.endTime)
        .put("reason", leave.reason)
        .put("created_at", leave.createdAt)

    private fun decodeStaffLeave(json: JSONObject): StaffLeave? = runCatching {
        val leave = StaffLeave(
            id = json.getString("id").trim(),
            staffId = json.getString("staff_id").trim(),
            startDate = json.getString("start_date").trim(),
            endDate = json.getString("end_date").trim(),
            startTime = json.optString("start_time").takeIf { it.isNotBlank() && it != "null" },
            endTime = json.optString("end_time").takeIf { it.isNotBlank() && it != "null" },
            reason = json.optString("reason"),
            createdAt = json.optLong("created_at", System.currentTimeMillis())
        )
        if (leave.id.isBlank() || leave.staffId.isBlank() || !StaffLeaveEngine.isValid(leave)) null else leave
    }.getOrNull()

    private companion object {
        const val KEY_PENDING = "pending"
        const val KEY_LAST_SYNC = "last_sync"
        const val KEY_SEQUENCE = "sequence"
        const val KEY_APPOINTMENTS = "appointments"
        const val KEY_STAFF = "staff_v12"
        const val KEY_STAFF_LEAVES = "staff_leaves_v12"
        const val KEY_REMINDER_ENABLED = "reminder_enabled_v12"
        const val KEY_BUSINESS_PROFILE = "business_profile_v13"
        const val KEY_SERVICES = "services_v13"
        const val KEY_SETUP_STEP = "setup_step_v13"
        const val KEY_MIGRATION_V13 = "migration_v13_complete"
        const val KEY_MODE = "mode"
        const val KEY_BASE_URL = "base_url"
        const val KEY_TOKEN = "token"
        const val KEY_USER_ID = "user_id"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_PHONE = "phone"
    }
}
