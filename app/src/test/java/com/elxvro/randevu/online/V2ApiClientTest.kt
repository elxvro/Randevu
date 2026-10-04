package com.elxvro.randevu.online

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class V2ApiClientTest {
    private class FakeEngine : V2HttpEngine {
        val requests=mutableListOf<V2HttpRequest>()
        val responses=ArrayDeque<V2HttpResponse>()
        override suspend fun execute(request: V2HttpRequest): V2HttpResponse {
            requests += request
            return responses.removeFirst()
        }
    }

    @Test fun `registration uses public endpoint and parses authenticated session`() = runBlocking {
        val engine=FakeEngine()
        engine.responses += V2HttpResponse(201,"""{"ok":true,"session":{"owner_name":"Emre","email":"owner@example.com","business_slug":"salon","token":"demo-value","expires_at":"2026-11-03 12:00:00"}}""")
        val client=V2ApiClient("https://api.example.com",engine)
        val result=client.registerOwner("Emre","Salon","owner@example.com","12345678","","","Europe/Istanbul")
        assertTrue(result is V2TransportResult.Success)
        val request=engine.requests.single()
        assertEquals("POST",request.method)
        assertEquals("/v2/auth/register-owner",request.path)
        assertNull(request.token)
        assertEquals("owner@example.com",JSONObject(request.bodyJson!!).getString("email"))
    }

    @Test fun `bootstrap sends bearer and maps unauthorized`() = runBlocking {
        val engine=FakeEngine()
        engine.responses += V2HttpResponse(401,"""{"ok":false,"error":"unauthorized"}""")
        val result=V2ApiClient("https://api.example.com",engine).bootstrap("bearer-value")
        assertTrue(result is V2TransportResult.AuthRequired)
        assertEquals("bearer-value",engine.requests.single().token)
        assertEquals("/v2/sync/bootstrap",engine.requests.single().path)
    }

    @Test fun `business import resolves current version then updates`() = runBlocking {
        val engine=FakeEngine()
        engine.responses += V2HttpResponse(200,"""{"ok":true,"business":{"business_name":"Salon","version":2}}""")
        engine.responses += V2HttpResponse(200,"""{"ok":true,"business":{"business_name":"Yeni","version":3}}""")
        val mutation=OnlineMutation("op",OnlineEntityType.BUSINESS,"business",OnlineMutationType.UPSERT,null,"{\"business_name\":\"Yeni\"}",1L)
        val result=V2ApiClient("https://api.example.com",engine).apply("bearer-value",mutation)
        assertTrue(result is V2TransportResult.Success)
        assertEquals(listOf("GET","PUT"),engine.requests.map { it.method })
        assertEquals("/v2/business",engine.requests[0].path)
        assertEquals(2,JSONObject(engine.requests[1].bodyJson!!).getInt("expected_version"))
    }

    @Test fun `conflict preserves current server payload`() = runBlocking {
        val engine=FakeEngine()
        engine.responses += V2HttpResponse(409,"""{"ok":false,"error":"version_conflict","current":{"external_id":"s1","version":5}}""")
        val mutation=OnlineMutation("op",OnlineEntityType.SERVICE,"s1",OnlineMutationType.UPSERT,4,"{\"name\":\"X\"}",1L)
        val result=V2ApiClient("https://api.example.com",engine).apply("bearer-value",mutation)
        assertTrue(result is V2TransportResult.Conflict)
        assertEquals(5,JSONObject((result as V2TransportResult.Conflict).currentServerPayload).getInt("version"))
    }
}
