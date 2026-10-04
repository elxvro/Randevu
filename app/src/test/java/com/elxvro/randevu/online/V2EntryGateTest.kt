package com.elxvro.randevu.online

import org.junit.Assert.assertEquals
import org.junit.Test

class V2EntryGateTest {
    @Test fun `fresh install routes to auth`() {
        assertEquals(V2EntryDestination.AUTH, V2EntryGate.destination(false, false, false))
    }

    @Test fun `authenticated incomplete business routes to setup`() {
        assertEquals(V2EntryDestination.SETUP, V2EntryGate.destination(true, false, false))
        assertEquals(V2EntryDestination.SETUP, V2EntryGate.destination(true, true, false))
    }

    @Test fun `complete cloud business routes to app`() {
        assertEquals(V2EntryDestination.APP, V2EntryGate.destination(true, true, true))
    }
}
