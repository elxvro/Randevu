package com.elxvro.randevu.ui

import com.elxvro.randevu.business.ServiceRecord
import com.elxvro.randevu.staff.StaffRecord

object V13BookingCatalog {
    fun activeServices(services: List<ServiceRecord>): List<ServiceRecord> = services.filter { it.active && it.name.isNotBlank() && it.durationMinutes > 0 }
    fun activeStaff(staff: List<StaffRecord>): List<StaffRecord> = staff.filter { it.active && it.name.isNotBlank() }

    fun validationMessage(services: List<ServiceRecord>, staff: List<StaffRecord>): String? = when {
        activeServices(services).isEmpty() -> "Randevu oluşturmak için önce aktif bir hizmet ekleyin."
        activeStaff(staff).isEmpty() -> "Randevu oluşturmak için önce aktif bir personel ekleyin."
        else -> null
    }
}
