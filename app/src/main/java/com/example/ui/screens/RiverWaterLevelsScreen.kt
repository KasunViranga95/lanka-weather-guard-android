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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flood
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.FloodStatus
import com.example.model.HydrographTimeRange
import com.example.model.RiverGaugeStation
import com.example.ui.components.HydrographTimeRangeSelector
import com.example.ui.components.RiverHydrographCurve
import com.example.ui.components.RiverThresholdBar
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

data class IndexedRiverBasinItem(
    val indexNumber: Int,
    val riverName: String,
    val basinCode: String,
    val stations: List<RiverGaugeStation>,
    val worstStatus: FloodStatus
)

private fun stationMatchesSearchQuery(station: RiverGaugeStation, rawQuery: String): Boolean {
    val q = rawQuery.trim()
    if (q.isEmpty()) return true
    return station.stationName.contains(q, ignoreCase = true) ||
        station.riverName.contains(q, ignoreCase = true) ||
        station.basinCode.contains(q, ignoreCase = true) ||
        station.district.contains(q, ignoreCase = true) ||
        (station.tributaryOf?.contains(q, ignoreCase = true) == true) ||
        station.floodStatus.label.contains(q, ignoreCase = true) ||
        station.downstreamTowns.any { it.contains(q, ignoreCase = true) }
}

