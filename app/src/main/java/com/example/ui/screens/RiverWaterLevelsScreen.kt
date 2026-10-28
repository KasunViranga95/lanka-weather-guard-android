package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.FloodStatus
import com.example.model.HydrographTimeRange
import com.example.model.RiverGaugeStation
import com.example.model.WaterTrend
import com.example.ui.components.HydrographTimeRangeSelector
import com.example.ui.components.RiverHydrographCurve
import com.example.ui.components.RiverThresholdBar
import com.example.ui.components.StatusPill
import com.example.ui.components.friendlyLabel
import com.example.ui.theme.AdaptiveBorderColor
import com.example.ui.theme.AdaptiveElevatedColor
import com.example.ui.theme.AdaptivePrimaryText
import com.example.ui.theme.AdaptiveSecondaryText
import com.example.ui.theme.AdaptiveSurfaceColor
import java.util.Locale
import kotlin.math.roundToInt

private enum class RiverStatusFilter(val label: String) {
    ALL("All"),
    FLOODING("Flooding"),
    WATCH("Near alert"),
    NORMAL("Normal");

    fun matches(status: FloodStatus): Boolean = when (this) {
        ALL -> true
        FLOODING -> status == FloodStatus.MINOR_FLOOD || status == FloodStatus.MAJOR_FLOOD
        WATCH -> status == FloodStatus.ALERT
        NORMAL -> status == FloodStatus.NORMAL
    }
}

private fun stationMatchesSearchQuery(station: RiverGaugeStation, rawQuery: String): Boolean {
    val q = rawQuery.trim()
    if (q.isEmpty()) return true
    return station.stationName.contains(q, ignoreCase = true) ||
        station.riverName.contains(q, ignoreCase = true) ||
        station.basinCode.contains(q, ignoreCase = true) ||
        station.district.contains(q, ignoreCase = true) ||
        (station.tributaryOf?.contains(q, ignoreCase = true) == true) ||
        station.downstreamTowns.any { it.contains(q, ignoreCase = true) }
}

private fun formatLevel(metres: Double) = String.format(Locale.US, "%.2f m", metres)

/**
 * Rivers tab: a search box, one row of status filters and a single list of gauges grouped by river.
 * Rivers with flooding gauges come first; tap a gauge for its thresholds and water-level graph.
 */
