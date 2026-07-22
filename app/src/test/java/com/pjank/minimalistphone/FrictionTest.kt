package com.pjank.minimalistphone

import org.junit.Assert.assertEquals
import org.junit.Test

class FrictionTest {

    /** Heat after [n] back-to-back pickups (no meaningful decay between them). */
    private fun rapidPickups(n: Int): Double {
        var heat = 0.0
        repeat(n) { heat = Friction.decayedHeat(heat, 0) + 1.0 }
        return heat
    }

    @Test
    fun `a couple of pickups cost nothing`() {
        assertEquals(0, Friction.delaySeconds(rapidPickups(1)))
        assertEquals(0, Friction.delaySeconds(rapidPickups(2)))
    }

    @Test
    fun `rapid pickups escalate the delay`() {
        assertEquals(4, Friction.delaySeconds(rapidPickups(3)))
        assertEquals(8, Friction.delaySeconds(rapidPickups(4)))
        assertEquals(12, Friction.delaySeconds(rapidPickups(5)))
    }

    @Test
    fun `delay is capped`() {
        assertEquals(30, Friction.delaySeconds(rapidPickups(10)))
        assertEquals(30, Friction.delaySeconds(rapidPickups(100)))
    }

    @Test
    fun `heat halves every half-life`() {
        val halfLifeMs = (Friction.HALF_LIFE_MINUTES * 60_000).toLong()
        assertEquals(4.0, Friction.decayedHeat(8.0, halfLifeMs), 1e-9)
        assertEquals(2.0, Friction.decayedHeat(8.0, 2 * halfLifeMs), 1e-9)
        assertEquals(8.0, Friction.decayedHeat(8.0, 0), 1e-9)
    }

    @Test
    fun `a break forgives a hot streak`() {
        // Five frantic pickups cost 12s; after a 45-minute break even the next
        // pickup is free again.
        val hot = rapidPickups(5)
        assertEquals(12, Friction.delaySeconds(hot))
        val afterBreak = Friction.decayedHeat(hot, 45 * 60_000L) + 1.0
        assertEquals(0, Friction.delaySeconds(afterBreak))
    }

    @Test
    fun `spread-out pickups stay free all day`() {
        // One pickup every 20 minutes, 36 times (a 12-hour day): heat never
        // accumulates enough to charge anything.
        var heat = 0.0
        repeat(36) {
            heat = Friction.decayedHeat(heat, 20 * 60_000L) + 1.0
            assertEquals(0, Friction.delaySeconds(heat))
        }
    }
}
