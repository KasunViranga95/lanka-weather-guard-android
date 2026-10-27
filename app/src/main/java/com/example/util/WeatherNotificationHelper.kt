package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.AlertSeverity
import com.example.model.DispatchedNotificationRecord
import com.example.model.GovernmentAnnouncement
import com.example.model.NotificationIntervalOption
import com.example.model.RegionalWeatherReport
import com.example.model.RiverGaugeStation
import com.example.model.SriLankaRegion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object WeatherNotificationHelper {

    const val EXTRA_FROM_NOTIFICATION = "extra_from_notification"
    const val EXTRA_NOTIFICATION_REGION_ID = "extra_notification_region_id"
    const val EXTRA_NOTIFICATION_CITY = "extra_notification_city"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

    private const val CHANNEL_ID = "lanka_weather_alerts_v2"
    private const val CHANNEL_NAME = "Sri Lanka Live Weather, Forecast & Gov Alerts"
    private var notificationSequence = 2000

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Scheduled real-time temperature, precipitation, current weather, forecast, river flood levels, and official Sri Lanka government announcements."
                enableVibration(true)
                enableLights(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun buildAndDispatchAreaNotification(
        context: Context,
        region: SriLankaRegion,
        nearestCity: String,
        primaryStation: RiverGaugeStation,
        report: RegionalWeatherReport,
        isFromGps: Boolean,
        intervalOption: NotificationIntervalOption = NotificationIntervalOption.MIN_15,
        govAnnouncement: GovernmentAnnouncement? = null
    ): DispatchedNotificationRecord {
        createNotificationChannel(context)

        val rain24hMm = maxOf(report.rain24hMm, primaryStation.catchmentRainMm24h)
        val liveHourlyPrecipMm = report.precipitationMm
        val liveTempC = report.currentTempC
        val feelsLikeC = report.feelsLikeC

        val severity = when {
            primaryStation.floodStatus.severityRank >= 4 -> AlertSeverity.CRITICAL_RED
            primaryStation.floodStatus.severityRank == 3 || rain24hMm >= 100.0 -> AlertSeverity.MAJOR_AMBER
            primaryStation.floodStatus.severityRank == 2 || rain24hMm >= 45.0 -> AlertSeverity.ADVISORY_YELLOW
            else -> AlertSeverity.SAFE_GREEN
        }

        val tempStr = String.format(Locale.US, "%.1f°C", liveTempC)
        val precipRateStr = String.format(Locale.US, "%.1f mm/h", liveHourlyPrecipMm)
        val rain24hStr = String.format(Locale.US, "%.0f mm", rain24hMm)

        // Current Weather Summary
        val currentWeatherSummary = buildString {
            append("${report.weatherDescription} • Temp $tempStr (Feels ${feelsLikeC.roundToInt()}°C) • ")
            append("Precip $precipRateStr (24h: $rain24hStr) • Gusts ${report.windGustKmh.roundToInt()} km/h • ")
            append("River ${primaryStation.riverName} (${primaryStation.stationName}): ${String.format(Locale.US, "%.2fm", primaryStation.currentLevelM)} [${primaryStation.floodStatus.shortLabel}]")
        }

        // Weather Forecast Summary (Next 6h & Tomorrow Outlook)
        val peakNextHourly = report.hourly.maxByOrNull { it.rainMm }
        val tomorrowForecast = report.daily.getOrElse(1) { report.daily.firstOrNull() }
        val forecastSummary = buildString {
            if (peakNextHourly != null) {
                append("Next Hours Peak: ${String.format(Locale.US, "%.1f mm/h", peakNextHourly.rainMm)} (${peakNextHourly.rainProbability}% rain at ${peakNextHourly.timeLabel}, ${peakNextHourly.tempC.roundToInt()}°C)")
            }
            if (tomorrowForecast != null) {
                if (isNotEmpty()) append(" | ")
                append("Tomorrow (${tomorrowForecast.dayLabel}): ${tomorrowForecast.weatherSummary}, ")
                append("${tomorrowForecast.minTempC.roundToInt()}°C–${tomorrowForecast.maxTempC.roundToInt()}°C, ")
                append("Est. Rain ${tomorrowForecast.totalRainMm.roundToInt()} mm (${tomorrowForecast.maxRainProb}%)")
            }
        }

        // Official Government Announcement Summary
        val govHeadline = govAnnouncement?.headline
            ?: "DMC & Met Dept Directive for $nearestCity (${region.name}): Monitor ${primaryStation.riverName} flood levels & landslide slopes."
        val govRef = govAnnouncement?.referenceCode ?: "DMC/SL/${region.name.take(3).uppercase(Locale.US)}/2025"
        val govPortalUrl = govAnnouncement?.officialUrl ?: "https://www.dmc.gov.lk"
        val govAgency = govAnnouncement?.issuingMinistryOrAgency ?: "Disaster Management Centre (DMC 117)"

        val title = "🇱🇰 $nearestCity (${region.name}) • 🌡 $tempStr • 🌧 $precipRateStr"
        val body = buildString {
            appendLine("• CURRENT: $currentWeatherSummary")
            appendLine("• FORECAST: $forecastSummary")
            append("• GOV OFFICIAL [$govRef]: $govHeadline ($govAgency • $govPortalUrl)")
        }

        val timestamp = SimpleDateFormat("HH:mm:ss 'SLST'", Locale.US).format(Date())
        val record = DispatchedNotificationRecord(
            id = "notif_${System.currentTimeMillis()}",
            title = title,
            body = body,
            district = region.name,
            nearestCity = nearestCity,
            riverBasin = "${primaryStation.riverName} (${primaryStation.basinCode})",
            severity = severity,
            timestampText = timestamp,
            sourceMethod = if (isFromGps) "GPS Auto-Detected" else "Selected District & Nearest City",
            intervalLabel = intervalOption.label,
            officialPortalUrl = govPortalUrl,
            temperatureC = liveTempC,
            feelsLikeC = feelsLikeC,
            hourlyPrecipitationMm = liveHourlyPrecipMm,
            rain24hMm = rain24hMm,
            currentWeatherSummary = currentWeatherSummary,
            forecastSummary = forecastSummary,
            govAnnouncementHeadline = govHeadline,
            govReferenceCode = govRef
        )

        postRecordToDeviceNotifications(
            context = context,
            record = record,
            regionId = region.id
        )

        return record
    }

    /**
     * Posts (or re-posts when selected by the user) a generated notification record directly to the
     * Android device notification shade. Clicking the notification in the system drawer automatically
     * navigates back to MainActivity and focuses the selected location.
     */
    fun postRecordToDeviceNotifications(
        context: Context,
        record: DispatchedNotificationRecord,
        regionId: String = record.district.lowercase(Locale.US).replace(" ", "_")
    ): Boolean {
        createNotificationChannel(context)

        val canPost = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (!canPost) return false

        return try {
            val tapIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_FROM_NOTIFICATION, true)
                putExtra(EXTRA_NOTIFICATION_REGION_ID, regionId)
                putExtra(EXTRA_NOTIFICATION_CITY, record.nearestCity)
                putExtra(EXTRA_NOTIFICATION_ID, record.id)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                record.id.hashCode(),
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val govWebIntent = Intent(Intent.ACTION_VIEW, Uri.parse(record.officialPortalUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val govPendingIntent = PendingIntent.getActivity(
                context,
                (record.id + "_gov").hashCode(),
                govWebIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val tempStr = String.format(Locale.US, "%.1f°C", record.temperatureC)
            val precipRateStr = String.format(Locale.US, "%.1f mm/h", record.hourlyPrecipitationMm)

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setSubText("${record.intervalLabel} • Temp $tempStr • Precip $precipRateStr")
                .setContentTitle(record.title)
                .setContentText("Tap to open Lanka Weather Guard • ${record.currentWeatherSummary}")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .setBigContentTitle(record.title)
                        .bigText(record.body)
                        .setSummaryText("Tap notification to open app • Gov Ref: ${record.govReferenceCode}")
                )
                .addAction(
                    android.R.drawable.ic_menu_compass,
                    "Open App",
                    pendingIntent
                )
                .addAction(
                    android.R.drawable.ic_menu_view,
                    "Official Gov News",
                    govPendingIntent
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationSequence = (notificationSequence + 1) % 9000 + 1000
            NotificationManagerCompat.from(context)
                .notify(notificationSequence, notification)
            true
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }
}
