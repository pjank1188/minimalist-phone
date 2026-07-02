package com.pjank.minimalistphone

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Time-based access rules. Work apps are only usable during work hours; outside the
 * window they're hidden from the launcher and bounced back to home if reached another way.
 */
object Schedule {

    private val zone = ZoneId.of("America/New_York")
    private const val START_HOUR = 8   // inclusive (08:00)
    private const val END_HOUR = 18    // exclusive (18:00)

    /**
     * Apps usable only Mon–Fri, 08:00–18:00 Eastern. Microsoft Authenticator is
     * deliberately NOT here — 2FA codes must be reachable at any hour.
     */
    val TIME_RESTRICTED = setOf(
        "com.microsoft.teams",          // Microsoft Teams
        "com.microsoft.office.outlook", // Microsoft Outlook
    )

    /**
     * Instagram gets its own, much tighter window: every day 17:00–18:00 Eastern only,
     * hidden (and bounced) the rest of the day. Separate from the work-hours rule so the
     * daily-vs-weekday logic and the hours don't get tangled together.
     */
    private const val INSTAGRAM = "com.instagram.android"
    private const val IG_START_HOUR = 17 // inclusive (17:00 / 5 PM)
    private const val IG_END_HOUR = 18   // exclusive (18:00 / 6 PM)

    /** True during Instagram's daily window: 17:00–18:00 Eastern. */
    fun instagramAllowed(now: ZonedDateTime = ZonedDateTime.now(zone)): Boolean =
        now.hour in IG_START_HOUR until IG_END_HOUR

    /**
     * Temporary bypass: until this date (exclusive), work apps are allowed at any hour.
     * Set 2026-06-16 to cover the next 3 days; the restriction resumes automatically on
     * this date. To end the bypass early, set this to a past date and rebuild.
     */
    private val BYPASS_UNTIL: LocalDate = LocalDate.of(2026, 6, 19)

    /** True during the allowed window: weekdays, 08:00–18:00 Eastern (or during the bypass). */
    fun workAppsAllowed(now: ZonedDateTime = ZonedDateTime.now(zone)): Boolean {
        if (now.toLocalDate().isBefore(BYPASS_UNTIL)) return true
        val isWeekday = now.dayOfWeek != DayOfWeek.SATURDAY && now.dayOfWeek != DayOfWeek.SUNDAY
        val inHours = now.hour in START_HOUR until END_HOUR
        return isWeekday && inHours
    }

    /** True if [pkg] is time-restricted and we're currently outside its allowed window. */
    fun isRestrictedNow(pkg: String): Boolean = when (pkg) {
        INSTAGRAM -> !instagramAllowed()
        in TIME_RESTRICTED -> !workAppsAllowed()
        else -> false
    }
}
