package com.pjank.minimalistphone

import java.time.DayOfWeek
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

    /** True during the allowed window: weekdays, 08:00–18:00 Eastern. */
    fun workAppsAllowed(now: ZonedDateTime = ZonedDateTime.now(zone)): Boolean {
        val isWeekday = now.dayOfWeek != DayOfWeek.SATURDAY && now.dayOfWeek != DayOfWeek.SUNDAY
        val inHours = now.hour in START_HOUR until END_HOUR
        return isWeekday && inHours
    }

    /** True if [pkg] is time-restricted and we're currently outside the allowed window. */
    fun isRestrictedNow(pkg: String): Boolean =
        pkg in TIME_RESTRICTED && !workAppsAllowed()
}
