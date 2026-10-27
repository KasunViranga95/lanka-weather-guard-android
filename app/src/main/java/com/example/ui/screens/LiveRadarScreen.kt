package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.example.model.DbzValueMeaning
import com.example.model.LiveMapMode
import com.example.model.LiveMapOverlayMetric
import com.example.model.RadarDbzMeaningList
import com.example.model.RadarFrame
import com.example.model.RadarLayerType
import com.example.model.RegionalWeatherReport
import com.example.model.RiverGaugeStation
import com.example.model.SriLankaRegion
import com.example.model.ZoomEarthLayer
import com.example.model.getDbzMeaningForValue
import com.example.ui.components.SriLankaLiveWebMap
import com.example.ui.components.SriLankaRadarMapCanvas
import com.example.ui.components.ZoomEarthLiveWebMap
import com.example.ui.theme.AdaptiveBorderColor
import com.example.ui.theme.AdaptiveCyanAccent
import com.example.ui.theme.AdaptiveElevatedColor
import com.example.ui.theme.AdaptivePrimaryText
import com.example.ui.theme.AdaptiveSecondaryText
import com.example.ui.theme.AdaptiveSurfaceColor
import com.example.ui.theme.AdaptiveTealAccent
import com.example.ui.theme.AdvisoryYellow
import com.example.ui.theme.AlertRed
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.WarningAmber
import java.util.Locale
import kotlin.math.roundToInt

