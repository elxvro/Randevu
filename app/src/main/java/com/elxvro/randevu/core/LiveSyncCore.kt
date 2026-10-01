package com.elxvro.randevu.core

import java.time.Duration
import java.time.LocalDateTime

enum class SyncOperationType {
    CREATE,
    UPDATE,
    DELETE,
    STATUS
}

data class PendingSyncOperation(
    val operationId: String,
    val type: SyncOperationType,
    val appointmentId: String,
    val appointment: Appointment? = null,
    val status: AppointmentStatus? = null
)

data class LiveSyncState(
    val pending: List<PendingSyncOperation> = emptyList(),
    val syncing: Boolean = false,
    val lastSyncAt: String? = null,
    val errorMessage: String? = null,
    val nextSequence: Long = 1L
)

data class DashboardStats(
    val total: Int,
    val today: Int,
    val pending: Int,
    val confirmed: Int,
    val completed: Int,
    val cancelled: Int
)

sealed interface LiveSyncAction {
    data class QueueCreate(val appointment: Appointment) : LiveSyncAction
    data class QueueUpdate(val appointment: Appointment) : LiveSyncAction
    data class QueueDelete(val appointmentId: String) : LiveSyncAction
    data class QueueStatus(val appointmentId: String, val status: AppointmentStatus) : LiveSyncAction
    data object SyncStarted : LiveSyncAction
    data class OperationSynced(val operationId: String) : LiveSyncAction
    data class SyncCompleted(val syncedAt: String) : LiveSyncAction
    data class SyncFailed(val message: String) : LiveSyncAction
    data object ClearError : LiveSyncAction
}

object LiveSyncEngine {
    fun reduce(state: LiveSyncState, action: LiveSyncAction): LiveSyncState = when (action) {
        is LiveSyncAction.QueueCreate -> {
            val operation = PendingSyncOperation(
                operationId = operationId(state, SyncOperationType.CREATE, action.appointment.id),
                type = SyncOperationType.CREATE,
                appointmentId = action.appointment.id,
                appointment = action.appointment
            )
            state.copy(
                pending = state.pending.filterNot { it.appointmentId == action.appointment.id } + operation,
                nextSequence = state.nextSequence + 1,
                errorMessage = null
            )
        }

        is LiveSyncAction.QueueUpdate -> {
            val existing = state.pending.lastOrNull { it.appointmentId == action.appointment.id }
            val operation = if (existing?.type == SyncOperationType.CREATE) {
                existing.copy(appointment = action.appointment)
            } else {
                PendingSyncOperation(
                    operationId = operationId(state, SyncOperationType.UPDATE, action.appointment.id),
                    type = SyncOperationType.UPDATE,
                    appointmentId = action.appointment.id,
                    appointment = action.appointment
                )
            }
            state.copy(
                pending = state.pending.filterNot { it.appointmentId == action.appointment.id } + operation,
                nextSequence = if (existing?.type == SyncOperationType.CREATE) state.nextSequence else state.nextSequence + 1,
                errorMessage = null
            )
        }

        is LiveSyncAction.QueueDelete -> {
            val existing = state.pending.lastOrNull { it.appointmentId == action.appointmentId }
            if (existing?.type == SyncOperationType.CREATE) {
                state.copy(
                    pending = state.pending.filterNot { it.appointmentId == action.appointmentId },
                    errorMessage = null
                )
            } else {
                val operation = PendingSyncOperation(
                    operationId = operationId(state, SyncOperationType.DELETE, action.appointmentId),
                    type = SyncOperationType.DELETE,
                    appointmentId = action.appointmentId
                )
                state.copy(
                    pending = state.pending.filterNot { it.appointmentId == action.appointmentId } + operation,
                    nextSequence = state.nextSequence + 1,
                    errorMessage = null
                )
            }
        }

        is LiveSyncAction.QueueStatus -> {
            val existing = state.pending.lastOrNull { it.appointmentId == action.appointmentId }
            val operation = when {
                existing?.type == SyncOperationType.CREATE && existing.appointment != null ->
                    existing.copy(appointment = existing.appointment.copy(status = action.status))

                existing?.type == SyncOperationType.UPDATE && existing.appointment != null ->
                    existing.copy(appointment = existing.appointment.copy(status = action.status))

                else -> PendingSyncOperation(
                    operationId = operationId(state, SyncOperationType.STATUS, action.appointmentId),
                    type = SyncOperationType.STATUS,
                    appointmentId = action.appointmentId,
                    status = action.status
                )
            }
            val reused = operation.operationId == existing?.operationId
            state.copy(
                pending = state.pending.filterNot { it.appointmentId == action.appointmentId } + operation,
                nextSequence = if (reused) state.nextSequence else state.nextSequence + 1,
                errorMessage = null
            )
        }

        LiveSyncAction.SyncStarted -> state.copy(syncing = true, errorMessage = null)

        is LiveSyncAction.OperationSynced -> state.copy(
            pending = state.pending.filterNot { it.operationId == action.operationId }
        )

        is LiveSyncAction.SyncCompleted -> state.copy(
            syncing = false,
            lastSyncAt = action.syncedAt,
            errorMessage = null
        )

        is LiveSyncAction.SyncFailed -> state.copy(
            syncing = false,
            errorMessage = action.message.ifBlank { "Senkronizasyon başarısız" }
        )

        LiveSyncAction.ClearError -> state.copy(errorMessage = null)
    }

