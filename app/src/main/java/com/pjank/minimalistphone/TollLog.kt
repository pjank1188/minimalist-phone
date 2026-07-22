package com.pjank.minimalistphone

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate

/**
 * Daily friction-toll log: total seconds the home screen was held behind the dead
 * overlay, keyed by date. Unlike [Pickups] this keeps [HISTORY_DAYS] of history, so the
 * number can be watched trending down (`adb shell run-as com.pjank.minimalistphone cat
 * shared_prefs/toll_log.xml` dumps it all). Today's total also shows on the home screen
 * next to the pickup count.
 */
object TollLog {

    private const val PREFS = "toll_log"
    private const val HISTORY_DAYS = 30L

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Add [seconds] to today's toll. Entries older than [HISTORY_DAYS] are dropped. */
    fun record(context: Context, seconds: Int) {
        val today = LocalDate.now()
        val key = today.toString()
        val cutoff = today.minusDays(HISTORY_DAYS).toString()
        val p = prefs(context)
        val editor = p.edit()
        p.all.keys.filter { it < cutoff }.forEach { editor.remove(it) }
        editor.putInt(key, p.getInt(key, 0) + seconds)
        editor.apply()
    }

    /** Total toll seconds charged today. */
    fun today(context: Context): Int =
        prefs(context).getInt(LocalDate.now().toString(), 0)

    /** "42s" under a minute, "3m 10s" past it. */
    fun format(seconds: Int): String =
        if (seconds < 60) "${seconds}s" else "${seconds / 60}m ${seconds % 60}s"
}
