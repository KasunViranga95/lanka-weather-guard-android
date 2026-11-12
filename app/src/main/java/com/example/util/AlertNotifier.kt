package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.SavedPlace
import com.example.data.UserPreferencesStore
import com.example.model.AlertSeverity
import com.example.model.WeatherAlert

/**
 * Sends a notification only when something new matters for a saved place: a new alert, or an existing
 * one getting more serious. Alerts that clear are forgotten, so they notify again if they come back.
 */
object AlertNotifier {

    const val EXTRA_FROM_NOTIFICATION = "extra_from_notification"
    const val EXTRA_NOTIFICATION_REGION_ID = "extra_notification_region_id"
    const val EXTRA_NOTIFICATION_CITY = "extra_notification_city"

    private const val CHANNEL_ID = "weather_flood_alerts"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                enableVibration(true)
            }
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
            // Channel from earlier versions that sent a notification on every refresh
            context.getSystemService(NotificationManager::class.java)?.deleteNotificationChannel("lanka_weather_alerts_v2")
        }
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /**
     * Posts the alerts for [place] that are new or more severe than last time; returns how many were sent.
     * [context] should already use the app language (see [LocaleHelper.wrap]).
     */
    fun notifyNew(context: Context, place: SavedPlace, alerts: List<WeatherAlert>): Int {
        val previous = UserPreferencesStore.loadNotifiedAlerts()
        val current = alerts
            .filter { it.severity != AlertSeverity.SAFE_GREEN }
            .associateBy { "${place.key}|${it.id}" }

        val toSend = current.filter { (key, alert) -> (previous[key] ?: 0) < alert.severity.priority }
        // Keep other places' entries; replace this place's with what is active now
        val updated = previous.filterKeys { !it.startsWith("${place.key}|") } +
            current.mapValues { (_, alert) -> alert.severity.priority }
        UserPreferencesStore.saveNotifiedAlerts(updated)

        if (toSend.isEmpty() || !canPost(context)) return 0
        createChannel(context)
        toSend.forEach { (key, alert) -> post(context, place, key, alert) }
        return toSend.size
    }

    private fun post(context: Context, place: SavedPlace, key: String, alert: WeatherAlert) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_FROM_NOTIFICATION, true)
            putExtra(EXTRA_NOTIFICATION_REGION_ID, place.regionId)
            putExtra(EXTRA_NOTIFICATION_CITY, place.city)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            key.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val body = buildString {
            append(alert.summary)
            alert.recommendedActions.firstOrNull()?.let { append("\n\n").append(it) }
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_alert)
            .setColor(alert.severity.badgeColor.let { android.graphics.Color.argb((it.alpha * 255).toInt(), (it.red * 255).toInt(), (it.green * 255).toInt(), (it.blue * 255).toInt()) })
            .setContentTitle(alert.title)
            .setContentText(alert.summary.lineSequence().first())
            .setSubText(place.city)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(
                if (alert.severity == AlertSeverity.CRITICAL_RED) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH
            )
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(key.hashCode(), notification)
        } catch (_: SecurityException) {
            // Permission was revoked between the check and the call
        }
    }
}
