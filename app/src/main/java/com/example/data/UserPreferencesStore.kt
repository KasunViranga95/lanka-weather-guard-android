package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.NotificationIntervalOption
import com.example.model.SriLankaRegion

data class SavedUserPreferences(
    val regionId: String?,
    val nearestCity: String?,
    val intervalId: String?,
    val isFromGps: Boolean,
    val isDarkTheme: Boolean
)

object UserPreferencesStore {

    private const val PREFS_NAME = "lanka_weather_guard_user_prefs"
    private const val KEY_REGION_ID = "saved_region_id"
    private const val KEY_NEAREST_CITY = "saved_nearest_city"
    private const val KEY_INTERVAL_ID = "saved_notification_interval_id"
    private const val KEY_IS_FROM_GPS = "saved_is_from_gps"
    private const val KEY_IS_DARK_THEME = "saved_is_dark_theme"

    @Volatile
    private var appContext: Context? = null

    private var inMemoryFallback = SavedUserPreferences(
        regionId = null,
        nearestCity = null,
        intervalId = null,
        isFromGps = false,
        isDarkTheme = true
    )

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun prefs(): SharedPreferences? {
        return appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun loadPreferences(): SavedUserPreferences {
        val sp = prefs() ?: return inMemoryFallback
        return SavedUserPreferences(
            regionId = sp.getString(KEY_REGION_ID, null),
            nearestCity = sp.getString(KEY_NEAREST_CITY, null),
            intervalId = sp.getString(KEY_INTERVAL_ID, null),
            isFromGps = sp.getBoolean(KEY_IS_FROM_GPS, false),
            isDarkTheme = sp.getBoolean(KEY_IS_DARK_THEME, true)
        )
    }

    fun saveLocation(
        region: SriLankaRegion,
        nearestCity: String,
        isFromGps: Boolean,
        intervalOption: NotificationIntervalOption? = null
    ) {
        inMemoryFallback = inMemoryFallback.copy(
            regionId = region.id,
            nearestCity = nearestCity,
            isFromGps = isFromGps,
            intervalId = intervalOption?.id ?: inMemoryFallback.intervalId
        )
        prefs()?.edit()?.apply {
            putString(KEY_REGION_ID, region.id)
            putString(KEY_NEAREST_CITY, nearestCity)
            putBoolean(KEY_IS_FROM_GPS, isFromGps)
            if (intervalOption != null) {
                putString(KEY_INTERVAL_ID, intervalOption.id)
            }
            apply()
        }
    }

    fun saveNotificationInterval(intervalOption: NotificationIntervalOption) {
        inMemoryFallback = inMemoryFallback.copy(intervalId = intervalOption.id)
        prefs()?.edit()?.putString(KEY_INTERVAL_ID, intervalOption.id)?.apply()
    }

    fun saveDarkTheme(isDarkTheme: Boolean) {
        inMemoryFallback = inMemoryFallback.copy(isDarkTheme = isDarkTheme)
        prefs()?.edit()?.putBoolean(KEY_IS_DARK_THEME, isDarkTheme)?.apply()
    }
}
