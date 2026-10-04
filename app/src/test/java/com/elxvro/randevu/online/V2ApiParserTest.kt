package com.elxvro.randevu.online

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class V2ApiParserTest {
    @Test fun `auth response separates bearer from session metadata`() {
        val parsed=V2ApiParser.authSession(JSONObject(
            """{"ok":true,"session":{"owner_name":"Emre","email":"owner@example.com","business_slug":"salon","token":"demo-value","expires_at":"2026-11-03 12:00:00"}}"""
        ))
        assertEquals("demo-value",parsed.token)
        assertEquals("Emre",parsed.session.ownerName)
        assertEquals("owner@example.com",parsed.session.email)
        assertEquals("salon",parsed.session.businessSlug)
    }

    @Test fun `bootstrap response becomes online snapshot`() {
        val root=JSONObject(
            """{"ok":true,"business":{"business_name":"Salon","version":2},"services":[{"external_id":"s1","version":3}],"staff":[{"external_id":"p1","version":4}],"staff_leaves":[],"appointments":[{"external_id":"a1","version":5}],"server_time":"2026-10-04T12:00:00+00:00"}"""
        )
        val snapshot=V2ApiParser.bootstrap(root)
        assertEquals("Salon",JSONObject(snapshot.businessJson!!).getString("business_name"))
        assertEquals("s1",JSONObject(snapshot.servicesJson.single()).getString("external_id"))
        assertEquals("a1",JSONObject(snapshot.appointmentsJson.single()).getString("external_id"))
        assertEquals("2026-10-04T12:00:00+00:00",snapshot.serverTime)
    }
}
