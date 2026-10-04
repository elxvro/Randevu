package com.elxvro.randevu.online

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class V2SyncCoordinatorTest {
    private class MemoryStore : V2SyncPersistence {
        var snapshot: OnlineSnapshot?=null
        var queue=emptyList<OnlineMutation>()
        val versions=mutableMapOf<String,Int>()
        var conflict: SyncConflict?=null
        override fun pending()=queue
        override fun savePending(value:List<OnlineMutation>){ queue=value }
        override fun replaceSnapshot(value:OnlineSnapshot){ snapshot=value }
        override fun setVersion(entity:OnlineEntityType,id:String,version:Int){ versions["${entity.name}:$id"]=version }
        override fun saveConflict(value:SyncConflict?){ conflict=value }
        override fun markBootstrap(serverTime:String){}
    }
    private class FakeTransport : V2SyncTransport {
        var bootstrapResult: V2TransportResult<OnlineSnapshot> = V2TransportResult.Success(OnlineSnapshot(serverTime="t"))
        var mutationResult: V2TransportResult<V2MutationAck> = V2TransportResult.Success(V2MutationAck(2,null))
        override suspend fun bootstrap(token:String)=bootstrapResult
        override suspend fun apply(token:String,mutation:OnlineMutation)=mutationResult
    }

    @Test fun `bootstrap replaces cache only when queue is empty`()= runBlocking {
        val store=MemoryStore(); val transport=FakeTransport()
        val expected=OnlineSnapshot(serverTime="2026-10-04T12:00:00+00:00",servicesJson=listOf("{\"external_id\":\"s\"}"))
        transport.bootstrapResult=V2TransportResult.Success(expected)
        val out=V2SyncCoordinator("token",transport,store).bootstrap()
        assertEquals(OnlineConnectionState.ONLINE,out.state)
        assertEquals(expected,store.snapshot)
    }

    @Test fun `success removes acknowledged mutation and updates version`()= runBlocking {
        val store=MemoryStore(); val transport=FakeTransport()
        val mutation=OnlineMutation("1",OnlineEntityType.SERVICE,"s1",OnlineMutationType.UPSERT,1,"{}",1)
        store.queue=listOf(mutation); transport.mutationResult=V2TransportResult.Success(V2MutationAck(2,null))
        val out=V2SyncCoordinator("token",transport,store).flush()
        assertTrue(store.queue.isEmpty())
        assertEquals(2,store.versions["SERVICE:s1"])
        assertEquals(OnlineConnectionState.ONLINE,out.state)
    }

    @Test fun `offline auth and conflict never drop queued mutation`()= runBlocking {
        val cases=listOf(
            V2TransportResult.Offline<V2MutationAck>("offline") to OnlineConnectionState.OFFLINE,
            V2TransportResult.AuthRequired<V2MutationAck>() to OnlineConnectionState.AUTH_REQUIRED,
            V2TransportResult.Conflict<V2MutationAck>("{\"external_id\":\"s1\",\"version\":5}") to OnlineConnectionState.CONFLICT
        )
        cases.forEach { (result,state) ->
            val store=MemoryStore(); val transport=FakeTransport()
            val mutation=OnlineMutation("1",OnlineEntityType.SERVICE,"s1",OnlineMutationType.UPSERT,1,"{}",1)
            store.queue=listOf(mutation); transport.mutationResult=result
            val out=V2SyncCoordinator("token",transport,store).flush()
            assertEquals(state,out.state)
            assertEquals(1,store.queue.size)
            if(state==OnlineConnectionState.CONFLICT) assertEquals("s1",store.conflict?.externalId)
        }
    }
}
