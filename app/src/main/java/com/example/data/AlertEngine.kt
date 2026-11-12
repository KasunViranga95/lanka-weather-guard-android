package com.example.data

import android.content.res.Resources
import com.example.R
import com.example.model.AlertSeverity
import com.example.model.FloodStatus
import com.example.model.RegionalWeatherReport
import com.example.model.RiverGaugeStation
import com.example.model.SriLankaRegion
import com.example.model.WeatherAlert
import com.example.util.WeatherFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Works out alerts from live data only: Irrigation Department gauge readings against their published
 * flood levels, and the Open-Meteo forecast for the place against Met Department rain/wind wording and
 * NBRO landslide rainfall limits. Nothing here is pre-written.
 */
object AlertEngine {

    // Met Dept rain wording: "heavy" above 100 mm, "very heavy" above 150 mm; 75 mm is the advisory level
    private const val RAIN_ADVISORY_MM = 75.0
    private const val RAIN_HEAVY_MM = 100.0
    private const val RAIN_VERY_HEAVY_MM = 150.0

    // NBRO landslide early warning: 75 mm/24h watch, 100 mm alert, 150 mm evacuate
    private const val LANDSLIDE_WATCH_MM = 75.0
    private const val LANDSLIDE_ALERT_MM = 100.0
    private const val LANDSLIDE_EVACUATE_MM = 150.0

    private const val GUST_ADVISORY_KMH = 50.0
    private const val GUST_STRONG_KMH = 65.0
    private const val GUST_SEVERE_KMH = 80.0

    // Heat index bands used by the Met Dept: "extreme caution" from 41 °C, "danger" from 54 °C
    private const val HEAT_CAUTION_C = 41.0
    private const val HEAT_DANGER_C = 54.0

    fun build(
        res: Resources,
        region: SriLankaRegion,
        city: String,
        report: RegionalWeatherReport,
        stations: List<RiverGaugeStation>,
        nearStation: RiverGaugeStation?
    ): List<WeatherAlert> {
        val alerts = buildList {
            addAll(riverAlerts(res, region, stations, nearStation))
            if (report.isLiveApiData) addAll(weatherAlerts(res, region, city, report, nearStation))
        }
        return alerts.sortedWith(
            compareByDescending<WeatherAlert> { it.isUserDistrictMatch }.thenByDescending { it.severity.priority }
        )
    }

    private fun riverAlerts(
        res: Resources,
        region: SriLankaRegion,
        stations: List<RiverGaugeStation>,
        nearStation: RiverGaugeStation?
    ): List<WeatherAlert> =
        stations
            .filter { it.hasLiveReading && it.floodStatus != FloodStatus.NORMAL }
            .groupBy { it.riverName }
            .map { (river, group) ->
                val sorted = group.sortedByDescending { it.floodStatus.severityRank }
                val worst = sorted.first()
                val severity = when (worst.floodStatus) {
                    FloodStatus.MAJOR_FLOOD -> AlertSeverity.CRITICAL_RED
                    FloodStatus.MINOR_FLOOD -> AlertSeverity.MAJOR_AMBER
                    else -> AlertSeverity.ADVISORY_YELLOW
                }
                val title = when (worst.floodStatus) {
                    FloodStatus.MAJOR_FLOOD -> res.getString(R.string.alert_river_major_title, river)
                    FloodStatus.MINOR_FLOOD -> res.getString(R.string.alert_river_minor_title, river)
                    else -> res.getString(R.string.alert_river_watch_title, river)
                }
                val summary = sorted.joinToString("\n") { st ->
                    res.getString(
                        R.string.alert_river_reading_line,
                        st.stationName.substringBefore(" ("),
                        String.format(Locale.US, "%.2f", st.currentLevelM),
                        res.getString(st.floodStatus.labelRes),
                        res.getString(st.trend.labelRes)
                    )
                }
                val actions = when (worst.floodStatus) {
                    FloodStatus.MAJOR_FLOOD -> listOf(
                        res.getString(R.string.action_river_major_1),
                        res.getString(R.string.action_river_major_2),
                        res.getString(R.string.action_call_117)
                    )
                    FloodStatus.MINOR_FLOOD -> listOf(
                        res.getString(R.string.action_river_minor_1),
                        res.getString(R.string.action_river_minor_2),
                        res.getString(R.string.action_river_minor_3)
                    )
                    else -> listOf(
                        res.getString(R.string.action_river_watch_1),
                        res.getString(R.string.action_prepare_bag)
                    )
                }
                val isLocal = group.any { st ->
                    st.district.equals(region.name, ignoreCase = true) || st.id == nearStation?.id
                } || region.primaryRiverBasin.contains(river, ignoreCase = true)
                val latestReading = group.mapNotNull { it.readingTimeMillis }.maxOrNull()
                WeatherAlert(
                    id = "river_${river.lowercase(Locale.US).replace(' ', '_')}",
                    title = title,
                    issuingAgency = res.getString(R.string.alert_source_gauges),
                    district = worst.district,
                    riverBasin = river,
                    severity = severity,
                    summary = summary,
                    recommendedActions = actions,
                    issuedTimeText = latestReading?.let { res.getString(R.string.alert_reading_at, WeatherFormat.time(res, it)) }.orEmpty(),
                    validUntilText = "",
                    rainfallMm24h = group.maxOf { it.catchmentRainMm24h },
                    windGustKmh = 0.0,
                    isUserDistrictMatch = isLocal
                )
            }

