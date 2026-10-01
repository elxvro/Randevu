package com.elxvro.randevu.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveSyncCoreTest {
    private fun appointment(
        id: String,
        customer: String = "Ayşe Yılmaz",
        phone: String = "05550000000",
        staff: String = "Elif",
        date: String = "2026-10-02",
        time: String = "10:00",
        status: AppointmentStatus = AppointmentStatus.PENDING
    ) = Appointment(
        id = id,
        customer = customer,
        phone = phone,
        service = "Saç Kesimi",
        staff = staff,
        date = date,
        time = time,
        status = status,
        note = ""
    )

    @Test
    fun queueCompactsMultipleChangesForSameAppointment() {
        var state = LiveSyncState()
        val original = appointment("42")
        val changed = original.copy(time = "11:00")

        state = LiveSyncEngine.reduce(state, LiveSyncAction.QueueCreate(original))
        state = LiveSyncEngine.reduce(state, LiveSyncAction.QueueUpdate(changed))

        assertEquals(1, state.pending.size)
        assertEquals(SyncOperationType.CREATE, state.pending.single().type)
        assertEquals("11:00", state.pending.single().appointment?.time)
    }

    @Test
    fun syncLifecycleKeepsErrorAndPendingWorkPredictable() {
        var state = LiveSyncState()
        state = LiveSyncEngine.reduce(state, LiveSyncAction.QueueStatus("7", AppointmentStatus.CONFIRMED))
        state = LiveSyncEngine.reduce(state, LiveSyncAction.SyncStarted)
        assertTrue(state.syncing)

        state = LiveSyncEngine.reduce(state, LiveSyncAction.SyncFailed("İnternet yok"))
        assertFalse(state.syncing)
        assertEquals("İnternet yok", state.errorMessage)
        assertEquals(1, state.pending.size)

        val opId = state.pending.single().operationId
        state = LiveSyncEngine.reduce(state, LiveSyncAction.OperationSynced(opId))
        state = LiveSyncEngine.reduce(state, LiveSyncAction.SyncCompleted("2026-10-02T01:30:00+03:00"))
        assertTrue(state.pending.isEmpty())
        assertEquals("2026-10-02T01:30:00+03:00", state.lastSyncAt)
    }

    @Test
    fun dashboardAndHistoryAreDerivedFromAppointments() {
        val items = listOf(
            appointment("1", phone = "05001112233", status = AppointmentStatus.CONFIRMED),
            appointment("2", phone = "05001112233", date = "2026-10-03", status = AppointmentStatus.COMPLETED),
            appointment("3", customer = "Mehmet", phone = "05009998877", status = AppointmentStatus.CANCELLED)
        )

        val stats = LiveSyncEngine.dashboard(items, "2026-10-02")
        assertEquals(3, stats.total)
        assertEquals(2, stats.today)
        assertEquals(1, stats.confirmed)
        assertEquals(1, stats.completed)
        assertEquals(1, stats.cancelled)

        val history = LiveSyncEngine.customerHistory(items, "05001112233")
        assertEquals(listOf("2", "1"), history.map { it.id })
    }

    @Test
    fun staffCalendarAndReminderWindowUseDateTime() {
        val items = listOf(
            appointment("1", staff = "Elif", date = "2026-10-02", time = "10:10", status = AppointmentStatus.CONFIRMED),
            appointment("2", staff = "Elif", date = "2026-10-02", time = "13:00", status = AppointmentStatus.PENDING),
            appointment("3", staff = "Mert", date = "2026-10-02", time = "10:20", status = AppointmentStatus.CONFIRMED)
        )

        assertEquals(listOf("1", "2"), LiveSyncEngine.staffCalendar(items, "Elif", "2026-10-02").map { it.id })
        assertEquals(
            listOf("1"),
            LiveSyncEngine.reminderCandidates(
                appointments = items,
                nowIso = "2026-10-02T10:00:00",
                withinMinutes = 30
            ).map { it.id }
        )
    }
}
