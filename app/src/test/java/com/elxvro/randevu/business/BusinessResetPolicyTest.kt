package com.elxvro.randevu.business

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessResetPolicyTest {
    @Test fun `setup only reset preserves operational data`() {
        val plan = BusinessResetPolicy.plan(destructive = false)
        assertTrue(plan.clearProfile)
        assertTrue(plan.clearServices)
        assertFalse(plan.clearAppointments)
        assertFalse(plan.clearStaff)
        assertFalse(plan.clearLeaves)
    }

    @Test fun `destructive reset clears operational data`() {
        val plan = BusinessResetPolicy.plan(destructive = true)
        assertTrue(plan.clearProfile)
        assertTrue(plan.clearServices)
        assertTrue(plan.clearAppointments)
        assertTrue(plan.clearStaff)
        assertTrue(plan.clearLeaves)
    }
}
