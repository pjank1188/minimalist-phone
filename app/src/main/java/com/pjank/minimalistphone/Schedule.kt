package com.pjank.minimalistphone

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Time-based access rules. A restricted app is hidden from the launcher and bounced back
 * to home if reached another way (recents, notifications, share sheets). Three rules:
 * work apps only during work hours, Instagram only in its daily window, and bedtime,
 * which hides everything but the essentials overnight.
 */
object Schedule {

    /** Why an app is blocked right now — also the toast shown when it's bounced. */
    enum class Restriction(val message: String) {
        WORK("work apps are off right now"),
        INSTAGRAM("instagram is 5–6 pm only"),
        BEDTIME("it's bedtime"),
    }

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

    // Bedtime: 22:00–06:00 every day. The whole allow-list is hidden (and bounced) except
    // the essentials below.
    private const val BEDTIME_START_HOUR = 22 // inclusive (22:00 / 10 PM)
    private const val BEDTIME_END_HOUR = 6    // exclusive (06:00)

    /**
     * The only allow-listed apps that stay usable during bedtime: emergencies (phone,
     * Signal), alarms (clock), and 2FA — codes must be reachable at any hour, same
     * principle that keeps Microsoft Authenticator out of [TIME_RESTRICTED]. Apps outside
     * the allow-list (Settings, incoming-call UI, permission dialogs) are never bounced.
     */
    val BEDTIME_ALLOWED = setOf(
        "com.google.android.dialer",    // Phone
        "org.thoughtcrime.securesms",   // Signal
        "com.google.android.deskclock", // Clock (alarms)
        "com.azure.authenticator",      // Microsoft Authenticator (2FA)
        "proton.android.pass",          // Proton Pass (2FA)
        "com.okta.android.auth",        // Okta Verify (2FA)
    )

    /** True during bedtime: 22:00–06:00. The window wraps midnight. */
    fun isBedtime(now: ZonedDateTime = ZonedDateTime.now(zone)): Boolean =
        now.hour >= BEDTIME_START_HOUR || now.hour < BEDTIME_END_HOUR

    /** The rule blocking [pkg] right now, or null if it's currently allowed. */
    fun restrictionFor(pkg: String, now: ZonedDateTime = ZonedDateTime.now(zone)): Restriction? {
        when (pkg) {
            INSTAGRAM -> if (!instagramAllowed(now)) return Restriction.INSTAGRAM
            in TIME_RESTRICTED -> if (!workAppsAllowed(now)) return Restriction.WORK
        }
        val allowListed = pkg in AllowList.PACKAGES || pkg in AllowList.WORK ||
            pkg in AllowList.UTILITIES
        if (isBedtime(now) && allowListed && pkg !in BEDTIME_ALLOWED) return Restriction.BEDTIME
        return null
    }

    /** True if [pkg] is blocked by any time rule right now. */
    fun isRestrictedNow(pkg: String): Boolean = restrictionFor(pkg) != null
}
