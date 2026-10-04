package com.elxvro.randevu.online

import com.elxvro.randevu.business.BusinessProfile
import com.elxvro.randevu.business.ServiceRecord
import com.elxvro.randevu.core.Appointment
import com.elxvro.randevu.core.AppointmentStatus
import com.elxvro.randevu.staff.StaffRecord
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class V2MigrationMappingTest {
    @Test fun `legacy appointment maps service and staff names to external ids`() {
        val profile=BusinessProfile("Salon","","","Europe/Istanbul","Emre",true)
        val service=ServiceRecord("svc-1","Kesim",30,true)
        val staff=StaffRecord("staff-1","Deniz","Uzman","",true)
        val appointment=Appointment("apt-1","Ayşe","+905551234567","Kesim","Deniz","2026-10-12","10:00",AppointmentStatus.CONFIRMED,"")
        val op=V13OnlineMigration.plan(profile,listOf(service),listOf(staff),emptyList(),listOf(appointment),emptySet())
            .single { it.entityType==OnlineEntityType.APPOINTMENT }
        val json=JSONObject(op.payloadJson)
        assertEquals("svc-1",json.getString("service_external_id"))
        assertEquals("staff-1",json.getString("staff_external_id"))
    }
}