@OptIn(ExperimentalLayoutApi::class)
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
    val orderedRiverNames = remember {
        listOf(
            "Kelani Ganga",
            "Kalu Ganga",
            "Gin Ganga",
            "Nilwala Ganga",
            "Mahaweli Ganga",
            "Maha Oya",
            "Attanagalu Oya",
            "Deduru Oya",
            "Bentara Ganga",
            "Walawe Ganga",
            "Kirindi Oya",
            "Menik Ganga",
            "Kumbukkan Oya",
            "Heda Oya",
            "Yan Oya",
            "Malwathu Oya"
        )
    }

    val trimmedQuery = searchQuery.trim()

    // Build Indexed River List (01 to 16) with their measuring locations
    val allIndexedRivers = remember(stations, orderedRiverNames) {
        orderedRiverNames.mapIndexed { idx, riverName ->
            val matchingStations = stations.filter {
                it.riverName.contains(riverName, ignoreCase = true)
            }
            val worst = matchingStations.maxByOrNull { it.floodStatus.severityRank }?.floodStatus
                ?: FloodStatus.NORMAL
            val code = matchingStations.firstOrNull()?.basinCode ?: String.format(Locale.US, "RB-%02d", idx + 1)
            IndexedRiverBasinItem(
                indexNumber = idx + 1,
                riverName = riverName,
                basinCode = code,
                stations = matchingStations,
                worstStatus = worst
            )
        }
    }

    // Filter Step 1 River Index by searchQuery so searching immediately highlights matching rivers
    val filteredIndexedRivers = remember(allIndexedRivers, trimmedQuery) {
        if (trimmedQuery.isEmpty()) {
            allIndexedRivers
        } else {
            allIndexedRivers.filter { item ->
                item.riverName.contains(trimmedQuery, ignoreCase = true) ||
                    item.basinCode.contains(trimmedQuery, ignoreCase = true) ||
                    String.format(Locale.US, "%02d", item.indexNumber) == trimmedQuery ||
                    item.stations.any { st -> stationMatchesSearchQuery(st, trimmedQuery) }
            }
        }
    }

    // Stations matching the currently selected river in Step 1 AND the search query
    val stationsForSelectedRiver = remember(stations, selectedBasinFilter, trimmedQuery) {
        val matchingByBasinAndQuery = stations.filter { station ->
            val matchesBasin = selectedBasinFilter == "All Basins" ||
                station.riverName.contains(selectedBasinFilter, ignoreCase = true)
            val matchesQuery = stationMatchesSearchQuery(station, trimmedQuery)
            matchesBasin && matchesQuery
        }

        // If the user typed a search query that matches stations in another river, show those matches
        val effectiveList = if (matchingByBasinAndQuery.isEmpty() && trimmedQuery.isNotEmpty()) {
            stations.filter { stationMatchesSearchQuery(it, trimmedQuery) }
        } else {
            matchingByBasinAndQuery
        }

        effectiveList.sortedByDescending {
            it.floodStatus.severityRank * 100 + (it.currentLevelM / it.majorFloodLevelM * 100).toInt()
        }
    }

    // Step 2: Currently selected measuring location within the chosen river / search result
    var selectedMeasuringStationId by remember(selectedBasinFilter, trimmedQuery) {
        mutableStateOf(stationsForSelectedRiver.firstOrNull()?.id)
    }

    val activeMeasuringStation = remember(stationsForSelectedRiver, selectedMeasuringStationId) {
        stationsForSelectedRiver.firstOrNull { it.id == selectedMeasuringStationId }
            ?: stationsForSelectedRiver.firstOrNull()
    }

    var showAllRiverIndexGrid by remember { mutableStateOf(true) }

    val majorCount = stations.count { it.floodStatus == FloodStatus.MAJOR_FLOOD }
    val minorCount = stations.count { it.floodStatus == FloodStatus.MINOR_FLOOD }
    val alertCount = stations.count { it.floodStatus == FloodStatus.ALERT }
    val normalCount = stations.count { it.floodStatus == FloodStatus.NORMAL }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("river_levels_list"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Summary Header & Working River Basin Search Bar
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(RadarCyan.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Flood,
                                    contentDescription = null,
                                    tint = AdaptiveCyanAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Sri Lanka River Index",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = AdaptivePrimaryText,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "16 River Basins • ${stations.size} Measuring Locations",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AdaptiveSecondaryText
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FloodSummaryPill(
                            count = majorCount,
                            label = "Major",
                            color = AlertRed,
                            modifier = Modifier.weight(1f)
                        )
                        FloodSummaryPill(
                            count = minorCount,
                            label = "Minor",
                            color = WarningAmber,
                            modifier = Modifier.weight(1f)
                        )
                        FloodSummaryPill(
                            count = alertCount,
                            label = "Alert",
                            color = AdvisoryYellow,
                            modifier = Modifier.weight(1f)
                        )
                        FloodSummaryPill(
                            count = normalCount,
                            label = "Normal",
                            color = SafeEmerald,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { newQuery ->
                            onSearchQueryChanged(newQuery)
                        },
                        placeholder = { Text(stringResource(R.string.search_rivers_hint)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = AdaptiveCyanAccent
                            )
                        },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(
                                    onClick = {
                                        onSearchQueryChanged("")
                                        onBasinFilterSelected("All Basins")
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search"
                                    )
                                }
                            }
                        } else null,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_rivers_input")
                    )

                    if (trimmedQuery.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Found ${filteredIndexedRivers.size} ${if (filteredIndexedRivers.size == 1) "river" else "rivers"} & ${stationsForSelectedRiver.size} ${if (stationsForSelectedRiver.size == 1) "station" else "stations"} for \"$trimmedQuery\"",
                                style = MaterialTheme.typography.labelMedium,
                                color = AdaptiveCyanAccent,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                color = RadarCyan.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable {
                                    onSearchQueryChanged("")
                                    onBasinFilterSelected("All Basins")
                                }
                            ) {
                                Text(
                                    text = "Reset",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AdaptiveCyanAccent,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. STEP 1: Indexed Rivers Navigation Card (01 to 16) — without any duplicate horizontal rotatable sub-menu
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = AdaptiveSurfaceColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AdaptiveBorderColor, RoundedCornerShape(14.dp))
                    .testTag("river_index_directory_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAllRiverIndexGrid = !showAllRiverIndexGrid },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Step 1: Select River from Index (01–16)",
                                style = MaterialTheme.typography.titleSmall,
                                color = AdaptiveCyanAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (selectedBasinFilter == "All Basins") {
                                    "Showing ${filteredIndexedRivers.size} indexed rivers • Tap any river tile below"
                                } else {
                                    "Selected River: $selectedBasinFilter (${stationsForSelectedRiver.size} measuring locations)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = AdaptiveSecondaryText
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (selectedBasinFilter != "All Basins") {
                                Surface(
                                    color = RadarCyan.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .clickable { onBasinFilterSelected("All Basins") }
                                        .testTag("basin_chip_all_basins")
                                ) {
                                    Text(
                                        text = "Show All 16",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AdaptiveCyanAccent,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            Icon(
                                imageVector = if (showAllRiverIndexGrid) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = AdaptiveCyanAccent
                            )
                        }
                    }

                    // 2-Column Numbered River Index Grid (01 to 16)
                    AnimatedVisibility(visible = showAllRiverIndexGrid) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            HorizontalDivider(color = AdaptiveBorderColor)
                            if (filteredIndexedRivers.isEmpty()) {
                                Text(
                                    text = "No river basin matches \"$trimmedQuery\". Tap Reset above to view all 16 rivers.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AdaptiveSecondaryText,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            } else {
                                filteredIndexedRivers.chunked(2).forEach { pair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        pair.forEach { item ->
                                            val isRiverSelected = selectedBasinFilter.equals(item.riverName, ignoreCase = true)
                                            Surface(
                                                color = if (isRiverSelected) {
                                                    RadarCyan.copy(alpha = 0.16f)
                                                } else {
                                                    AdaptiveElevatedColor
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .border(
                                                        width = if (isRiverSelected) 1.5.dp else 1.dp,
                                                        color = if (isRiverSelected) AdaptiveCyanAccent else AdaptiveBorderColor,
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable {
                                                        if (isRiverSelected) {
                                                            onBasinFilterSelected("All Basins")
                                                        } else {
                                                            onBasinFilterSelected(item.riverName)
                                                            if (trimmedQuery.isNotEmpty() &&
                                                                !item.riverName.contains(trimmedQuery, ignoreCase = true) &&
                                                                item.stations.none { stationMatchesSearchQuery(it, trimmedQuery) }
                                                            ) {
                                                                onSearchQueryChanged("")
                                                            }
                                                        }
                                                    }
                                                    .testTag("river_index_tile_${item.indexNumber}")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = String.format(
                                                                Locale.US,
                                                                "%02d. %s",
                                                                item.indexNumber,
                                                                item.riverName
                                                            ),
                                                            style = MaterialTheme.typography.labelLarge,
                                                            color = if (isRiverSelected) AdaptiveCyanAccent else AdaptivePrimaryText,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "${item.stations.size} ${if (item.stations.size == 1) "station" else "stations"} • ${item.basinCode}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = AdaptiveSecondaryText,
                                                            maxLines = 1
                                                        )
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(9.dp)
                                                            .clip(CircleShape)
                                                            .background(item.worstStatus.color)
                                                    )
                                                }
                                            }
                                        }
                                        if (pair.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. STEP 2: Select Relevant Measuring Location (Gauge Station) for the Selected River
        if (stationsForSelectedRiver.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AdaptiveSurfaceColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AdaptiveCyanAccent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .testTag("measuring_location_selector_card")
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
                                    text = "Step 2: Select Measuring Location (${stationsForSelectedRiver.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = AdaptiveCyanAccent,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (selectedBasinFilter == "All Basins") {
                                        "Tap a measuring station below to inspect its water level & historical flood graph"
                                    } else {
                                        "Measuring locations along $selectedBasinFilter:"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AdaptiveSecondaryText
                                )
                            }
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            stationsForSelectedRiver.forEachIndexed { index, station ->
                                val isSelected = station.id == activeMeasuringStation?.id
                                val statusColor = station.floodStatus.color
                                Surface(
                                    color = if (isSelected) {
                                        RadarCyan.copy(alpha = 0.18f)
                                    } else {
                                        AdaptiveElevatedColor
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) AdaptiveCyanAccent else statusColor.copy(alpha = 0.45f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            selectedMeasuringStationId = station.id
                                        }
                                        .testTag("select_measuring_station_${station.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(statusColor)
                                        )
                                        Text(
                                            text = "${index + 1}. ${station.stationName} (${String.format(Locale.US, "%.2fm", station.currentLevelM)})",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isSelected) AdaptiveCyanAccent else AdaptivePrimaryText,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Selected",
                                                tint = AdaptiveCyanAccent,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AdaptiveSurfaceColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AdaptiveBorderColor, RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No river basin or measuring station matches \"$searchQuery\"",
                            style = MaterialTheme.typography.titleSmall,
                            color = AdaptivePrimaryText,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Try searching by river name (e.g., Kelani, Kalu, Nilwala, Mahaweli), station name (e.g., Hanwella, Ratnapura), or district.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AdaptiveSecondaryText
                        )
                        Button(
                            onClick = {
                                onSearchQueryChanged("")
                                onBasinFilterSelected("All Basins")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RadarCyan,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Show All 16 Rivers")
                        }
                    }
                }
            }
        }

        // 4. Featured Selected Measuring Location Detail Card (Immediate inspection)
        if (activeMeasuringStation != null) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = AdaptiveCyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Selected Measuring Location: ${activeMeasuringStation.stationName} (${activeMeasuringStation.riverName})",
                            style = MaterialTheme.typography.titleSmall,
                            color = AdaptivePrimaryText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    RiverGaugeStationCard(
                        station = activeMeasuringStation,
                        globalTimeRange = selectedHydrographTimeRange,
                        onGlobalTimeRangeSelected = onHydrographTimeRangeSelected,
                        initiallyExpanded = true
                    )
                }
            }
        }

        // 5. Other Measuring Locations in the Current Filter
        val remainingStations = stationsForSelectedRiver.filter { it.id != activeMeasuringStation?.id }
        if (remainingStations.isNotEmpty()) {
            item {
                Text(
                    text = "Other Measuring Locations (${remainingStations.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = AdaptiveSecondaryText,
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(remainingStations, key = { it.id }) { station ->
                RiverGaugeStationCard(
                    station = station,
                    globalTimeRange = selectedHydrographTimeRange,
                    onGlobalTimeRangeSelected = onHydrographTimeRangeSelected,
                    initiallyExpanded = false
                )
            }
        }
    }
}

@Composable
private fun FloodSummaryPill(
    count: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp)
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AdaptivePrimaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RiverGaugeStationCard(
    station: RiverGaugeStation,
    globalTimeRange: HydrographTimeRange,
    onGlobalTimeRangeSelected: (HydrographTimeRange) -> Unit,
    initiallyExpanded: Boolean = false
) {
    var expanded by remember(station.id, initiallyExpanded) {
        mutableStateOf(initiallyExpanded || station.floodStatus == FloodStatus.MAJOR_FLOOD)
    }
    var localRangeOverride by remember(globalTimeRange) { mutableStateOf(globalTimeRange) }
    val statusColor = station.floodStatus.color
    val deltaSign = if (station.deltaM >= 0) "+" else ""

    val rainScale = (station.catchmentRainMm24h / 250.0).toFloat().coerceIn(0.06f, 1.0f)
    val rainAccent = when {
        station.catchmentRainMm24h >= 150.0 -> AlertRed
        station.catchmentRainMm24h >= 100.0 -> WarningAmber
        else -> AdaptiveCyanAccent
    }
    val rainSeverityLabel = when {
        station.catchmentRainMm24h >= 150.0 -> "Extreme (>150mm)"
        station.catchmentRainMm24h >= 100.0 -> "Heavy (>100mm)"
        station.catchmentRainMm24h >= 50.0 -> "Moderate (50–100mm)"
        else -> "Light (<50mm)"
    }

    val flowScale = (station.dischargeM3s / 2000.0).toFloat().coerceIn(0.06f, 1.0f)
    val flowAccent = when {
        station.dischargeM3s >= 1200.0 -> AlertRed
        station.dischargeM3s >= 700.0 -> WarningAmber
        else -> AdaptiveTealAccent
    }
    val flowSeverityLabel = when {
        station.dischargeM3s >= 1200.0 -> "Peak Flood Surge"
        station.dischargeM3s >= 700.0 -> "High Basin Flow"
        else -> "Normal Discharge"
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AdaptiveSurfaceColor),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (station.floodStatus.severityRank >= 3) 1.5.dp else 1.dp,
                color = if (station.floodStatus.severityRank >= 3) statusColor.copy(alpha = 0.7f) else AdaptiveBorderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { expanded = !expanded }
            .testTag("river_station_card_${station.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Water,
                            contentDescription = null,
                            tint = AdaptiveCyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = buildString {
                                append("${station.riverName} • ${station.basinCode}")
                                if (station.tributaryOf != null) {
                                    append(" (${station.tributaryOf})")
                                }
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = AdaptiveCyanAccent,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = station.stationName,
                        style = MaterialTheme.typography.titleLarge,
                        color = AdaptivePrimaryText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${station.district} District • ${station.lastUpdatedText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AdaptiveSecondaryText
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = station.floodStatus.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Dedicated Water Level & Hourly Trend Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AdaptiveElevatedColor)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Water Level",
                        style = MaterialTheme.typography.labelSmall,
                        color = AdaptiveSecondaryText
                    )
                    Text(
                        text = String.format(Locale.US, "%.2f m", station.currentLevelM),
                        style = MaterialTheme.typography.headlineMedium,
                        color = AdaptivePrimaryText,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = (if (station.deltaM > 0.03) WarningAmber else SafeEmerald).copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${station.trend.symbol} $deltaSign${String.format(Locale.US, "%.2f m/hr", station.deltaM)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (station.deltaM > 0.03) WarningAmber else SafeEmerald,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = station.trend.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = AdaptiveSecondaryText
                        )
                    }
                }
            }

            // Scaled 2-Column Catchment Rain (0-250mm) & River Flow (0-2000 m³/s) Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ScaledRiverMetricBox(
                    icon = Icons.Default.WaterDrop,
                    title = "Catchment Rain (24h)",
                    valueText = "${station.catchmentRainMm24h.roundToInt()} mm",
                    scaleFraction = rainScale,
                    scaleRangeText = "Scale: 0–250 mm",
                    statusText = rainSeverityLabel,
                    accentColor = rainAccent,
                    modifier = Modifier.weight(1f)
                )

                ScaledRiverMetricBox(
                    icon = Icons.Default.Speed,
                    title = "River Flow Rate",
                    valueText = "${station.dischargeM3s.roundToInt()} m³/s",
                    scaleFraction = flowScale,
                    scaleRangeText = "Scale: 0–2000 m³/s",
                    statusText = flowSeverityLabel,
                    accentColor = flowAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            // Water Level Threshold Bar (Alert / Minor / Major)
            RiverThresholdBar(station = station)

            // Expandable Historical Hydrograph & Downstream At-Risk Towns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Downstream: ${station.downstreamTowns.take(3).joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AdaptiveSecondaryText,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (expanded) "Hide Graph" else "Show Graph",
                        style = MaterialTheme.typography.labelSmall,
                        color = AdaptiveCyanAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Hide hydrograph" else "Show hydrograph",
                        tint = AdaptiveCyanAccent
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AdaptiveElevatedColor)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Historical Water Level & Flood Threshold Graph (${localRangeOverride.fullTitle})",
                        style = MaterialTheme.typography.titleSmall,
                        color = AdaptiveCyanAccent,
                        fontWeight = FontWeight.Bold
                    )

                    HydrographTimeRangeSelector(
                        selectedRange = localRangeOverride,
                        onRangeSelected = { range ->
                            localRangeOverride = range
                            onGlobalTimeRangeSelected(range)
                        },
                        stationIdTagPrefix = station.id
                    )

                    RiverHydrographCurve(
                        station = station,
                        timeRange = localRangeOverride
                    )

                    Text(
                        text = "Downstream Watch Zones: ${station.downstreamTowns.joinToString(" • ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AdaptivePrimaryText
                    )
                }
            }
        }
    }
}

@Composable
private fun ScaledRiverMetricBox(
    icon: ImageVector,
    title: String,
    valueText: String,
    scaleFraction: Float,
    scaleRangeText: String,
    statusText: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AdaptiveElevatedColor)
            .border(1.dp, accentColor.copy(alpha = 0.30f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
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
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = AdaptiveSecondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = valueText,
            style = MaterialTheme.typography.titleLarge,
            color = AdaptivePrimaryText,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        LinearProgressIndicator(
            progress = { scaleFraction },
            color = accentColor,
            trackColor = AdaptiveSurfaceColor,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = scaleRangeText,
                style = MaterialTheme.typography.labelSmall,
                color = AdaptiveSecondaryText,
                maxLines = 1
            )
        }
    }
}
