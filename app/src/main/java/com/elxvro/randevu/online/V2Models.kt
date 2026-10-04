package com.elxvro.randevu.online

data class V2Session(
    val ownerName: String,
    val email: String,
    val businessSlug: String,
    val expiresAt: String
)

enum class V2EntryDestination { AUTH, SETUP, APP }

object V2EntryGate {
    fun destination(hasToken: Boolean, profileComplete: Boolean, hasActiveService: Boolean): V2EntryDestination = when {
        !hasToken -> V2EntryDestination.AUTH
        !profileComplete || !hasActiveService -> V2EntryDestination.SETUP
        else -> V2EntryDestination.APP
    }
}
