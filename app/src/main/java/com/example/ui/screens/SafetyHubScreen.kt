package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import com.example.ui.components.SectionCard
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
import androidx.compose.material3.IconButton
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

    val filteredGovNews = remember(governmentAnnouncements, selectedGovCategoryFilter) {
        when (selectedGovCategoryFilter) {
            "All Official" -> governmentAnnouncements
            "My Residing Area" -> governmentAnnouncements.filter { it.isResidingAreaMatch }
            else -> governmentAnnouncements.filter {
                it.category.equals(selectedGovCategoryFilter, ignoreCase = true)
            }
        }
    }

    // The "Be prepared" rows start collapsed so the tab opens on the emergency numbers
    var expandSheltersTile by remember { mutableStateOf(false) }
    var expandChecklistTile by remember { mutableStateOf(false) }
    var expandScheduleTile by remember { mutableStateOf(false) }

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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Emergency numbers – always visible, one tap to call
        item {
            SectionCard(
                title = "Emergency numbers",
                actionLabel = "Share",
                onAction = onShareSafetyReport,
                modifier = Modifier.testTag("expandable_hotlines_tile")
            ) {
                hotlines.forEachIndexed { idx, hotline ->
                    if (idx > 0) HorizontalDivider(color = AdaptiveBorderColor)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onDialNumber(hotline.number) }
                            .padding(vertical = 6.dp)
                            .testTag("hotline_card_${hotline.number}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(hotline.agency, style = MaterialTheme.typography.bodyLarge, color = AdaptivePrimaryText, fontWeight = FontWeight.SemiBold)
                            Text(hotline.role, style = MaterialTheme.typography.bodySmall, color = AdaptiveSecondaryText)
                        }
                        Text(hotline.number, style = MaterialTheme.typography.titleMedium, color = hotline.color, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.Call, contentDescription = "Call ${hotline.number}", tint = hotline.color, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // 2. Be prepared – shelters, checklist and alert schedule as rows of one card
        item {
            SectionCard(title = "Be prepared") {
                ExpandableRow(
                    icon = Icons.Default.HomeWork,
                    title = "Evacuation shelters",
                    subtitle = "${sortedShelters.size} near ${selectedRegion.name}",
                    expanded = expandSheltersTile,
                    onToggle = { expandSheltersTile = !expandSheltersTile },
                    testTag = "expandable_shelters_tile"
                ) {
                    sortedShelters.forEach { shelter ->
                        val isLocalMatch = shelter.district.equals(selectedRegion.name, ignoreCase = true)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = shelter.name + if (isLocalMatch) "  · Nearest" else "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AdaptivePrimaryText,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${shelter.district} · ${shelter.elevationMeters} m above sea level · ${shelter.capacityPeople} people",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AdaptiveSecondaryText
                                )
                            }
                            IconButton(onClick = { onDialNumber(shelter.contactPhone) }) {
                                Icon(Icons.Default.Call, contentDescription = "Call ${shelter.name}", tint = AdaptiveCyanAccent)
                            }
                        }
                    }
                }
                HorizontalDivider(color = AdaptiveBorderColor)
                ExpandableRow(
                    icon = Icons.Default.CheckBox,
                    title = "Flood readiness checklist",
                    subtitle = "$completedCount of ${checklist.size} done",
                    expanded = expandChecklistTile,
                    onToggle = { expandChecklistTile = !expandChecklistTile },
                    testTag = "expandable_checklist_tile"
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        color = SafeEmerald,
                        trackColor = AdaptiveElevatedColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    checklist.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onToggleChecklistItem(item.id) }
                                .padding(vertical = 4.dp)
                                .testTag("checklist_item_${item.id}"),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (item.isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = item.title,
                                tint = if (item.isChecked) SafeEmerald else AdaptiveSecondaryText,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(item.title, style = MaterialTheme.typography.bodyMedium, color = AdaptivePrimaryText, fontWeight = FontWeight.SemiBold)
                                Text(item.detail, style = MaterialTheme.typography.bodySmall, color = AdaptiveSecondaryText)
                            }
                        }
                    }
                }
                HorizontalDivider(color = AdaptiveBorderColor)
                ExpandableRow(
                    icon = Icons.Default.NotificationsActive,
                    title = "Alert notifications",
                    subtitle = "${selectedNotificationInterval.label} for $selectedNearestCity",
                    expanded = expandScheduleTile,
                    onToggle = { expandScheduleTile = !expandScheduleTile },
                    testTag = "safety_notification_interval_card"
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(NotificationIntervalOption.entries.toList(), key = { it.name }) { option ->
                            FilterChip(
                                selected = option == selectedNotificationInterval,
                                onClick = { onSelectNotificationInterval(option) },
                                label = { Text(option.shortChipLabel) },
                                modifier = Modifier.testTag("safety_interval_chip_${option.id}")
                            )
                        }
                    }
                }
            }
        }

        // 3. Official updates, filtered by source
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Official updates",
                    style = MaterialTheme.typography.titleMedium,
                    color = AdaptivePrimaryText,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(govCategories, key = { it }) { cat ->
                        FilterChip(
                            selected = cat == selectedGovCategoryFilter,
                            onClick = { onSelectGovCategoryFilter(cat) },
                            label = { Text(govCategoryLabel(cat)) },
                            modifier = Modifier.testTag("gov_news_filter_${cat.lowercase().replace(" ", "_")}")
                        )
                    }
                }
            }
        }

        if (filteredGovNews.isEmpty()) {
            item {
                Text("No updates in this category.", style = MaterialTheme.typography.bodyMedium, color = AdaptiveSecondaryText)
            }
        }
        items(filteredGovNews, key = { it.id }) { announcement ->
            ExpandableGovernmentAnnouncementTile(
                announcement = announcement,
                onOpenOfficialUrl = { onOpenOfficialUrl(announcement.officialUrl) }
            )
        }
    }
}

private fun govCategoryLabel(category: String): String = when (category) {
    "All Official" -> "All"
    "My Residing Area" -> "My area"
    "DMC Flood Bulletin" -> "Floods (DMC)"
    "Met Dept Warning" -> "Weather (Met Dept)"
    "NBRO Landslide" -> "Landslides (NBRO)"
    "Public Services & Schools" -> "Schools & services"
    else -> category
}

/** A list row inside a card that reveals its content below when tapped. */
@Composable
private fun ExpandableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    testTag: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().testTag(testTag)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onToggle() }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = AdaptiveCyanAccent, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = AdaptivePrimaryText, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AdaptiveSecondaryText)
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = AdaptiveSecondaryText
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(start = 34.dp, top = 4.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
        }
    }
}

@Composable
private fun ExpandableGovernmentAnnouncementTile(
    announcement: GovernmentAnnouncement,
    onOpenOfficialUrl: () -> Unit
) {
    var isExpanded by remember(announcement.id) { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AdaptiveSurfaceColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .testTag("gov_announcement_card_${announcement.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(announcement.severity.badgeColor)
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = announcement.category + if (announcement.isResidingAreaMatch) " · Your area" else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = AdaptiveSecondaryText
                    )
                    Text(
                        text = announcement.headline,
                        style = MaterialTheme.typography.bodyLarge,
                        color = AdaptivePrimaryText,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${announcement.issuingMinistryOrAgency.substringBefore(" •")} · ${announcement.publishedTimeText}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AdaptiveSecondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = AdaptiveSecondaryText
                )
            }

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
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
