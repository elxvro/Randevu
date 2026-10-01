package com.elxvro.randevu.core

import java.time.LocalDate
import java.time.LocalTime

data class ServiceConfig(
    val id: String,
    val name: String,
    val durationMinutes: Int,
    val price: Int,
    val active: Boolean = true
)

data class StaffMember(
    val id: String,
    val name: String,
    val title: String,
    val active: Boolean = true,
    val serviceIds: Set<String> = emptySet(),
    val branchId: String = "main"
)

data class BreakWindow(val start: String, val end: String)

data class WorkingDay(
    val dayOfWeek: Int,
    val open: String,
    val close: String,
    val enabled: Boolean = true,
    val breaks: List<BreakWindow> = emptyList()
)

data class ClosedDay(val date: String, val reason: String)

data class Branch(
    val id: String,
    val name: String,
    val address: String,
    val phone: String,
    val active: Boolean = true
)

data class BusinessProfile(
    val name: String,
    val phone: String,
    val address: String,
    val description: String
)

data class BusinessState(
    val profile: BusinessProfile,
    val services: List<ServiceConfig>,
    val staff: List<StaffMember>,
    val workingDays: List<WorkingDay>,
    val closedDays: List<ClosedDay>,
    val branches: List<Branch>
)

sealed interface BusinessAction {
    data class UpdateProfile(val profile: BusinessProfile) : BusinessAction
    data class UpsertService(val service: ServiceConfig) : BusinessAction
    data class DeleteService(val id: String) : BusinessAction
    data class ToggleService(val id: String) : BusinessAction
    data class UpsertStaff(val member: StaffMember) : BusinessAction
    data class DeleteStaff(val id: String) : BusinessAction
    data class ToggleStaff(val id: String) : BusinessAction
    data class UpsertWorkingDay(val day: WorkingDay) : BusinessAction
    data class AddClosedDay(val day: ClosedDay) : BusinessAction
    data class RemoveClosedDay(val date: String) : BusinessAction
    data class UpsertBranch(val branch: Branch) : BusinessAction
    data class ToggleBranch(val id: String) : BusinessAction
    data class DeleteBranch(val id: String) : BusinessAction
}

object BusinessEngine {
    fun reduce(state: BusinessState, action: BusinessAction): BusinessState = when (action) {
        is BusinessAction.UpdateProfile -> state.copy(profile = action.profile)
        is BusinessAction.UpsertService -> state.copy(
            services = state.services.upsert(action.service) { it.id }
        )
        is BusinessAction.DeleteService -> state.copy(
            services = state.services.filterNot { it.id == action.id },
            staff = state.staff.map { member ->
                member.copy(serviceIds = member.serviceIds - action.id)
            }
        )
        is BusinessAction.ToggleService -> state.copy(
            services = state.services.map {
                if (it.id == action.id) it.copy(active = !it.active) else it
            }
        )
        is BusinessAction.UpsertStaff -> state.copy(
            staff = state.staff.upsert(action.member) { it.id }
        )
        is BusinessAction.DeleteStaff -> state.copy(
            staff = state.staff.filterNot { it.id == action.id }
        )
        is BusinessAction.ToggleStaff -> state.copy(
            staff = state.staff.map {
                if (it.id == action.id) it.copy(active = !it.active) else it
            }
        )
        is BusinessAction.UpsertWorkingDay -> state.copy(
            workingDays = state.workingDays.upsert(action.day) { it.dayOfWeek }.sortedBy { it.dayOfWeek }
        )
        is BusinessAction.AddClosedDay -> state.copy(
            closedDays = state.closedDays.filterNot { it.date == action.day.date } + action.day
        )
        is BusinessAction.RemoveClosedDay -> state.copy(
            closedDays = state.closedDays.filterNot { it.date == action.date }
        )
        is BusinessAction.UpsertBranch -> state.copy(
            branches = state.branches.upsert(action.branch) { it.id }
        )
        is BusinessAction.ToggleBranch -> state.copy(
            branches = state.branches.map {
                if (it.id == action.id) it.copy(active = !it.active) else it
            }
        )
        is BusinessAction.DeleteBranch -> state.copy(
            branches = state.branches.filterNot { it.id == action.id }
        )
    }

