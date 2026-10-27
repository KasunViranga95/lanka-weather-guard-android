package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dehaze
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FloodStatus
import com.example.model.HourlyForecastItem
import com.example.model.RegionalWeatherReport
import com.example.model.RiverGaugeStation
import com.example.model.WaterTrend
import com.example.ui.theme.AdaptiveBorderColor
import com.example.ui.theme.AdaptiveCyanAccent
import com.example.ui.theme.AdaptiveElevatedColor
import com.example.ui.theme.AdaptivePrimaryText
import com.example.ui.theme.AdaptiveSecondaryText
import com.example.ui.theme.AdaptiveSurfaceColor
import com.example.ui.theme.AlertRed
import com.example.ui.theme.LocalIsDarkTheme
import java.util.Locale
import kotlin.math.roundToInt

/** Plain-language icon for a WMO weather code. */
fun weatherIconFor(code: Int): ImageVector = when (code) {
    0, 1 -> Icons.Filled.WbSunny
    2 -> Icons.Filled.WbCloudy
    3 -> Icons.Filled.Cloud
    45, 48 -> Icons.Filled.Dehaze
    in 51..57 -> Icons.Filled.Grain
    in 61..67, in 80..82 -> Icons.Filled.Umbrella
    in 95..99 -> Icons.Filled.Thunderstorm
    else -> Icons.Filled.Cloud
}

/** Short, friendly condition text for a WMO weather code. */
fun weatherLabelFor(code: Int): String = when (code) {
    0 -> "Clear sky"
    1 -> "Mostly sunny"
    2 -> "Partly cloudy"
    3 -> "Cloudy"
    45, 48 -> "Foggy"
    in 51..55 -> "Drizzle"
    56, 57 -> "Freezing drizzle"
    61 -> "Light rain"
    63 -> "Rain"
    65 -> "Heavy rain"
    66, 67 -> "Freezing rain"
    80 -> "Light showers"
    81 -> "Showers"
    82 -> "Violent showers"
    95 -> "Thunderstorms"
    96, 99 -> "Thunderstorms with hail"
    else -> "Mixed conditions"
}

/** Plain-language river state shown to people (instead of technical gauge terms). */
fun FloodStatus.friendlyLabel(): String = when (this) {
    FloodStatus.NORMAL -> "Normal"
    FloodStatus.ALERT -> "Watch – near alert level"
    FloodStatus.MINOR_FLOOD -> "Minor flooding"
    FloodStatus.MAJOR_FLOOD -> "Major flooding"
}

private fun formatTemp(value: Double) = "${value.roundToInt()}°"

/**
 * Big, glanceable current-conditions card. While the first live sync is still running (or there
 * has never been a live sync) temperatures are shown as "--" instead of placeholder numbers.
 */
@Composable
fun CurrentConditionsHero(
    report: RegionalWeatherReport,
    cityName: String,
    districtName: String,
    isSyncing: Boolean,
    onChangeArea: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val hasLiveData = report.isLiveApiData
    val isRainy = report.weatherCode >= 51
    val gradient = when {
        isRainy && isDark -> listOf(Color(0xFF1B2A41), Color(0xFF2C4A6B))
        isRainy -> listOf(Color(0xFF3F5F82), Color(0xFF6E8FB3))
        isDark -> listOf(Color(0xFF0F3460), Color(0xFF16697A))
        else -> listOf(Color(0xFF1877F2), Color(0xFF34A1D9))
    }
    val today = report.daily.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(gradient))
            .padding(20.dp)
            .testTag("home_current_conditions"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Right now",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White.copy(alpha = 0.85f),
            fontWeight = FontWeight.SemiBold
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (hasLiveData) formatTemp(report.currentTempC) else "--°",
                color = Color.White,
                fontSize = 76.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 80.sp
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Icon(
                    imageVector = weatherIconFor(report.weatherCode),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = if (hasLiveData) weatherLabelFor(report.weatherCode) else "Loading…",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (hasLiveData) {
            Text(
                text = buildString {
                    append("Feels like ${formatTemp(report.feelsLikeC)}")
                    if (today != null) append("  ·  H ${formatTemp(today.maxTempC)}  L ${formatTemp(today.minTempC)}")
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.92f)
            )
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = when {
                isSyncing && !hasLiveData -> "Getting the latest weather for $cityName…"
                !hasLiveData -> "No connection – pull the refresh button to try again"
                else -> "Updated ${report.syncedTimestamp.substringAfterLast("• ").removeSuffix(" SLST")} · " +
                    if (report.syncedTimestamp.contains("model blend")) "blend of 4 weather models" else "Open-Meteo"
            },
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.75f)
        )
    }
}

/** Four simple stat tiles: rain today, chance of rain, wind and humidity. */
@Composable
fun QuickStatsRow(report: RegionalWeatherReport, modifier: Modifier = Modifier) {
    val live = report.isLiveApiData
    val today = report.daily.firstOrNull()
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatTile(
            icon = Icons.Filled.Umbrella,
            label = "Rain today",
            value = if (live) "${report.rain24hMm.roundToInt()} mm" else "--",
            highlight = live && report.rain24hMm >= 50.0,
            modifier = Modifier.weight(1f)
        )
        StatTile(
            icon = Icons.Filled.Grain,
            label = "Rain chance",
            value = if (live && today != null) "${today.maxRainProb}%" else "--",
            modifier = Modifier.weight(1f)
        )
        StatTile(
            icon = Icons.Filled.Air,
            label = "Wind",
            value = if (live) "${report.windSpeedKmh.roundToInt()} km/h" else "--",
            highlight = live && report.windGustKmh >= 50.0,
            modifier = Modifier.weight(1f)
        )
        StatTile(
            icon = Icons.Filled.WaterDrop,
            label = "Humidity",
            value = if (live) "${report.humidityPercent}%" else "--",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    val accent = if (highlight) AlertRed else AdaptiveCyanAccent
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(AdaptiveSurfaceColor)
            .border(1.dp, if (highlight) AlertRed.copy(alpha = 0.5f) else AdaptiveBorderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = if (highlight) AlertRed else AdaptivePrimaryText,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AdaptiveSecondaryText,
            maxLines = 1
        )
    }
}

