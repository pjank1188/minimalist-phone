package com.pjank.minimalistphone

/**
 * Escalating unlock friction: the first [FREE_PICKUPS] unlocks of the day cost nothing;
 * every one after that holds the home screen behind a dead black screen for a few
 * seconds — longer the more the phone has been picked up. First checks are free,
 * compulsive re-checks pay a toll. Like the allow-list, the knobs are hard-coded.
 *
 * Deliberately friction, not prison: notifications and recents still open apps
 * directly, and the delay is capped low enough to never matter in an emergency.
 */
object Friction {

    /** Unlocks per day before the toll starts. */
    const val FREE_PICKUPS = 10

    private const val SECONDS_PER_EXTRA_PICKUP = 2
    private const val MAX_SECONDS = 20

    /** How long the home screen stays dead for unlock number [pickupsToday], in seconds. */
    fun delaySeconds(pickupsToday: Int): Int =
        ((pickupsToday - FREE_PICKUPS) * SECONDS_PER_EXTRA_PICKUP).coerceIn(0, MAX_SECONDS)
}
