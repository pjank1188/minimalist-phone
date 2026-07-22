package com.pjank.minimalistphone

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate

/**
 * Daily unlock counter and pickup heat. [WorkHoursService] calls [record] on every
 * ACTION_USER_PRESENT (keyguard dismissed); the home screen shows [today]'s count as a
 * quiet reminder of how often the phone gets picked up, and [Friction] turns the decaying
 * heat of recent pickups into an unlock toll. No history, no streaks — just today's
 * number (which resets implicitly because entries are keyed by date) and one heat value.
 */
object Pickups {

    private const val PREFS = "pickups"
    private const val KEY_LAST_UNLOCK = "last_unlock_ms"
    private const val KEY_HEAT = "heat"

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Increment today's count and add a point of pickup heat (decaying what was left
     * from earlier pickups first). Stale days are dropped so the file never grows.
     */
    fun record(context: Context) {
        val key = LocalDate.now().toString()
        val p = prefs(context)
        val nowMs = System.currentTimeMillis()
        val heat = Friction.decayedHeat(
            p.getFloat(KEY_HEAT, 0f).toDouble(),
            nowMs - p.getLong(KEY_LAST_UNLOCK, nowMs),
        ) + 1.0
        val editor = p.edit()
        p.all.keys
            .filter { it != key && it != KEY_LAST_UNLOCK && it != KEY_HEAT }
            .forEach { editor.remove(it) }
        editor.putInt(key, p.getInt(key, 0) + 1)
        editor.putLong(KEY_LAST_UNLOCK, nowMs)
        editor.putFloat(KEY_HEAT, heat.toFloat())
        editor.apply()
    }

    /** Pickup heat as of right now (decayed since the last recorded unlock). */
    fun currentHeat(context: Context): Double {
        val p = prefs(context)
        return Friction.decayedHeat(
            p.getFloat(KEY_HEAT, 0f).toDouble(),
            System.currentTimeMillis() - p.getLong(KEY_LAST_UNLOCK, System.currentTimeMillis()),
        )
    }

    /** Wall-clock time of the most recent recorded unlock, or 0 if none yet. */
    fun lastUnlockMs(context: Context): Long =
        prefs(context).getLong(KEY_LAST_UNLOCK, 0L)

    /** How many times the phone has been unlocked today. */
    fun today(context: Context): Int =
        prefs(context).getInt(LocalDate.now().toString(), 0)
}
