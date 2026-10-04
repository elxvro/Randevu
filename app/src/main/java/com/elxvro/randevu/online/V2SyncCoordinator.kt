package com.elxvro.randevu.online

data class OnlineSnapshot(
    val serverTime: String = "",
    val businessJson: String? = null,
    val servicesJson: List<String> = emptyList(),
    val staffJson: List<String> = emptyList(),
    val staffLeavesJson: List<String> = emptyList(),
    val appointmentsJson: List<String> = emptyList()
)

data class V2MutationAck(
    val version: Int,
    val recordJson: String?
)

sealed class V2TransportResult<out T> {
    data class Success<T>(val value: T) : V2TransportResult<T>()
    data class Offline<T>(val message: String? = null) : V2TransportResult<T>()
    data class AuthRequired<T>(val message: String? = null) : V2TransportResult<T>()
    data class Conflict<T>(val currentServerPayload: String) : V2TransportResult<T>()
    data class Failure<T>(val httpCode: Int?, val message: String? = null) : V2TransportResult<T>()
}

interface V2SyncTransport {
    suspend fun bootstrap(token: String): V2TransportResult<OnlineSnapshot>
    suspend fun apply(token: String, mutation: OnlineMutation): V2TransportResult<V2MutationAck>
}

interface V2SyncPersistence {
    fun pending(): List<OnlineMutation>
    fun savePending(value: List<OnlineMutation>)
    fun replaceSnapshot(value: OnlineSnapshot)
    fun setVersion(entity: OnlineEntityType, id: String, version: Int)
    fun saveConflict(value: SyncConflict?)
    fun markBootstrap(serverTime: String)
    fun markImported(entity: OnlineEntityType, id: String) {}
}

class V2SyncCoordinator(
    private val sessionToken: String,
    private val transport: V2SyncTransport,
    private val persistence: V2SyncPersistence
) {
    suspend fun bootstrap(): V2SyncOutcome {
        val pending = persistence.pending()
        if (pending.isNotEmpty()) {
            return V2SyncOutcome(OnlineConnectionState.SYNCING, pending.size, message = "Bekleyen işlemler önce senkronize edilmeli.")
        }
        return when (val result = transport.bootstrap(sessionToken)) {
            is V2TransportResult.Success -> {
                persistence.replaceSnapshot(result.value)
                persistence.markBootstrap(result.value.serverTime)
                persistence.saveConflict(null)
                V2SyncOutcome(OnlineConnectionState.ONLINE, 0)
            }
            is V2TransportResult.AuthRequired -> V2SyncOutcome(OnlineConnectionState.AUTH_REQUIRED, 0, message = result.message)
            is V2TransportResult.Conflict -> V2SyncOutcome(OnlineConnectionState.CONFLICT, 0, message = "Sunucu verisiyle çakışma var.")
            is V2TransportResult.Offline -> V2SyncOutcome(OnlineConnectionState.OFFLINE, 0, message = result.message)
            is V2TransportResult.Failure -> V2SyncOutcome(OnlineConnectionState.OFFLINE, 0, message = result.message)
        }
    }

    suspend fun enqueueAndSync(mutation: OnlineMutation): V2SyncOutcome {
        persistence.savePending(OnlineSyncQueue.enqueue(persistence.pending(), mutation))
        return flush()
    }

    suspend fun flush(): V2SyncOutcome {
        var queue = persistence.pending().sortedBy { it.createdAtEpochMs }
        if (queue.isEmpty()) return V2SyncOutcome(OnlineConnectionState.ONLINE, 0)

        for (mutation in queue.toList()) {
            when (val result = transport.apply(sessionToken, mutation)) {
                is V2TransportResult.Success -> {
                    persistence.setVersion(mutation.entityType, mutation.externalId, result.value.version)
                    if (mutation.operationId.startsWith("import-")) {
                        persistence.markImported(mutation.entityType, mutation.externalId)
                    }
                    queue = OnlineSyncQueue.remove(queue, mutation.operationId)
                    persistence.savePending(queue)
                    persistence.saveConflict(null)
                }
                is V2TransportResult.Offline ->
                    return V2SyncOutcome(OnlineConnectionState.OFFLINE, queue.size, message = result.message)
                is V2TransportResult.AuthRequired ->
                    return V2SyncOutcome(OnlineConnectionState.AUTH_REQUIRED, queue.size, message = result.message)
                is V2TransportResult.Conflict -> {
                    val conflict = SyncConflict(mutation.entityType, mutation.externalId, result.currentServerPayload)
                    persistence.saveConflict(conflict)
                    return V2SyncOutcome(OnlineConnectionState.CONFLICT, queue.size, conflict)
                }
                is V2TransportResult.Failure ->
                    return V2SyncOutcome(OnlineConnectionState.OFFLINE, queue.size, message = result.message)
            }
        }
        return V2SyncOutcome(OnlineConnectionState.ONLINE, queue.size)
    }
}
