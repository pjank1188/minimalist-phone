package com.pjank.minimalistphone

import org.junit.Assert.assertEquals
import org.junit.Test

class FrictionTest {

    @Test
    fun `pickups within the free allowance cost nothing`() {
        assertEquals(0, Friction.delaySeconds(0))
        assertEquals(0, Friction.delaySeconds(1))
        assertEquals(0, Friction.delaySeconds(Friction.FREE_PICKUPS))
    }

    @Test
    fun `delay escalates past the free allowance`() {
        assertEquals(2, Friction.delaySeconds(Friction.FREE_PICKUPS + 1))
        assertEquals(4, Friction.delaySeconds(Friction.FREE_PICKUPS + 2))
        assertEquals(10, Friction.delaySeconds(Friction.FREE_PICKUPS + 5))
    }

    @Test
    fun `delay is capped`() {
        assertEquals(20, Friction.delaySeconds(Friction.FREE_PICKUPS + 10))
        assertEquals(20, Friction.delaySeconds(Friction.FREE_PICKUPS + 11))
        assertEquals(20, Friction.delaySeconds(100))
    }
}
