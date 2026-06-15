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
        "com.google.android.calendar",         // Calendar
        "org.thoughtcrime.securesms",          // Signal
        "com.groupme.android",                 // GroupMe
        "com.google.android.gm",               // Gmail
        "com.apple.android.music",             // Apple Music

        // DAVx5 (at.bitfire.davdroid) stays installed to bridge the shared iCloud
        // calendar into Google Calendar via CalDAV, but doesn't need a menu entry —
        // it syncs in the background.
    )

    /**
     * Work apps — live in the managed Work profile (user 10). They only appear once
     * installed there via Company Portal; until then they're silently skipped. Rendered
     * like [PACKAGES] but separated by a gap to set the work block apart.
     */
    val WORK = listOf(
        "com.microsoft.teams",                 // Microsoft Teams
        "com.microsoft.office.outlook",        // Microsoft Outlook
        "com.azure.authenticator",             // Microsoft Authenticator
    )

    /**
     * Rarely-used utility apps (banking, travel, tools). Shown below the main list in
     * smaller, dimmer text — present when needed, visually out of the way the rest of
     * the time. Uninstalled packages are silently skipped, same as [PACKAGES].
     */
    val UTILITIES = listOf(
        "com.chase.sig.android",               // Chase
        "com.konylabs.capitalone",             // Capital One
        "com.fidelity.android",                // Fidelity Investments
        "com.delta.mobile.android",            // Fly Delta
        "com.mobiquityinc.awsevents",          // AWS Events (AWS Summit NYC)
        "proton.android.pass",                 // Proton Pass (authenticator / 2FA)
        "com.google.android.deskclock",        // Clock
    )
}
