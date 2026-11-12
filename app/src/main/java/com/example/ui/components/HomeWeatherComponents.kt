package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Dehaze
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AlertSeverity
import com.example.model.DailyForecastItem
import com.example.model.FloodStatus
import com.example.model.HourlyForecastItem
import com.example.model.RegionalWeatherReport
import com.example.model.RiverGaugeStation
import com.example.model.WaterTrend
import com.example.model.WeatherAlert
import com.example.ui.theme.AdaptiveBorderColor
import com.example.ui.theme.AdaptiveCyanAccent
import com.example.ui.theme.AdaptiveElevatedColor
import com.example.ui.theme.AdaptivePrimaryText
import com.example.ui.theme.AdaptiveSecondaryText
import com.example.ui.theme.AdaptiveSurfaceColor
import com.example.ui.theme.AlertRed
import com.example.util.WeatherFormat
import java.util.Locale
import kotlin.math.roundToInt

/** Plain-language icon for a WMO weather code; clear skies at night show a moon. */
fun weatherIconFor(code: Int, isDay: Boolean = true): ImageVector = when (code) {
    0, 1 -> if (isDay) Icons.Filled.WbSunny else Icons.Filled.NightsStay
    2 -> Icons.Filled.WbCloudy
    3 -> Icons.Filled.Cloud
    45, 48 -> Icons.Filled.Dehaze
    in 51..57 -> Icons.Filled.Grain
    in 61..67, in 80..82 -> Icons.Filled.Umbrella
    in 95..99 -> Icons.Filled.Thunderstorm
    else -> Icons.Filled.Cloud
}

/** Colour for a condition icon: yellow sun, grey cloud, blue rain, purple storm. */
fun weatherTintFor(code: Int, isDay: Boolean = true): Color = when (code) {
    0, 1 -> if (isDay) Color(0xFFF9AB00) else Color(0xFF8C9EFF)
    2 -> if (isDay) Color(0xFFF9AB00) else Color(0xFF9AA0A6)
    3, 45, 48 -> Color(0xFF9AA0A6)
    in 51..67, in 80..82 -> Color(0xFF4285F4)
    in 95..99 -> Color(0xFF8E6CEF)
    else -> Color(0xFF9AA0A6)
}

private val RainBlue = Color(0xFF4285F4)

private fun formatTemp(value: Double) = "${value.roundToInt()}°"

@Composable
private fun res() = LocalContext.current.resources

/**
 * Top of the Weather screen: big temperature, condition, high/low and a one-line rain outlook.
 * Sits directly on the page background (no card), like most modern weather apps.
 */