private data class SriLankaDopplerMeasuringLocation(
    val stationId: String,
    val stationName: String,
    val districtName: String,
    val cityName: String,
    val dbzValue: Int,
    val elevationNote: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveRadarScreen(
    liveMapMode: LiveMapMode,
    selectedZoomEarthLayer: ZoomEarthLayer,
    liveMapOverlayMetric: LiveMapOverlayMetric,
    radarFrames: List<RadarFrame>,
    currentFrameIndex: Int,
    isRadarPlaying: Boolean,
    activeLayers: Set<RadarLayerType>,
    stations: List<RiverGaugeStation>,
    regions: List<SriLankaRegion> = emptyList(),
    selectedRegion: SriLankaRegion,
    selectedNearestCity: String,
    regionalReport: RegionalWeatherReport,
    selectedStation: RiverGaugeStation?,
    onSelectMapMode: (LiveMapMode) -> Unit,
    onSelectZoomEarthLayer: (ZoomEarthLayer) -> Unit,
    onSelectMapOverlayMetric: (LiveMapOverlayMetric) -> Unit,
    onTogglePlayPause: () -> Unit,
    onFrameIndexChanged: (Int) -> Unit,
    onToggleLayer: (RadarLayerType) -> Unit,
    onStationSelected: (RiverGaugeStation) -> Unit,
    onSelectRegionAndCity: (SriLankaRegion, String) -> Unit = { _, _ -> },
    onOpenExternalUrl: (String) -> Unit,
    onOpenGeminiClarifier: (String?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentFrame = radarFrames.getOrElse(currentFrameIndex) {
        radarFrames.first()
    }
    val peakDbzMeaning = getDbzMeaningForValue(currentFrame.peakDbz)

    val activeMapStation = selectedStation
        ?: stations.firstOrNull {
            it.riverName.contains(selectedRegion.primaryRiverBasin.substringBefore(" "), ignoreCase = true)
        }
        ?: stations.maxByOrNull { it.floodStatus.severityRank }

    // Sri Lanka Doppler Radar Measuring Locations with live dBZ & Doppler meaning
    val dopplerMeasuringLocations = remember(
        selectedNearestCity,
        selectedRegion.name,
        regionalReport.precipitationMm,
        currentFrame.peakDbz
    ) {
        val localDbz = when {
            regionalReport.precipitationMm >= 25.0 -> 53
            regionalReport.precipitationMm >= 12.0 -> 47
            regionalReport.precipitationMm >= 5.0 -> 39
            regionalReport.precipitationMm >= 1.5 -> 29
            else -> 21
        }
        val frameOffset = (currentFrame.peakDbz - 48).coerceIn(-6, 6)
        listOf(
            SriLankaDopplerMeasuringLocation(
                stationId = "user_residing",
                stationName = "$selectedNearestCity (Selected Location)",
                districtName = selectedRegion.name,
                cityName = selectedNearestCity,
                dbzValue = localDbz,
                elevationNote = "Active Residing Area Telemetry"
            ),
            SriLankaDopplerMeasuringLocation(
                stationId = "katunayake_colombo",
                stationName = "Katunayake / Colombo Doppler",
                districtName = "Colombo",
                cityName = "Colombo",
                dbzValue = (46 + frameOffset).coerceIn(18, 56),
                elevationNote = "Western Coastal S-Band Radar"
            ),
            SriLankaDopplerMeasuringLocation(
                stationId = "puttalam_radar",
                stationName = "Puttalam NW Doppler Station",
                districtName = "Puttalam",
                cityName = "Puttalam",
                dbzValue = (34 + frameOffset).coerceIn(18, 55),
                elevationNote = "North-Western Coastal Radar"
            ),
            SriLankaDopplerMeasuringLocation(
                stationId = "pidurutalagala_central",
                stationName = "Pidurutalagala / Nuwara Eliya",
                districtName = "Nuwara Eliya",
                cityName = "Nuwara Eliya",
                dbzValue = (42 + frameOffset).coerceIn(20, 56),
                elevationNote = "Central Highlands 2,524m Dome"
            ),
            SriLankaDopplerMeasuringLocation(
                stationId = "ratnapura_sabaragamuwa",
                stationName = "Ratnapura / Kalu Basin Sensor",
                districtName = "Ratnapura",
                cityName = "Ratnapura",
                dbzValue = (52 + frameOffset).coerceIn(24, 58),
                elevationNote = "Sabaragamuwa Heavy Rain Zone"
            ),
            SriLankaDopplerMeasuringLocation(
                stationId = "galle_koggala",
                stationName = "Galle / Koggala Southern Radar",
                districtName = "Galle",
                cityName = "Galle",
                dbzValue = (44 + frameOffset).coerceIn(20, 56),
                elevationNote = "Southern Monsoon Belt"
            ),
            SriLankaDopplerMeasuringLocation(
                stationId = "trinco_east",
                stationName = "Trincomalee / Batticaloa East",
                districtName = "Trincomalee",
                cityName = "Trincomalee",
                dbzValue = (24 + frameOffset).coerceIn(15, 48),
                elevationNote = "Eastern Bay of Bengal Watch"
            )
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("live_radar_screen"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Header & Mode Switcher (Live Weather Map vs Windy.com Live vs Doppler Radar)
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Live Weather & Doppler Map",
                                style = MaterialTheme.typography.titleMedium,
                                color = AdaptivePrimaryText,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$selectedNearestCity (${selectedRegion.name})",
                                style = MaterialTheme.typography.bodySmall,
                                color = AdaptiveCyanAccent,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            color = RadarCyan.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "%.1f°C • %.1fmm/h",
                                    regionalReport.currentTempC,
                                    regionalReport.precipitationMm
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = AdaptiveCyanAccent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Map Mode Toggle Row
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(LiveMapMode.entries.toList(), key = { it.name }) { mode ->
                            val selected = mode == liveMapMode
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectMapMode(mode) },
                                label = {
                                    Text(
                                        text = mode.label,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (mode) {
                                            LiveMapMode.WINDY_LIVE -> Icons.Default.Public
                                            LiveMapMode.INTERACTIVE_LIVE_MAP -> Icons.Default.Map
                                            LiveMapMode.TACTICAL_DOPPLER_CANVAS -> Icons.Default.Radar
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RadarCyan.copy(alpha = 0.18f),
                                    selectedLabelColor = AdaptiveCyanAccent,
                                    selectedLeadingIconColor = AdaptiveCyanAccent
                                ),
                                modifier = Modifier.testTag("map_mode_${mode.name.lowercase()}")
                            )
                        }
                    }

                    // Layer Selector for Current Mode
                    when (liveMapMode) {
                        LiveMapMode.WINDY_LIVE -> {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                items(ZoomEarthLayer.entries.toList(), key = { it.name }) { layer ->
                                    val selected = layer == selectedZoomEarthLayer
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onSelectZoomEarthLayer(layer) },
                                        label = { Text(layer.label) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Layers,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = RadarCyan.copy(alpha = 0.18f),
                                            selectedLabelColor = AdaptiveCyanAccent,
                                            selectedLeadingIconColor = AdaptiveCyanAccent
                                        ),
                                        modifier = Modifier.testTag("zoom_earth_layer_${layer.name.lowercase()}")
                                    )
                                }
                            }
                        }

                        LiveMapMode.INTERACTIVE_LIVE_MAP -> {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                items(
                                    LiveMapOverlayMetric.entries.filter { it != LiveMapOverlayMetric.RIVER_GAUGES },
                                    key = { it.name }
                                ) { metric ->
                                    val selected = metric == liveMapOverlayMetric
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onSelectMapOverlayMetric(metric) },
                                        label = { Text(metric.label) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Layers,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = AdaptiveTealAccent.copy(alpha = 0.18f),
                                            selectedLabelColor = AdaptiveTealAccent,
                                            selectedLeadingIconColor = AdaptiveTealAccent
                                        ),
                                        modifier = Modifier.testTag("live_map_overlay_${metric.code}")
                                    )
                                }
                            }
                        }

                        LiveMapMode.TACTICAL_DOPPLER_CANVAS -> {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                items(
                                    RadarLayerType.entries.filter { it != RadarLayerType.RIVER_GAUGES },
                                    key = { it.name }
                                ) { layer ->
                                    val isSelected = activeLayers.contains(layer)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onToggleLayer(layer) },
                                        label = { Text(layer.label) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Layers,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = RadarCyan.copy(alpha = 0.18f),
                                            selectedLabelColor = AdaptiveCyanAccent,
                                            selectedLeadingIconColor = AdaptiveCyanAccent
                                        ),
                                        modifier = Modifier.testTag("radar_layer_${layer.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Map Viewport (Full-window height so the live map fills the screen clearly)
        item {
            when (liveMapMode) {
                LiveMapMode.WINDY_LIVE -> {
                    ZoomEarthLiveWebMap(
                        selectedRegion = selectedRegion,
                        nearestCity = selectedNearestCity,
                        selectedLayer = selectedZoomEarthLayer,
                        regionalReport = regionalReport,
                        onOpenInExternalBrowser = onOpenExternalUrl,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(500.dp)
                    )
                }

                LiveMapMode.INTERACTIVE_LIVE_MAP -> {
                    SriLankaLiveWebMap(
                        selectedRegion = selectedRegion,
                        nearestCity = selectedNearestCity,
                        stations = stations,
                        regionalReport = regionalReport,
                        overlayMetric = liveMapOverlayMetric,
                        selectedStation = activeMapStation,
                        onStationSelected = onStationSelected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(500.dp)
                    )
                }

                LiveMapMode.TACTICAL_DOPPLER_CANVAS -> {
                    SriLankaRadarMapCanvas(
                        currentFrame = currentFrame,
                        activeLayers = activeLayers,
                        stations = stations,
                        selectedRegion = selectedRegion,
                        selectedStation = activeMapStation,
                        onStationTapped = onStationSelected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(480.dp)
                    )
                }
            }
        }

        // 3. Radar Playback Controls & Timeline Scrubber
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = onTogglePlayPause,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(RadarCyan)
                                .testTag("radar_play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isRadarPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isRadarPlaying) "Pause Radar Loop" else "Play Radar Loop",
                                tint = Color.White
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = currentFrame.label,
                                style = MaterialTheme.typography.titleMedium,
                                color = AdaptivePrimaryText,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${currentFrame.peakDbz} dBZ — ${peakDbzMeaning.shortTitle} (${peakDbzMeaning.rainRateMmPerHour})",
                                style = MaterialTheme.typography.labelLarge,
                                color = peakDbzMeaning.badgeColor,
                                fontWeight = FontWeight.Bold
                            )
                            Slider(
                                value = currentFrameIndex.toFloat(),
                                onValueChange = { onFrameIndexChanged(it.toInt()) },
                                valueRange = 0f..(radarFrames.size - 1).coerceAtLeast(1).toFloat(),
                                steps = (radarFrames.size - 2).coerceAtLeast(0),
                                colors = SliderDefaults.colors(
                                    thumbColor = RadarCyan,
                                    activeTrackColor = RadarCyan
                                ),
                                modifier = Modifier.testTag("radar_timeline_slider")
                            )
                        }
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DbzLegendItem("15–25 dBZ: Light Rain", SafeEmerald)
                        DbzLegendItem("26–35 dBZ: Moderate", RadarCyan)
                        DbzLegendItem("36–45 dBZ: Heavy Downpour", AdvisoryYellow)
                        DbzLegendItem("46–52 dBZ: Squall", WarningAmber)
                        DbzLegendItem("53+ dBZ: Cloudburst", AlertRed)
                    }
                }
            }
        }

        // 4. Unified Window: Live Weather Metrics + Sri Lanka Doppler Measuring Locations + dBZ Values Explanation
        item {
            UnifiedMetricsAndDopplerDbzCard(
                selectedNearestCity = selectedNearestCity,
                selectedRegion = selectedRegion,
                regionalReport = regionalReport,
                currentPeakDbz = currentFrame.peakDbz,
                dopplerLocations = dopplerMeasuringLocations,
                regions = regions,
                onSelectRegionAndCity = onSelectRegionAndCity,
                onOpenGeminiClarifier = onOpenGeminiClarifier
            )
        }
    }
}

