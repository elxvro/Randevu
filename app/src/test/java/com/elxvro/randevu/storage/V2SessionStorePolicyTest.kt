package com.elxvro.randevu.storage

import org.junit.Assert.assertEquals
import org.junit.Test

class V2SessionStorePolicyTest {
    @Test fun `session policy uses android keystore aes gcm`() {
        assertEquals("randevu_v2_api_token",V2SessionStorePolicy.keyAlias)
        assertEquals("AES/GCM/NoPadding",V2SessionStorePolicy.transformation)
        assertEquals("session_metadata_v2",V2SessionStorePolicy.metadataKey)
        assertEquals("session_token_encrypted_v2",V2SessionStorePolicy.encryptedTokenKey)
    }
}
