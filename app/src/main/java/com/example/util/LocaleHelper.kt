package com.example.util

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * App language: English, Sinhala or Tamil, or "" to follow the phone. On Android 13+ the choice is kept
 * by the system (and also shows in the phone's per-app language settings); older versions save it here
 * and apply it to each screen, notification and widget.
 */
object LocaleHelper {

    val SUPPORTED = listOf("", "en", "si", "ta")

    private const val PREFS = "lanka_weather_guard_locale"
    private const val KEY = "language"

    fun current(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = context.getSystemService(LocaleManager::class.java)?.applicationLocales
            return if (locales == null || locales.isEmpty) "" else locales[0].language
        }
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "") ?: ""
    }

    fun set(activity: Activity, tag: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.getSystemService(LocaleManager::class.java)?.applicationLocales =
                if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
            return
        }
        activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, tag).commit()
        activity.recreate()
    }

    /** A context whose resources use the chosen language (the system already does this on Android 13+). */
    fun wrap(context: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return context
        val tag = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "") ?: ""
        if (tag.isEmpty()) return context
        val locale = Locale.forLanguageTag(tag)
        val config = Configuration(context.resources.configuration).apply { setLocale(locale) }
        return context.createConfigurationContext(config)
    }
}
