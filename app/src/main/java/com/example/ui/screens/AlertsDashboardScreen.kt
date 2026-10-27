package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.FloodStatus
import com.example.model.RegionalWeatherReport
import com.example.model.RiverGaugeStation
import com.example.model.SriLankaRegion
import com.example.model.WeatherAlert
import com.example.ui.components.AlertsSummaryCard
import com.example.ui.components.CurrentConditionsHero
import com.example.ui.components.EmergencyCallButton
import com.example.ui.components.HourlyForecastStrip
import com.example.ui.components.NearestRiverCard

/** Home: weather now, alerts, next hours and the nearest river – nothing else. */
@Composable
fun AlertsDashboardScreen(
    selectedRegion: SriLankaRegion,
    selectedNearestCity: String,
    alerts: List<WeatherAlert>,
    regionalReport: RegionalWeatherReport,
    riverStations: List<RiverGaugeStation>,
    primaryStation: RiverGaugeStation?,
    isSyncing: Boolean,
    onOpenResidingLocationPicker: () -> Unit,
    onSendAreaNotificationNow: () -> Unit,
    onOpenRiversTab: () -> Unit,
    onDialEmergencyDmc: () -> Unit,
    modifier: Modifier = Modifier
) {
    val majorFloodCount = remember(riverStations) {
        riverStations.count { it.floodStatus == FloodStatus.MAJOR_FLOOD }
    }
    val minorFloodCount = remember(riverStations) {
        riverStations.count { it.floodStatus == FloodStatus.MINOR_FLOOD }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("alerts_dashboard_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Four cards, in the order people look for them: weather now, alerts, next hours, nearest river
        item {
            CurrentConditionsHero(
                report = regionalReport,
                cityName = selectedNearestCity,
                districtName = selectedRegion.name,
                isSyncing = isSyncing,
                onChangeArea = onOpenResidingLocationPicker
            )
        }

        item {
            AlertsSummaryCard(
                alerts = alerts,
                onNotifyMe = onSendAreaNotificationNow
            )
        }

        item { HourlyForecastStrip(hourly = regionalReport.hourly, isLive = regionalReport.isLiveApiData) }

        item {
            NearestRiverCard(
                station = primaryStation,
                floodingStationCount = majorFloodCount + minorFloodCount,
                onOpenRivers = onOpenRiversTab
            )
        }

        item { EmergencyCallButton(onCallEmergency = onDialEmergencyDmc) }
    }
}
