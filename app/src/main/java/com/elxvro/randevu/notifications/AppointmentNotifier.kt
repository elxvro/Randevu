package com.elxvro.randevu.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.elxvro.randevu.core.Appointment

object AppointmentNotifier {
    private const val CHANNEL_ID = "randevu_upcoming"

    fun notifyUpcoming(context: Context, appointments: List<Appointment>) {
        if (appointments.isEmpty()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Yaklaşan randevular",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Yaklaşan randevu hatırlatmaları"
            }
        )

        appointments.take(3).forEach { appointment ->
            val notification = Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
                .setContentTitle("Yaklaşan randevu • ${appointment.time}")
                .setContentText("${appointment.customer} • ${appointment.service} • ${appointment.staff}")
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .build()
            runCatching {
                manager.notify(appointment.id.hashCode(), notification)
            }
        }
    }
}
