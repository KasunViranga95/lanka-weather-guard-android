package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ActivitySafetyAssessment
import com.example.model.DailyForecastItem
import com.example.model.HourlyForecastItem
import com.example.model.RegionalWeatherReport
import com.example.model.SriLankaRegion
import com.example.ui.theme.AdaptiveBorderColor
import com.example.ui.theme.AdaptiveCyanAccent
import com.example.ui.theme.AdaptiveElevatedColor
import com.example.ui.theme.AdaptivePrimaryText
import com.example.ui.theme.AdaptiveSecondaryText
import com.example.ui.theme.AdaptiveSurfaceColor
import com.example.ui.theme.AdaptiveTealAccent
import com.example.ui.theme.AlertRed
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.WarningAmber
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun RegionalForecastScreen(
    regions: List<SriLankaRegion>,
    selectedRegion: SriLankaRegion,
    report: RegionalWeatherReport,
    onRegionSelected: (SriLankaRegion) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val bannerGradient = if (isDark) {
        listOf(Color(0xFF0E294A), Color(0xFF0A1D35))
    } else {
        listOf(Color(0xFFDDF1FC), Color(0xFFCDE7F7))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("regional_forecast_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header & Region Selector
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CompassCalibration,
                        contentDescription = null,
                        tint = AdaptiveCyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "REGIONAL FORECAST & ACTIVITY PLANNER",
                        style = MaterialTheme.typography.labelLarge,
                        color = AdaptiveCyanAccent
                    )
                }
                Text(
                    text = "${selectedRegion.name} • ${selectedRegion.province}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AdaptivePrimaryText
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("forecast_region_selector")
                ) {
                    items(regions, key = { it.id }) { region ->
                        val isSelected = region.id == selectedRegion.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { onRegionSelected(region) },
                            label = { Text(region.name) },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RadarCyan.copy(alpha = 0.22f),
                                selectedLabelColor = AdaptiveCyanAccent,
                                selectedLeadingIconColor = AdaptiveCyanAccent
                            ),
                            modifier = Modifier.testTag("forecast_region_${region.id}")
                        )
                    }
                }
            }
        }

        // 2. Current Regional Atmospheric Telemetry Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AdaptiveBorderColor, RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier
                        .background(Brush.linearGradient(colors = bannerGradient))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp)
                        ) {
                            Text(
                                text = report.weatherDescription,
                                style = MaterialTheme.typography.titleLarge,
                                color = AdaptivePrimaryText,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Basin: ${selectedRegion.primaryRiverBasin} • Elev: ${selectedRegion.elevationMeters}m",
                                style = MaterialTheme.typography.bodySmall,
                                color = AdaptiveCyanAccent
                            )
                            Text(
                                text = "Live Precip: ${String.format(Locale.US, "%.1f mm/h", report.precipitationMm)} • Feels Like ${report.feelsLikeC.roundToInt()}°C",
                                style = MaterialTheme.typography.labelMedium,
                                color = AdaptiveTealAccent
                            )
                        }
                        Text(
                            text = String.format(Locale.US, "%.1f°C", report.currentTempC),
                            style = MaterialTheme.typography.headlineLarge,
                            color = AdaptivePrimaryText,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ForecastStatItem(
                            icon = Icons.Default.Umbrella,
                            label = "24h Rain",
                            value = "${report.rain24hMm.roundToInt()} mm"
                        )
                        ForecastStatItem(
                            icon = Icons.Default.Air,
                            label = "Max Gust",
                            value = "${report.windGustKmh.roundToInt()} km/h"
                        )
                        ForecastStatItem(
                            icon = Icons.Default.WaterDrop,
                            label = "Humidity",
                            value = "${report.humidityPercent}%"
                        )
                        ForecastStatItem(
                            icon = Icons.Default.Speed,
                            label = "Pressure",
                            value = "${report.pressureHpa.roundToInt()} hPa"
                        )
                    }
                }
            }
        }

        // 3. Citizen Activity Safety Planner
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = AdaptiveTealAccent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "ACTIVITY SAFETY PLANNER (${selectedRegion.name.uppercase()})",
                    style = MaterialTheme.typography.labelLarge,
                    color = AdaptivePrimaryText
                )
            }
        }

        items(report.activityAssessments, key = { it.activityName }) { assessment ->
            ActivitySafetyCard(assessment = assessment)
        }

        // 4. 24-Hour Precipitation & Wind Timeline
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AdaptiveSurfaceColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AdaptiveBorderColor, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = AdaptiveCyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "HOURLY MONSOON INTENSITY OUTLOOK",
                            style = MaterialTheme.typography.labelLarge,
                            color = AdaptiveCyanAccent
                        )
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(report.hourly, key = { it.timeLabel }) { hour ->
                            HourlyForecastPill(hour = hour)
                        }
                    }
                }
            }
        }

        // 5. 7-Day Regional Monsoon Forecast
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = AdaptiveCyanAccent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "7-DAY REGIONAL MONSOON OUTLOOK",
                    style = MaterialTheme.typography.labelLarge,
                    color = AdaptivePrimaryText
                )
            }
        }

        items(report.daily, key = { it.dayLabel }) { dayItem ->
            DailyForecastRowCard(day = dayItem)
        }
    }
}