    fun isAvailable(
        state: BusinessState,
        date: String,
        time: String,
        durationMinutes: Int,
        staffId: String,
        appointments: List<Appointment>,
        serviceId: String? = null
    ): Boolean {
        val targetDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return false
        val start = runCatching { LocalTime.parse(time) }.getOrNull() ?: return false
        if (durationMinutes <= 0) return false
        val end = start.plusMinutes(durationMinutes.toLong())

        if (state.closedDays.any { it.date == date }) return false

        val workingDay = state.workingDays.firstOrNull { it.dayOfWeek == targetDate.dayOfWeek.value }
            ?: return false
        if (!workingDay.enabled) return false

        val open = runCatching { LocalTime.parse(workingDay.open) }.getOrNull() ?: return false
        val close = runCatching { LocalTime.parse(workingDay.close) }.getOrNull() ?: return false
        if (start.isBefore(open) || end.isAfter(close)) return false

        val member = state.staff.firstOrNull { it.id == staffId } ?: return false
        if (!member.active) return false
        val branch = state.branches.firstOrNull { it.id == member.branchId }
        if (branch != null && !branch.active) return false

        if (serviceId != null) {
            val service = state.services.firstOrNull { it.id == serviceId } ?: return false
            if (!service.active || serviceId !in member.serviceIds) return false
        }

        val hitsBreak = workingDay.breaks.any { breakWindow ->
            val breakStart = runCatching { LocalTime.parse(breakWindow.start) }.getOrNull() ?: return@any false
            val breakEnd = runCatching { LocalTime.parse(breakWindow.end) }.getOrNull() ?: return@any false
            overlaps(start, end, breakStart, breakEnd)
        }
        if (hitsBreak) return false

        return appointments.none { appointment ->
            if (appointment.status == AppointmentStatus.CANCELLED) return@none false
            if (appointment.date != date || appointment.staff != member.name) return@none false
            val existingStart = runCatching { LocalTime.parse(appointment.time) }.getOrNull() ?: return@none false
            val existingDuration = state.services.firstOrNull { it.name == appointment.service }?.durationMinutes ?: 30
            val existingEnd = existingStart.plusMinutes(existingDuration.toLong())
            overlaps(start, end, existingStart, existingEnd)
        }
    }

    private fun overlaps(
        startA: LocalTime,
        endA: LocalTime,
        startB: LocalTime,
        endB: LocalTime
    ): Boolean = startA.isBefore(endB) && endA.isAfter(startB)

    private inline fun <T, K> List<T>.upsert(item: T, key: (T) -> K): List<T> {
        val itemKey = key(item)
        return if (any { key(it) == itemKey }) {
            map { if (key(it) == itemKey) item else it }
        } else {
            this + item
        }
    }
}

fun defaultBusinessState(): BusinessState = BusinessState(
    profile = BusinessProfile(
        name = "Elite Kuaför",
        phone = "+90 555 111 22 33",
        address = "Kadıköy, İstanbul",
        description = "Saç, sakal ve kişisel bakım hizmetleri"
    ),
    services = listOf(
        ServiceConfig("hair", "Erkek Saç Kesimi", 30, 250),
        ServiceConfig("beard", "Sakal Tıraşı", 20, 150),
        ServiceConfig("combo", "Saç + Sakal", 50, 350),
        ServiceConfig("skin", "Cilt Bakımı", 45, 450)
    ),
    staff = listOf(
        StaffMember("staff-1", "Ahmet Demir", "Uzman Kuaför", true, setOf("hair", "beard", "combo"), "main"),
        StaffMember("staff-2", "Mehmet Kaya", "Kıdemli Kuaför", true, setOf("hair", "beard", "combo", "skin"), "main")
    ),
    workingDays = (1..7).map { day ->
        WorkingDay(
            dayOfWeek = day,
            open = if (day == 7) "10:00" else "09:00",
            close = if (day == 7) "17:00" else "19:00",
            enabled = day != 7,
            breaks = if (day in 1..6) listOf(BreakWindow("13:00", "13:30")) else emptyList()
        )
    },
    closedDays = emptyList(),
    branches = listOf(
        Branch("main", "Merkez Şube", "Kadıköy, İstanbul", "+90 555 111 22 33", true)
    )
)
