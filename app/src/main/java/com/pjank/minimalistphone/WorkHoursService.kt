package com.pjank.minimalistphone

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

/**
 * Watches which app is in the foreground and bounces time-blocked work apps
 * (see [Schedule]) straight back to the home screen outside work hours. Covers
 * the paths the launcher can't filter: recents, notifications, share sheets.
 */
class WorkHoursService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return

        if (Schedule.isRestrictedNow(pkg)) {
            performGlobalAction(GLOBAL_ACTION_HOME)
            Toast.makeText(this, "work apps are off right now", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onInterrupt() {}
}
