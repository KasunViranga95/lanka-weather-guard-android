package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.model.SriLankaRegion
import com.example.ui.theme.AdaptiveCyanAccent
import com.example.ui.theme.AdaptiveElevatedColor
import com.example.ui.theme.AdaptivePrimaryText
import com.example.ui.theme.AdaptiveSecondaryText
import com.example.ui.theme.AdaptiveSurfaceColor
import com.example.ui.theme.WarningAmber

/** Search for a town, or pick a district and then a town in it. The choice is added to saved places. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlacePickerDialog(
    regions: List<SriLankaRegion>,
    initialRegion: SriLankaRegion,
    initialCity: String,
    wasPermissionDenied: Boolean,
    onUseMyLocation: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (SriLankaRegion, String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var chosenRegion by remember { mutableStateOf(initialRegion) }
    var chosenCity by remember {
        mutableStateOf(initialCity.takeIf { initialRegion.nearestCities.contains(it) } ?: initialRegion.nearestCities.firstOrNull() ?: initialRegion.name)
    }
    val matches = remember(regions, query) {
        val q = query.trim()
        if (q.isEmpty()) emptyList() else regions.flatMap { reg ->
            reg.nearestCities
                .filter { it.contains(q, ignoreCase = true) || reg.name.contains(q, ignoreCase = true) }
                .map { reg to it }
        }
            // "Ella" should list Ella before Wellampitiya: exact names, then names starting with it, then the rest
            .sortedBy { (_, city) ->
                val name = city.lowercase().removeSuffix(" town").removeSuffix(" city")
                when {
                    name == q.lowercase() -> 0
                    name.startsWith(q.lowercase()) -> 1
                    else -> 2
                }
            }
            .take(12)
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = AdaptiveSurfaceColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .testTag("residing_location_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.choose_place),
                        style = MaterialTheme.typography.titleLarge,
                        color = AdaptivePrimaryText,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_location_dialog_button")) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = AdaptiveSecondaryText)
                    }
                }
                if (wasPermissionDenied) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.LocationOff, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.location_not_available), style = MaterialTheme.typography.bodySmall, color = AdaptiveSecondaryText)
                    }
                } else {
                    OutlinedButton(onClick = onUseMyLocation, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.use_my_location), modifier = Modifier.padding(start = 8.dp))
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.search_town_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear_search))
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("district_city_search_input")
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    if (query.isNotBlank()) {
                        if (matches.isEmpty()) {
                            item { Text(stringResource(R.string.no_town_matches), style = MaterialTheme.typography.bodyMedium, color = AdaptiveSecondaryText) }
                        }
                        items(matches, key = { "${it.first.id}_${it.second}" }) { (reg, city) ->
                            val selected = chosenRegion.id == reg.id && chosenCity == city
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (selected) AdaptiveCyanAccent.copy(alpha = 0.16f) else AdaptiveElevatedColor)
                                    .clickable {
                                        chosenRegion = reg
                                        chosenCity = city
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(city, style = MaterialTheme.typography.bodyLarge, color = AdaptivePrimaryText)
                                    Text(stringResource(R.string.district_name, reg.name), style = MaterialTheme.typography.bodySmall, color = AdaptiveSecondaryText)
                                }
                                if (selected) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AdaptiveCyanAccent, modifier = Modifier.size(20.dp))
                            }
                        }
                    } else {
                        item {
                            Text(stringResource(R.string.district), style = MaterialTheme.typography.labelLarge, color = AdaptiveSecondaryText)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                                items(regions, key = { it.id }) { region ->
                                    FilterChip(
                                        selected = region.id == chosenRegion.id,
                                        onClick = {
                                            chosenRegion = region
                                            chosenCity = region.nearestCities.firstOrNull() ?: region.name
                                        },
                                        label = { Text(region.name) },
                                        modifier = Modifier.testTag("dialog_district_${region.id}")
                                    )
                                }
                            }
                        }
                        item {
                            Text(stringResource(R.string.town), style = MaterialTheme.typography.labelLarge, color = AdaptiveSecondaryText)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 6.dp)
                            ) {
                                chosenRegion.nearestCities.forEach { city ->
                                    FilterChip(
                                        selected = city == chosenCity,
                                        onClick = { chosenCity = city },
                                        label = { Text(city) },
                                        modifier = Modifier.testTag("dialog_city_${city.lowercase().replace(" ", "_")}")
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { onConfirm(chosenRegion, chosenCity) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_residing_location_button")
                ) {
                    Text(stringResource(R.string.show_weather_for, chosenCity))
                }
            }
        }
    }
}
