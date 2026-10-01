package com.elxvro.randevu.core

enum class ApiMode {
    DEMO,
    REMOTE
}

enum class ConnectionState {
    IDLE,
    CHECKING,
    CONNECTED,
    ERROR
}

data class BackendConfig(
    val baseUrl: String = "",
    val mode: ApiMode = ApiMode.DEMO
) {
    fun normalizedBaseUrl(): String = baseUrl.trim().trimEnd('/')

    fun isRemoteConfigurationValid(): Boolean {
        if (mode == ApiMode.DEMO) return true
        val normalized = normalizedBaseUrl()
        return normalized.startsWith("https://") && normalized.length > "https://".length
    }
}

data class BackendSession(
    val authenticated: Boolean = false,
    val userId: Long? = null,
    val token: String = "",
    val displayName: String = "",
    val phone: String = ""
)

data class BackendState(
    val config: BackendConfig = BackendConfig(),
    val connectionState: ConnectionState = ConnectionState.CONNECTED,
    val serverName: String = "Yerel Demo",
    val errorMessage: String? = null,
    val session: BackendSession = BackendSession()
)

sealed interface BackendAction {
    data class SetMode(val mode: ApiMode) : BackendAction
    data class SetBaseUrl(val baseUrl: String) : BackendAction
    data object CheckConnection : BackendAction
    data class ConnectionSucceeded(val serverName: String) : BackendAction
    data class ConnectionFailed(val message: String) : BackendAction
    data class SignInSucceeded(
        val userId: Long,
        val token: String,
        val displayName: String,
        val phone: String
    ) : BackendAction
    data object SignOut : BackendAction
}

object BackendEngine {
    fun reduce(state: BackendState, action: BackendAction): BackendState = when (action) {
        is BackendAction.SetMode -> {
            val config = state.config.copy(mode = action.mode)
            if (action.mode == ApiMode.DEMO) {
                state.copy(
                    config = config,
                    connectionState = ConnectionState.CONNECTED,
                    serverName = "Yerel Demo",
                    errorMessage = null
                )
            } else {
                state.copy(
                    config = config,
                    connectionState = ConnectionState.IDLE,
                    serverName = "",
                    errorMessage = null
                )
            }
        }

        is BackendAction.SetBaseUrl -> state.copy(
            config = state.config.copy(baseUrl = action.baseUrl),
            connectionState = if (state.config.mode == ApiMode.DEMO) {
                ConnectionState.CONNECTED
            } else {
                ConnectionState.IDLE
            },
            errorMessage = null
        )

        BackendAction.CheckConnection -> {
            if (state.config.mode == ApiMode.DEMO) {
                state.copy(
                    connectionState = ConnectionState.CONNECTED,
                    serverName = "Yerel Demo",
                    errorMessage = null
                )
            } else {
                state.copy(
                    connectionState = ConnectionState.CHECKING,
                    serverName = "",
                    errorMessage = null
                )
            }
        }

        is BackendAction.ConnectionSucceeded -> state.copy(
            connectionState = ConnectionState.CONNECTED,
            serverName = action.serverName.ifBlank { "Randevu API" },
            errorMessage = null
        )

        is BackendAction.ConnectionFailed -> state.copy(
            connectionState = ConnectionState.ERROR,
            serverName = "",
            errorMessage = action.message
        )

        is BackendAction.SignInSucceeded -> state.copy(
            session = BackendSession(
                authenticated = true,
                userId = action.userId,
                token = action.token,
                displayName = action.displayName.trim(),
                phone = action.phone.trim()
            )
        )

        BackendAction.SignOut -> state.copy(session = BackendSession())
    }
}
