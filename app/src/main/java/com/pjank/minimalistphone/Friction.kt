package com.pjank.minimalistphone

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Burst-sensitive unlock friction. Every unlock adds a point of "heat"; heat decays
 * continuously with a [HALF_LIFE_MINUTES]-minute half-life, and once it climbs past the
 * free allowance the home screen is held behind a dead black screen — longer the hotter
 * the streak. Compulsive re-checks pay a fast-growing toll; spaced-out pickups stay free
 * all day, however many there are, and a half-hour break wipes the slate almost clean.
 * Like the allow-list, the knobs are hard-coded.
 *
 * Deliberately friction, not prison: notifications and recents still open apps
 * directly, and the delay is capped low enough to never matter in an emergency.
 */
object Friction {

    /** Half-life of pickup heat: each 15 minutes untouched halves it. */
    const val HALF_LIFE_MINUTES = 15.0

    /**
     * Heat below which unlocks cost nothing. Each unlock adds 1 heat, so this means
     * roughly two free pickups per 15-minute stretch before the toll starts.
     */
    const val FREE_HEAT = 2.0

    private const val SECONDS_PER_HEAT = 4
    private const val MAX_SECONDS = 30

    /** [heat] as it stands [elapsedMs] later, after exponential decay. */
    fun decayedHeat(heat: Double, elapsedMs: Long): Double {
        if (elapsedMs <= 0) return heat
        val halfLives = elapsedMs / (HALF_LIFE_MINUTES * 60_000.0)
        return heat * 2.0.pow(-halfLives)
    }

    /** How long the home screen stays dead at the given [heat], in seconds. */
    fun delaySeconds(heat: Double): Int =
        ((heat - FREE_HEAT) * SECONDS_PER_HEAT).roundToInt().coerceIn(0, MAX_SECONDS)
}
