package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ActivitySafetyAssessment
import com.example.model.DailyForecastItem
import com.example.model.RegionalWeatherReport
import com.example.model.SriLankaRegion
import com.example.ui.components.HourlyForecastStrip
import com.example.ui.components.SectionCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.AdaptiveBorderColor
import com.example.ui.theme.AdaptiveCyanAccent
import com.example.ui.theme.AdaptivePrimaryText
import com.example.ui.theme.AdaptiveSecondaryText
import kotlin.math.roundToInt

/** Forecast tab: district picker, next hours, the week ahead and activity advice – one card each. */
@Composable
fun RegionalForecastScreen(
    regions: List<SriLankaRegion>,
    selectedRegion: SriLankaRegion,
    report: RegionalWeatherReport,
    onRegionSelected: (SriLankaRegion) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAllActivities by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("regional_forecast_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${selectedRegion.name} District",
                    style = MaterialTheme.typography.headlineSmall,
                    color = AdaptivePrimaryText,
                    fontWeight = FontWeight.Bold
                )
                if (report.isLiveApiData) {
                    Text(
                        text = "Now ${report.currentTempC.roundToInt()}° · ${report.weatherDescription} · " +
                            "gusts ${report.windGustKmh.roundToInt()} km/h · ${report.pressureHpa.roundToInt()} hPa",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AdaptiveSecondaryText
                    )
                }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("forecast_region_selector")
                ) {
                    items(regions, key = { it.id }) { region ->
                        FilterChip(
                            selected = region.id == selectedRegion.id,
                            onClick = { onRegionSelected(region) },
                            label = { Text(region.name) },
                            modifier = Modifier.testTag("forecast_region_${region.id}")
                        )
                    }
                }
            }
        }

        item { HourlyForecastStrip(hourly = report.hourly, isLive = report.isLiveApiData) }

        item {
            SectionCard(title = "Next 7 days") {
                report.daily.forEachIndexed { idx, day ->
                    if (idx > 0) HorizontalDivider(color = AdaptiveBorderColor)
                    DailyForecastRow(day)
                }
            }
        }

        if (report.activityAssessments.isNotEmpty()) {
            item {
                SectionCard(title = "Plan your day") {
                    val shown = if (showAllActivities) report.activityAssessments else report.activityAssessments.take(3)
                    shown.forEachIndexed { idx, assessment ->
                        if (idx > 0) HorizontalDivider(color = AdaptiveBorderColor)
                        ActivityRow(assessment)
                    }
                    if (report.activityAssessments.size > 3) {
                        TextButton(
                            onClick = { showAllActivities = !showAllActivities },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(if (showAllActivities) "Show less" else "Show all ${report.activityAssessments.size}")
                        }
                    }
                }
            }
        }
    }
}

private fun iconForSummary(summary: String, rainProb: Int): ImageVector {
    val s = summary.lowercase()
    return when {
        "thunder" in s -> Icons.Filled.Thunderstorm
        "rain" in s || "shower" in s || rainProb >= 60 -> Icons.Filled.Umbrella
        "sun" in s || "clear" in s -> Icons.Filled.WbSunny
        "partly" in s -> Icons.Filled.WbCloudy
        else -> Icons.Filled.Cloud
    }
}

@Composable
private fun DailyForecastRow(day: DailyForecastItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("forecast_day_${day.dayLabel}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.width(84.dp)) {
            Text(day.dayLabel, style = MaterialTheme.typography.bodyLarge, color = AdaptivePrimaryText, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(day.dateLabel, style = MaterialTheme.typography.labelSmall, color = AdaptiveSecondaryText)
        }
        Icon(
            imageVector = iconForSummary(day.weatherSummary, day.maxRainProb),
            contentDescription = day.weatherSummary,
            tint = AdaptiveCyanAccent,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = day.weatherSummary,
                style = MaterialTheme.typography.bodySmall,
                color = AdaptivePrimaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${day.maxRainProb}% · ${day.totalRainMm.roundToInt()} mm",
                style = MaterialTheme.typography.labelSmall,
                color = if (day.maxRainProb >= 60) AdaptiveCyanAccent else AdaptiveSecondaryText
            )
        }
        Text(
            text = "${day.maxTempC.roundToInt()}° / ${day.minTempC.roundToInt()}°",
            style = MaterialTheme.typography.titleSmall,
            color = AdaptivePrimaryText,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ActivityRow(assessment: ActivitySafetyAssessment) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = assessment.activityName,
                style = MaterialTheme.typography.bodyLarge,
                color = AdaptivePrimaryText,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            StatusPill(
                text = assessment.statusLabel.lowercase().replaceFirstChar { it.uppercase() },
                color = assessment.statusColor
            )
        }
        Text(assessment.recommendation, style = MaterialTheme.typography.bodySmall, color = AdaptiveSecondaryText)
    }
}
