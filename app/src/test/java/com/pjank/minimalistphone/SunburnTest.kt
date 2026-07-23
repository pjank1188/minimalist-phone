package com.pjank.minimalistphone

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SunburnTest {

    @Test
    fun `turns on at the on-threshold, not before`() {
        assertFalse(Sunburn.shouldBeActive(Sunburn.ON_HEAT - 0.1, isActive = false))
        assertTrue(Sunburn.shouldBeActive(Sunburn.ON_HEAT, isActive = false))
    }

    @Test
    fun `stays on until heat decays below the off-threshold`() {
        assertTrue(Sunburn.shouldBeActive(Sunburn.ON_HEAT - 0.1, isActive = true))
        assertTrue(Sunburn.shouldBeActive(Sunburn.OFF_HEAT + 0.1, isActive = true))
        assertFalse(Sunburn.shouldBeActive(Sunburn.OFF_HEAT, isActive = true))
    }

    @Test
    fun `hysteresis - mid-band heat keeps whatever state it had`() {
        val mid = (Sunburn.ON_HEAT + Sunburn.OFF_HEAT) / 2
        assertFalse(Sunburn.shouldBeActive(mid, isActive = false))
        assertTrue(Sunburn.shouldBeActive(mid, isActive = true))
    }
}
