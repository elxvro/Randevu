package com.elxvro.randevu.core

import org.junit.Assert.assertEquals
import org.junit.Test

class LiveSyncApiContractTest {
    @Test
    fun endpointPathsAreStable() {
        assertEquals("/appointments", LiveSyncApiContract.appointmentsPath())
        assertEquals("/appointments/42/status", LiveSyncApiContract.statusPath("42"))
        assertEquals("/auth/login", LiveSyncApiContract.loginPath())
        assertEquals("/dashboard", LiveSyncApiContract.dashboardPath())
    }

    @Test
    fun statusAndDateTimeUseBackendWireFormat() {
        assertEquals("confirmed", LiveSyncApiContract.toWireStatus(AppointmentStatus.CONFIRMED))
        assertEquals("2026-10-02 09:30:00", LiveSyncApiContract.startsAt("2026-10-02", "09:30"))
    }
}
