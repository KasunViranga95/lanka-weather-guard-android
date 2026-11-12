package com.example.util

import android.content.res.Resources
import androidx.annotation.StringRes
import com.example.R
import com.example.model.RegionalWeatherReport
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Text for weather values, shared by the app screens, the widget and notifications. */
object WeatherFormat {

    @StringRes
    fun conditionRes(code: Int): Int = when (code) {
        0 -> R.string.cond_clear
        1 -> R.string.cond_mostly_clear
        2 -> R.string.cond_partly_cloudy
        3 -> R.string.cond_cloudy
        45, 48 -> R.string.cond_fog
        in 51..57 -> R.string.cond_drizzle
        61, 66 -> R.string.cond_light_rain
        63 -> R.string.cond_rain
        65, 67 -> R.string.cond_heavy_rain
        80 -> R.string.cond_light_showers
        81 -> R.string.cond_showers
        82 -> R.string.cond_heavy_showers
        95 -> R.string.cond_thunderstorms
        96, 99 -> R.string.cond_thunderstorms_hail
        else -> R.string.cond_mixed
    }

    fun conditionEmoji(code: Int, isDay: Boolean = true): String = when (code) {
        0, 1 -> if (isDay) "☀️" else "🌙"
        2 -> if (isDay) "⛅" else "☁️"
        3 -> "☁️"
        45, 48 -> "🌫️"
        in 51..67, in 80..82 -> "🌧️"
        in 95..99 -> "⛈️"
        else -> "☁️"
    }

    /** "15:00" -> "3 PM", "06:04" -> "6:04 AM" (in the app language). */
    fun hour(res: Resources, hhmm: String): String {
        val h = hhmm.substringBefore(":").toIntOrNull() ?: return hhmm
        val m = hhmm.substringAfter(":", "00").take(2)
        val h12 = if (h % 12 == 0) 12 else h % 12
        val clock = if (m == "00") "$h12" else "$h12:$m"
        return res.getString(if (h < 12) R.string.time_am else R.string.time_pm, clock)
    }

    /** Clock time for a moment, e.g. "4:07 PM". */
    fun time(res: Resources, millis: Long): String =
        hour(res, SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone("Asia/Colombo") }.format(millis))

    /** "Today" for the first day, otherwise the short weekday name in the app language. */
    fun dayName(res: Resources, index: Int, isoDate: String): String {
        if (index == 0) return res.getString(R.string.today)
        val locale = res.configuration.locales[0]
        return runCatching {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(isoDate)!!
            SimpleDateFormat("EEE", locale).format(date)
        }.getOrElse { isoDate }
    }

    fun compassPoint(res: Resources, deg: Int): String =
        res.getStringArray(R.array.compass_points)[(((deg % 360) + 382.5) / 45).toInt() % 8]

    @StringRes
    fun uvLevelRes(uv: Double): Int = when {
        uv < 3 -> R.string.uv_low
        uv < 6 -> R.string.uv_moderate
        uv < 8 -> R.string.uv_high
        uv < 11 -> R.string.uv_very_high
        else -> R.string.uv_extreme
    }

    /** One sentence about rain for the next 12 hours, from the hourly forecast. */
    fun rainOutlook(res: Resources, report: RegionalWeatherReport): String {
        val now = report.hourly.firstOrNull()
        if (now != null && now.rainProbability >= 60 && report.weatherCode >= 51) return res.getString(R.string.outlook_raining_now)
        val firstWet = report.hourly.drop(1).take(12).firstOrNull { it.rainProbability >= 60 }
        if (firstWet != null) return res.getString(R.string.outlook_rain_from, hour(res, firstWet.timeLabel))
        val chance = report.daily.firstOrNull()?.maxRainProb ?: 0
        return if (chance < 30) res.getString(R.string.outlook_dry) else res.getString(R.string.outlook_showers_possible, chance)
    }
}
