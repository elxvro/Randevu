package com.elxvro.randevu.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.elxvro.randevu.MainActivity
import com.elxvro.randevu.core.Appointment

object AppointmentNotifier {
    const val CHANNEL_ID = "appointment_reminders"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Randevu Hatırlatmaları",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Yaklaşan randevular için yerel hatırlatmalar"
            }
        )
    }

    fun showReminder(
        context: Context,
        appointmentId: String,
        customer: String,
        service: String,
        staff: String,
        time: String,
        offsetHours: Long
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            appointmentId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val timing = if (offsetHours == 24L) "24 saat sonra" else "2 saat sonra"
        val body = buildString {
            append(customer.ifBlank { "Müşteri" })
            if (service.isNotBlank()) append(" • ").append(service)
            if (staff.isNotBlank()) append(" • ").append(staff)
            if (time.isNotBlank()) append(" • ").append(time)
            append(" • ").append(timing)
        }
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("Randevu Hatırlatması")
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify("$appointmentId-$offsetHours".hashCode(), notification)
    }

    fun notifyUpcoming(context: Context, appointments: List<Appointment>) {
        appointments.take(3).forEach { appointment ->
            showReminder(
                context = context,
                appointmentId = appointment.id,
                customer = appointment.customer,
                service = appointment.service,
                staff = appointment.staff,
                time = appointment.time,
                offsetHours = 2L
            )
        }
    }
}
