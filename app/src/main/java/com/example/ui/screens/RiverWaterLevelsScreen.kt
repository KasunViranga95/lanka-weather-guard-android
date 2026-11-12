package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.FloodStatus
import com.example.model.GaugeFeed
import com.example.model.Reservoir
import com.example.model.ReservoirStatus
import com.example.model.HydrographTimeRange
import com.example.model.RiverGaugeStation
import com.example.ui.components.HydrographTimeRangeSelector
import com.example.ui.components.RiverHydrographCurve
import com.example.ui.components.RiverThresholdBar
import com.example.ui.components.StatusPill
import com.example.ui.components.trendIcon
import com.example.ui.theme.AdaptiveBorderColor
import com.example.ui.theme.AdaptiveCyanAccent
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.AdaptiveElevatedColor
import com.example.ui.theme.AdaptivePrimaryText
import com.example.ui.theme.AdaptiveSecondaryText
import com.example.ui.theme.AdaptiveSurfaceColor
import com.example.util.WeatherFormat
import java.util.Locale
import kotlin.math.roundToInt

private enum class RiverStatusFilter(val labelRes: Int) {
    ALL(R.string.filter_all),
    FLOODING(R.string.filter_flooding),
    WATCH(R.string.filter_near_alert),
    NORMAL(R.string.filter_normal);