    private fun weatherAlerts(
        res: Resources,
        region: SriLankaRegion,
        city: String,
        report: RegionalWeatherReport,
        nearStation: RiverGaugeStation?
    ): List<WeatherAlert> = buildList {
        val issued = res.getString(R.string.alert_forecast_at, WeatherFormat.time(res, report.fetchedAtMillis.takeIf { it > 0 } ?: System.currentTimeMillis()))
        val source = res.getString(R.string.alert_source_forecast)
        fun local(id: String, title: String, severity: AlertSeverity, summary: String, actions: List<String>) = WeatherAlert(
            id = id,
            title = title,
            issuingAgency = source,
            district = region.name,
            riverBasin = null,
            severity = severity,
            summary = summary,
            recommendedActions = actions,
            issuedTimeText = issued,
            validUntilText = "",
            rainfallMm24h = report.rain24hMm,
            windGustKmh = report.windGustKmh,
            isUserDistrictMatch = true
        )

        // Heavy rain: next 24 hours from the hourly forecast, otherwise tomorrow's total
        val next24Rain = report.hourly.take(24).sumOf { it.rainMm }
        val tomorrowRain = report.daily.getOrNull(1)?.totalRainMm ?: 0.0
        val (rainMm, rainWhen) = if (next24Rain >= RAIN_ADVISORY_MM || next24Rain >= tomorrowRain) {
            next24Rain to res.getString(R.string.when_next_24h)
        } else {
            tomorrowRain to res.getString(R.string.when_tomorrow)
        }
        if (rainMm >= RAIN_ADVISORY_MM) {
            val severity = when {
                rainMm >= RAIN_VERY_HEAVY_MM -> AlertSeverity.CRITICAL_RED
                rainMm >= RAIN_HEAVY_MM -> AlertSeverity.MAJOR_AMBER
                else -> AlertSeverity.ADVISORY_YELLOW
            }
            add(
                local(
                    id = "rain",
                    title = res.getString(if (rainMm >= RAIN_VERY_HEAVY_MM) R.string.alert_rain_very_heavy_title else R.string.alert_rain_heavy_title),
                    severity = severity,
                    summary = res.getString(R.string.alert_rain_summary, rainMm.roundToInt(), rainWhen, city),
                    actions = buildList {
                        add(res.getString(R.string.action_rain_1))
                        add(res.getString(R.string.action_rain_2))
                        if (severity == AlertSeverity.CRITICAL_RED) add(res.getString(R.string.action_rain_3))
                    }
                )
            )
        }

        // Strong wind: strongest gust in the next 24 hours
        val maxGust = (report.hourly.take(24).maxOfOrNull { it.windGustKmh } ?: 0.0).coerceAtLeast(report.windGustKmh)
        if (maxGust >= GUST_ADVISORY_KMH) {
            add(
                local(
                    id = "wind",
                    title = res.getString(R.string.alert_wind_title),
                    severity = when {
                        maxGust >= GUST_SEVERE_KMH -> AlertSeverity.CRITICAL_RED
                        maxGust >= GUST_STRONG_KMH -> AlertSeverity.MAJOR_AMBER
                        else -> AlertSeverity.ADVISORY_YELLOW
                    },
                    summary = res.getString(R.string.alert_wind_summary, maxGust.roundToInt()),
                    actions = buildList {
                        add(res.getString(R.string.action_wind_1))
                        add(res.getString(R.string.action_wind_2))
                        if (region.isCoastalZone) add(res.getString(R.string.action_wind_sea))
                    }
                )
            )
        }

        // Thunderstorms in the next 12 hours
        val stormIndex = report.hourly.take(12).indexOfFirst { it.weatherCode in 95..99 }
        if (stormIndex >= 0) {
            add(
                local(
                    id = "thunder",
                    title = res.getString(R.string.alert_thunder_title),
                    severity = AlertSeverity.ADVISORY_YELLOW,
                    summary = if (stormIndex == 0) {
                        res.getString(R.string.alert_thunder_now)
                    } else {
                        res.getString(R.string.alert_thunder_summary, WeatherFormat.hour(res, report.hourly[stormIndex].timeLabel))
                    },
                    actions = listOf(
                        res.getString(R.string.action_thunder_1),
                        res.getString(R.string.action_thunder_2),
                        res.getString(R.string.action_thunder_3)
                    )
                )
            )
        }

        // Landslides: hill districts only, using the wettest of forecast and measured 24-hour rain
        if (region.isLandslideProne) {
            val slopeRain = maxOf(next24Rain, report.rain24hMm, nearStation?.takeIf { it.hasLiveReading }?.catchmentRainMm24h ?: 0.0)
            if (slopeRain >= LANDSLIDE_WATCH_MM) {
                add(
                    local(
                        id = "landslide",
                        title = res.getString(
                            when {
                                slopeRain >= LANDSLIDE_EVACUATE_MM -> R.string.alert_landslide_very_high_title
                                slopeRain >= LANDSLIDE_ALERT_MM -> R.string.alert_landslide_high_title
                                else -> R.string.alert_landslide_title
                            }
                        ),
                        severity = when {
                            slopeRain >= LANDSLIDE_EVACUATE_MM -> AlertSeverity.CRITICAL_RED
                            slopeRain >= LANDSLIDE_ALERT_MM -> AlertSeverity.MAJOR_AMBER
                            else -> AlertSeverity.ADVISORY_YELLOW
                        },
                        summary = res.getString(R.string.alert_landslide_summary, slopeRain.roundToInt(), region.name),
                        actions = listOf(
                            res.getString(R.string.action_landslide_1),
                            res.getString(R.string.action_landslide_2),
                            res.getString(R.string.action_landslide_3)
                        )
                    ).copy(issuingAgency = res.getString(R.string.alert_source_nbro))
                )
            }
        }

        // Heat
        if (report.feelsLikeC >= HEAT_CAUTION_C) {
            add(
                local(
                    id = "heat",
                    title = res.getString(R.string.alert_heat_title),
                    severity = if (report.feelsLikeC >= HEAT_DANGER_C) AlertSeverity.MAJOR_AMBER else AlertSeverity.ADVISORY_YELLOW,
                    summary = res.getString(R.string.alert_heat_summary, report.feelsLikeC.roundToInt()),
                    actions = listOf(
                        res.getString(R.string.action_heat_1),
                        res.getString(R.string.action_heat_2),
                        res.getString(R.string.action_heat_3)
                    )
                )
            )
        }
    }
}