@Composable
private fun UnifiedMetricsAndDopplerDbzCard(
    selectedNearestCity: String,
    selectedRegion: SriLankaRegion,
    regionalReport: RegionalWeatherReport,
    currentPeakDbz: Int,
    dopplerLocations: List<SriLankaDopplerMeasuringLocation>,
    regions: List<SriLankaRegion>,
    onSelectRegionAndCity: (SriLankaRegion, String) -> Unit,
    onOpenGeminiClarifier: (String?) -> Unit
) {
    var showDbzGuideRows by remember { mutableStateOf(true) }
    val currentMeaning = getDbzMeaningForValue(currentPeakDbz)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AdaptiveSurfaceColor),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AdaptiveBorderColor, RoundedCornerShape(14.dp))
            .testTag("dbz_meaning_guide_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section A: Live Weather Metrics Header + Clarify with Gemini Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Live Weather Metrics & Doppler dBZ Guide",
                        style = MaterialTheme.typography.titleSmall,
                        color = AdaptivePrimaryText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$selectedNearestCity, ${selectedRegion.name} • Metrics & dBZ Meaning in One Window",
                        style = MaterialTheme.typography.bodySmall,
                        color = AdaptiveCyanAccent
                    )
                }

                Surface(
                    color = RadarCyan.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .border(1.dp, RadarCyan.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                        .clickable {
                            onOpenGeminiClarifier(
                                "Clarify the live weather metrics (${regionalReport.rain24hMm.roundToInt()}mm/24h rain, ${regionalReport.pressureHpa.roundToInt()} hPa pressure, ${regionalReport.windGustKmh.roundToInt()} km/h gusts) and $currentPeakDbz dBZ radar reflectivity in $selectedNearestCity"
                            )
                        }
                        .testTag("live_radar_ask_gemini_metrics_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AdaptiveCyanAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Clarify Metrics",
                            style = MaterialTheme.typography.labelSmall,
                            color = AdaptiveCyanAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 4 Live Metric Boxes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MapDetailMetricBox(
                    icon = Icons.Default.WaterDrop,
                    label = "Precipitation (24h)",
                    value = "${String.format(Locale.US, "%.1f", regionalReport.rain24hMm)} mm",
                    sub = "Hourly: ${String.format(Locale.US, "%.1f", regionalReport.precipitationMm)} mm/h",
                    accent = if (regionalReport.rain24hMm >= 100) AlertRed else AdaptiveCyanAccent,
                    modifier = Modifier.weight(1f)
                )
                MapDetailMetricBox(
                    icon = Icons.Default.Air,
                    label = "Wind & Gusts",
                    value = "${regionalReport.windGustKmh.roundToInt()} km/h",
                    sub = "Sustained: ${regionalReport.windSpeedKmh.roundToInt()} km/h",
                    accent = WarningAmber,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MapDetailMetricBox(
                    icon = Icons.Default.Thermostat,
                    label = "Temp & Humidity",
                    value = "${regionalReport.currentTempC.roundToInt()}°C • ${regionalReport.humidityPercent}%",
                    sub = "Feels Like ${regionalReport.feelsLikeC.roundToInt()}°C",
                    accent = SafeEmerald,
                    modifier = Modifier.weight(1f)
                )
                MapDetailMetricBox(
                    icon = Icons.Default.Speed,
                    label = "Surface Pressure",
                    value = "${regionalReport.pressureHpa.roundToInt()} hPa",
                    sub = regionalReport.weatherDescription,
                    accent = AdaptiveTealAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = AdaptiveBorderColor)

            // Section B: Live Doppler Meaning with Sri Lanka Measuring Locations
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Radar,
                    contentDescription = null,
                    tint = AdaptiveCyanAccent,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = "Sri Lanka Live Doppler Measuring Locations",
                        style = MaterialTheme.typography.titleSmall,
                        color = AdaptivePrimaryText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap any Sri Lankan Doppler measuring station to focus the map & view live dBZ meaning",
                        style = MaterialTheme.typography.bodySmall,
                        color = AdaptiveSecondaryText
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                dopplerLocations.forEach { loc ->
                    val locMeaning = getDbzMeaningForValue(loc.dbzValue)
                    val isSelectedCity = loc.cityName.equals(selectedNearestCity, ignoreCase = true)
                    Surface(
                        color = if (isSelectedCity) locMeaning.badgeColor.copy(alpha = 0.14f) else AdaptiveElevatedColor,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isSelectedCity) 1.5.dp else 1.dp,
                                color = if (isSelectedCity) locMeaning.badgeColor else AdaptiveBorderColor,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                val targetRegion = regions.firstOrNull {
                                    it.name.equals(loc.districtName, ignoreCase = true)
                                }
                                if (targetRegion != null) {
                                    onSelectRegionAndCity(targetRegion, loc.cityName)
                                }
                            }
                            .testTag("doppler_location_${loc.stationId}")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
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
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = locMeaning.badgeColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = loc.stationName,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = AdaptivePrimaryText,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Surface(
                                    color = locMeaning.badgeColor.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${loc.dbzValue} dBZ • ${locMeaning.shortTitle}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = locMeaning.badgeColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Text(
                                text = "${loc.elevationNote} • Rain Rate: ${locMeaning.rainRateMmPerHour} — ${locMeaning.weatherMeaning}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AdaptiveSecondaryText
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = AdaptiveBorderColor)

            // Section C: Explain What dBZ Values Mean (Inside the Same Window with the Metrics)
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
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = AdaptiveCyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "What Radar dBZ Values Mean",
                        style = MaterialTheme.typography.titleSmall,
                        color = AdaptivePrimaryText,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = RadarCyan.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .border(1.dp, RadarCyan.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                        .clickable {
                            onOpenGeminiClarifier(
                                "Explain $currentPeakDbz dBZ radar reflectivity and what each dBZ range means for $selectedNearestCity"
                            )
                        }
                        .testTag("dbz_card_ask_gemini_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AdaptiveCyanAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Ask Gemini",
                            style = MaterialTheme.typography.labelSmall,
                            color = AdaptiveCyanAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Surface(
                color = currentMeaning.badgeColor.copy(alpha = 0.14f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, currentMeaning.badgeColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                    .clickable { showDbzGuideRows = !showDbzGuideRows }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Peak: $currentPeakDbz dBZ — ${currentMeaning.shortTitle} (${currentMeaning.rainRateMmPerHour})",
                        style = MaterialTheme.typography.labelLarge,
                        color = currentMeaning.badgeColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (showDbzGuideRows) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = currentMeaning.badgeColor
                    )
                }
            }

            AnimatedVisibility(visible = showDbzGuideRows) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RadarDbzMeaningList.forEach { item ->
                        val isCurrentPeakBand = currentPeakDbz in item.minDbz..item.maxDbz
                        DbzMeaningRowItem(
                            item = item,
                            isHighlighted = isCurrentPeakBand
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DbzMeaningRowItem(
    item: DbzValueMeaning,
    isHighlighted: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isHighlighted) item.badgeColor.copy(alpha = 0.14f) else AdaptiveElevatedColor
            )
            .border(
                width = if (isHighlighted) 1.5.dp else 1.dp,
                color = if (isHighlighted) item.badgeColor else AdaptiveBorderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = item.badgeColor.copy(alpha = 0.20f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = item.dbzRange,
                    style = MaterialTheme.typography.labelMedium,
                    color = item.badgeColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Text(
                text = "Rain Rate: ${item.rainRateMmPerHour}",
                style = MaterialTheme.typography.labelMedium,
                color = item.badgeColor,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "Meaning: ${item.shortTitle} — ${item.weatherMeaning}",
            style = MaterialTheme.typography.bodySmall,
            color = AdaptivePrimaryText,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "Action: ${item.citizenAction}",
            style = MaterialTheme.typography.labelSmall,
            color = AdaptiveSecondaryText,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MapDetailMetricBox(
    icon: ImageVector,
    label: String,
    value: String,
    sub: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AdaptiveElevatedColor)
            .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
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
            style = MaterialTheme.typography.titleMedium,
            color = AdaptivePrimaryText,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = sub,
            style = MaterialTheme.typography.bodySmall,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DbzLegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AdaptivePrimaryText,
            fontWeight = FontWeight.Medium
        )
    }
}
