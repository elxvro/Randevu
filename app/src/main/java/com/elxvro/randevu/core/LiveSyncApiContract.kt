package com.elxvro.randevu.core

object LiveSyncApiContract {
    fun loginPath(): String = "/auth/login"
    fun appointmentsPath(): String = "/appointments"
    fun appointmentPath(id: String): String = "/appointments/${id.trim()}"
    fun statusPath(id: String): String = "${appointmentPath(id)}/status"
    fun dashboardPath(): String = "/dashboard"
    fun staffCalendarPath(staffId: String, date: String): String =
        "/staff/${staffId.trim()}/calendar?date=${date.trim()}"

    fun toWireStatus(status: AppointmentStatus): String = status.name.lowercase()

    fun startsAt(date: String, time: String): String {
        val cleanDate = date.trim()
        val cleanTime = time.trim()
        val withSeconds = when (cleanTime.count { it == ':' }) {
            0 -> "$cleanTime:00:00"
            1 -> "$cleanTime:00"
            else -> cleanTime
        }
        return "$cleanDate $withSeconds"
    }
}
