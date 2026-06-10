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
    )
}