    fun matches(station: RiverGaugeStation): Boolean = when (this) {
        ALL -> true
        FLOODING -> station.floodStatus == FloodStatus.MINOR_FLOOD || station.floodStatus == FloodStatus.MAJOR_FLOOD
        WATCH -> station.floodStatus == FloodStatus.ALERT
        NORMAL -> station.hasLiveReading && station.floodStatus == FloodStatus.NORMAL
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
 * Rivers with flooding gauges come first; tap a gauge for its flood levels and water-level graph.
 */
@Composable
fun RiverWaterLevelsScreen(
    stations: List<RiverGaugeStation>,
    reservoirs: List<Reservoir>,
    selectedBasinFilter: String,
    searchQuery: String,
    selectedHydrographTimeRange: HydrographTimeRange,
    onBasinFilterSelected: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onHydrographTimeRangeSelected: (HydrographTimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    var statusFilter by rememberSaveable { mutableStateOf(RiverStatusFilter.ALL) }
    var reservoirFilter by rememberSaveable { mutableStateOf(ReservoirFilter.ALL) }
    var showReservoirs by rememberSaveable { mutableStateOf(false) }
    var expandedReservoirId by rememberSaveable { mutableStateOf<String?>(null) }
    val gaugedRivers = remember(stations) { stations.map { it.riverName }.toSet() }
    var expandedStationId by rememberSaveable { mutableStateOf<String?>(null) }
    val res = LocalContext.current.resources

    val riverGroups = remember(stations, selectedBasinFilter, searchQuery, statusFilter) {
        stations
            .filter { selectedBasinFilter.isEmpty() || it.riverName.contains(selectedBasinFilter, ignoreCase = true) }
            .filter { stationMatchesSearchQuery(it, searchQuery) }
            .filter { statusFilter.matches(it) }
            .groupBy { it.riverName }
            .map { (river, list) ->
                river to list.sortedWith(compareByDescending<RiverGaugeStation> { it.floodStatus.severityRank }.thenByDescending { it.hasLiveReading })
            }
            .sortedWith(
                compareByDescending<Pair<String, List<RiverGaugeStation>>> { (_, list) ->
                    list.maxOf { it.floodStatus.severityRank }
                }.thenBy { it.first }
            )
    }
    val liveCount = stations.count { it.hasLiveReading }
    val latestReading = stations.mapNotNull { it.readingTimeMillis }.maxOrNull()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("river_levels_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(false to R.string.view_rivers, true to R.string.view_reservoirs).forEachIndexed { idx, (reservoirsView, label) ->
                    SegmentedButton(
                        selected = showReservoirs == reservoirsView,
                        onClick = { showReservoirs = reservoirsView },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = 2),
                        modifier = Modifier.testTag(if (reservoirsView) "view_reservoirs" else "view_rivers")
                    ) { Text(stringResource(label)) }
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text(stringResource(if (showReservoirs) R.string.search_reservoirs_hint else R.string.search_rivers_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear_search))
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

        if (showReservoirs) {
            reservoirItems(
                reservoirs = reservoirs,
                searchQuery = searchQuery,
                filter = reservoirFilter,
                onFilterChange = { reservoirFilter = it },
                gaugedRivers = gaugedRivers,
                expandedId = expandedReservoirId,
                onToggle = { id -> expandedReservoirId = if (expandedReservoirId == id) null else id },
                onShowRiverGauges = { river ->
                    // Jump to the Rivers view showing only that river's gauges
                    onSearchQueryChanged("")
                    onBasinFilterSelected(river)
                    statusFilter = RiverStatusFilter.ALL
                    showReservoirs = false
                }
            )
            return@LazyColumn
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (selectedBasinFilter.isNotEmpty()) {
                    item {
                        InputChip(
                            selected = true,
                            onClick = { onBasinFilterSelected("") },
                            label = { Text(selectedBasinFilter) },
                            trailingIcon = {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.show_all_rivers), modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
                items(RiverStatusFilter.entries.toList(), key = { it.name }) { filter ->
                    val count = stations.count { filter.matches(it) }
                    FilterChip(
                        selected = statusFilter == filter,
                        onClick = { statusFilter = filter },
                        label = { Text("${stringResource(filter.labelRes)} ($count)") },
                        modifier = Modifier.testTag("river_filter_${filter.name.lowercase(Locale.US)}")
                    )
                }
            }
        }

        item {
            Text(
                text = when {
                    liveCount == 0 -> stringResource(R.string.rivers_loading_or_offline)
                    latestReading != null -> stringResource(R.string.rivers_summary, liveCount, stations.size, WeatherFormat.time(res, latestReading))
                    else -> stringResource(R.string.rivers_summary_no_time, liveCount, stations.size)
                },
                style = MaterialTheme.typography.labelMedium,
                color = AdaptiveSecondaryText
            )
        }

        if (riverGroups.isEmpty()) {
            item {
                Text(
                    text = if (searchQuery.isBlank()) stringResource(R.string.no_gauges_in_category)
                    else stringResource(R.string.no_gauge_matches, searchQuery.trim()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AdaptiveSecondaryText,
                    modifier = Modifier.padding(vertical = 24.dp)
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
            fontWeight = FontWeight.SemiBold,
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
    val live = station.hasLiveReading
    val res = LocalContext.current.resources
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
                    .background(if (live) status.color else AdaptiveBorderColor)
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
                    text = "${station.district} · ${stringResource(if (live) status.labelRes else R.string.no_recent_reading)}" +
                        if (live && station.marksEstimated && status != FloodStatus.NORMAL) " (${stringResource(R.string.estimated_levels_short)})" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (!live || status == FloodStatus.NORMAL) AdaptiveSecondaryText else status.color
                )
            }
            if (live) {
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = trendIcon(station.trend),
                            contentDescription = stringResource(station.trend.labelRes),
                            tint = AdaptiveSecondaryText,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = formatLevel(station.currentLevelM),
                            style = MaterialTheme.typography.titleMedium,
                            color = AdaptivePrimaryText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    station.publishedLevelText?.let {
                        Text("($it)", style = MaterialTheme.typography.labelSmall, color = AdaptiveSecondaryText)
                    }
                }
            } else {
                Text("—", style = MaterialTheme.typography.titleMedium, color = AdaptiveSecondaryText)
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = stringResource(if (expanded) R.string.hide_details else R.string.show_details),
                tint = AdaptiveSecondaryText
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (live) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatusPill(text = stringResource(status.labelRes), color = status.color)
                        Text(
                            text = stringResource(
                                if (station.feed == GaugeFeed.IRRIGATION) R.string.change_last_hour else R.string.change_per_hour_3h,
                                stringResource(station.trend.labelRes),
                                String.format(Locale.US, "%+.2f", station.deltaM)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = AdaptiveSecondaryText
                        )
                    }

                    RiverThresholdBar(station = station)

                    if (station.marksEstimated) {
                        Text(stringResource(R.string.estimated_levels_note), style = MaterialTheme.typography.bodySmall, color = AdaptiveSecondaryText)
                    }

                    if (station.sourceUnit == "ft") {
                        Text(stringResource(R.string.gauge_in_feet_note), style = MaterialTheme.typography.bodySmall, color = AdaptiveSecondaryText)
                    }

                    if (station.history24hM.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(AdaptiveElevatedColor)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            HydrographTimeRangeSelector(
                                selectedRange = timeRange,
                                onRangeSelected = onTimeRangeSelected,
                                stationIdTagPrefix = station.id
                            )
                            RiverHydrographCurve(station = station, timeRange = timeRange)
                        }
                    } else {
                        Text(stringResource(R.string.no_graph_latest_only), style = MaterialTheme.typography.bodySmall, color = AdaptiveSecondaryText)
                    }

                    Text(
                        text = if (station.rainMeasured) {
                            stringResource(R.string.catchment_rain_flow, station.catchmentRainMm24h.roundToInt(), station.dischargeM3s.roundToInt())
                        } else {
                            stringResource(R.string.river_flow_only, station.dischargeM3s.roundToInt())
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = AdaptivePrimaryText
                    )
                }

                AffectedAreas(station)
                GaugePlaceLinks(station)

                station.readingTimeMillis?.let {
                    Text(
                        text = stringResource(R.string.gauge_read_at, WeatherFormat.time(res, it)) + " · " +
                            stringResource(R.string.gauge_source, stringResource(station.feed.labelRes)),
                        style = MaterialTheme.typography.labelSmall,
                        color = AdaptiveSecondaryText
                    )
                }
            }
        }
    }
}

/** Towns below the gauge that the river reaches; called out in the gauge's colour when the river is high. */
@Composable
private fun AffectedAreas(station: RiverGaugeStation) {
    if (station.downstreamTowns.isEmpty()) return
    val high = station.hasLiveReading && station.floodStatus != FloodStatus.NORMAL
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (high) station.floodStatus.color.copy(alpha = 0.12f) else AdaptiveElevatedColor)
            .padding(12.dp)
            .testTag("affected_areas_${station.id}"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.areas_may_flood),
            style = MaterialTheme.typography.labelLarge,
            color = if (high) station.floodStatus.color else AdaptivePrimaryText,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(if (high) R.string.areas_at_risk_now else R.string.areas_may_flood_note),
            style = MaterialTheme.typography.bodySmall,
            color = AdaptiveSecondaryText
        )
        Text(
            text = station.downstreamTowns.joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = AdaptivePrimaryText,
            fontWeight = if (high) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/** Opens the gauge's spot in the maps app, and its page on waterlevels.lk. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun GaugePlaceLinks(station: RiverGaugeStation) {
    val open = rememberSafeUriOpener()
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { open(mapsUrlForPoint(station.latitude, station.longitude)) },
            modifier = Modifier.testTag("gauge_map_${station.id}")
        ) {
            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.show_on_map), modifier = Modifier.padding(start = 8.dp))
        }
        station.infoUrl?.let { url ->
            OutlinedButton(onClick = { open(url) }, modifier = Modifier.testTag("gauge_info_${station.id}")) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.more_info), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

private fun mapsUrlForPoint(latitude: Double, longitude: Double): String =
    String.format(Locale.US, "https://www.google.com/maps/search/?api=1&query=%.5f,%.5f", latitude, longitude)

private fun mapsUrlForSearch(query: String): String =
    "https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query)

/** Opens a link in the browser or maps app; does nothing if the phone has no app for it. */
@Composable
private fun rememberSafeUriOpener(): (String) -> Unit {
    val uriHandler = LocalUriHandler.current
    return remember(uriHandler) { { url: String -> runCatching { uriHandler.openUri(url) } } }
}

private enum class ReservoirFilter(val labelRes: Int) {
    ALL(R.string.filter_all),
    LETTING_OUT(R.string.reservoir_filter_out),
    NEARLY_FULL(R.string.reservoir_nearly_full);

    fun matches(reservoir: Reservoir): Boolean = when (this) {
        ALL -> true
        LETTING_OUT -> reservoir.status == ReservoirStatus.SPILLING || reservoir.status == ReservoirStatus.RELEASING
        NEARLY_FULL -> reservoir.isNearlyFull
    }
}

private fun reservoirMatchesSearchQuery(reservoir: Reservoir, rawQuery: String): Boolean {
    val q = rawQuery.trim()
    if (q.isEmpty()) return true
    return reservoir.name.contains(q, ignoreCase = true) ||
        (reservoir.river?.contains(q, ignoreCase = true) == true) ||
        (reservoir.district?.contains(q, ignoreCase = true) == true)
}

/** Reservoir view of the Rivers tab: filters, then one card per status, those letting water out first. */
private fun LazyListScope.reservoirItems(
    reservoirs: List<Reservoir>,
    searchQuery: String,
    filter: ReservoirFilter,
    onFilterChange: (ReservoirFilter) -> Unit,
    gaugedRivers: Set<String>,
    expandedId: String?,
    onToggle: (String) -> Unit,
    onShowRiverGauges: (String) -> Unit
) {
    item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ReservoirFilter.entries.toList(), key = { it.name }) { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { onFilterChange(f) },
                    label = { Text("${stringResource(f.labelRes)} (${reservoirs.count { f.matches(it) }})") },
                    modifier = Modifier.testTag("reservoir_filter_${f.name.lowercase(Locale.US)}")
                )
            }
        }
    }
    item {
        Text(
            text = if (reservoirs.isEmpty()) stringResource(R.string.reservoirs_unavailable)
            else stringResource(R.string.reservoirs_summary, reservoirs.size, reservoirs.count { ReservoirFilter.LETTING_OUT.matches(it) }),
            style = MaterialTheme.typography.labelMedium,
            color = AdaptiveSecondaryText
        )
    }
    val groups = reservoirs
        .filter { filter.matches(it) && reservoirMatchesSearchQuery(it, searchQuery) }
        .groupBy { it.status }
        .toList()
        .sortedByDescending { it.first.rank }
        .map { (status, list) ->
            status to list.sortedWith(compareByDescending<Reservoir> { it.flowCusecs ?: -1.0 }.thenByDescending { it.storagePercent ?: -1.0 }.thenBy { it.name })
        }
    if (reservoirs.isNotEmpty() && groups.isEmpty()) {
        item {
            Text(
                text = if (searchQuery.isBlank()) stringResource(R.string.no_reservoirs_in_category)
                else stringResource(R.string.no_reservoir_matches, searchQuery.trim()),
                style = MaterialTheme.typography.bodyMedium,
                color = AdaptiveSecondaryText,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        }
    }
    items(groups, key = { "reservoirs_${it.first.name}" }) { (status, list) ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(AdaptiveSurfaceColor)
                .testTag("reservoir_group_${status.name.lowercase(Locale.US)}")
        ) {
            Text(
                text = "${stringResource(status.labelRes)} (${list.size})",
                style = MaterialTheme.typography.titleMedium,
                color = AdaptivePrimaryText,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp)
            )
            list.forEachIndexed { idx, reservoir ->
                if (idx > 0) HorizontalDivider(color = AdaptiveBorderColor, modifier = Modifier.padding(horizontal = 16.dp))
                ReservoirRow(
                    reservoir = reservoir,
                    expanded = expandedId == reservoir.id,
                    onToggle = { onToggle(reservoir.id) },
                    riverHasGauges = reservoir.river in gaugedRivers,
                    onShowRiverGauges = onShowRiverGauges
                )
            }
        }
    }
    if (reservoirs.isNotEmpty()) {
        item {
            Text(
                text = stringResource(R.string.reservoir_note, Reservoir.NEARLY_FULL_PERCENT.roundToInt()),
                style = MaterialTheme.typography.bodySmall,
                color = AdaptiveSecondaryText
            )
        }
    }
}

