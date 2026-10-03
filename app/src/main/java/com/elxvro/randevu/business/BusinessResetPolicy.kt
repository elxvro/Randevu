package com.elxvro.randevu.business

data class BusinessResetPlan(
    val clearProfile: Boolean,
    val clearServices: Boolean,
    val clearAppointments: Boolean,
    val clearStaff: Boolean,
    val clearLeaves: Boolean
)

object BusinessResetPolicy {
    fun plan(destructive: Boolean): BusinessResetPlan = BusinessResetPlan(
        clearProfile = true,
        clearServices = true,
        clearAppointments = destructive,
        clearStaff = destructive,
        clearLeaves = destructive
    )
}
