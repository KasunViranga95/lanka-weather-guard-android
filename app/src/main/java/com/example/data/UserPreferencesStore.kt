package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.SriLankaRegion

data class SavedUserPreferences(
    val regionId: String?,
    val nearestCity: String?,
    val isFromGps: Boolean,
    val isDarkTheme: Boolean,
    // The phone's position when the place came from "Use my location"; kept on the phone only
    val gpsLatitude: Double? = null,
    val gpsLongitude: Double? = null
) {
    val gpsLocation: Pair<Double, Double>?
        get() = if (isFromGps && gpsLatitude != null && gpsLongitude != null) gpsLatitude to gpsLongitude else null
}

/** A place the user follows: a town and the district it belongs to. */
data class SavedPlace(val regionId: String, val city: String) {
    val key: String get() = "$regionId|$city"
}

object UserPreferencesStore {

    private const val PREFS_NAME = "lanka_weather_guard_user_prefs"
    // The phone's GPS position lives in its own file, which backup_rules.xml and data_extraction_rules.xml
    // keep out of Google backups and device transfers: the privacy policy promises it stays on the phone
    const val DEVICE_LOCATION_PREFS_NAME = "lanka_weather_guard_device_location"
    private const val KEY_REGION_ID = "saved_region_id"
    private const val KEY_NEAREST_CITY = "saved_nearest_city"
    private const val KEY_IS_FROM_GPS = "saved_is_from_gps"
    private const val KEY_IS_DARK_THEME = "saved_is_dark_theme"
    private const val KEY_GPS_LAT = "saved_gps_lat"
    private const val KEY_GPS_LON = "saved_gps_lon"
    private const val KEY_PLACES = "saved_places"
    private const val KEY_ALERTS_ENABLED = "alerts_enabled"
    private const val KEY_NOTIFIED = "notified_alerts"
    private const val KEY_CHECKLIST = "checklist_done"

    const val MAX_PLACES = 6

    @Volatile
    private var appContext: Context? = null

