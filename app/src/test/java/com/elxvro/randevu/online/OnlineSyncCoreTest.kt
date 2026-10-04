package com.elxvro.randevu.online

import org.junit.Assert.*
import org.junit.Test

class OnlineSyncCoreTest {
    private fun op(
        entity: OnlineEntityType = OnlineEntityType.APPOINTMENT,
        id: String = "x",
        type: OnlineMutationType = OnlineMutationType.UPSERT,
        version: Int? = null,
        retry: Int = 0,
        created: Long = 1L
    ) = OnlineMutation("op-$id-$created",entity,id,type,version,"{}",created,retry,null)

    @Test fun `upsert coalesces per entity and external id while retaining retry count`() {
        val first=op(retry=3,created=1)
        val next=op(retry=0,created=2)
        val result=OnlineSyncQueue.enqueue(listOf(first),next)
        assertEquals(1,result.size)
        assertEquals(3,result.single().retryCount)
        assertEquals(1L,result.single().createdAtEpochMs)
    }

    @Test fun `unsynced create followed by delete disappears`() {
        val result=OnlineSyncQueue.enqueue(listOf(op(version=null)),op(type=OnlineMutationType.DELETE,version=null,created=2))
        assertTrue(result.isEmpty())
    }

    @Test fun `server known update followed by delete becomes delete`() {
        val result=OnlineSyncQueue.enqueue(listOf(op(version=4)),op(type=OnlineMutationType.DELETE,version=4,created=2))
        assertEquals(OnlineMutationType.DELETE,result.single().mutationType)
        assertEquals(4,result.single().expectedVersion)
    }

    @Test fun `same external id in different entities is independent`() {
        val a=op(entity=OnlineEntityType.SERVICE,id="same")
        val b=op(entity=OnlineEntityType.STAFF,id="same",created=2)
        assertEquals(2,OnlineSyncQueue.enqueue(listOf(a),b).size)
    }

    @Test fun `http failure states preserve conflict payload`() {
        assertEquals(OnlineConnectionState.AUTH_REQUIRED, OnlineSyncCore.failure(401,null,null).state)
        val conflict=OnlineSyncCore.failure(409,OnlineEntityType.SERVICE,"{\"external_id\":\"s1\",\"version\":4}")
        assertEquals(OnlineConnectionState.CONFLICT,conflict.state)
        assertEquals("s1",conflict.conflict?.externalId)
        assertTrue(conflict.conflict?.currentServerPayload?.contains("\"version\":4")==true)
        assertEquals(OnlineConnectionState.OFFLINE, OnlineSyncCore.failure(null,null,null).state)
    }
}
