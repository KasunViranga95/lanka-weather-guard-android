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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.model.EmergencyShelter
import com.example.model.GovernmentAnnouncement
import com.example.model.NotificationIntervalOption
import com.example.model.SafetyChecklistItem
import com.example.model.SriLankaRegion
import com.example.ui.theme.AdaptiveBorderColor
import com.example.ui.theme.AdaptiveCyanAccent
import com.example.ui.theme.AdaptiveElevatedColor
import com.example.ui.theme.AdaptivePrimaryText
import com.example.ui.theme.AdaptiveSecondaryText
import com.example.ui.theme.AdaptiveSurfaceColor
import com.example.ui.theme.AdaptiveTealAccent
import com.example.ui.theme.AlertRed
import com.example.ui.theme.MonsoonTeal
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.WarningAmber

data class EmergencyHotline(
    val agency: String,
    val number: String,
    val role: String,
    val color: Color
)

@Composable
fun SafetyHubScreen(
    selectedRegion: SriLankaRegion,
    selectedNearestCity: String,
    selectedNotificationInterval: NotificationIntervalOption,
    governmentAnnouncements: List<GovernmentAnnouncement>,
    selectedGovCategoryFilter: String,
    shelters: List<EmergencyShelter>,
    checklist: List<SafetyChecklistItem>,
    onSelectGovCategoryFilter: (String) -> Unit,
    onSelectNotificationInterval: (NotificationIntervalOption) -> Unit,
    onOpenOfficialUrl: (String) -> Unit,
    onToggleChecklistItem: (String) -> Unit,
    onDialNumber: (String) -> Unit,
    onShareSafetyReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hotlines = listOf(
        EmergencyHotline("DMC Emergency Ops", "117", "24/7 Disaster Relief & Evacuation", AlertRed),
        EmergencyHotline("Suwa Seriya Ambulance", "1990", "Free National Pre-Hospital Ambulance", SafeEmerald),
        EmergencyHotline("Police Emergency", "119", "Immediate Rescue & Traffic Dispatch", WarningAmber),
        EmergencyHotline("SL Navy Flood Rescue", "0112445368", "Rapid Boat Deployment (Kalutara/Ratnapura/Galle)", RadarCyan),
        EmergencyHotline("NBRO Landslide Desk", "0112588946", "Slope Failure & Early Warning Hotline", MonsoonTeal),
        EmergencyHotline("Dept of Meteorology", "0112686686", "National Cyclone & Monsoon Desk", RadarCyan)
    )

    val govCategories = listOf(
        "All Official",
        "My Residing Area",
        "DMC Flood Bulletin",
        "Met Dept Warning",
        "NBRO Landslide",
        "Public Services & Schools"
    )

    val residingAnnouncement = governmentAnnouncements.firstOrNull { it.category == "Residing Area Directive" }
        ?: governmentAnnouncements.firstOrNull()

    val filteredGovNews = remember(governmentAnnouncements, selectedGovCategoryFilter) {
        when (selectedGovCategoryFilter) {
            "All Official" -> governmentAnnouncements
            "My Residing Area" -> governmentAnnouncements.filter { it.isResidingAreaMatch }
            else -> governmentAnnouncements.filter {
                it.category.equals(selectedGovCategoryFilter, ignoreCase = true)
            }
        }
    }

    // All top safety & contact tiles start collapsed so the user can click any tile to expand and view
    var expandHotlinesTile by remember { mutableStateOf(false) }
    var expandSheltersTile by remember { mutableStateOf(false) }
    var expandChecklistTile by remember { mutableStateOf(false) }
    var expandScheduleTile by remember { mutableStateOf(false) }
    var expandAllMessages by remember { mutableStateOf(false) }

    val completedCount = checklist.count { it.isChecked }
    val progress = if (checklist.isNotEmpty()) completedCount.toFloat() / checklist.size else 0f
    val sortedShelters = remember(shelters, selectedRegion.name) {
        shelters.sortedByDescending {
            it.district.equals(selectedRegion.name, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("safety_hub_list"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Clean Safety Hub Header Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = AdaptiveSurfaceColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AdaptiveBorderColor, RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SafeEmerald.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HealthAndSafety,
                                contentDescription = null,
                                tint = SafeEmerald,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Safety Hub & Emergency Directory",
                                style = MaterialTheme.typography.titleMedium,
                                color = AdaptivePrimaryText,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$selectedNearestCity, ${selectedRegion.name} • Tap any tile below to expand & view",
                                style = MaterialTheme.typography.bodySmall,
                                color = AdaptiveSecondaryText
                            )
                        }
                    }

                    Button(
                        onClick = onShareSafetyReport,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RadarCyan,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("share_safety_report_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. TOP TILE 1: 24/7 Emergency Contacts & Hotlines (Click tile to expand & dial)
        item {
            ExpandableSectionTile(
                title = "24/7 Emergency Contacts & Hotlines",
                subtitle = "DMC 117 • Ambulance 1990 • Police 119 • Navy Rescue (Tap to expand)",
                icon = Icons.Default.Call,
                accentColor = AlertRed,
                expanded = expandHotlinesTile,
                onToggleExpand = { expandHotlinesTile = !expandHotlinesTile },
                testTag = "expandable_hotlines_tile"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    hotlines.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { hotline ->
                                Surface(
                                    color = AdaptiveElevatedColor,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(1.dp, hotline.color.copy(alpha = 0.40f), RoundedCornerShape(12.dp))
                                        .clickable { onDialNumber(hotline.number) }
                                        .testTag("hotline_card_${hotline.number}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = hotline.number,
                                                style = MaterialTheme.typography.titleLarge,
                                                color = hotline.color,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Call,
                                                contentDescription = "Dial ${hotline.number}",
                                                tint = hotline.color,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Text(
                                            text = hotline.agency,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = AdaptivePrimaryText,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = hotline.role,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AdaptiveSecondaryText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. TOP TILE 2: High-Ground Evacuation Shelters (Click tile to expand & view)
        item {
            ExpandableSectionTile(
                title = "High-Ground Evacuation Shelters (${sortedShelters.size})",
                subtitle = "Nearest shelters for ${selectedRegion.name} District (Tap to expand)",
                icon = Icons.Default.HomeWork,
                accentColor = WarningAmber,
                expanded = expandSheltersTile,
                onToggleExpand = { expandSheltersTile = !expandSheltersTile },
                testTag = "expandable_shelters_tile"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    sortedShelters.forEach { shelter ->
                        val isLocalMatch = shelter.district.equals(selectedRegion.name, ignoreCase = true)
                        Surface(
                            color = AdaptiveElevatedColor,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isLocalMatch) 1.5.dp else 1.dp,
                                    color = if (isLocalMatch) SafeEmerald else AdaptiveBorderColor,
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = shelter.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = AdaptivePrimaryText,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${shelter.district} District • Elevation: ${shelter.elevationMeters}m ASL • ${shelter.distanceKmText}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AdaptiveCyanAccent
                                        )
                                    }

                                    if (isLocalMatch) {
                                        Surface(
                                            color = SafeEmerald.copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "Nearest",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SafeEmerald,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "Facilities: ${shelter.facilities.joinToString(" • ")} (Capacity: ${shelter.capacityPeople} people)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AdaptiveSecondaryText
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(
                                        onClick = { onDialNumber(shelter.contactPhone) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Call,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Call Shelter (${shelter.contactPhone})")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. TOP TILE 3: Household Flood Readiness Checklist (Click tile to expand & check items)
        item {
            ExpandableSectionTile(
                title = "Household Flood Readiness Checklist",
                subtitle = "$completedCount of ${checklist.size} safety actions completed (Tap to expand)",
                icon = Icons.Default.CheckBox,
                accentColor = SafeEmerald,
                expanded = expandChecklistTile,
                onToggleExpand = { expandChecklistTile = !expandChecklistTile },
                testTag = "expandable_checklist_tile"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LinearProgressIndicator(
                        progress = { progress },
                        color = SafeEmerald,
                        trackColor = AdaptiveElevatedColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    checklist.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(AdaptiveElevatedColor)
                                .clickable { onToggleChecklistItem(item.id) }
                                .padding(12.dp)
                                .testTag("checklist_item_${item.id}"),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (item.isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = item.title,
                                tint = if (item.isChecked) SafeEmerald else AdaptiveCyanAccent,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = AdaptivePrimaryText,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = item.detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AdaptiveSecondaryText
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. TOP TILE 4: Alert & Notification Interval Schedule (Click tile to expand & configure)
        item {
            ExpandableSectionTile(
                title = "Weather Alert Notification Schedule",
                subtitle = "Current schedule: ${selectedNotificationInterval.label} for $selectedNearestCity (Tap to expand)",
                icon = Icons.Default.NotificationsActive,
                accentColor = RadarCyan,
                expanded = expandScheduleTile,
                onToggleExpand = { expandScheduleTile = !expandScheduleTile },
                testTag = "safety_notification_interval_card"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Select how frequently Lanka Weather Guard sends automated flood and weather alerts for $selectedNearestCity (${selectedRegion.name}):",
                        style = MaterialTheme.typography.bodySmall,
                        color = AdaptiveSecondaryText
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(NotificationIntervalOption.entries.toList(), key = { it.name }) { option ->
                            val isSelected = option == selectedNotificationInterval
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectNotificationInterval(option) },
                                label = {
                                    Text(
                                        text = option.shortChipLabel,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RadarCyan.copy(alpha = 0.20f),
                                    selectedLabelColor = AdaptiveCyanAccent
                                ),
                                modifier = Modifier.testTag("safety_interval_chip_${option.id}")
                            )
                        }
                    }
                }
            }
        }

        // 6. Featured Residing Area Official Government Expandable Tile (Starts collapsed, click to expand)
        if (residingAnnouncement != null) {
            item {
                ResidingAreaOfficialGovernmentTile(
                    announcement = residingAnnouncement,
                    selectedNearestCity = selectedNearestCity,
                    selectedDistrict = selectedRegion.name,
                    onOpenOfficialUrl = { onOpenOfficialUrl(residingAnnouncement.officialUrl) }
                )
            }
        }

        // 7. Official Government Announcements Filter & "Expand All Tiles" Control Bar
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Newspaper,
                                contentDescription = null,
                                tint = AdaptiveCyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Official Government Tiles (${filteredGovNews.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = AdaptivePrimaryText,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            color = RadarCyan.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .clickable { expandAllMessages = !expandAllMessages }
                                .testTag("toggle_expand_all_gov_tiles")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (expandAllMessages) "Collapse All" else "Expand All",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AdaptiveCyanAccent,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = if (expandAllMessages) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = AdaptiveCyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(govCategories, key = { it }) { cat ->
                            val selected = cat == selectedGovCategoryFilter
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectGovCategoryFilter(cat) },
                                label = { Text(cat) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RadarCyan.copy(alpha = 0.18f),
                                    selectedLabelColor = AdaptiveCyanAccent
                                ),
                                modifier = Modifier.testTag("gov_news_filter_${cat.lowercase().replace(" ", "_")}")
                            )
                        }
                    }
                }
            }
        }

        // 8. Expandable Government Message Tiles (Tap any tile to show the FULL message & directives)
        itemsIndexed(filteredGovNews, key = { _, it -> it.id }) { idx, announcement ->
            ExpandableGovernmentAnnouncementTile(
                indexNumber = idx + 1,
                announcement = announcement,
                forceExpand = expandAllMessages,
                onOpenOfficialUrl = { onOpenOfficialUrl(announcement.officialUrl) }
            )
        }
    }
}

@Composable
private fun ExpandableSectionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    testTag: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AdaptiveSurfaceColor),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (expanded) 1.5.dp else 1.dp,
                color = if (expanded) accentColor.copy(alpha = 0.65f) else AdaptiveBorderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onToggleExpand() }
            .testTag(testTag)
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            color = AdaptivePrimaryText,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = AdaptiveSecondaryText
                        )
                    }
                }

                Surface(
                    color = accentColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (expanded) "Hide" else "View",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HorizontalDivider(color = AdaptiveBorderColor)
                    content()
                }
            }
        }
    }
}

@Composable
private fun ResidingAreaOfficialGovernmentTile(
    announcement: GovernmentAnnouncement,
    selectedNearestCity: String,
    selectedDistrict: String,
    onOpenOfficialUrl: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val badgeColor = announcement.severity.badgeColor

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AdaptiveSurfaceColor),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, AdaptiveCyanAccent, RoundedCornerShape(14.dp))
            .clickable { expanded = !expanded }
            .testTag("safety_hub_residing_gov_card")
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(RadarCyan.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = AdaptiveCyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Residing Area Directive: $selectedNearestCity",
                            style = MaterialTheme.typography.titleSmall,
                            color = AdaptiveCyanAccent,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${announcement.issuingMinistryOrAgency} • Tap tile to expand",
                            style = MaterialTheme.typography.bodySmall,
                            color = AdaptiveSecondaryText
                        )
                    }
                }

                Surface(
                    color = badgeColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (expanded) "Hide" else selectedDistrict,
                            style = MaterialTheme.typography.labelMedium,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Text(
                text = announcement.headline,
                style = MaterialTheme.typography.titleMedium,
                color = AdaptivePrimaryText,
                fontWeight = FontWeight.Bold
            )

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = announcement.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AdaptivePrimaryText
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AdaptiveElevatedColor)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Full Official Instructions & Safety Directives:",
                            style = MaterialTheme.typography.labelLarge,
                            color = AdaptiveTealAccent,
                            fontWeight = FontWeight.Bold
                        )
                        announcement.keyDirectives.forEach { directive ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SafeEmerald,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(15.dp)
                                )
                                Text(
                                    text = directive,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AdaptivePrimaryText
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = AdaptiveBorderColor)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ref: ${announcement.referenceCode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AdaptiveSecondaryText
                        )

                        Button(
                            onClick = onOpenOfficialUrl,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RadarCyan,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("residing_gov_official_link_btn")
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
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandableGovernmentAnnouncementTile(
    indexNumber: Int,
    announcement: GovernmentAnnouncement,
    forceExpand: Boolean,
    onOpenOfficialUrl: () -> Unit
) {
    var localExpanded by remember(announcement.id) { mutableStateOf(false) }
    val isExpanded = forceExpand || localExpanded
    val badgeColor = announcement.severity.badgeColor

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AdaptiveSurfaceColor),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (announcement.isResidingAreaMatch) 1.5.dp else 1.dp,
                color = if (announcement.isResidingAreaMatch) badgeColor.copy(alpha = 0.7f) else AdaptiveBorderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { localExpanded = !localExpanded }
            .testTag("gov_announcement_card_${announcement.id}")
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = badgeColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "#$indexNumber • ${announcement.category}",
                            style = MaterialTheme.typography.labelMedium,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    if (announcement.isResidingAreaMatch) {
                        Surface(
                            color = RadarCyan.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Your Area",
                                style = MaterialTheme.typography.labelSmall,
                                color = AdaptiveCyanAccent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
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
                            text = if (isExpanded) "Hide" else "View",
                            style = MaterialTheme.typography.labelSmall,
                            color = AdaptiveCyanAccent,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = AdaptiveCyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Text(
                text = announcement.headline,
                style = MaterialTheme.typography.titleMedium,
                color = AdaptivePrimaryText,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${announcement.issuingMinistryOrAgency} • ${announcement.publishedTimeText}",
                style = MaterialTheme.typography.bodySmall,
                color = AdaptiveCyanAccent
            )

            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = announcement.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AdaptivePrimaryText
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AdaptiveElevatedColor)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Official Directives (Ref: ${announcement.referenceCode})",
                            style = MaterialTheme.typography.titleSmall,
                            color = AdaptiveTealAccent,
                            fontWeight = FontWeight.Bold
                        )
                        announcement.keyDirectives.forEach { line ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SafeEmerald,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(15.dp)
                                )
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AdaptivePrimaryText
                                )
                            }
                        }

                        Text(
                            text = "Target Districts: ${announcement.targetDistricts.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AdaptiveSecondaryText
                        )
                    }

                    HorizontalDivider(color = AdaptiveBorderColor)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tap tile to collapse ▲",
                            style = MaterialTheme.typography.labelMedium,
                            color = AdaptiveCyanAccent,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedButton(
                            onClick = onOpenOfficialUrl,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                            modifier = Modifier.testTag("gov_news_link_${announcement.id}")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = announcement.officialPortalName,
                                style = MaterialTheme.typography.labelSmall,
                                color = AdaptiveCyanAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