    fun dashboard(appointments: List<Appointment>, today: String): DashboardStats = DashboardStats(
        total = appointments.size,
        today = appointments.count { it.date == today },
        pending = appointments.count { it.status == AppointmentStatus.PENDING },
        confirmed = appointments.count { it.status == AppointmentStatus.CONFIRMED },
        completed = appointments.count { it.status == AppointmentStatus.COMPLETED },
        cancelled = appointments.count { it.status == AppointmentStatus.CANCELLED }
    )

    fun customerHistory(appointments: List<Appointment>, customerOrPhone: String): List<Appointment> {
        val query = customerOrPhone.trim().lowercase()
        if (query.isBlank()) return emptyList()
        return appointments
            .filter {
                it.phone.trim() == customerOrPhone.trim() ||
                    it.customer.lowercase().contains(query)
            }
            .sortedWith(compareByDescending<Appointment> { it.date }.thenByDescending { it.time })
    }

    fun staffCalendar(appointments: List<Appointment>, staff: String, date: String): List<Appointment> =
        appointments
            .filter {
                it.staff.equals(staff.trim(), ignoreCase = true) &&
                    it.date == date &&
                    it.status != AppointmentStatus.CANCELLED
            }
            .sortedBy { it.time }

    fun reminderCandidates(
        appointments: List<Appointment>,
        nowIso: String,
        withinMinutes: Long
    ): List<Appointment> {
        if (withinMinutes <= 0) return emptyList()
        val now = runCatching { LocalDateTime.parse(nowIso) }.getOrNull() ?: return emptyList()
        val eligible = appointments
            .asSequence()
            .filter { it.status == AppointmentStatus.CONFIRMED }
            .mapNotNull { appointment ->
                val at = appointmentDateTime(appointment) ?: return@mapNotNull null
                val minutes = Duration.between(now, at).toMinutes()
                if (minutes in 0..withinMinutes) appointment to at else null
            }
            .sortedBy { it.second }
            .toList()

        val seenCustomers = mutableSetOf<String>()
        return eligible.mapNotNull { (appointment, _) ->
            val customerKey = appointment.phone.filter(Char::isDigit).ifBlank {
                appointment.customer.trim().lowercase()
            }
            if (seenCustomers.add(customerKey)) appointment else null
        }
    }

    private fun appointmentDateTime(appointment: Appointment): LocalDateTime? {
        val normalizedTime = when (appointment.time.count { it == ':' }) {
            0 -> "${appointment.time}:00:00"
            1 -> "${appointment.time}:00"
            else -> appointment.time
        }
        return runCatching { LocalDateTime.parse("${appointment.date}T$normalizedTime") }.getOrNull()
    }

    private fun operationId(
        state: LiveSyncState,
        type: SyncOperationType,
        appointmentId: String
    ): String = "${type.name.lowercase()}:$appointmentId:${state.nextSequence}"
}
