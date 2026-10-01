package com.elxvro.randevu.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UxCoreTest {
    @Test
    fun signInAndSignOutManageLocalSession() {
        var state = UxState()
        state = UxEngine.reduce(state, UxAction.SignIn(UserRole.CUSTOMER))
        assertTrue(state.signedIn)
        assertEquals(UserRole.CUSTOMER, state.role)

        state = UxEngine.reduce(state, UxAction.SignOut)
        assertFalse(state.signedIn)
        assertNull(state.role)
    }

    @Test
    fun themeAndNotificationPreferencesAreMutable() {
        var state = UxState()
        state = UxEngine.reduce(state, UxAction.SetTheme(ThemeMode.DARK))
        state = UxEngine.reduce(state, UxAction.SetNotifications(false))
        assertEquals(ThemeMode.DARK, state.themeMode)
        assertFalse(state.notificationsEnabled)
    }

    @Test
    fun transientMessagesCanBeShownAndCleared() {
        var state = UxState()
        state = UxEngine.reduce(state, UxAction.ShowMessage("Kaydedildi"))
        assertEquals("Kaydedildi", state.message)
        state = UxEngine.reduce(state, UxAction.ClearMessage)
        assertNull(state.message)
    }
}