/** Next 12 hours: time, icon, temperature and chance of rain. */
@Composable
fun HourlyForecastStrip(hourly: List<HourlyForecastItem>, isLive: Boolean, modifier: Modifier = Modifier) {
    SectionCard(title = "Next 12 hours", modifier = modifier) {
        if (!isLive || hourly.isEmpty()) {
            Text(
                text = "Hourly forecast will appear once the latest weather loads.",
                style = MaterialTheme.typography.bodyMedium,
                color = AdaptiveSecondaryText
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(hourly) { hour ->
                    Column(
                        modifier = Modifier
                            .width(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(AdaptiveElevatedColor)
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(hour.timeLabel, style = MaterialTheme.typography.labelSmall, color = AdaptiveSecondaryText)
                        Icon(
                            imageVector = weatherIconFor(hour.weatherCode),
                            contentDescription = weatherLabelFor(hour.weatherCode),
                            tint = AdaptiveCyanAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = formatTemp(hour.tempC),
                            style = MaterialTheme.typography.titleSmall,
                            color = AdaptivePrimaryText,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${hour.rainProbability}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hour.rainProbability >= 60) AdaptiveCyanAccent else AdaptiveSecondaryText
                        )
                    }
                }
            }
        }
    }
}

/** The river gauge that matters most for the user's area, in plain language. */
@Composable
fun NearestRiverCard(
    station: RiverGaugeStation?,
    floodingStationCount: Int,
    onOpenRivers: () -> Unit,
    modifier: Modifier = Modifier
) {
    SectionCard(
        title = "Your nearest river",
        actionLabel = "All rivers",
        onAction = onOpenRivers,
        modifier = modifier
    ) {
        if (station == null) {
            Text("Loading river levels…", style = MaterialTheme.typography.bodyMedium, color = AdaptiveSecondaryText)
            return@SectionCard
        }
        val status = station.floodStatus
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${station.riverName} at ${station.stationName.substringBefore(" (")}",
                    style = MaterialTheme.typography.titleSmall,
                    color = AdaptivePrimaryText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = String.format(Locale.US, "%.2f m", station.currentLevelM),
                        style = MaterialTheme.typography.headlineSmall,
                        color = AdaptivePrimaryText,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = when (station.trend) {
                            WaterTrend.RISING_FAST, WaterTrend.RISING -> Icons.AutoMirrored.Filled.TrendingUp
                            WaterTrend.FALLING -> Icons.AutoMirrored.Filled.TrendingDown
                            WaterTrend.STABLE -> Icons.AutoMirrored.Filled.TrendingFlat
                        },
                        contentDescription = station.trend.label,
                        tint = AdaptiveSecondaryText,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(station.trend.label, style = MaterialTheme.typography.bodySmall, color = AdaptiveSecondaryText)
                }
            }
            StatusPill(text = status.friendlyLabel(), color = status.color)
        }
        LinearProgressIndicator(
            progress = { (station.currentLevelM / station.majorFloodLevelM).toFloat().coerceIn(0f, 1f) },
            color = status.color,
            trackColor = AdaptiveElevatedColor,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
        )
        Text(
            text = String.format(
                Locale.US,
                "Alert at %.1f m · Major flood at %.1f m",
                station.alertLevelM,
                station.majorFloodLevelM
            ),
            style = MaterialTheme.typography.labelSmall,
            color = AdaptiveSecondaryText
        )
        if (floodingStationCount > 0) {
            Text(
                text = "$floodingStationCount river gauge${if (floodingStationCount == 1) " is" else "s are"} at flood level across Sri Lanka",
                style = MaterialTheme.typography.bodySmall,
                color = AlertRed,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** Three large, clearly labelled actions. */
@Composable
fun HomeQuickActions(
    onChangeArea: () -> Unit,
    onNotifyMe: () -> Unit,
    onCallEmergency: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        QuickActionButton(Icons.Filled.EditLocationAlt, "Change area", AdaptiveCyanAccent, onChangeArea, Modifier.weight(1f))
        QuickActionButton(Icons.Filled.NotificationsActive, "Alert me now", AdaptiveCyanAccent, onNotifyMe, Modifier.weight(1f))
        QuickActionButton(Icons.Filled.Phone, "Call 117", AlertRed, onCallEmergency, Modifier.weight(1f))
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(AdaptiveSurfaceColor)
            .border(1.dp, AdaptiveBorderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = AdaptivePrimaryText, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

/** Consistent rounded card with a title row used across the home screen. */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AdaptiveSurfaceColor)
            .border(1.dp, AdaptiveBorderColor, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = AdaptivePrimaryText, fontWeight = FontWeight.Bold)
            if (actionLabel != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onAction() }
                        .padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(actionLabel, style = MaterialTheme.typography.labelLarge, color = AdaptiveCyanAccent, fontWeight = FontWeight.SemiBold)
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = AdaptiveCyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        content()
    }
}
