package com.elxvro.randevu.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackendCoreTest {
    @Test
    fun remoteBaseUrlIsNormalizedAndValidated() {
        val config = BackendConfig(baseUrl = "  https://api.example.com/v1/  ", mode = ApiMode.REMOTE)
        assertEquals("https://api.example.com/v1", config.normalizedBaseUrl())
        assertTrue(config.isRemoteConfigurationValid())

        val insecure = config.copy(baseUrl = "http://api.example.com")
        assertFalse(insecure.isRemoteConfigurationValid())
    }

    @Test
    fun connectionLifecycleTracksServerState() {
        var state = BackendState()
        state = BackendEngine.reduce(state, BackendAction.SetMode(ApiMode.REMOTE))
        state = BackendEngine.reduce(state, BackendAction.SetBaseUrl("https://demo.example.com/api"))
        state = BackendEngine.reduce(state, BackendAction.CheckConnection)
        assertEquals(ConnectionState.CHECKING, state.connectionState)

        state = BackendEngine.reduce(state, BackendAction.ConnectionSucceeded("Randevu API"))
        assertEquals(ConnectionState.CONNECTED, state.connectionState)
        assertEquals("Randevu API", state.serverName)
        assertNull(state.errorMessage)
    }

    @Test
    fun authenticatedSessionCanBeCreatedAndCleared() {
        var state = BackendState()
        state = BackendEngine.reduce(
            state,
            BackendAction.SignInSucceeded(
                userId = 42L,
                token = "token-42",
                displayName = "Emre",
                phone = "5550000000"
            )
        )
        assertTrue(state.session.authenticated)
        assertEquals(42L, state.session.userId)
        assertEquals("token-42", state.session.token)

        state = BackendEngine.reduce(state, BackendAction.SignOut)
        assertFalse(state.session.authenticated)
        assertNull(state.session.userId)
        assertEquals("", state.session.token)
    }

    @Test
    fun demoModeWorksWithoutRemoteServer() {
        val state = BackendState()
        assertEquals(ApiMode.DEMO, state.config.mode)
        assertEquals(ConnectionState.CONNECTED, state.connectionState)
        assertEquals("Yerel Demo", state.serverName)
    }
}
