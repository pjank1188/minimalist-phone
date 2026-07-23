package com.pjank.minimalistphone

import android.content.Context
import android.provider.Settings
import android.widget.Toast

/**
 * Sunburn: when pickup heat (see [Friction]) spikes into doom-loop territory the whole
 * display goes grayscale, and it stays gray until the heat has decayed well back down —
 * friction that follows you into the apps instead of stopping at the home screen.
 * Thresholds have hysteresis so the screen doesn't flicker at the boundary: gray at
 * [ON_HEAT], color again only below [OFF_HEAT] (roughly 15 minutes of leaving the
 * phone alone).
 *
 * Uses the accessibility color-correction (daltonizer) secure settings, which need
 * WRITE_SECURE_SETTINGS — granted once over adb (see DEVICE-SETUP.md). Ungranted, this
 * whole feature is a silent no-op, and Settings > Accessibility can always override
 * manually: this object only flips the toggle when crossing its own thresholds.
 */
object Sunburn {

    /** Heat at which the screen goes gray — a burst of ~7 rapid pickups. */
    const val ON_HEAT = 6.0

    /** Heat below which color returns: one half-life (~15 min untouched) after [ON_HEAT]. */
    const val OFF_HEAT = 3.0

    private const val PREFS = "sunburn"
    private const val KEY_ACTIVE = "active"

    private const val DALTONIZER_ENABLED = "accessibility_display_daltonizer_enabled"
    private const val DALTONIZER_MODE = "accessibility_display_daltonizer"
    private const val MODE_MONOCHROMACY = 0

    /** Pure threshold logic: whether sunburn should be on at [heat], given it [isActive]. */
    fun shouldBeActive(heat: Double, isActive: Boolean): Boolean =
        if (isActive) heat > OFF_HEAT else heat >= ON_HEAT

    /**
     * Re-evaluate against current heat and flip grayscale on a threshold crossing.
     * Called on every unlock and every foreground-app change, so color returns while
     * the phone is in use, not just at the next pickup.
     */
    fun sync(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val wasActive = prefs.getBoolean(KEY_ACTIVE, false)
        val nowActive = shouldBeActive(Pickups.currentHeat(context), wasActive)
        if (nowActive == wasActive) return
        try {
            val resolver = context.contentResolver
            if (nowActive) {
                Settings.Secure.putInt(resolver, DALTONIZER_MODE, MODE_MONOCHROMACY)
                Settings.Secure.putInt(resolver, DALTONIZER_ENABLED, 1)
                Toast.makeText(context, "sunburn — gray until you cool off", Toast.LENGTH_LONG).show()
            } else {
                Settings.Secure.putInt(resolver, DALTONIZER_ENABLED, 0)
            }
            prefs.edit().putBoolean(KEY_ACTIVE, nowActive).apply()
        } catch (e: SecurityException) {
            // WRITE_SECURE_SETTINGS not granted — sunburn silently disabled.
        }
    }
}
