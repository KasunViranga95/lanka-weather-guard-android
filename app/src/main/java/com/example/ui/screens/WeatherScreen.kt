package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.FloodStatus
import com.example.model.RegionalWeatherReport
import com.example.model.RiverGaugeStation
import com.example.model.WeatherAlert
import com.example.ui.components.AlertBanner
import com.example.ui.components.ConditionsGrid
import com.example.ui.components.CurrentConditionsHeader
import com.example.ui.components.DailyForecastCard
import com.example.ui.components.EmergencyCallButton
import com.example.ui.components.HourlyForecastStrip
import com.example.ui.components.NearestRiverCard
import com.example.ui.theme.AdaptiveSecondaryText
import com.example.util.WeatherFormat

/**
 * Weather screen, read top to bottom like a standard weather app: now, alerts, hourly, the week,
 * today's details and the nearest river. Pull down to refresh.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    selectedNearestCity: String,
    alerts: List<WeatherAlert>,
    alertsEnabled: Boolean,
    onAlertsEnabledChange: (Boolean) -> Unit,
    regionalReport: RegionalWeatherReport,
    riverStations: List<RiverGaugeStation>,
    primaryStation: RiverGaugeStation?,
    isSyncing: Boolean,
    onRefresh: () -> Unit,
    onOpenRiversTab: () -> Unit,
    onDialEmergencyDmc: () -> Unit,
    modifier: Modifier = Modifier
) {
    val floodingCount = remember(riverStations) {
        riverStations.count { it.floodStatus == FloodStatus.MAJOR_FLOOD || it.floodStatus == FloodStatus.MINOR_FLOOD }
    }
    val res = LocalContext.current.resources

    PullToRefreshBox(
        isRefreshing = isSyncing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("alerts_dashboard_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                CurrentConditionsHeader(
                    report = regionalReport,
                    cityName = selectedNearestCity,
                    isSyncing = isSyncing
                )
            }
            if (alerts.isNotEmpty()) {
                item {
                    AlertBanner(
                        alerts = alerts,
                        alertsEnabled = alertsEnabled,
                        onAlertsEnabledChange = onAlertsEnabledChange
                    )
                }
            }
            item { HourlyForecastStrip(hourly = regionalReport.hourly, isLive = regionalReport.isLiveApiData) }
            if (regionalReport.isLiveApiData && regionalReport.daily.isNotEmpty()) {
                item { DailyForecastCard(daily = regionalReport.daily) }
            }
            item { ConditionsGrid(report = regionalReport) }
            item {
                NearestRiverCard(
                    station = primaryStation,
                    floodingStationCount = floodingCount,
                    onOpenRivers = onOpenRiversTab
                )
            }
            item { EmergencyCallButton(onCallEmergency = onDialEmergencyDmc) }
            if (regionalReport.isLiveApiData && regionalReport.fetchedAtMillis > 0) {
                item {
                    Text(
                        text = stringResource(
                            R.string.updated_source,
                            WeatherFormat.time(res, regionalReport.fetchedAtMillis)
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = AdaptiveSecondaryText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
