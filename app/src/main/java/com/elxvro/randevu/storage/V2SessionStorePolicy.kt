package com.elxvro.randevu.storage

object V2SessionStorePolicy {
    const val keyAlias = "randevu_v2_api_token"
    const val transformation = "AES/GCM/NoPadding"
    const val metadataKey = "session_metadata_v2"
    const val encryptedTokenKey = "session_token_encrypted_v2"
    const val baseUrlKey = "api_base_url_v2"
    const val prefsName = "randevu_v2_session"
}
