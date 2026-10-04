package com.elxvro.randevu.storage

import com.elxvro.randevu.online.V2Session
import java.util.Base64
import org.json.JSONObject

object V2SessionCodec {
    fun encodeMetadata(session: V2Session): String = JSONObject()
        .put("owner_name", session.ownerName)
        .put("email", session.email)
        .put("business_slug", session.businessSlug)
        .put("expires_at", session.expiresAt)
        .toString()

    fun decodeMetadata(raw: String?): V2Session? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val json = JSONObject(raw)
            val email = json.getString("email").trim()
            val slug = json.getString("business_slug").trim()
            if (email.isBlank() || slug.isBlank()) return@runCatching null
            V2Session(
                ownerName = json.optString("owner_name").trim(),
                email = email,
                businessSlug = slug,
                expiresAt = json.optString("expires_at").trim()
            )
        }.getOrNull()
    }
}

data class V2TokenEnvelopeData(val iv: ByteArray, val ciphertext: ByteArray)

object V2TokenEnvelope {
    fun encode(iv: ByteArray, ciphertext: ByteArray): String {
        require(iv.isNotEmpty() && ciphertext.isNotEmpty())
        return Base64.getEncoder().withoutPadding().encodeToString(iv) + "." +
            Base64.getEncoder().withoutPadding().encodeToString(ciphertext)
    }

    fun decode(raw: String?): V2TokenEnvelopeData? {
        if (raw.isNullOrBlank()) return null
        val parts = raw.split('.')
        if (parts.size != 2 || parts.any { it.isBlank() }) return null
        return runCatching {
            val iv = Base64.getDecoder().decode(parts[0])
            val ciphertext = Base64.getDecoder().decode(parts[1])
            if (iv.isEmpty() || ciphertext.isEmpty()) null else V2TokenEnvelopeData(iv, ciphertext)
        }.getOrNull()
    }
}
