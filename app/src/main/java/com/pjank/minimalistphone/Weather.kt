package com.pjank.minimalistphone

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

/** Current conditions: temperature in °F and a short lowercase description. */
data class WeatherInfo(val tempF: Int, val condition: String)

/**
 * Fetches current weather from Open-Meteo — free, no API key, no account. Blocking call;
 * run it off the main thread.
 */
object Weather {

    fun fetch(lat: Double, lon: Double): WeatherInfo? = try {
        val url = URL(
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,weather_code" +
                "&temperature_unit=fahrenheit"
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
        }
        val body = try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
        val current = JSONObject(body).getJSONObject("current")
        WeatherInfo(
            tempF = current.getDouble("temperature_2m").roundToInt(),
            condition = conditionFor(current.getInt("weather_code")),
        )
    } catch (e: Exception) {
        null
    }

    /** Maps WMO weather codes to a short word. */
    private fun conditionFor(code: Int): String = when (code) {
        0 -> "clear"
        1, 2 -> "partly cloudy"
        3 -> "cloudy"
        45, 48 -> "fog"
        in 51..57 -> "drizzle"
        66, 67 -> "freezing rain"
        in 61..65 -> "rain"
        in 71..77 -> "snow"
        in 80..82 -> "showers"
        in 85..86 -> "snow"
        95, 96, 99 -> "thunderstorm"
        else -> ""
    }
}
