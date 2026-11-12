package com.example.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.AlertEngine
import com.example.data.OfflineCache
import com.example.data.SriLankaWeatherRepository
import com.example.data.UserPreferencesStore
import com.example.util.AlertNotifier
import com.example.util.LocaleHelper
import com.example.widget.WeatherWidget
import java.util.concurrent.TimeUnit

/**
 * Runs about every 30 minutes, even when the app is closed: downloads the forecast for each saved place
 * and the latest river readings, saves them for offline use, refreshes the widget, and notifies about
 * alerts that are new or have become more serious.
 */
class WeatherCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext
        UserPreferencesStore.init(app)
        OfflineCache.init(app)

        val alertsOn = UserPreferencesStore.alertsEnabled()
        if (!alertsOn && !WeatherWidget.isPlaced(app)) return Result.success()

        val repository = SriLankaWeatherRepository()
        val places = UserPreferencesStore.loadPlaces()
        if (places.isEmpty()) return Result.success()

        val firstRegion = repository.regionById(places.first().regionId) ?: repository.regions.first()
        val stations = repository.fetchLiveRiverStations(firstRegion)
        val localized = LocaleHelper.wrap(app)
        var anyLive = stations.any { it.hasLiveReading }

        for (place in places) {
            val region = repository.regionById(place.regionId) ?: continue
            val report = repository.fetchRegionalReport(region, place.city)
            if (!report.isLiveApiData || report.isFromCache) continue
            anyLive = true
            if (!alertsOn) continue
            val near = repository.findPrimaryStationForRegion(region, place.city, stations)
            val alerts = AlertEngine.build(localized.resources, region, place.city, report, stations, near)
                .filter { it.isUserDistrictMatch }
            AlertNotifier.notifyNew(localized, place, alerts)
        }

        WeatherWidget.updateAll(app)
        return if (anyLive) Result.success() else Result.retry()
    }

    companion object {
        private const val PERIODIC_NAME = "weather-check"
        private const val NOW_NAME = "weather-check-now"

        private val networkConstraint = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /** Schedules the regular check; safe to call on every app start. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WeatherCheckWorker>(30, TimeUnit.MINUTES, 10, TimeUnit.MINUTES)
                .setConstraints(networkConstraint)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** Runs one check right away (e.g. after the widget is added or a place changes). */
        fun runNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<WeatherCheckWorker>()
                .setConstraints(networkConstraint)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(NOW_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
