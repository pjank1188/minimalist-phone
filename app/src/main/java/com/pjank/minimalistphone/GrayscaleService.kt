package com.pjank.minimalistphone

import android.accessibilityservice.AccessibilityService
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

/**
 * Watches which app is in the foreground and toggles system-wide grayscale: OFF (color)
 * while a [AllowList.COLOR_APPS] app like the camera is showing, ON everywhere else.
 *
 * Toggling the color-correction "daltonizer" secure settings needs WRITE_SECURE_SETTINGS,
 * granted once over adb:
 *   adb shell pm grant com.pjank.minimalistphone android.permission.WRITE_SECURE_SETTINGS
 */
class GrayscaleService : AccessibilityService() {

    /** Window changes from these packages are ignored so they don't flip grayscale. */
    private val ignored = setOf(
        "com.android.systemui",   // status bar / notification shade / quick settings
    )

    override fun onServiceConnected() {
        // Start from a known state: grayscale on.
        setGrayscale(true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg in ignored) return

        // Time-blocked work app reached outside hours: bounce straight back home.
        if (Schedule.isRestrictedNow(pkg)) {
            performGlobalAction(GLOBAL_ACTION_HOME)
            Toast.makeText(this, "work apps are off right now", Toast.LENGTH_SHORT).show()
            return
        }

        setGrayscale(enabled = pkg !in AllowList.COLOR_APPS)
    }

    override fun onInterrupt() {}

    private fun setGrayscale(enabled: Boolean) {
        val cr = contentResolver
        val target = if (enabled) 1 else 0
        val current = Settings.Secure.getInt(cr, DALTONIZER_ENABLED, 0)
        if (current == target) return

        if (enabled) {
            // 0 == monochromacy (grayscale)
            Settings.Secure.putInt(cr, DALTONIZER_MODE, 0)
        }
        Settings.Secure.putInt(cr, DALTONIZER_ENABLED, target)
    }

    private companion object {
        const val DALTONIZER_ENABLED = "accessibility_display_daltonizer_enabled"
        const val DALTONIZER_MODE = "accessibility_display_daltonizer"
    }
}
