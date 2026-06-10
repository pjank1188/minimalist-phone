package com.pjank.minimalistphone

/**
 * The hard-coded allow-list. Changing what appears on the home screen means editing
 * this file and rebuilding the app — that friction is intentional and is the whole point.
 */
object AllowList {

    /**
     * Discovery switch.
     *
     * Set to `true`, run the app, and the home screen lists EVERY launchable app on the
     * device as "Label  —  package.name", so you can read off the real package names
     * (vendor apps like the camera/dialer differ per device). Copy the ones you want
     * into [PACKAGES] below, then set this back to `false` and rebuild.
     */
    const val SHOW_ALL = false

    /**
     * Apps allowed on the home screen, shown in this order. Package names verified
     * against the Seeker's installed apps. Any package that isn't installed is
     * silently skipped, so this stays safe if an app is later removed.
     */
    val PACKAGES = listOf(
        "com.google.android.dialer",           // Phone
        "com.google.android.apps.messaging",   // Messages
        "com.google.android.apps.maps",        // Maps
        "com.android.camera",                  // Camera
        "com.google.android.deskclock",        // Clock
        "com.google.android.calendar",         // Calendar
        "proton.android.pass",                 // Proton Pass (authenticator / 2FA)

        // Work apps — live in the managed Work profile (user 10). They only appear once
        // installed there via Company Portal; until then they're silently skipped.
        "com.microsoft.teams",                 // Microsoft Teams
        "com.microsoft.office.outlook",        // Microsoft Outlook
        "com.azure.authenticator",             // Microsoft Authenticator

        // Shared with wife — iCloud calendar/reminders bridged via DAVx5 (CalDAV).
        "org.tasks",                           // Tasks.org (shared Reminders)
        "at.bitfire.davdroid",                 // DAVx5 (sync config)
    )

    /**
     * Apps that should show in COLOR. While one of these is in the foreground, the
     * GrayscaleService turns off system grayscale; everywhere else stays grayscale.
     */
    val COLOR_APPS = setOf(
        "com.android.camera",                  // Camera
    )
}
