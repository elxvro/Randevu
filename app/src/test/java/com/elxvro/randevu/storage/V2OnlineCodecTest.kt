package com.elxvro.randevu.storage

import com.elxvro.randevu.online.*
import org.junit.Assert.*
import org.junit.Test

class V2OnlineCodecTest {
    @Test fun `pending versions conflict and import markers round trip`() {
        val mutation=OnlineMutation("op1",OnlineEntityType.SERVICE,"s1",OnlineMutationType.UPSERT,2,"{}",10L,3,"offline")
        assertEquals(mutation,V2OnlineCodec.decodePending(V2OnlineCodec.encodePending(listOf(mutation))).single())

        val versions=mapOf("BUSINESS:business" to 2,"SERVICE:s1" to 5)
        assertEquals(versions,V2OnlineCodec.decodeVersions(V2OnlineCodec.encodeVersions(versions)))

        val conflict=SyncConflict(OnlineEntityType.STAFF,"p1","{\"external_id\":\"p1\",\"version\":4}")
        assertEquals(conflict,V2OnlineCodec.decodeConflict(V2OnlineCodec.encodeConflict(conflict)))

        val imported=setOf("BUSINESS:business","SERVICE:s1")
        assertEquals(imported,V2OnlineCodec.decodeImported(V2OnlineCodec.encodeImported(imported)))
    }

    @Test fun `snapshot round trip preserves server collections`() {
        val snapshot=OnlineSnapshot(
            "t",
            "{\"business_name\":\"Salon\"}",
            listOf("{\"external_id\":\"s\"}"),
            emptyList(),
            emptyList(),
            listOf("{\"external_id\":\"a\"}")
        )
        assertEquals(snapshot,V2OnlineCodec.decodeSnapshot(V2OnlineCodec.encodeSnapshot(snapshot)))
    }
}
