package com.elxvro.randevu.online

import com.elxvro.randevu.business.BusinessProfile
import com.elxvro.randevu.business.ServiceRecord
import com.elxvro.randevu.core.Appointment
import com.elxvro.randevu.core.AppointmentStatus
import com.elxvro.randevu.staff.StaffLeave
import com.elxvro.randevu.staff.StaffRecord
import org.junit.Assert.*
import org.junit.Test

class V13OnlineMigrationTest {
    private val profile=BusinessProfile("Salon","+905551112233","Bursa","Europe/Istanbul","Emre",true)
    private val service=ServiceRecord("svc-1","Kesim",30,true)
    private val staff=StaffRecord("staff-1","Deniz","Uzman","",true)
    private val leave=StaffLeave("leave-1","staff-1","2026-10-10","2026-10-10",null,null,"İzin",1L)
    private val appointment=Appointment("apt-1","Ayşe","+905551234567","Kesim","Deniz","2026-10-12","10:00",AppointmentStatus.CONFIRMED,"")

    @Test fun `migration order is stable and imported ids are skipped`() {
        val all=V13OnlineMigration.plan(profile,listOf(service),listOf(staff),listOf(leave),listOf(appointment),emptySet())
        assertEquals(
            listOf(OnlineEntityType.BUSINESS,OnlineEntityType.SERVICE,OnlineEntityType.STAFF,OnlineEntityType.STAFF_LEAVE,OnlineEntityType.APPOINTMENT),
            all.map { it.entityType }
        )
        val resumed=V13OnlineMigration.plan(profile,listOf(service),listOf(staff),listOf(leave),listOf(appointment),setOf("BUSINESS:business","SERVICE:svc-1"))
        assertEquals(listOf(OnlineEntityType.STAFF,OnlineEntityType.STAFF_LEAVE,OnlineEntityType.APPOINTMENT),resumed.map { it.entityType })
        assertEquals(listOf("staff-1","leave-1","apt-1"),resumed.map { it.externalId })
    }

    @Test fun `planning never mutates or erases legacy cache values`() {
        val services=listOf(service)
        V13OnlineMigration.plan(profile,services,listOf(staff),listOf(leave),listOf(appointment),emptySet())
        assertEquals(service,services.single())
        assertEquals("apt-1",appointment.id)
    }
}
