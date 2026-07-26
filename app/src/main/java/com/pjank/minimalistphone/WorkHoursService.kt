package com.pjank.minimalistphone

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

/**
 * Watches which app is in the foreground and bounces time-blocked apps (see [Schedule])
 * straight back to the home screen outside their windows — work hours, Instagram's daily
 * hour, bedtime. Covers the paths the launcher can't filter: recents, notifications,
 * share sheets.
 *
 * Also watches Chrome's omnibox and bounces blocklisted sites (see [WebBlocklist]),
 * closing the web versions of apps that are blocked or disabled.
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
            if (intent.action == Intent.ACTION_USER_PRESENT) {
                Pickups.record(context)
            }
        }
    }

    /**
     * Content-changed events arrive in bursts while a page loads, so one blocked site
     * would fire the bounce and toast several times before Chrome leaves the foreground.
     * After a bounce, skip re-checks for a beat.
     */
    private var lastWebBounceMs = 0L

    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        registerReceiver(unlockReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        try {
            unregisterReceiver(unlockReceiver)
        } catch (e: IllegalArgumentException) {
            // Never registered (service killed before onServiceConnected) — fine.
        }
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        when (event?.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val pkg = event.packageName?.toString() ?: return
                Schedule.restrictionFor(pkg)?.let { restriction ->
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    Toast.makeText(this, restriction.message, Toast.LENGTH_SHORT).show()
                }
            }

            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (event.packageName?.toString() != CHROME) return
                if (SystemClock.elapsedRealtime() - lastWebBounceMs < WEB_BOUNCE_COOLDOWN_MS) return
                val urlBar = rootInActiveWindow
                    ?.findAccessibilityNodeInfosByViewId(URL_BAR_ID)
                    ?.firstOrNull() ?: return
                // A focused omnibox holds whatever is being typed — a draft, not a
                // visited page. Only judge settled URLs.
                if (urlBar.isFocused) return
                val host = WebBlocklist.hostOf(urlBar.text?.toString() ?: return) ?: return
                WebBlocklist.restrictionFor(host)?.let { message ->
                    lastWebBounceMs = SystemClock.elapsedRealtime()
                    // HOME alone would leave Chrome parked on the blocked tab, and
                    // every later visit to Chrome would bounce again — the browser
                    // becomes unusable until the tab dies. BACK first gets Chrome off
                    // the page (a link-opened tab closes outright); HOME follows after
                    // a beat so it can't race ahead of the back press.
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    handler.postDelayed(
                        { performGlobalAction(GLOBAL_ACTION_HOME) },
                        WEB_BOUNCE_HOME_DELAY_MS,
                    )
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onInterrupt() {}

    private companion object {
        const val CHROME = "com.android.chrome"
        const val URL_BAR_ID = "com.android.chrome:id/url_bar"
        const val WEB_BOUNCE_COOLDOWN_MS = 2_000L
        const val WEB_BOUNCE_HOME_DELAY_MS = 250L
    }
}
