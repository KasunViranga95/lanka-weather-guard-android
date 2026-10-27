package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flood
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AlertSeverity
import com.example.model.DispatchedNotificationRecord
import com.example.model.FloodStatus
import com.example.model.GovernmentAnnouncement
import com.example.model.NotificationIntervalOption
import com.example.model.RegionalWeatherReport
import com.example.model.RiverGaugeStation
import com.example.model.SriLankaRegion
import com.example.model.WeatherAlert
import com.example.ui.theme.AdaptiveBorderColor
import com.example.ui.theme.AdaptiveCyanAccent
import com.example.ui.theme.AdaptiveElevatedColor
import com.example.ui.theme.AdaptivePrimaryText
import com.example.ui.theme.AdaptiveSecondaryText
import com.example.ui.theme.AdaptiveSurfaceColor
import com.example.ui.theme.AdaptiveTealAccent
import com.example.ui.theme.AlertRed
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.MonsoonTeal
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.WarningAmber
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun AlertsDashboardScreen(
    regions: List<SriLankaRegion>,
    selectedRegion: SriLankaRegion,
    selectedNearestCity: String,
    selectedNotificationInterval: NotificationIntervalOption,
    gpsStatusMessage: String,
    latestNotification: DispatchedNotificationRecord?,
    notificationHistory: List<DispatchedNotificationRecord> = emptyList(),
    residingGovAnnouncement: GovernmentAnnouncement?,
    alerts: List<WeatherAlert>,
    selectedSeverityFilter: AlertSeverity?,
    regionalReport: RegionalWeatherReport,
    riverStations: List<RiverGaugeStation>,
    onRegionSelected: (SriLankaRegion) -> Unit,
    onSelectNotificationInterval: (NotificationIntervalOption) -> Unit,
    onRequestGpsLocation: () -> Unit,
    onOpenResidingLocationPicker: () -> Unit,
    onSendAreaNotificationNow: () -> Unit,
    onSelectGeneratedNotification: (DispatchedNotificationRecord) -> Unit = {},
    onRefreshTelemetry: () -> Unit,
    onSeverityFilterSelected: (AlertSeverity?) -> Unit,
    onOpenRadarTab: () -> Unit,
    onOpenRiversTab: () -> Unit,
    onOpenSafetyTab: () -> Unit,
    onOpenOfficialUrl: (String) -> Unit,
    onDialEmergencyDmc: () -> Unit,
    onSetDarkTheme: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val filteredAlerts = remember(alerts, selectedSeverityFilter) {
        if (selectedSeverityFilter == null) {
            alerts
        } else {
            alerts.filter { it.severity == selectedSeverityFilter }
        }
    }

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
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Clean Facebook-Style Location & Weather Status Card
        item {
            HeroCommandBanner(
                selectedRegion = selectedRegion,
                selectedNearestCity = selectedNearestCity,
                gpsStatusMessage = gpsStatusMessage,
                regionalReport = regionalReport,
                majorFloodCount = majorFloodCount,
                minorFloodCount = minorFloodCount,
                riverStationTotal = riverStations.size,
                onRequestGpsLocation = onRequestGpsLocation,
                onOpenResidingLocationPicker = onOpenResidingLocationPicker,
                onSendAreaNotificationNow = onSendAreaNotificationNow,
                onRefreshTelemetry = onRefreshTelemetry,
                onOpenRadarTab = onOpenRadarTab,
                onOpenRiversTab = onOpenRiversTab,
                onDialEmergencyDmc = onDialEmergencyDmc
            )
        }

        // 3. Sri Lanka 25-District Quick Selector Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = AdaptiveSurfaceColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AdaptiveBorderColor, RoundedCornerShape(14.dp))
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
                        Text(
                            text = "Sri Lanka Districts",
                            style = MaterialTheme.typography.titleSmall,
                            color = AdaptivePrimaryText,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Search City →",
                            style = MaterialTheme.typography.labelMedium,
                            color = AdaptiveCyanAccent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { onOpenResidingLocationPicker() }
                                .testTag("search_district_city_link")
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("region_selector_row")
                    ) {
                        items(regions, key = { it.id }) { region ->
                            val isSelected = region.id == selectedRegion.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { onRegionSelected(region) },
                                label = {
                                    Text(
                                        text = region.name,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RadarCyan.copy(alpha = 0.18f),
                                    selectedLabelColor = AdaptiveCyanAccent,
                                    selectedLeadingIconColor = AdaptiveCyanAccent
                                ),
                                modifier = Modifier.testTag("region_chip_${region.id}")
                            )
                        }
                    }
                }
            }
        }

        // 4. Active Safety Alerts Filter Header
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = AdaptiveSurfaceColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AdaptiveBorderColor, RoundedCornerShape(14.dp))
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
                        Text(
                            text = "Active Weather Alerts (${filteredAlerts.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = AdaptivePrimaryText,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = regionalReport.syncedTimestamp,
                            style = MaterialTheme.typography.bodySmall,
                            color = AdaptiveSecondaryText
                        )
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedSeverityFilter == null,
                                onClick = { onSeverityFilterSelected(null) },
                                label = { Text("All (${alerts.size})") },
                                modifier = Modifier.testTag("filter_alert_all")
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedSeverityFilter == AlertSeverity.CRITICAL_RED,
                                onClick = { onSeverityFilterSelected(AlertSeverity.CRITICAL_RED) },
                                label = { Text("Red Warning") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AlertRed.copy(alpha = 0.18f),
                                    selectedLabelColor = AlertRed
                                ),
                                modifier = Modifier.testTag("filter_alert_red")
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedSeverityFilter == AlertSeverity.MAJOR_AMBER,
                                onClick = { onSeverityFilterSelected(AlertSeverity.MAJOR_AMBER) },
                                label = { Text("Amber Watch") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WarningAmber.copy(alpha = 0.18f),
                                    selectedLabelColor = WarningAmber
                                ),
                                modifier = Modifier.testTag("filter_alert_amber")
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedSeverityFilter == AlertSeverity.ADVISORY_YELLOW,
                                onClick = { onSeverityFilterSelected(AlertSeverity.ADVISORY_YELLOW) },
                                label = { Text("Advisory") },
                                modifier = Modifier.testTag("filter_alert_yellow")
                            )
                        }
                    }
                }
            }
        }

        // 5. Facebook-Style Expandable Alert Feed Cards
        items(filteredAlerts, key = { it.id }) { alert ->
            WeatherAlertCard(
                alert = alert,
                onOpenRiversTab = onOpenRiversTab,
                onOpenRadarTab = onOpenRadarTab
            )
        }

        // 6. Official Government Announcement Summary Tile
        if (residingGovAnnouncement != null) {
            item {
                ResidingGovernmentBulletinSummaryCard(
                    announcement = residingGovAnnouncement,
                    onOpenOfficialUrl = { onOpenOfficialUrl(residingGovAnnouncement.officialUrl) },
                    onViewAllGovNews = onOpenSafetyTab
                )
            }
        }

        // 7. Auto-Notification Interval Selector
        item {
            NotificationIntervalSelectorCard(
                selectedNearestCity = selectedNearestCity,
                selectedRegionName = selectedRegion.name,
                selectedInterval = selectedNotificationInterval,
                onSelectInterval = onSelectNotificationInterval
            )
        }

        // 8. Latest Dispatched Phone Notification (Selecting a generated notification posts it to Device Notifications)
        if (latestNotification != null) {
            item {
                DispatchedNotificationBanner(
                    record = latestNotification,
                    notificationHistory = notificationHistory,
                    onSelectGeneratedNotification = onSelectGeneratedNotification,
                    onChangeLocation = onOpenResidingLocationPicker,
                    onSendNow = onSendAreaNotificationNow,
                    onOpenOfficialUrl = { onOpenOfficialUrl(latestNotification.officialPortalUrl) }
                )
            }
        }
    }
}

