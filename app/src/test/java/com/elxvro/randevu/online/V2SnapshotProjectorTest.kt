package com.elxvro.randevu.online

import com.elxvro.randevu.core.AppointmentStatus
import org.junit.Assert.*
import org.junit.Test

class V2SnapshotProjectorTest {
    @Test fun `bootstrap snapshot projects into legacy cache and version index`() {
        val snapshot=OnlineSnapshot(
            serverTime="2026-10-04T12:00:00+00:00",
            businessJson="""{"business_name":"Salon","phone":"90555","address":"Bursa","timezone":"Europe/Istanbul","opening_time":"09:00","closing_time":"18:00","version":2}""",
            servicesJson=listOf("""{"external_id":"s1","name":"Kesim","duration_minutes":30,"active":true,"version":3}"""),
            staffJson=listOf("""{"external_id":"p1","name":"Deniz","title":"Uzman","phone":"","active":true,"version":4}"""),
            staffLeavesJson=listOf("""{"external_id":"l1","staff_external_id":"p1","start_date":"2026-10-10","end_date":"2026-10-10","start_time":null,"end_time":null,"reason":"İzin","version":5}"""),
            appointmentsJson=listOf("""{"external_id":"a1","customer_name":"Ayşe","customer_phone":"90555111","service":"Kesim","staff":"Deniz","date":"2026-10-12","time":"10:00","status":"confirmed","note":"","version":6}""")
        )
        val projected=V2SnapshotProjector.project(snapshot,"Emre")
        assertEquals("Salon",projected.profile?.businessName)
        assertTrue(projected.profile?.setupCompleted==true)
        assertEquals("s1",projected.services.single().id)
        assertEquals("p1",projected.staff.single().id)
        assertEquals("l1",projected.leaves.single().id)
        assertEquals(AppointmentStatus.CONFIRMED,projected.appointments.single().status)
        assertEquals(2,projected.versions["BUSINESS:business"])
        assertEquals(3,projected.versions["SERVICE:s1"])
        assertEquals(6,projected.versions["APPOINTMENT:a1"])
    }

    @Test fun `profile remains incomplete without active service`() {
        val snapshot=OnlineSnapshot(
            businessJson="""{"business_name":"Salon","timezone":"Europe/Istanbul","version":1}""",
            servicesJson=listOf("""{"external_id":"s1","name":"Kapalı","duration_minutes":30,"active":false,"version":1}""")
        )
        assertFalse(V2SnapshotProjector.project(snapshot,"Emre").profile!!.setupCompleted)
    }
}