@Composable
fun CurrentConditionsHeader(
    report: RegionalWeatherReport,
    cityName: String,
    isSyncing: Boolean,
    modifier: Modifier = Modifier
) {
    val hasLiveData = report.isLiveApiData
    val today = report.daily.firstOrNull()
    val res = res()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .testTag("home_current_conditions"),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (report.isFromCache && hasLiveData) {
            OfflineChip(report.fetchedAtMillis)
            Spacer(Modifier.height(6.dp))
        }
        Text(stringResource(R.string.now), style = MaterialTheme.typography.titleSmall, color = AdaptiveSecondaryText)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (hasLiveData) formatTemp(report.currentTempC) else "--°",
                color = AdaptivePrimaryText,
                fontSize = 84.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 88.sp
            )
            Spacer(Modifier.width(12.dp))
            Icon(
                imageVector = weatherIconFor(report.weatherCode, report.isDay),
                contentDescription = null,
                tint = weatherTintFor(report.weatherCode, report.isDay),
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.weight(1f))
            if (hasLiveData) {
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = stringResource(WeatherFormat.conditionRes(report.weatherCode)),
                        style = MaterialTheme.typography.titleMedium,
                        color = AdaptivePrimaryText,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = stringResource(R.string.feels_like, formatTemp(report.feelsLikeC)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AdaptiveSecondaryText,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
        if (hasLiveData) {
            if (today != null) {
                Text(
                    text = stringResource(R.string.high_low, formatTemp(today.maxTempC), formatTemp(today.minTempC)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AdaptivePrimaryText
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(WeatherFormat.rainOutlook(res, report), style = MaterialTheme.typography.bodyMedium, color = AdaptiveSecondaryText)
        } else {
            Text(
                text = if (isSyncing) stringResource(R.string.loading_weather_for, cityName) else stringResource(R.string.no_connection_pull),
                style = MaterialTheme.typography.bodyMedium,
                color = AdaptiveSecondaryText
            )
        }
    }
}

/** Small "Offline · saved at 4:07 PM" label shown when the network failed and saved data is on screen. */
@Composable
private fun OfflineChip(savedAtMillis: Long) {
    val res = res()
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(AdaptiveElevatedColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Filled.CloudOff, contentDescription = null, tint = AdaptiveSecondaryText, modifier = Modifier.size(14.dp))
        Text(
            text = stringResource(R.string.offline_saved_at, WeatherFormat.time(res, savedAtMillis)),
            style = MaterialTheme.typography.labelMedium,
            color = AdaptiveSecondaryText
        )
    }
}

/**
 * Compact banner for active alerts. Shows the most relevant one; tapping opens all alerts in a sheet.
 * Renders nothing when there are no alerts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertBanner(
    alerts: List<WeatherAlert>,
    alertsEnabled: Boolean,
    onAlertsEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (alerts.isEmpty()) return
    val top = alerts.first()
    val color = top.severity.badgeColor
    var showSheet by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.14f))
            .clickable { showSheet = true }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("home_alerts_card"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = top.title,
                style = MaterialTheme.typography.bodyLarge,
                color = AdaptivePrimaryText,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    append(stringResource(top.severity.shortLabelRes()))
                    append(" · ").append(top.district)
                    if (alerts.size > 1) append(" · ").append(stringResource(R.string.n_more, alerts.size - 1))
                },
                style = MaterialTheme.typography.bodySmall,
                color = AdaptiveSecondaryText
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.see_all_alerts), tint = AdaptiveSecondaryText)
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = AdaptiveSurfaceColor
        ) {
            AlertsSheetContent(alerts, alertsEnabled, onAlertsEnabledChange)
        }
    }
}

@Composable
private fun AlertsSheetContent(
    alerts: List<WeatherAlert>,
    alertsEnabled: Boolean,
    onAlertsEnabledChange: (Boolean) -> Unit
) {
    var openId by remember { mutableStateOf(alerts.firstOrNull()?.id) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.weather_alerts_count, alerts.size),
            style = MaterialTheme.typography.titleLarge,
            color = AdaptivePrimaryText,
            fontWeight = FontWeight.SemiBold
        )
        alerts.forEachIndexed { idx, alert ->
            if (idx > 0) HorizontalDivider(color = AdaptiveBorderColor)
            val open = openId == alert.id
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { openId = if (open) null else alert.id }
                    .padding(vertical = 6.dp)
                    .testTag("home_alert_${alert.id}"),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(alert.severity.badgeColor)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = alert.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = AdaptivePrimaryText,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = if (open) Int.MAX_VALUE else 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${stringResource(alert.severity.shortLabelRes())} · ${alert.district}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AdaptiveSecondaryText
                        )
                    }
                    Icon(
                        imageVector = if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = stringResource(if (open) R.string.hide_details else R.string.show_details),
                        tint = AdaptiveSecondaryText
                    )
                }
                if (open) {
                    Text(alert.summary, style = MaterialTheme.typography.bodyMedium, color = AdaptivePrimaryText)
                    alert.recommendedActions.forEach { action ->
                        Text("• $action", style = MaterialTheme.typography.bodyMedium, color = AdaptiveSecondaryText)
                    }
                    Text(
                        text = listOf(alert.issuingAgency, alert.issuedTimeText).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = AdaptiveSecondaryText
                    )
                }
            }
        }
        HorizontalDivider(color = AdaptiveBorderColor)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = AdaptiveCyanAccent)
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.alert_notifications), style = MaterialTheme.typography.bodyLarge, color = AdaptivePrimaryText)
                Text(stringResource(R.string.alert_notifications_summary), style = MaterialTheme.typography.bodySmall, color = AdaptiveSecondaryText)
            }
            Switch(checked = alertsEnabled, onCheckedChange = onAlertsEnabledChange)
        }
        Text(
            text = stringResource(R.string.alerts_disclaimer),
            style = MaterialTheme.typography.labelSmall,
            color = AdaptiveSecondaryText
        )
    }
}

fun AlertSeverity.shortLabelRes(): Int = when (this) {
    AlertSeverity.CRITICAL_RED -> R.string.severity_severe
    AlertSeverity.MAJOR_AMBER -> R.string.severity_warning
    AlertSeverity.ADVISORY_YELLOW -> R.string.severity_advisory
    AlertSeverity.SAFE_GREEN -> R.string.severity_info
}

/** Hourly row: temperature, icon, chance of rain and time for the next 24 hours. */
@Composable
fun HourlyForecastStrip(hourly: List<HourlyForecastItem>, isLive: Boolean, modifier: Modifier = Modifier) {
    val res = res()
    SectionCard(title = stringResource(R.string.hourly_forecast), modifier = modifier.testTag("home_hourly_card")) {
        if (!isLive || hourly.isEmpty()) {
            Text(
                text = stringResource(R.string.hourly_will_appear),
                style = MaterialTheme.typography.bodyMedium,
                color = AdaptiveSecondaryText
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                itemsIndexed(hourly) { idx, hour ->
                    Column(
                        modifier = Modifier.width(60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = formatTemp(hour.tempC), style = MaterialTheme.typography.titleMedium, color = AdaptivePrimaryText)
                        Icon(
                            imageVector = weatherIconFor(hour.weatherCode, hour.isDay),
                            contentDescription = stringResource(WeatherFormat.conditionRes(hour.weatherCode)),
                            tint = weatherTintFor(hour.weatherCode, hour.isDay),
                            modifier = Modifier.size(28.dp)
                        )
                        RainChance(hour.rainProbability)
                        Text(
                            text = if (idx == 0) stringResource(R.string.now) else WeatherFormat.hour(res, hour.timeLabel),
                            style = MaterialTheme.typography.labelMedium,
                            color = AdaptiveSecondaryText,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/** Small blue "💧 40%" label; keeps its height when the chance is too low to show. */
@Composable
private fun RainChance(percent: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.heightIn(min = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (percent >= 10) {
            Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = RainBlue, modifier = Modifier.size(12.dp))
            Text("$percent%", style = MaterialTheme.typography.labelMedium, color = RainBlue)
        }
    }
}

/** One row per day: name, chance of rain, icon and high/low – the classic weather-app list. */
@Composable
fun DailyForecastCard(daily: List<DailyForecastItem>, modifier: Modifier = Modifier) {
    val res = res()
    SectionCard(title = stringResource(R.string.n_day_forecast, daily.size), modifier = modifier.testTag("home_daily_card")) {
        daily.forEachIndexed { idx, day ->
            if (idx > 0) HorizontalDivider(color = AdaptiveBorderColor)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .testTag("forecast_day_$idx"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = WeatherFormat.dayName(res, idx, day.dateLabel),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AdaptivePrimaryText,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                RainChance(day.maxRainProb, Modifier.width(56.dp))
                Icon(
                    imageVector = weatherIconFor(day.weatherCode),
                    contentDescription = stringResource(WeatherFormat.conditionRes(day.weatherCode)),
                    tint = weatherTintFor(day.weatherCode),
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "${formatTemp(day.maxTempC)} / ${formatTemp(day.minTempC)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AdaptivePrimaryText,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(88.dp)
                )
            }
        }
    }
}

/** 2×2 grid of today's details: wind, humidity, UV and sunrise/sunset. */
@Composable
fun ConditionsGrid(report: RegionalWeatherReport, modifier: Modifier = Modifier) {
    val res = res()
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.current_conditions),
            style = MaterialTheme.typography.titleMedium,
            color = AdaptivePrimaryText,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )
        val live = report.isLiveApiData
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ConditionTile(
                title = stringResource(R.string.wind),
                icon = Icons.Filled.Air,
                value = if (live) stringResource(R.string.speed_kmh, report.windSpeedKmh.roundToInt()) else "--",
                caption = if (live) {
                    report.windDirectionDeg
                        ?.let { stringResource(R.string.wind_from_gusts, WeatherFormat.compassPoint(res, it), report.windGustKmh.roundToInt()) }
                        ?: stringResource(R.string.gusts_n, report.windGustKmh.roundToInt())
                } else "",
                modifier = Modifier.weight(1f),
                trailing = report.windDirectionDeg?.takeIf { live }?.let { deg ->
                    {
                        // Arrow points the way the wind is blowing to
                        Icon(
                            Icons.Filled.Navigation,
                            contentDescription = null,
                            tint = AdaptiveCyanAccent,
                            modifier = Modifier
                                .size(22.dp)
                                .rotate(deg + 180f)
                        )
                    }
                }
            )
            ConditionTile(
                title = stringResource(R.string.humidity),
                icon = Icons.Filled.WaterDrop,
                value = if (live) "${report.humidityPercent}%" else "--",
                caption = when {
                    !live -> ""
                    report.humidityPercent >= 80 -> stringResource(R.string.humidity_very_humid)
                    report.humidityPercent >= 60 -> stringResource(R.string.humidity_humid)
                    else -> stringResource(R.string.humidity_comfortable)
                },
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val uv = report.uvIndex
            ConditionTile(
                title = stringResource(R.string.uv_index),
                icon = Icons.Filled.WbSunny,
                value = if (live && uv != null) uv.roundToInt().toString() else "--",
                caption = if (live && uv != null) stringResource(WeatherFormat.uvLevelRes(uv)) else "",
                modifier = Modifier.weight(1f),
                footer = if (live && uv != null) {
                    { UvBar(uv) }
                } else null
            )
            ConditionTile(
                title = stringResource(R.string.sunrise),
                icon = Icons.Filled.WbTwilight,
                value = report.sunrise?.let { WeatherFormat.hour(res, it) } ?: "--",
                caption = report.sunset?.let { stringResource(R.string.sunset_at, WeatherFormat.hour(res, it)) } ?: "",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ConditionTile(
    title: String,
    icon: ImageVector,
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(AdaptiveSurfaceColor)
            .heightIn(min = 120.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = AdaptiveSecondaryText, modifier = Modifier.size(16.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, color = AdaptiveSecondaryText, maxLines = 1)
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = AdaptivePrimaryText,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            trailing?.invoke()
        }
        if (caption.isNotEmpty()) {
            Text(caption, style = MaterialTheme.typography.bodySmall, color = AdaptiveSecondaryText, maxLines = 2)
        }
        footer?.invoke()
    }
}

/** Green-to-purple UV scale with a dot at today's value. */
@Composable
private fun UvBar(uv: Double) {
    val fraction = (uv / 12.0).toFloat().coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .height(6.dp)
            .clip(CircleShape)
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF34A853), Color(0xFFFBBC04), Color(0xFFFA7B17), Color(0xFFEA4335), Color(0xFFA142F4))
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
fun trendIcon(trend: WaterTrend): ImageVector = when (trend) {
    WaterTrend.RISING_FAST, WaterTrend.RISING -> Icons.AutoMirrored.Filled.TrendingUp
    WaterTrend.FALLING -> Icons.AutoMirrored.Filled.TrendingDown
    WaterTrend.STABLE -> Icons.AutoMirrored.Filled.TrendingFlat
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
        title = stringResource(R.string.river_near_you),
        actionLabel = stringResource(R.string.all_rivers),
        onAction = onOpenRivers,
        modifier = modifier
    ) {
        if (station == null) {
            Text(stringResource(R.string.loading_rivers), style = MaterialTheme.typography.bodyMedium, color = AdaptiveSecondaryText)
            return@SectionCard
        }
        Text(
            text = stringResource(R.string.river_at_station, station.riverName, station.stationName.substringBefore(" (")),
            style = MaterialTheme.typography.bodyLarge,
            color = AdaptivePrimaryText,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!station.hasLiveReading) {
            Text(stringResource(R.string.no_recent_reading), style = MaterialTheme.typography.bodyMedium, color = AdaptiveSecondaryText)
            return@SectionCard
        }
        val status = station.floodStatus
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = String.format(Locale.US, "%.2f m", station.currentLevelM),
                        style = MaterialTheme.typography.headlineSmall,
                        color = AdaptivePrimaryText
                    )
                    Icon(
                        imageVector = trendIcon(station.trend),
                        contentDescription = stringResource(station.trend.labelRes),
                        tint = AdaptiveSecondaryText,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(stringResource(station.trend.labelRes), style = MaterialTheme.typography.bodySmall, color = AdaptiveSecondaryText)
                }
                station.publishedLevelText?.let {
                    Text(stringResource(R.string.gauge_reads, it), style = MaterialTheme.typography.labelSmall, color = AdaptiveSecondaryText)
                }
            }
            StatusPill(text = stringResource(status.labelRes), color = status.color)
        }
        LinearProgressIndicator(
            progress = { (station.currentLevelM / station.majorFloodLevelM).toFloat().coerceIn(0f, 1f) },
            color = status.color,
            trackColor = AdaptiveElevatedColor,
            drawStopIndicator = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
        )
        Text(
            text = stringResource(
                R.string.alert_and_major_levels,
                String.format(Locale.US, "%.1f", station.alertLevelM),
                String.format(Locale.US, "%.1f", station.majorFloodLevelM)
            ),
            style = MaterialTheme.typography.bodySmall,
            color = AdaptiveSecondaryText
        )
        if (floodingStationCount > 0) {
            Text(
                text = res().getQuantityString(R.plurals.gauges_flooding, floodingStationCount, floodingStationCount),
                style = MaterialTheme.typography.bodySmall,
                color = AlertRed,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** Emergency action at the bottom of the Weather screen. */
@Composable
fun EmergencyCallButton(onCallEmergency: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onCallEmergency,
        border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("home_call_117")
    ) {
        Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.emergency_call_117), fontWeight = FontWeight.SemiBold)
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

/** Rounded, borderless card with a title row – the one card style used across the app. */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(AdaptiveSurfaceColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = AdaptivePrimaryText,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f, fill = false)
            )
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

/** Plain-language river state shown to people (instead of technical gauge terms). */
@Composable
fun FloodStatus.friendlyLabel(): String = stringResource(labelRes)
