package com.elxvro.randevu.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.elxvro.randevu.online.V2ApiContract
import com.elxvro.randevu.online.V2AuthenticatedSession
import com.elxvro.randevu.online.V2Session
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class V2SessionStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        V2SessionStorePolicy.prefsName,
        Context.MODE_PRIVATE
    )

    fun saveBaseUrl(value: String): Boolean {
        val normalized = V2ApiContract.normalizeBaseUrl(value)
        if (normalized.isBlank()) return false
        prefs.edit().putString(V2SessionStorePolicy.baseUrlKey, normalized).apply()
        return true
    }

    fun loadBaseUrl(): String = prefs.getString(V2SessionStorePolicy.baseUrlKey, null).orEmpty()

    fun saveAuthenticated(value: V2AuthenticatedSession) {
        val cipher = Cipher.getInstance(V2SessionStorePolicy.transformation)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val envelope = V2TokenEnvelope.encode(
            cipher.iv,
            cipher.doFinal(value.token.toByteArray(Charsets.UTF_8))
        )
        prefs.edit()
            .putString(V2SessionStorePolicy.metadataKey, V2SessionCodec.encodeMetadata(value.session))
            .putString(V2SessionStorePolicy.encryptedTokenKey, envelope)
            .apply()
    }

    fun loadSession(): V2Session? =
        V2SessionCodec.decodeMetadata(prefs.getString(V2SessionStorePolicy.metadataKey, null))

    fun loadToken(): String? {
        val envelope = V2TokenEnvelope.decode(
            prefs.getString(V2SessionStorePolicy.encryptedTokenKey, null)
        ) ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(V2SessionStorePolicy.transformation)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(128, envelope.iv)
            )
            cipher.doFinal(envelope.ciphertext).toString(Charsets.UTF_8)
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    fun hasToken(): Boolean = !loadToken().isNullOrBlank()

    fun clearSession() {
        prefs.edit()
            .remove(V2SessionStorePolicy.metadataKey)
            .remove(V2SessionStorePolicy.encryptedTokenKey)
            .apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(V2SessionStorePolicy.keyAlias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                V2SessionStorePolicy.keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
}
