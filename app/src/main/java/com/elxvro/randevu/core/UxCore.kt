package com.elxvro.randevu.core

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

enum class UserRole {
    CUSTOMER,
    BUSINESS
}

data class UxState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val signedIn: Boolean = false,
    val role: UserRole? = null,
    val displayName: String = "",
    val notificationsEnabled: Boolean = true,
    val message: String? = null
)

sealed interface UxAction {
    data class SignIn(val role: UserRole, val displayName: String = "") : UxAction
    data object SignOut : UxAction
    data class SetTheme(val themeMode: ThemeMode) : UxAction
    data class SetNotifications(val enabled: Boolean) : UxAction
    data class ShowMessage(val text: String) : UxAction
    data object ClearMessage : UxAction
}

object UxEngine {
    fun reduce(state: UxState, action: UxAction): UxState = when (action) {
        is UxAction.SignIn -> state.copy(
            signedIn = true,
            role = action.role,
            displayName = action.displayName.trim(),
            message = "Hoş geldin"
        )
        UxAction.SignOut -> state.copy(
            signedIn = false,
            role = null,
            displayName = "",
            message = "Çıkış yapıldı"
        )
        is UxAction.SetTheme -> state.copy(themeMode = action.themeMode)
        is UxAction.SetNotifications -> state.copy(notificationsEnabled = action.enabled)
        is UxAction.ShowMessage -> state.copy(message = action.text)
        UxAction.ClearMessage -> state.copy(message = null)
    }
}
