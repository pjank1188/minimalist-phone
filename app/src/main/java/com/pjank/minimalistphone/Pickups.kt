package com.pjank.minimalistphone

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate

/**
 * Daily unlock counter. [GrayscaleService] calls [record] on every ACTION_USER_PRESENT
 * (keyguard dismissed); the home screen shows [today]'s count as a quiet reminder of how
 * often the phone gets picked up. No history, no streaks — just today's number, which
 * resets implicitly because entries are keyed by date.
 */
object Pickups {

    private const val PREFS = "pickups"

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Increment today's count. Stale days are dropped so the file never grows. */
    fun record(context: Context) {
        val key = LocalDate.now().toString()
        val p = prefs(context)
        val editor = p.edit()
        p.all.keys.filter { it != key }.forEach { editor.remove(it) }
        editor.putInt(key, p.getInt(key, 0) + 1)
        editor.apply()
    }

    /** How many times the phone has been unlocked today. */
    fun today(context: Context): Int =
        prefs(context).getInt(LocalDate.now().toString(), 0)
}
