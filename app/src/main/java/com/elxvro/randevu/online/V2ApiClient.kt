package com.elxvro.randevu.online

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class V2HttpRequest(
    val baseUrl: String,
    val path: String,
    val method: String,
    val token: String? = null,
    val bodyJson: String? = null
)

data class V2HttpResponse(val code: Int, val body: String)

interface V2HttpEngine {
    suspend fun execute(request: V2HttpRequest): V2HttpResponse
}

class UrlConnectionV2HttpEngine : V2HttpEngine {
    override suspend fun execute(request: V2HttpRequest): V2HttpResponse = withContext(Dispatchers.IO) {
        val connection = (URL(request.baseUrl + request.path).openConnection() as HttpURLConnection).apply {
            requestMethod = request.method
            connectTimeout = 12_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
            request.token?.takeIf { it.isNotBlank() }?.let {
                setRequestProperty("Authorization", "Bearer $it")
            }
            if (request.bodyJson != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        try {
            request.bodyJson?.let { body ->
                connection.outputStream.use { stream ->
                    stream.write(body.toByteArray(Charsets.UTF_8))
                }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            V2HttpResponse(code, body)
        } finally {
            connection.disconnect()
        }
    }
}

class V2ApiClient(
    baseUrl: String,
    private val engine: V2HttpEngine = UrlConnectionV2HttpEngine()
) : V2SyncTransport {
    private val baseUrl: String = V2ApiContract.normalizeBaseUrl(baseUrl).also {
        require(it.isNotBlank()) { "HTTPS base URL required" }
    }

    suspend fun registerOwner(
        ownerName: String,
        businessName: String,
        email: String,
        password: String,
        phone: String,
        address: String,
        timezone: String
    ): V2TransportResult<V2AuthenticatedSession> {
        val request = V2HttpRequest(
            baseUrl = baseUrl,
            path = V2ApiContract.registerPath,
            method = "POST",
            bodyJson = V2ApiContract.registerJson(
                ownerName, businessName, email, password, phone, address, timezone
            ).toString()
        )
        return authRequest(request)
    }

    suspend fun login(email: String, password: String): V2TransportResult<V2AuthenticatedSession> =
        authRequest(
            V2HttpRequest(
                baseUrl = baseUrl,
                path = V2ApiContract.loginPath,
                method = "POST",
                bodyJson = V2ApiContract.loginJson(email, password).toString()
            )
        )

    suspend fun logout(token: String): V2TransportResult<Unit> {
        val response = safeExecute(
            V2HttpRequest(baseUrl, V2ApiContract.logoutPath, "POST", token, "{}")
        ) ?: return V2TransportResult.Offline("Sunucuya ulaşılamıyor.")
        if (response.code !in 200..299) return mapFailure(response)
        return V2TransportResult.Success(Unit)
    }

    suspend fun me(token: String): V2TransportResult<JSONObject> {
        val response = safeExecute(
            V2HttpRequest(baseUrl, V2ApiContract.mePath, "GET", token)
        ) ?: return V2TransportResult.Offline("Sunucuya ulaşılamıyor.")
        if (response.code !in 200..299) return mapFailure(response)
        return runCatching { V2TransportResult.Success(JSONObject(response.body)) }
            .getOrElse { V2TransportResult.Failure(response.code, "Geçersiz sunucu yanıtı.") }
    }

    override suspend fun bootstrap(token: String): V2TransportResult<OnlineSnapshot> {
        val response = safeExecute(
            V2HttpRequest(baseUrl, V2ApiContract.bootstrapPath, "GET", token)
        ) ?: return V2TransportResult.Offline("Sunucuya ulaşılamıyor.")
        if (response.code !in 200..299) return mapFailure(response)
        return runCatching {
            V2TransportResult.Success(V2ApiParser.bootstrap(JSONObject(response.body)))
        }.getOrElse {
            V2TransportResult.Failure(response.code, "Senkronizasyon yanıtı okunamadı.")
        }
    }

    override suspend fun apply(
        token: String,
        mutation: OnlineMutation
    ): V2TransportResult<V2MutationAck> {
        var resolvedVersion: Int? = null
        if (V2MutationRequestFactory.needsResolvedVersion(mutation)) {
            val current = safeExecute(
                V2HttpRequest(baseUrl, "/v2/business", "GET", token)
            ) ?: return V2TransportResult.Offline("Sunucuya ulaşılamıyor.")
            if (current.code !in 200..299) return mapFailure(current)
            resolvedVersion = runCatching {
                JSONObject(current.body).getJSONObject("business").getInt("version")
            }.getOrNull() ?: return V2TransportResult.Failure(
                current.code,
                "İşletme sürümü okunamadı."
            )
        }

        val contract = runCatching {
            V2MutationRequestFactory.create(mutation, resolvedVersion)
        }.getOrElse {
            return V2TransportResult.Failure(null, "Senkronizasyon isteği oluşturulamadı.")
        }
        val response = safeExecute(
            V2HttpRequest(
                baseUrl = baseUrl,
                path = contract.path,
                method = contract.method,
                token = token,
                bodyJson = contract.bodyJson
            )
        ) ?: return V2TransportResult.Offline("Sunucuya ulaşılamıyor.")

        if (response.code !in 200..299) return mapFailure(response)

        if (mutation.mutationType == OnlineMutationType.DELETE) {
            return V2TransportResult.Success(
                V2MutationAck((mutation.expectedVersion ?: resolvedVersion ?: 0) + 1, null)
            )
        }

        return runCatching {
            val root = JSONObject(response.body)
            val key = when (mutation.entityType) {
                OnlineEntityType.BUSINESS -> "business"
                OnlineEntityType.SERVICE -> "service"
                OnlineEntityType.STAFF -> "staff_member"
                OnlineEntityType.STAFF_LEAVE -> "staff_leave"
                OnlineEntityType.APPOINTMENT -> "appointment"
            }
            val record = root.getJSONObject(key)
            V2TransportResult.Success(
                V2MutationAck(record.optInt("version", 1), record.toString())
            )
        }.getOrElse {
            V2TransportResult.Failure(response.code, "Sunucu kaydı okunamadı.")
        }
    }

    private suspend fun authRequest(
        request: V2HttpRequest
    ): V2TransportResult<V2AuthenticatedSession> {
        val response = safeExecute(request)
            ?: return V2TransportResult.Offline("Sunucuya ulaşılamıyor.")
        if (response.code !in 200..299) return mapFailure(response)
        return runCatching {
            V2TransportResult.Success(V2ApiParser.authSession(JSONObject(response.body)))
        }.getOrElse {
            V2TransportResult.Failure(response.code, "Oturum yanıtı okunamadı.")
        }
    }

    private suspend fun safeExecute(request: V2HttpRequest): V2HttpResponse? =
        try {
            engine.execute(request)
        } catch (_: Throwable) {
            null
        }

    private fun <T> mapFailure(response: V2HttpResponse): V2TransportResult<T> {
        val root = runCatching { JSONObject(response.body) }.getOrNull()
        val message = root?.optString("message")?.takeIf { it.isNotBlank() }
        return when (response.code) {
            401 -> V2TransportResult.AuthRequired(message)
            409 -> {
                val current = root?.optJSONObject("current")?.toString()
                if (current != null) V2TransportResult.Conflict(current)
                else V2TransportResult.Failure(response.code, message ?: "Çakışma oluştu.")
            }
            else -> V2TransportResult.Failure(response.code, message)
        }
    }
}
