package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.NotificationIntervalOption
import com.example.model.SriLankaRegion
import com.example.ui.theme.AdaptiveBorderColor
import com.example.ui.theme.AdaptiveCyanAccent
import com.example.ui.theme.AdaptiveElevatedColor
import com.example.ui.theme.AdaptivePrimaryText
import com.example.ui.theme.AdaptiveSecondaryText
import com.example.ui.theme.AdaptiveSurfaceColor
import com.example.ui.theme.AdaptiveTealAccent
import com.example.ui.theme.MonsoonTeal
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResidingLocationDialog(
    regions: List<SriLankaRegion>,
    initialRegion: SriLankaRegion,
    initialCity: String,
    initialInterval: NotificationIntervalOption,
    wasPermissionDenied: Boolean,
    onDismiss: () -> Unit,
    onConfirmSelection: (SriLankaRegion, String, NotificationIntervalOption) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var chosenRegion by remember { mutableStateOf(initialRegion) }
    var chosenCity by remember {
        mutableStateOf(
            if (initialCity.isNotBlank() && initialRegion.nearestCities.contains(initialCity)) {
                initialCity
            } else {
                initialRegion.nearestCities.firstOrNull() ?: initialRegion.name
            }
        )
    }
    var chosenInterval by remember { mutableStateOf(initialInterval) }

    val matchingRegions = remember(regions, searchQuery) {
        if (searchQuery.isBlank()) {
            regions
        } else {
            regions.filter { reg ->
                reg.name.contains(searchQuery, ignoreCase = true) ||
                    reg.province.contains(searchQuery, ignoreCase = true) ||
                    reg.primaryRiverBasin.contains(searchQuery, ignoreCase = true) ||
                    reg.nearestCities.any { it.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    val globalMatchingCities = remember(regions, searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            regions.flatMap { reg ->
                reg.nearestCities
                    .filter { city ->
                        city.contains(searchQuery, ignoreCase = true) ||
                            reg.name.contains(searchQuery, ignoreCase = true)
                    }
                    .map { city -> reg to city }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = AdaptiveSurfaceColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp)
                .border(1.5.dp, RadarCyan.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                .testTag("residing_location_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                imageVector = if (wasPermissionDenied) Icons.Default.LocationOff else Icons.Default.LocationCity,
                                contentDescription = null,
                                tint = if (wasPermissionDenied) WarningAmber else AdaptiveCyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (wasPermissionDenied) {
                                    "LOCATION PERMISSION NOT GRANTED • MANUAL SETUP"
                                } else {
                                    "SET RESIDING DISTRICT, CITY & ALERT INTERVAL"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (wasPermissionDenied) WarningAmber else AdaptiveCyanAccent
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Select Residing Area & Notification Schedule",
                            style = MaterialTheme.typography.titleLarge,
                            color = AdaptivePrimaryText,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Search any of the 25 Sri Lankan districts, choose your nearest city/town, and pick how often notifications are sent to your mobile's notification hub.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AdaptiveSecondaryText
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_location_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = AdaptiveSecondaryText
                        )
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { query ->
                        searchQuery = query
                        val firstHit = regions.firstOrNull { reg ->
                            reg.name.contains(query, ignoreCase = true) ||
                                reg.nearestCities.any { it.contains(query, ignoreCase = true) }
                        }
                        if (firstHit != null) {
                            chosenRegion = firstHit
                            val matchedCity = firstHit.nearestCities.firstOrNull {
                                it.contains(query, ignoreCase = true)
                            } ?: firstHit.nearestCities.first()
                            chosenCity = matchedCity
                        }
                    },
                    placeholder = { Text("Search district or nearest city (e.g. Ratnapura, Kaduwela, Kitulgala)…") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = AdaptiveCyanAccent
                        )
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("district_city_search_input")
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    if (searchQuery.isNotBlank() && globalMatchingCities.isNotEmpty()) {
                        item {
                            Text(
                                text = "MATCHING CITIES & DISTRICTS (${globalMatchingCities.size})",
                                style = MaterialTheme.typography.labelLarge,
                                color = AdaptiveTealAccent
                            )
                        }
                        items(globalMatchingCities.take(10), key = { "${it.first.id}_${it.second}" }) { (reg, city) ->
                            val isSelected = chosenRegion.id == reg.id && chosenCity == city
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) RadarCyan.copy(alpha = 0.20f) else AdaptiveElevatedColor)
                                    .border(
                                        1.dp,
                                        if (isSelected) RadarCyan else AdaptiveBorderColor,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        chosenRegion = reg
                                        chosenCity = city
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "$city • ${reg.name} District",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = AdaptivePrimaryText,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${reg.province} • Basin: ${reg.primaryRiverBasin}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AdaptiveSecondaryText
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = AdaptiveCyanAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Step 1: Select District
                    item {
                        Text(
                            text = "1. SELECT DISTRICT IN SRI LANKA (${matchingRegions.size} DISTRICTS)",
                            style = MaterialTheme.typography.labelLarge,
                            color = AdaptiveCyanAccent
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(matchingRegions, key = { it.id }) { region ->
                                val isSelected = region.id == chosenRegion.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        chosenRegion = region
                                        chosenCity = region.nearestCities.firstOrNull() ?: region.name
                                    },
                                    label = {
                                        Text(
                                            text = region.name,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
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
                                        selectedContainerColor = RadarCyan.copy(alpha = 0.25f),
                                        selectedLabelColor = AdaptiveCyanAccent,
                                        selectedLeadingIconColor = AdaptiveCyanAccent
                                    ),
                                    modifier = Modifier.testTag("dialog_district_${region.id}")
                                )
                            }
                        }
                    }

                    // Step 2: Select Nearest City
                    item {
                        Text(
                            text = "2. SELECT NEAREST CITY / TOWN IN ${chosenRegion.name.uppercase()} DISTRICT",
                            style = MaterialTheme.typography.labelLarge,
                            color = AdaptiveCyanAccent
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            chosenRegion.nearestCities.forEach { city ->
                                val isCitySelected = city == chosenCity
                                FilterChip(
                                    selected = isCitySelected,
                                    onClick = { chosenCity = city },
                                    label = {
                                        Text(
                                            text = city,
                                            fontWeight = if (isCitySelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SafeEmerald.copy(alpha = 0.22f),
                                        selectedLabelColor = SafeEmerald
                                    ),
                                    modifier = Modifier.testTag("dialog_city_${city.lowercase().replace(" ", "_")}")
                                )
                            }
                        }
                    }

                    // Step 3: Select Notification Creation Time Interval
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = AdaptiveTealAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "3. MOBILE NOTIFICATION HUB INTERVAL",
                                style = MaterialTheme.typography.labelLarge,
                                color = AdaptiveTealAccent
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            NotificationIntervalOption.entries.forEach { option ->
                                val isOptionSelected = option == chosenInterval
                                FilterChip(
                                    selected = isOptionSelected,
                                    onClick = { chosenInterval = option },
                                    label = {
                                        Text(
                                            text = option.shortChipLabel,
                                            fontWeight = if (isOptionSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MonsoonTeal.copy(alpha = 0.24f),
                                        selectedLabelColor = AdaptiveTealAccent
                                    ),
                                    modifier = Modifier.testTag("dialog_interval_${option.id}")
                                )
                            }
                        }
                    }

                    // Selected Area Preview Box
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(AdaptiveElevatedColor)
                                .border(1.dp, SafeEmerald.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(SafeEmerald)
                                    )
                                    Text(
                                        text = "ACTIVE NOTIFICATION SCHEDULE (${chosenInterval.label.uppercase()})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SafeEmerald
                                    )
                                }
                                Text(
                                    text = "$chosenCity, ${chosenRegion.name} District (${chosenRegion.province})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = AdaptivePrimaryText,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Monitored River Basin: ${chosenRegion.primaryRiverBasin}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AdaptiveCyanAccent
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = { onConfirmSelection(chosenRegion, chosenCity, chosenInterval) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RadarCyan,
                        contentColor = Color(0xFF041222)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("confirm_residing_location_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Notify $chosenCity (${chosenInterval.shortChipLabel})",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