@Composable
private fun ReservoirRow(
    reservoir: Reservoir,
    expanded: Boolean,
    onToggle: () -> Unit,
    riverHasGauges: Boolean,
    onShowRiverGauges: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("reservoir_${reservoir.id}"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(reservoir.status.color)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reservoir.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AdaptivePrimaryText,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val place = listOfNotNull(reservoir.district, reservoir.river).joinToString(" · ")
                if (place.isNotEmpty()) {
                    Text(place, style = MaterialTheme.typography.bodySmall, color = AdaptiveSecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            reservoir.storagePercent?.let { pct ->
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.reservoir_full_pct, pct.roundToInt()),
                        style = MaterialTheme.typography.titleSmall,
                        color = AdaptivePrimaryText,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (reservoir.isNearlyFull) {
                        Text(stringResource(R.string.reservoir_nearly_full), style = MaterialTheme.typography.labelSmall, color = WarningAmber)
                    }
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = stringResource(if (expanded) R.string.hide_details else R.string.show_details),
                tint = AdaptiveSecondaryText
            )
        }
        reservoir.storagePercent?.let { pct ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(AdaptiveElevatedColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((pct / 100.0).toFloat().coerceIn(0f, 1f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (reservoir.isNearlyFull) WarningAmber else AdaptiveCyanAccent)
                )
            }
        }
        val cusecs = reservoir.flowCusecs
        val m3s = reservoir.flowM3s
        if (cusecs != null && m3s != null) {
            val flowRes = when (reservoir.flowKind) {
                "spill" -> R.string.reservoir_flow_spill
                "outlets" -> R.string.reservoir_flow_outlets
                else -> R.string.reservoir_flow_downstream
            }
            Text(
                text = stringResource(flowRes, String.format(Locale.US, "%,.0f", cusecs), String.format(Locale.US, if (m3s < 10) "%.1f" else "%,.0f", m3s)),
                style = MaterialTheme.typography.bodySmall,
                color = AdaptivePrimaryText
            )
        }
        if (reservoir.readingDay != null && reservoir.agency != null) {
            Text(
                text = stringResource(R.string.reservoir_report, reservoir.readingDay, reservoir.agency),
                style = MaterialTheme.typography.labelSmall,
                color = AdaptiveSecondaryText
            )
        }
        AnimatedVisibility(visible = expanded) {
            ReservoirDetails(reservoir, riverHasGauges, onShowRiverGauges)
        }
    }
}

