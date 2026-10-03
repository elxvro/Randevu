package com.elxvro.randevu.ui

import com.elxvro.randevu.business.ServiceRecord
import com.elxvro.randevu.staff.StaffRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V13BookingCatalogTest {
    @Test fun `catalog contains only active owner records`() {
        val services = listOf(ServiceRecord("1", "Gerçek Hizmet", 30, true), ServiceRecord("2", "Kapalı", 30, false))
        val staff = listOf(StaffRecord("1", "Gerçek Personel", "Uzman", "", true), StaffRecord("2", "Pasif", "Uzman", "", false))
        assertEquals(listOf("Gerçek Hizmet"), V13BookingCatalog.activeServices(services).map { it.name })
        assertEquals(listOf("Gerçek Personel"), V13BookingCatalog.activeStaff(staff).map { it.name })
    }

    @Test fun `empty service or staff gives useful guidance`() {
        assertTrue(V13BookingCatalog.validationMessage(emptyList(), listOf(StaffRecord("1", "A", "", "", true)))!!.contains("hizmet", true))
        assertTrue(V13BookingCatalog.validationMessage(listOf(ServiceRecord("1", "A", 30, true)), emptyList())!!.contains("personel", true))
    }

    @Test fun `catalog never injects salon demo names`() {
        assertTrue(V13BookingCatalog.activeServices(emptyList()).isEmpty())
        assertTrue(V13BookingCatalog.activeStaff(emptyList()).isEmpty())
    }
}
