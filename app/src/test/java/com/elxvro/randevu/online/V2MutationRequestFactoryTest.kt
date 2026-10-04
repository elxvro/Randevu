package com.elxvro.randevu.online

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class V2MutationRequestFactoryTest {
    private fun mutation(
        entity: OnlineEntityType,
        type: OnlineMutationType=OnlineMutationType.UPSERT,
        version:Int?=null,
        payload:String="""{"name":"X"}""",
        id:String="x 1"
    )=OnlineMutation("op",entity,id,type,version,payload,1L)

    @Test fun `new catalog entities post while known entities update with expected version`() {
        val create=V2MutationRequestFactory.create(mutation(OnlineEntityType.SERVICE,id="svc-1"))
        assertEquals("POST",create.method)
        assertEquals("/v2/services",create.path)
        assertEquals("svc-1",JSONObject(create.bodyJson!!).getString("external_id"))

        val update=V2MutationRequestFactory.create(mutation(OnlineEntityType.SERVICE,version=3,id="svc 1"))
        assertEquals("PUT",update.method)
        assertEquals("/v2/services/svc%201",update.path)
        assertEquals(3,JSONObject(update.bodyJson!!).getInt("expected_version"))
    }

    @Test fun `delete sends expected version and appointment path`() {
        val req=V2MutationRequestFactory.create(mutation(OnlineEntityType.APPOINTMENT,OnlineMutationType.DELETE,4,id="apt-1"))
        assertEquals("DELETE",req.method)
        assertEquals("/v2/appointments/apt-1",req.path)
        assertEquals(4,JSONObject(req.bodyJson!!).getInt("expected_version"))
    }

    @Test fun `business import resolves server version before put`() {
        val op=mutation(OnlineEntityType.BUSINESS,version=null,id="business")
        assertTrue(V2MutationRequestFactory.needsResolvedVersion(op))
        val req=V2MutationRequestFactory.create(op,resolvedVersion=2)
        assertEquals("PUT",req.method)
        assertEquals("/v2/business",req.path)
        assertEquals(2,JSONObject(req.bodyJson!!).getInt("expected_version"))
    }
}
