package com.example

import android.app.Application
import com.example.data.OfflineCache
import com.example.data.UserPreferencesStore
import com.example.util.AlertNotifier
import com.example.work.WeatherCheckWorker

class LankaWeatherApp : Application() {
    override fun onCreate() {
        super.onCreate()
        UserPreferencesStore.init(this)
        OfflineCache.init(this)
        AlertNotifier.createChannel(this)
        WeatherCheckWorker.schedule(this)
    }
}
