package com.elxvro.randevu.online

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class V2ImportResumeTest {
    private class Store : V2SyncPersistence {
        var queue=listOf(
            OnlineMutation("import-service-s1",OnlineEntityType.SERVICE,"s1",OnlineMutationType.UPSERT,null,"{}",1L)
        )
        val imported=mutableSetOf<String>()
        override fun pending()=queue
        override fun savePending(value:List<OnlineMutation>){ queue=value }
        override fun replaceSnapshot(value:OnlineSnapshot){}
        override fun setVersion(entity:OnlineEntityType,id:String,version:Int){}
        override fun saveConflict(value:SyncConflict?){}
        override fun markBootstrap(serverTime:String){}
        override fun markImported(entity:OnlineEntityType,id:String){ imported += "${entity.name}:$id" }
    }
    private class Transport : V2SyncTransport {
        override suspend fun bootstrap(token:String)=V2TransportResult.Success(OnlineSnapshot(serverTime="t"))
        override suspend fun apply(token:String,mutation:OnlineMutation)=V2TransportResult.Success(V2MutationAck(1,"{}"))
    }

    @Test fun `acknowledged import marks entity before operation disappears`()= runBlocking {
        val store=Store()
        V2SyncCoordinator("t",Transport(),store).flush()
        assertEquals(setOf("SERVICE:s1"),store.imported)
        assertEquals(0,store.queue.size)
    }
}