@Composable
private fun ForecastStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AdaptiveCyanAccent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = AdaptivePrimaryText,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = AdaptiveSecondaryText
        )
    }
}

@Composable
private fun ActivitySafetyCard(assessment: ActivitySafetyAssessment) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AdaptiveSurfaceColor),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AdaptiveBorderColor, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = assessment.category.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = AdaptiveCyanAccent
                    )
                    Text(
                        text = assessment.activityName,
                        style = MaterialTheme.typography.titleMedium,
                        color = AdaptivePrimaryText
                    )
                }

                Surface(
                    color = assessment.statusColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${assessment.statusLabel} (${assessment.safetyScore}/100)",
                        style = MaterialTheme.typography.labelMedium,
                        color = assessment.statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { assessment.safetyScore / 100f },
                color = assessment.statusColor,
                trackColor = AdaptiveElevatedColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            Text(
                text = assessment.recommendation,
                style = MaterialTheme.typography.bodySmall,
                color = AdaptiveSecondaryText
            )
        }
    }
}

@Composable
private fun HourlyForecastPill(hour: HourlyForecastItem) {
    val rainColor = when {
        hour.rainMm >= 12.0 -> AlertRed
        hour.rainMm >= 5.0 -> WarningAmber
        else -> AdaptiveCyanAccent
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .width(88.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AdaptiveElevatedColor)
            .border(1.dp, rainColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp)
    ) {
        Text(
            text = hour.timeLabel,
            style = MaterialTheme.typography.labelMedium,
            color = AdaptiveSecondaryText
        )
        Icon(
            imageVector = Icons.Default.CloudQueue,
            contentDescription = null,
            tint = rainColor,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = "${hour.tempC.roundToInt()}°C",
            style = MaterialTheme.typography.titleMedium,
            color = AdaptivePrimaryText,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${String.format(Locale.US, "%.1f", hour.rainMm)} mm",
            style = MaterialTheme.typography.labelMedium,
            color = rainColor,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${hour.rainProbability}% • ${hour.windGustKmh.roundToInt()}km/h",
            style = MaterialTheme.typography.bodySmall,
            color = AdaptiveSecondaryText
        )
    }
}

@Composable
private fun DailyForecastRowCard(day: DailyForecastItem) {
    val badgeColor = day.riskLevel.badgeColor
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AdaptiveSurfaceColor),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AdaptiveBorderColor, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = day.dayLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = AdaptivePrimaryText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = day.dateLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = AdaptiveSecondaryText
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(badgeColor)
                    )
                }
                Text(
                    text = day.weatherSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = AdaptiveSecondaryText
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Rain: ${day.totalRainMm.roundToInt()} mm (${day.maxRainProb}%)",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (day.totalRainMm >= 80) AlertRed else AdaptiveCyanAccent,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${day.minTempC.roundToInt()}°C – ${day.maxTempC.roundToInt()}°C • Gust ${day.maxWindKmh.roundToInt()} km/h",
                    style = MaterialTheme.typography.bodySmall,
                    color = AdaptiveSecondaryText
                )
            }
        }
    }
}