@Composable
fun RiverWaterLevelsScreen(
    stations: List<RiverGaugeStation>,
    selectedBasinFilter: String,
    searchQuery: String,
    selectedHydrographTimeRange: HydrographTimeRange,
    onBasinFilterSelected: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onHydrographTimeRangeSelected: (HydrographTimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    var statusFilter by rememberSaveable { mutableStateOf(RiverStatusFilter.ALL) }
    var expandedStationId by rememberSaveable { mutableStateOf<String?>(null) }

    val riverGroups = remember(stations, selectedBasinFilter, searchQuery, statusFilter) {
        stations
            .filter { selectedBasinFilter == "All Basins" || it.riverName.contains(selectedBasinFilter, ignoreCase = true) }
            .filter { stationMatchesSearchQuery(it, searchQuery) }
            .filter { statusFilter.matches(it.floodStatus) }
            .groupBy { it.riverName }
            .map { (river, list) -> river to list.sortedByDescending { it.floodStatus.severityRank } }
            .sortedWith(
                compareByDescending<Pair<String, List<RiverGaugeStation>>> { (_, list) ->
                    list.maxOf { it.floodStatus.severityRank }
                }.thenBy { it.first }
            )
    }
    val shownCount = riverGroups.sumOf { it.second.size }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("river_levels_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text(stringResource(R.string.search_rivers_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_rivers_input")
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (selectedBasinFilter != "All Basins") {
                    item {
                        InputChip(
                            selected = true,
                            onClick = { onBasinFilterSelected("All Basins") },
                            label = { Text(selectedBasinFilter) },
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Show all rivers", modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
                items(RiverStatusFilter.entries.toList(), key = { it.name }) { filter ->
                    val count = stations.count { filter.matches(it.floodStatus) }
                    FilterChip(
                        selected = statusFilter == filter,
                        onClick = { statusFilter = filter },
                        label = { Text("${filter.label} ($count)") },
                        modifier = Modifier.testTag("river_filter_${filter.name.lowercase(Locale.US)}")
                    )
                }
            }
        }

        if (riverGroups.isEmpty()) {
            item {
                Text(
                    text = if (searchQuery.isBlank()) "No gauges in this category right now."
                    else "No river or gauge matches \"${searchQuery.trim()}\".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AdaptiveSecondaryText,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        } else {
            item {
                Text(
                    text = "$shownCount of ${stations.size} gauges · Irrigation Dept, updated hourly",
                    style = MaterialTheme.typography.labelMedium,
                    color = AdaptiveSecondaryText
                )
            }
        }

        items(riverGroups, key = { it.first }) { (river, riverStations) ->
            RiverGroupCard(
                riverName = river,
                stations = riverStations,
                expandedStationId = expandedStationId,
                onToggleStation = { id -> expandedStationId = if (expandedStationId == id) null else id },
                timeRange = selectedHydrographTimeRange,
                onTimeRangeSelected = onHydrographTimeRangeSelected
            )
        }
    }
}

@Composable
private fun RiverGroupCard(
    riverName: String,
    stations: List<RiverGaugeStation>,
    expandedStationId: String?,
    onToggleStation: (String) -> Unit,
    timeRange: HydrographTimeRange,
    onTimeRangeSelected: (HydrographTimeRange) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(AdaptiveSurfaceColor)
            .testTag("river_group_${riverName.lowercase(Locale.US).replace(' ', '_')}")
    ) {
        Text(
            text = riverName,
            style = MaterialTheme.typography.titleMedium,
            color = AdaptivePrimaryText,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp)
        )
        stations.forEachIndexed { idx, station ->
            if (idx > 0) HorizontalDivider(color = AdaptiveBorderColor, modifier = Modifier.padding(horizontal = 16.dp))
            StationRow(
                station = station,
                expanded = expandedStationId == station.id,
                onToggle = { onToggleStation(station.id) },
                timeRange = timeRange,
                onTimeRangeSelected = onTimeRangeSelected
            )
        }
    }
}

@Composable
private fun StationRow(
    station: RiverGaugeStation,
    expanded: Boolean,
    onToggle: () -> Unit,
    timeRange: HydrographTimeRange,
    onTimeRangeSelected: (HydrographTimeRange) -> Unit
) {
    val status = station.floodStatus
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("river_station_card_${station.id}"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(status.color)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.stationName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AdaptivePrimaryText,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${station.district} · ${status.friendlyLabel()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (status == FloodStatus.NORMAL) AdaptiveSecondaryText else status.color
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = when (station.trend) {
                            WaterTrend.RISING_FAST, WaterTrend.RISING -> Icons.AutoMirrored.Filled.TrendingUp
                            WaterTrend.FALLING -> Icons.AutoMirrored.Filled.TrendingDown
                            WaterTrend.STABLE -> Icons.AutoMirrored.Filled.TrendingFlat
                        },
                        contentDescription = station.trend.label,
                        tint = AdaptiveSecondaryText,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = formatLevel(station.currentLevelM),
                        style = MaterialTheme.typography.titleMedium,
                        color = AdaptivePrimaryText,
                        fontWeight = FontWeight.Bold
                    )
                }
                station.publishedLevelText?.let {
                    Text("($it)", style = MaterialTheme.typography.labelSmall, color = AdaptiveSecondaryText)
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Hide details" else "Show details",
                tint = AdaptiveSecondaryText
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(text = status.friendlyLabel(), color = status.color)
                    Text(
                        text = "${station.trend.label} · ${String.format(Locale.US, "%+.2f m/hr", station.deltaM)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AdaptiveSecondaryText
                    )
                }

                RiverThresholdBar(station = station)

                Text(
                    text = String.format(
                        Locale.US,
                        "Alert %.2f m · Minor flood %.2f m · Major flood %.2f m",
                        station.alertLevelM, station.minorFloodLevelM, station.majorFloodLevelM
                    ) + if (station.sourceUnit == "ft") "\nThis gauge is published in feet; shown here in metres." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = AdaptiveSecondaryText
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AdaptiveElevatedColor)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HydrographTimeRangeSelector(
                        selectedRange = timeRange,
                        onRangeSelected = onTimeRangeSelected,
                        stationIdTagPrefix = station.id
                    )
                    RiverHydrographCurve(station = station, timeRange = timeRange)
                }

                Text(
                    text = "Rain in catchment (24h): ${station.catchmentRainMm24h.roundToInt()} mm · " +
                        "Flow: ${station.dischargeM3s.roundToInt()} m³/s",
                    style = MaterialTheme.typography.bodySmall,
                    color = AdaptivePrimaryText
                )
                if (station.downstreamTowns.isNotEmpty()) {
                    Text(
                        text = "Downstream: ${station.downstreamTowns.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AdaptivePrimaryText
                    )
                }
                Text(station.lastUpdatedText, style = MaterialTheme.typography.labelSmall, color = AdaptiveSecondaryText)
            }
        }
    }
}
