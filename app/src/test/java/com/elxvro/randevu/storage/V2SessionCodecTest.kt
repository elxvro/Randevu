package com.elxvro.randevu.storage

import com.elxvro.randevu.online.V2Session
import org.junit.Assert.*
import org.junit.Test

class V2SessionCodecTest {
    @Test fun `session metadata never serializes bearer token or password`() {
        val raw = V2SessionCodec.encodeMetadata(
            V2Session("Emre","owner@example.com","salon","2026-11-03 12:00:00")
        )
        assertFalse(raw.lowercase().contains("token"))
        assertFalse(raw.lowercase().contains("password"))
        assertEquals("owner@example.com", V2SessionCodec.decodeMetadata(raw)?.email)
    }

    @Test fun `token envelope round trips binary iv and ciphertext`() {
        val iv = byteArrayOf(0,1,2,3,4,5,6,7,8,9,10,11)
        val cipher = byteArrayOf(12,13,14,15,16,17,18,19)
        val raw = V2TokenEnvelope.encode(iv,cipher)
        val decoded = V2TokenEnvelope.decode(raw)!!
        assertArrayEquals(iv, decoded.iv)
        assertArrayEquals(cipher, decoded.ciphertext)
    }

    @Test fun `malformed token envelope is rejected safely`() {
        assertNull(V2TokenEnvelope.decode("broken"))
    }
}
