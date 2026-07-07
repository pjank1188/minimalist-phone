package com.pjank.minimalistphone

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

/**
 * Watches which app is in the foreground and bounces time-blocked apps (see [Schedule])
 * straight back to the home screen outside their windows — work hours, Instagram's daily
 * hour, bedtime. Covers the paths the launcher can't filter: recents, notifications,
 * share sheets.
 *
 * Also hosts the pickup counter's unlock receiver, since this service is the app's only
 * component that's alive whenever the phone is on.
 */
class WorkHoursService : AccessibilityService() {

    /**
     * Counts unlocks for the home-screen pickup counter. ACTION_USER_PRESENT is an
     * implicit broadcast that can't be manifest-registered, so it's caught here at
     * runtime instead.
     */
    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_USER_PRESENT) Pickups.record(context)
        }
    }

    override fun onServiceConnected() {
        registerReceiver(unlockReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(unlockReceiver)
        } catch (e: IllegalArgumentException) {
            // Never registered (service killed before onServiceConnected) — fine.
        }
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return

        Schedule.restrictionFor(pkg)?.let { restriction ->
            performGlobalAction(GLOBAL_ACTION_HOME)
            Toast.makeText(this, restriction.message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onInterrupt() {}
}
