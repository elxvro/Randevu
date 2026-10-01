package com.elxvro.randevu.network

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class HealthResult(
    val ok: Boolean,
    val serverName: String = "",
    val message: String = ""
)

object ApiClient {
    suspend fun checkHealth(baseUrl: String): HealthResult = withContext(Dispatchers.IO) {
        val normalized = baseUrl.trim().trimEnd('/')
        if (!normalized.startsWith("https://")) {
            return@withContext HealthResult(false, message = "Sunucu adresi HTTPS olmalı")
        }

        var connection: HttpURLConnection? = null
        try {
            connection = (URL("$normalized/health").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Randevu-Android/0.5.0")
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (code !in 200..299) {
                return@withContext HealthResult(false, message = "Sunucu HTTP $code döndürdü")
            }

            val name = runCatching {
                val json = JSONObject(body)
                json.optString("service").ifBlank { json.optString("name") }
            }.getOrDefault("")

            HealthResult(
                ok = true,
                serverName = name.ifBlank { "Randevu API" },
                message = "Bağlantı başarılı"
            )
        } catch (error: Exception) {
            HealthResult(
                ok = false,
                message = error.message?.take(120) ?: "Sunucuya bağlanılamadı"
            )
        } finally {
            connection?.disconnect()
        }
    }
}