/** Where the reservoir is, where its water goes, and its page on waterlevels.lk. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ReservoirDetails(reservoir: Reservoir, riverHasGauges: Boolean, onShowRiverGauges: (String) -> Unit) {
    val open = rememberSafeUriOpener()
    val lettingOut = reservoir.status == ReservoirStatus.SPILLING || reservoir.status == ReservoirStatus.RELEASING
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
        reservoir.river?.let { river ->
            Text(
                text = stringResource(R.string.reservoir_downstream_note, river),
                style = MaterialTheme.typography.bodySmall,
                color = if (lettingOut) reservoir.status.color else AdaptivePrimaryText,
                fontWeight = if (lettingOut) FontWeight.SemiBold else FontWeight.Normal
            )
        }
        if (reservoir.infoUrl != null) {
            Text(stringResource(R.string.reservoir_more_info_note), style = MaterialTheme.typography.bodySmall, color = AdaptiveSecondaryText)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { open(mapsUrlForSearch(listOfNotNull("${reservoir.name} reservoir", reservoir.district, "Sri Lanka").joinToString(", "))) },
                modifier = Modifier.testTag("reservoir_map_${reservoir.id}")
            ) {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.show_on_map), modifier = Modifier.padding(start = 8.dp))
            }
            reservoir.infoUrl?.let { url ->
                OutlinedButton(onClick = { open(url) }, modifier = Modifier.testTag("reservoir_info_${reservoir.id}")) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.more_info), modifier = Modifier.padding(start = 8.dp))
                }
            }
            val river = reservoir.river
            if (river != null && riverHasGauges) {
                OutlinedButton(onClick = { onShowRiverGauges(river) }, modifier = Modifier.testTag("reservoir_river_${reservoir.id}")) {
                    Icon(Icons.Default.Water, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.see_river_gauges, river), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}
