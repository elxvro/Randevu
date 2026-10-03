package com.elxvro.randevu.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.elxvro.randevu.core.Appointment
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object AppointmentReminderScheduler {
    private val offsets = listOf(24L, 2L)

    fun schedule(context: Context, appointment: Appointment) {
        val appContext = context.applicationContext
        cancel(appContext, appointment.id)
        val now = LocalDateTime.now()
        ReminderPlan.entries(appointment, now).forEach { entry ->
            val delayMs = Duration.between(now, entry.triggerAt).toMillis().coerceAtLeast(0L)
            val data = Data.Builder()
                .putString(AppointmentReminderWorker.KEY_APPOINTMENT_ID, appointment.id)
                .putString(AppointmentReminderWorker.KEY_CUSTOMER, appointment.customer)
                .putString(AppointmentReminderWorker.KEY_SERVICE, appointment.service)
                .putString(AppointmentReminderWorker.KEY_STAFF, appointment.staff)
                .putString(AppointmentReminderWorker.KEY_TIME, appointment.time)
                .putLong(AppointmentReminderWorker.KEY_OFFSET_HOURS, entry.offsetHours)
                .build()
            val request = OneTimeWorkRequestBuilder<AppointmentReminderWorker>()
                .setInputData(data)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(appContext).enqueueUniqueWork(
                entry.workName,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    fun cancel(context: Context, appointmentId: String) {
        val manager = WorkManager.getInstance(context.applicationContext)
        offsets.forEach { offset -> manager.cancelUniqueWork(ReminderPlan.workName(appointmentId, offset)) }
    }

    fun cancelAll(context: Context, appointments: List<Appointment>) {
        appointments.forEach { cancel(context, it.id) }
    }

    fun rescheduleAll(context: Context, appointments: List<Appointment>) {
        appointments.forEach { schedule(context, it) }
    }
}
