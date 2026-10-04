package com.elxvro.randevu.online

import com.elxvro.randevu.business.BusinessProfile
import com.elxvro.randevu.business.ServiceRecord
import com.elxvro.randevu.core.*
import com.elxvro.randevu.staff.StaffRecord
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class V20MutationPlannerTest {
    @Test fun `service list diff creates updates and deletes with versions`() {
        val before=listOf(ServiceRecord("s1","Kesim",30,true),ServiceRecord("gone","Eski",20,true))
        val after=listOf(ServiceRecord("s1","Saç Kesim",45,true),ServiceRecord("s2","Bakım",30,true))
        val versions=mapOf("SERVICE:s1" to 3,"SERVICE:gone" to 4)
        val ops=V20MutationPlanner.serviceChanges(before,after,versions,100L)
        assertEquals(3,ops.size)
        val update=ops.single { it.externalId=="s1" }
        assertEquals(OnlineMutationType.UPSERT,update.mutationType)
        assertEquals(3,update.expectedVersion)
        assertEquals("Saç Kesim",JSONObject(update.payloadJson).getString("name"))
        val create=ops.single { it.externalId=="s2" }
        assertNull(create.expectedVersion)
        val delete=ops.single { it.externalId=="gone" }
        assertEquals(OnlineMutationType.DELETE,delete.mutationType)
        assertEquals(4,delete.expectedVersion)
    }

    @Test fun `appointment add maps selected names to service and staff external ids`() {
        val services=listOf(ServiceRecord("svc-1","Kesim",30,true))
        val staff=listOf(StaffRecord("staff-1","Deniz","Uzman","",true))
        val item=Appointment("apt-1","Ayşe","90555","Kesim","Deniz","2026-10-12","10:00",AppointmentStatus.CONFIRMED,"")
        val op=V20MutationPlanner.appointment(AppointmentAction.Add(item),emptyList(),services,staff,emptyMap(),100L)!!
        val json=JSONObject(op.payloadJson)
        assertEquals(OnlineEntityType.APPOINTMENT,op.entityType)
        assertNull(op.expectedVersion)
        assertEquals("svc-1",json.getString("service_external_id"))
        assertEquals("staff-1",json.getString("staff_external_id"))
    }

    @Test fun `appointment update and delete carry current server version`() {
        val services=listOf(ServiceRecord("svc-1","Kesim",30,true))
        val staff=listOf(StaffRecord("staff-1","Deniz","Uzman","",true))
        val item=Appointment("apt-1","Ayşe","90555","Kesim","Deniz","2026-10-12","10:00",AppointmentStatus.CONFIRMED,"")
        val versions=mapOf("APPOINTMENT:apt-1" to 7)
        val update=V20MutationPlanner.appointment(AppointmentAction.Update(item),listOf(item),services,staff,versions,100L)!!
        val delete=V20MutationPlanner.appointment(AppointmentAction.Delete("apt-1"),listOf(item),services,staff,versions,101L)!!
        assertEquals(7,update.expectedVersion)
        assertEquals(OnlineMutationType.DELETE,delete.mutationType)
        assertEquals(7,delete.expectedVersion)
    }

    @Test fun `business update uses cloud version`() {
        val profile=BusinessProfile("Salon","90555","Bursa","Europe/Istanbul","Emre",true)
        val op=V20MutationPlanner.business(profile,mapOf("BUSINESS:business" to 2),100L)
        assertEquals(2,op.expectedVersion)
        assertEquals("Salon",JSONObject(op.payloadJson).getString("business_name"))
    }
}