@Composable
private fun HeroCommandBanner(
    selectedRegion: SriLankaRegion,
    selectedNearestCity: String,
    gpsStatusMessage: String,
    regionalReport: RegionalWeatherReport,
    majorFloodCount: Int,
    minorFloodCount: Int,
    riverStationTotal: Int,
    onRequestGpsLocation: () -> Unit,
    onOpenResidingLocationPicker: () -> Unit,
    onSendAreaNotificationNow: () -> Unit,
    onRefreshTelemetry: () -> Unit,
    onOpenRadarTab: () -> Unit,
    onOpenRiversTab: () -> Unit,
    onDialEmergencyDmc: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AdaptiveSurfaceColor),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AdaptiveBorderColor, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Facebook-Style Post Header: Avatar on left, Location & Status in center, Action Icons on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(RadarCyan.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = AdaptiveCyanAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$selectedNearestCity, ${selectedRegion.name}",
                            style = MaterialTheme.typography.titleMedium,
                            color = AdaptivePrimaryText,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = gpsStatusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = AdaptiveSecondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onRequestGpsLocation,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AdaptiveElevatedColor)
                            .testTag("gps_detect_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = "Detect GPS Location",
                            tint = AdaptiveCyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onOpenResidingLocationPicker,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AdaptiveElevatedColor)
                            .testTag("open_location_picker_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditLocationAlt,
                            contentDescription = "Change District & City",
                            tint = AdaptiveTealAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onRefreshTelemetry,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AdaptiveElevatedColor)
                            .testTag("refresh_telemetry_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Weather",
                            tint = AdaptivePrimaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Clean 2x2 Weather Summary Grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TelemetryMetricPill(
                        icon = Icons.Default.Thermostat,
                        label = "Temperature",
                        value = String.format(Locale.US, "%.1f°C", regionalReport.currentTempC),
                        subtitle = "Feels like ${regionalReport.feelsLikeC.roundToInt()}°C",
                        accentColor = SafeEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryMetricPill(
                        icon = Icons.Default.WaterDrop,
                        label = "Rainfall",
                        value = "${String.format(Locale.US, "%.1f", regionalReport.precipitationMm)} mm/h",
                        subtitle = "24h total: ${regionalReport.rain24hMm.roundToInt()} mm",
                        accentColor = if (regionalReport.rain24hMm >= 100) AlertRed else AdaptiveCyanAccent,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TelemetryMetricPill(
                        icon = Icons.Default.Air,
                        label = "Wind Gusts",
                        value = "${regionalReport.windGustKmh.roundToInt()} km/h",
                        subtitle = "Steady: ${regionalReport.windSpeedKmh.roundToInt()} km/h",
                        accentColor = if (regionalReport.windGustKmh >= 55) WarningAmber else AdaptiveTealAccent,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryMetricPill(
                        icon = Icons.Default.WaterDrop,
                        label = "Humidity",
                        value = "${regionalReport.humidityPercent}%",
                        subtitle = "${regionalReport.pressureHpa.roundToInt()} hPa",
                        accentColor = AdaptiveCyanAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Concise River Status Bar
            Surface(
                color = AlertRed.copy(alpha = 0.10f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AlertRed.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .clickable { onOpenRiversTab() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flood,
                            contentDescription = null,
                            tint = AlertRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "River Flood Watch ($riverStationTotal Stations)",
                                style = MaterialTheme.typography.titleSmall,
                                color = AdaptivePrimaryText,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$majorFloodCount Major Flood • $minorFloodCount Minor Flood",
                                style = MaterialTheme.typography.bodySmall,
                                color = AlertRed,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(
                        text = "River Index →",
                        style = MaterialTheme.typography.labelLarge,
                        color = AdaptiveCyanAccent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            HorizontalDivider(color = AdaptiveBorderColor)

            // Facebook-Style Card Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenResidingLocationPicker,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AdaptiveCyanAccent),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("select_district_city_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.EditLocationAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Change Area",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onSendAreaNotificationNow,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MonsoonTeal,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("send_area_notification_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Notify Now",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenRadarTab,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RadarCyan,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("hero_open_radar_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Radar,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Live Weather Map",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onDialEmergencyDmc,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("hero_call_dmc_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneInTalk,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Call 117",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationIntervalSelectorCard(
    selectedNearestCity: String,
    selectedRegionName: String,
    selectedInterval: NotificationIntervalOption,
    onSelectInterval: (NotificationIntervalOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AdaptiveSurfaceColor),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, AdaptiveBorderColor, RoundedCornerShape(14.dp))
            .testTag("notification_interval_selector_card")
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = AdaptiveTealAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Notification Schedule",
                        style = MaterialTheme.typography.titleSmall,
                        color = AdaptivePrimaryText,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    color = AdaptiveTealAccent.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = selectedInterval.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = AdaptiveTealAccent,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "Auto-send weather & official updates for $selectedNearestCity ($selectedRegionName) every:",
                style = MaterialTheme.typography.bodySmall,
                color = AdaptiveSecondaryText
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(NotificationIntervalOption.entries.toList(), key = { it.id }) { option ->
                    val isSelected = option == selectedInterval
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectInterval(option) },
                        label = {
                            Text(
                                text = option.shortChipLabel,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RadarCyan.copy(alpha = 0.18f),
                            selectedLabelColor = AdaptiveCyanAccent
                        ),
                        modifier = Modifier.testTag("interval_chip_${option.id}")
                    )
                }
            }
        }
    }
}

@Composable
private fun ResidingGovernmentBulletinSummaryCard(
    announcement: GovernmentAnnouncement,
    onOpenOfficialUrl: () -> Unit,
    onViewAllGovNews: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AdaptiveSurfaceColor),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AdaptiveCyanAccent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .clickable { expanded = !expanded }
            .testTag("residing_gov_announcement_card")
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = AdaptiveCyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Official Government Update",
                        style = MaterialTheme.typography.titleSmall,
                        color = AdaptiveCyanAccent,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = if (expanded) "Hide ▲" else "Full Message ▼",
                    style = MaterialTheme.typography.labelSmall,
                    color = AdaptiveCyanAccent,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = announcement.headline,
                style = MaterialTheme.typography.titleMedium,
                color = AdaptivePrimaryText,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = announcement.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = AdaptiveSecondaryText,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AdaptiveElevatedColor)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    announcement.keyDirectives.forEach { directive ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SafeEmerald,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(14.dp)
                            )
                            Text(
                                text = directive,
                                style = MaterialTheme.typography.bodySmall,
                                color = AdaptivePrimaryText
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenOfficialUrl,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RadarCyan,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_official_gov_link_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = announcement.officialPortalName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onViewAllGovNews,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("view_all_gov_news_button")
                ) {
                    Text(
                        text = "Safety Hub →",
                        style = MaterialTheme.typography.labelMedium,
                        color = AdaptiveTealAccent,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun DispatchedNotificationBanner(
    record: DispatchedNotificationRecord,
    notificationHistory: List<DispatchedNotificationRecord>,
    onSelectGeneratedNotification: (DispatchedNotificationRecord) -> Unit,
    onChangeLocation: () -> Unit,
    onSendNow: () -> Unit,
    onOpenOfficialUrl: () -> Unit
) {
    var showFullDetails by remember { mutableStateOf(true) }
    val accent = record.severity.badgeColor
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AdaptiveSurfaceColor),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .clickable {
                showFullDetails = !showFullDetails
                onSelectGeneratedNotification(record)
            }
            .testTag("dispatched_notification_banner")
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Generated Device Notification (${record.intervalLabel})",
                        style = MaterialTheme.typography.titleSmall,
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = record.timestampText,
                        style = MaterialTheme.typography.bodySmall,
                        color = AdaptiveSecondaryText
                    )
                    Icon(
                        imageVector = if (showFullDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (showFullDetails) "Hide notification details" else "Show notification details",
                        tint = AdaptiveSecondaryText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = "Tap any generated notification below to show it in your Android device notifications. Tapping the device notification opens Lanka Weather Guard.",
                style = MaterialTheme.typography.bodySmall,
                color = AdaptiveSecondaryText
            )

            Surface(
                color = accent.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                    .clickable { onSelectGeneratedNotification(record) }
                    .testTag("select_generated_notification_card")
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = record.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AdaptivePrimaryText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap to Show in Device Notifications • Opens App on Click",
                        style = MaterialTheme.typography.labelSmall,
                        color = AdaptiveCyanAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AnimatedVisibility(visible = showFullDetails) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = SafeEmerald.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                                Text(
                                    text = "Temperature",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AdaptiveSecondaryText
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.1f°C", record.temperatureC)} (Feels ${record.feelsLikeC.roundToInt()}°C)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = AdaptivePrimaryText,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            color = RadarCyan.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                                Text(
                                    text = "Precipitation",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AdaptiveSecondaryText
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.1f mm/h", record.hourlyPrecipitationMm)} (${record.rain24hMm.roundToInt()}mm)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = AdaptivePrimaryText,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AdaptiveElevatedColor)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "Current: ${record.currentWeatherSummary}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AdaptivePrimaryText
                        )
                        Text(
                            text = "Forecast: ${record.forecastSummary}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AdaptiveCyanAccent,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Official [${record.govReferenceCode}]: ${record.govAnnouncementHeadline}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AdaptiveTealAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (notificationHistory.size > 1) {
                        Text(
                            text = "Recent Generated Notifications (Tap to Show on Device):",
                            style = MaterialTheme.typography.labelMedium,
                            color = AdaptivePrimaryText,
                            fontWeight = FontWeight.Bold
                        )
                        notificationHistory.take(4).forEach { itemRecord ->
                            val isCurrent = itemRecord.id == record.id
                            Surface(
                                color = if (isCurrent) RadarCyan.copy(alpha = 0.14f) else AdaptiveElevatedColor,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (isCurrent) RadarCyan else AdaptiveBorderColor,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSelectGeneratedNotification(itemRecord) }
                                    .testTag("generated_notification_item_${itemRecord.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${itemRecord.nearestCity}, ${itemRecord.district} • ${String.format(Locale.US, "%.1f°C", itemRecord.temperatureC)}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = AdaptivePrimaryText,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${itemRecord.timestampText} • Tap to post to device notification bar",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AdaptiveSecondaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = "Send to Device Notifications",
                                        tint = AdaptiveCyanAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { onSelectGeneratedNotification(record) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = RadarCyan,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("show_in_device_notifications_button")
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Show in Device Notifications",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Official News ↗",
                    style = MaterialTheme.typography.labelMedium,
                    color = AdaptiveCyanAccent,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onOpenOfficialUrl() }
                )
                Text(
                    text = "Generate New Alert",
                    style = MaterialTheme.typography.labelMedium,
                    color = SafeEmerald,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onSendNow() }
                )
                Text(
                    text = "Change Schedule →",
                    style = MaterialTheme.typography.labelMedium,
                    color = AdaptiveTealAccent,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onChangeLocation() }
                )
            }
        }
    }
}

@Composable
private fun TelemetryMetricPill(
    icon: ImageVector,
    label: String,
    value: String,
    subtitle: String = "",
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AdaptiveElevatedColor)
            .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = AdaptiveSecondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = AdaptivePrimaryText,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WeatherAlertCard(
    alert: WeatherAlert,
    onOpenRiversTab: () -> Unit,
    onOpenRadarTab: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val badgeColor = alert.severity.badgeColor

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = AdaptiveSurfaceColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (alert.isUserDistrictMatch) 1.5.dp else 1.dp,
                color = if (alert.isUserDistrictMatch) badgeColor else AdaptiveBorderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { expanded = !expanded }
            .testTag("alert_card_${alert.id}")
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = badgeColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = alert.severity.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = badgeColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (alert.isUserDistrictMatch) {
                        Surface(
                            color = RadarCyan.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Your Area",
                                style = MaterialTheme.typography.labelSmall,
                                color = AdaptiveCyanAccent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Surface(
                    color = AdaptiveElevatedColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (expanded) "Hide" else "Expand",
                            style = MaterialTheme.typography.labelSmall,
                            color = AdaptiveCyanAccent,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse alert" else "Expand alert",
                            tint = AdaptiveCyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Text(
                text = alert.title,
                style = MaterialTheme.typography.titleMedium,
                color = AdaptivePrimaryText,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${alert.issuingAgency} • ${alert.issuedTimeText}",
                style = MaterialTheme.typography.bodySmall,
                color = AdaptiveCyanAccent
            )

            Text(
                text = alert.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = AdaptiveSecondaryText,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rainfall: ${String.format(Locale.US, "%.1f mm", alert.rainfallMm24h)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = AdaptiveTealAccent
                )
                Text(
                    text = "•",
                    color = AdaptiveSecondaryText
                )
                Text(
                    text = "Wind Gusts: ${alert.windGustKmh.roundToInt()} km/h",
                    style = MaterialTheme.typography.labelMedium,
                    color = WarningAmber
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AdaptiveElevatedColor)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Recommended Safety Actions (${alert.validUntilText})",
                            style = MaterialTheme.typography.titleSmall,
                            color = AdaptivePrimaryText,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    alert.recommendedActions.forEach { step ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AdaptiveTealAccent,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(15.dp)
                            )
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AdaptivePrimaryText
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = if (alert.riverBasin?.contains("Ganga") == true || alert.riverBasin?.contains("Oya") == true) {
                                onOpenRiversTab
                            } else {
                                onOpenRadarTab
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (alert.riverBasin?.contains("Ganga") == true || alert.riverBasin?.contains("Oya") == true) {
                                    "View ${alert.riverBasin.substringBefore(" (")} River Level"
                                } else {
                                    "View on Live Map"
                                },
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
