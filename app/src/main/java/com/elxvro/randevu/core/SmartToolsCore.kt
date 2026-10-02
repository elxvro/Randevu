package com.elxvro.randevu.core

data class CustomerSummary(
    val customer: String,
    val phone: String,
    val totalAppointments: Int,
    val completedAppointments: Int,
    val cancelledAppointments: Int,
    val tags: Set<String>
)

data class BusinessReport(
    val activeAppointments: Int,
    val completedAppointments: Int,
    val pendingAppointments: Int,
    val confirmedAppointments: Int,
    val cancelledAppointments: Int,
    val expectedRevenue: Double,
    val realizedRevenue: Double,
    val topService: String?
)

object SmartToolsEngine {
    fun customerSummary(appointments: List<Appointment>, phone: String): CustomerSummary {
        val normalized = phone.filter(Char::isDigit)
        val items = appointments.filter { it.phone.filter(Char::isDigit) == normalized }
        val tags = items.flatMap { appointment ->
            appointment.note
                .split(',', '#', ';')
                .map(String::trim)
                .filter { it.isNotBlank() && it.length <= 24 && !it.contains(' ') }
        }.toSet()
        return CustomerSummary(
            customer = items.firstOrNull()?.customer.orEmpty(),
            phone = items.firstOrNull()?.phone ?: phone,
            totalAppointments = items.size,
            completedAppointments = items.count { it.status == AppointmentStatus.COMPLETED },
            cancelledAppointments = items.count { it.status == AppointmentStatus.CANCELLED },
            tags = tags
        )
    }

    fun whatsappMessage(appointment: Appointment, businessName: String): String =
        buildReminderMessage(appointment, businessName)

    fun smsMessage(appointment: Appointment, businessName: String): String =
        buildReminderMessage(appointment, businessName)

    private fun buildReminderMessage(appointment: Appointment, businessName: String): String {
        val brand = businessName.trim().ifBlank { "Randevu" }
        return "Merhaba ${appointment.customer}, $brand için ${appointment.date} tarihinde saat ${appointment.time} " +
            "${appointment.service} randevunuzu hatırlatırız. Değişiklik için bizimle iletişime geçebilirsiniz."
    }

    fun report(
        appointments: List<Appointment>,
        servicePrices: Map<String, Double>,
        startDate: String,
        endDate: String
    ): BusinessReport {
        val inRange = appointments.filter { it.date >= startDate && it.date <= endDate }
        val active = inRange.filter { it.status != AppointmentStatus.CANCELLED }
        val completed = active.filter { it.status == AppointmentStatus.COMPLETED }
        val topService = active
            .groupingBy { it.service }
            .eachCount()
            .maxWithOrNull(compareBy<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            ?.key
        return BusinessReport(
            activeAppointments = active.size,
            completedAppointments = completed.size,
            pendingAppointments = active.count { it.status == AppointmentStatus.PENDING },
            confirmedAppointments = active.count { it.status == AppointmentStatus.CONFIRMED },
            cancelledAppointments = inRange.count { it.status == AppointmentStatus.CANCELLED },
            expectedRevenue = active.sumOf { servicePrices[it.service] ?: 0.0 },
            realizedRevenue = completed.sumOf { servicePrices[it.service] ?: 0.0 },
            topService = topService
        )
    }

    fun canContact(phone: String): Boolean {
        val digits = phone.filter(Char::isDigit)
        return digits.length in 10..15
    }

    fun digitsOnlyPhone(phone: String): String = phone.filter(Char::isDigit)
}