    private var inMemoryFallback = SavedUserPreferences(
        regionId = null,
        nearestCity = null,
        isFromGps = false,
        isDarkTheme = true
    )

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun prefs(): SharedPreferences? {
        return appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun locationPrefs(): SharedPreferences? {
        val context = appContext ?: return null
        val location = context.getSharedPreferences(DEVICE_LOCATION_PREFS_NAME, Context.MODE_PRIVATE)
        // Version 5.3 kept the position in the main file, which is backed up: move it out once
        val main = prefs()
        if (main != null && main.contains(KEY_GPS_LAT)) {
            location.edit()
                .putString(KEY_GPS_LAT, main.getString(KEY_GPS_LAT, null))
                .putString(KEY_GPS_LON, main.getString(KEY_GPS_LON, null))
                .apply()
            main.edit().remove(KEY_GPS_LAT).remove(KEY_GPS_LON).apply()
        }
        return location
    }

    fun loadPreferences(): SavedUserPreferences {
        val sp = prefs() ?: return inMemoryFallback
        val location = locationPrefs()
        return SavedUserPreferences(
            regionId = sp.getString(KEY_REGION_ID, null),
            nearestCity = sp.getString(KEY_NEAREST_CITY, null),
            isFromGps = sp.getBoolean(KEY_IS_FROM_GPS, false),
            isDarkTheme = sp.getBoolean(KEY_IS_DARK_THEME, true),
            gpsLatitude = location?.getString(KEY_GPS_LAT, null)?.toDoubleOrNull(),
            gpsLongitude = location?.getString(KEY_GPS_LON, null)?.toDoubleOrNull()
        )
    }

    /** Makes [region]/[nearestCity] the place shown in the app and adds it to the saved places. */
    fun saveLocation(region: SriLankaRegion, nearestCity: String, isFromGps: Boolean, gpsLocation: Pair<Double, Double>? = null) {
        val gps = gpsLocation?.takeIf { isFromGps }
        inMemoryFallback = inMemoryFallback.copy(
            regionId = region.id,
            nearestCity = nearestCity,
            isFromGps = isFromGps,
            gpsLatitude = gps?.first,
            gpsLongitude = gps?.second
        )
        prefs()?.edit()?.apply {
            putString(KEY_REGION_ID, region.id)
            putString(KEY_NEAREST_CITY, nearestCity)
            putBoolean(KEY_IS_FROM_GPS, isFromGps)
            apply()
        }
        locationPrefs()?.edit()?.apply {
            if (gps != null) {
                putString(KEY_GPS_LAT, gps.first.toString())
                putString(KEY_GPS_LON, gps.second.toString())
            } else {
                remove(KEY_GPS_LAT)
                remove(KEY_GPS_LON)
            }
            apply()
        }
        addPlace(SavedPlace(region.id, nearestCity))
    }

    fun saveDarkTheme(isDarkTheme: Boolean) {
        inMemoryFallback = inMemoryFallback.copy(isDarkTheme = isDarkTheme)
        prefs()?.edit()?.putBoolean(KEY_IS_DARK_THEME, isDarkTheme)?.apply()
    }

    // ---- Saved places ----

    fun loadPlaces(): List<SavedPlace> {
        val raw = prefs()?.getString(KEY_PLACES, null)
        val places = raw?.split(";;")?.mapNotNull { entry ->
            val parts = entry.split("|", limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) SavedPlace(parts[0], parts[1]) else null
        }.orEmpty()
        if (places.isNotEmpty()) return places
        // First run after the update: start the list with the place already chosen
        val current = loadPreferences()
        return if (current.regionId != null && !current.nearestCity.isNullOrBlank()) {
            listOf(SavedPlace(current.regionId, current.nearestCity))
        } else {
            emptyList()
        }
    }

    private fun storePlaces(places: List<SavedPlace>) {
        prefs()?.edit()?.putString(KEY_PLACES, places.joinToString(";;") { it.key })?.apply()
    }

    fun addPlace(place: SavedPlace) {
        val places = loadPlaces()
        if (places.any { it.key.equals(place.key, ignoreCase = true) }) return
        storePlaces((places + place).takeLast(MAX_PLACES))
    }

    fun removePlace(place: SavedPlace) {
        storePlaces(loadPlaces().filterNot { it.key.equals(place.key, ignoreCase = true) })
    }

    // ---- Alerts ----

    fun alertsEnabled(): Boolean = prefs()?.getBoolean(KEY_ALERTS_ENABLED, true) ?: true

    fun setAlertsEnabled(enabled: Boolean) {
        prefs()?.edit()?.putBoolean(KEY_ALERTS_ENABLED, enabled)?.apply()
    }

    /** Alerts already sent, as "placeKey|alertId" → severity priority, so each one is sent only once. */
    fun loadNotifiedAlerts(): Map<String, Int> =
        prefs()?.getStringSet(KEY_NOTIFIED, emptySet()).orEmpty().mapNotNull { entry ->
            val key = entry.substringBeforeLast("=", "")
            val priority = entry.substringAfterLast("=", "").toIntOrNull()
            if (key.isNotEmpty() && priority != null) key to priority else null
        }.toMap()

    fun saveNotifiedAlerts(notified: Map<String, Int>) {
        prefs()?.edit()?.putStringSet(KEY_NOTIFIED, notified.map { (k, v) -> "$k=$v" }.toSet())?.apply()
    }

    // ---- Flood readiness checklist ----

    fun loadChecklist(): Set<String> = prefs()?.getStringSet(KEY_CHECKLIST, emptySet())?.toSet().orEmpty()

    fun saveChecklist(done: Set<String>) {
        prefs()?.edit()?.putStringSet(KEY_CHECKLIST, done)?.apply()
    }
}
