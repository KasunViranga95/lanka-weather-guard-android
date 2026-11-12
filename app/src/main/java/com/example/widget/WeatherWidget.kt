package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.R
import com.example.data.OfflineCache
import com.example.data.SriLankaWeatherRepository
import com.example.data.UserPreferencesStore
import com.example.model.FloodStatus
import com.example.util.LocaleHelper
import com.example.util.WeatherFormat
import com.example.work.WeatherCheckWorker
import java.util.Locale
import kotlin.math.roundToInt

/** What the widget shows, read from the data saved on the phone (the widget never downloads itself). */
private data class WidgetData(
    val city: String,
    val temp: String,
    val emoji: String,
    val condition: String,
    val highLow: String,
    val outlook: String,
    val river: String?,
    val riverColor: Color?,
    val updated: String,
    val hasWeather: Boolean
)

class WeatherWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SMALL, WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = load(context)
        provideContent { GlanceTheme { Content(data) } }
    }

    private fun load(context: Context): WidgetData {
        UserPreferencesStore.init(context)
        OfflineCache.init(context)
        val res = LocaleHelper.wrap(context).resources
        val repository = SriLankaWeatherRepository()
        val prefs = UserPreferencesStore.loadPreferences()
        val region = prefs.regionId?.let { repository.regionById(it) } ?: repository.regions.first()
        val city = prefs.nearestCity?.takeIf { it.isNotBlank() } ?: region.name
        val report = repository.cachedReport(region, city)
        val stations = repository.cachedStations().orEmpty()
        val station = stations.takeIf { it.isNotEmpty() }?.let { repository.findPrimaryStationForRegion(region, city, it) }
            ?.takeIf { it.hasLiveReading }

        val river = station?.let {
            res.getString(
                R.string.widget_river_line,
                it.riverName,
                res.getString(it.floodStatus.labelRes),
                String.format(Locale.US, "%.2f", it.currentLevelM)
            )
        }
        val riverColor = station?.floodStatus?.takeIf { it != FloodStatus.NORMAL }?.color
        if (report == null) {
            return WidgetData(city, "--°", "☁️", "", "", res.getString(R.string.widget_no_data), river, riverColor, "", false)
        }
        val today = report.daily.firstOrNull()
        return WidgetData(
            city = city,
            temp = "${report.currentTempC.roundToInt()}°",
            emoji = WeatherFormat.conditionEmoji(report.weatherCode, report.isDay),
            condition = res.getString(WeatherFormat.conditionRes(report.weatherCode)),
            highLow = today?.let { res.getString(R.string.widget_high_low, it.maxTempC.roundToInt(), it.minTempC.roundToInt()) }.orEmpty(),
            outlook = WeatherFormat.rainOutlook(res, report),
            river = river,
            riverColor = riverColor,
            updated = res.getString(R.string.updated_at, WeatherFormat.time(res, report.fetchedAtMillis)),
            hasWeather = true
        )
    }

    companion object {
        private val SMALL = DpSize(120.dp, 110.dp)
        private val WIDE = DpSize(250.dp, 110.dp)

        suspend fun isPlaced(context: Context): Boolean =
            GlanceAppWidgetManager(context).getGlanceIds(WeatherWidget::class.java).isNotEmpty()

        suspend fun updateAll(context: Context) = WeatherWidget().updateAll(context)
    }
}

@Composable
private fun Content(data: WidgetData) {
    val wide = LocalSize.current.width >= 250.dp
    val primary = GlanceTheme.colors.onSurface
    val secondary = GlanceTheme.colors.onSurfaceVariant
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(GlanceTheme.colors.widgetBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = data.city,
                style = TextStyle(color = primary, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight()
            )
            if (wide && data.updated.isNotEmpty()) {
                Text(text = data.updated, style = TextStyle(color = secondary, fontSize = 11.sp), maxLines = 1)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = data.temp, style = TextStyle(color = primary, fontSize = 40.sp))
            Spacer(GlanceModifier.width(8.dp))
            Text(text = data.emoji, style = TextStyle(fontSize = 28.sp))
            if (wide && data.hasWeather) {
                Spacer(GlanceModifier.width(12.dp))
                Column {
                    Text(text = data.condition, style = TextStyle(color = primary, fontSize = 14.sp), maxLines = 1)
                    Text(text = data.highLow, style = TextStyle(color = secondary, fontSize = 12.sp), maxLines = 1)
                }
            }
        }
        Text(text = data.outlook, style = TextStyle(color = secondary, fontSize = 12.sp), maxLines = if (wide) 1 else 2)
        if (wide && data.river != null) {
            Spacer(GlanceModifier.height(2.dp))
            Text(
                text = data.river,
                style = TextStyle(
                    color = data.riverColor?.let { ColorProvider(it) } ?: secondary,
                    fontSize = 12.sp,
                    fontWeight = if (data.riverColor != null) FontWeight.Bold else FontWeight.Normal
                ),
                maxLines = 1
            )
        }
    }
}

class WeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeatherWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Fill the new widget with fresh data straight away
        WeatherCheckWorker.runNow(context)
    }
}
