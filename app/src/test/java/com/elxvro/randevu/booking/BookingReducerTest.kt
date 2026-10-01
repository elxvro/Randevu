package com.elxvro.randevu.booking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookingReducerTest {
    @Test
    fun `selection flow updates the booking draft`() {
        var state = BookingDraft()
        state = BookingReducer.reduce(state, BookingAction.SelectService("Erkek Saç Kesimi", 250))
        state = BookingReducer.reduce(state, BookingAction.SelectStaff("Ahmet Demir", "Uzman Kuaför"))
        state = BookingReducer.reduce(state, BookingAction.SelectDate("22 Ekim 2026"))
        state = BookingReducer.reduce(state, BookingAction.SelectTime("13:30"))

        assertEquals("Erkek Saç Kesimi", state.serviceName)
        assertEquals(250, state.price)
        assertEquals("Ahmet Demir", state.staffName)
        assertEquals("22 Ekim 2026", state.date)
        assertEquals("13:30", state.time)
        assertTrue(state.isComplete)
    }

    @Test
    fun `draft is incomplete until every required selection exists`() {
        val state = BookingReducer.reduce(
            BookingDraft(),
            BookingAction.SelectService("Saç Boyama", 600)
        )
        assertFalse(state.isComplete)
    }
}
