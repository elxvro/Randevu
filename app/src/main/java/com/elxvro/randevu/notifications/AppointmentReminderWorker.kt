package com.elxvro.randevu.notifications

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class AppointmentReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {
    override fun doWork(): Result {
        val appointmentId = inputData.getString(KEY_APPOINTMENT_ID).orEmpty()
        if (appointmentId.isBlank()) return Result.failure()
        AppointmentNotifier.showReminder(
            context = applicationContext,
            appointmentId = appointmentId,
            customer = inputData.getString(KEY_CUSTOMER).orEmpty(),
            service = inputData.getString(KEY_SERVICE).orEmpty(),
            staff = inputData.getString(KEY_STAFF).orEmpty(),
            time = inputData.getString(KEY_TIME).orEmpty(),
            offsetHours = inputData.getLong(KEY_OFFSET_HOURS, 2L)
        )
        return Result.success()
    }

    companion object {
        const val KEY_APPOINTMENT_ID = "appointment_id"
        const val KEY_CUSTOMER = "customer"
        const val KEY_SERVICE = "service"
        const val KEY_STAFF = "staff"
        const val KEY_TIME = "time"
        const val KEY_OFFSET_HOURS = "offset_hours"
    }
}
