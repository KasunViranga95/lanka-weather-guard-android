package com.example.ui.screens

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.SectionCard
import com.example.ui.theme.AdaptiveBorderColor
import com.example.ui.theme.AdaptiveCyanAccent
import com.example.ui.theme.AdaptiveElevatedColor
import com.example.ui.theme.AdaptivePrimaryText
import com.example.ui.theme.AdaptiveSecondaryText
import com.example.ui.theme.AlertRed
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.WarningAmber

private data class Hotline(@StringRes val name: Int, @StringRes val role: Int, val number: String, val color: Color)

private val hotlines = listOf(
    Hotline(R.string.hotline_dmc, R.string.hotline_dmc_role, "117", AlertRed),
    Hotline(R.string.hotline_ambulance, R.string.hotline_ambulance_role, "1990", SafeEmerald),
    Hotline(R.string.hotline_police, R.string.hotline_police_role, "119", WarningAmber),
    Hotline(R.string.hotline_fire, R.string.hotline_fire_role, "110", WarningAmber),
    Hotline(R.string.hotline_nbro, R.string.hotline_nbro_role, "0112588946", Color.Unspecified),
    Hotline(R.string.hotline_met, R.string.hotline_met_role, "0112686686", Color.Unspecified)
)

private data class ChecklistEntry(val id: String, @StringRes val title: Int, @StringRes val detail: Int)

private val checklistEntries = listOf(
    ChecklistEntry("chk_documents", R.string.chk_documents, R.string.chk_documents_detail),
    ChecklistEntry("chk_water", R.string.chk_water, R.string.chk_water_detail),
    ChecklistEntry("chk_electricity", R.string.chk_electricity, R.string.chk_electricity_detail),
    ChecklistEntry("chk_landslide", R.string.chk_landslide, R.string.chk_landslide_detail),
    ChecklistEntry("chk_phone", R.string.chk_phone, R.string.chk_phone_detail)
)

private data class OfficialSource(@StringRes val name: Int, @StringRes val what: Int, val url: String)

private val officialSources = listOf(
    OfficialSource(R.string.source_dmc, R.string.source_dmc_what, "https://www.dmc.gov.lk"),
    OfficialSource(R.string.source_met, R.string.source_met_what, "https://www.meteo.gov.lk"),
    OfficialSource(R.string.source_irrigation, R.string.source_irrigation_what, "https://www.irrigation.gov.lk"),
    OfficialSource(R.string.source_nbro, R.string.source_nbro_what, "https://www.nbro.gov.lk")
)

@Composable
fun SafetyHubScreen(
    checkedChecklistIds: Set<String>,
    alertsEnabled: Boolean,
    onAlertsEnabledChange: (Boolean) -> Unit,
    onOpenOfficialUrl: (String) -> Unit,
    onToggleChecklistItem: (String) -> Unit,
    onDialNumber: (String) -> Unit,
    onShareSafetyReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    // The "Be prepared" rows start collapsed so the tab opens on the emergency numbers
    var expandLeaving by remember { mutableStateOf(false) }
    var expandChecklist by remember { mutableStateOf(false) }

    val completedCount = checklistEntries.count { it.id in checkedChecklistIds }

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
                title = stringResource(R.string.emergency_numbers),
                actionLabel = stringResource(R.string.share),
                onAction = onShareSafetyReport,
                modifier = Modifier.testTag("expandable_hotlines_tile")
            ) {
                hotlines.forEachIndexed { idx, hotline ->
                    if (idx > 0) HorizontalDivider(color = AdaptiveBorderColor)
                    val color = if (hotline.color == Color.Unspecified) AdaptiveCyanAccent else hotline.color
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
                            Text(stringResource(hotline.name), style = MaterialTheme.typography.bodyLarge, color = AdaptivePrimaryText, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(hotline.role), style = MaterialTheme.typography.bodySmall, color = AdaptiveSecondaryText)
                        }
                        Text(hotline.number, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.SemiBold)
                        Icon(
                            Icons.Default.Call,
                            contentDescription = stringResource(R.string.call_number, hotline.number),
                            tint = color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 2. Be prepared
        item {
            SectionCard(title = stringResource(R.string.be_prepared)) {
                ExpandableRow(
                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                    title = stringResource(R.string.if_you_must_leave),
                    subtitle = stringResource(R.string.if_you_must_leave_sub),
                    expanded = expandLeaving,
                    onToggle = { expandLeaving = !expandLeaving },
                    testTag = "expandable_leaving_tile"
                ) {
                    listOf(R.string.leave_1, R.string.leave_2, R.string.leave_3, R.string.leave_4).forEach {
                        Text("• ${stringResource(it)}", style = MaterialTheme.typography.bodyMedium, color = AdaptivePrimaryText)
                    }
                }
                HorizontalDivider(color = AdaptiveBorderColor)
                ExpandableRow(
                    icon = Icons.Default.CheckBox,
                    title = stringResource(R.string.flood_checklist),
                    subtitle = stringResource(R.string.n_of_m_done, completedCount, checklistEntries.size),
                    expanded = expandChecklist,
                    onToggle = { expandChecklist = !expandChecklist },
                    testTag = "expandable_checklist_tile"
                ) {
                    LinearProgressIndicator(
                        progress = { completedCount.toFloat() / checklistEntries.size },
                        color = SafeEmerald,
                        trackColor = AdaptiveElevatedColor,
                        drawStopIndicator = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    checklistEntries.forEach { entry ->
                        val checked = entry.id in checkedChecklistIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onToggleChecklistItem(entry.id) }
                                .padding(vertical = 4.dp)
                                .testTag("checklist_item_${entry.id}"),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = null,
                                tint = if (checked) SafeEmerald else AdaptiveSecondaryText,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(stringResource(entry.title), style = MaterialTheme.typography.bodyMedium, color = AdaptivePrimaryText, fontWeight = FontWeight.SemiBold)
                                Text(stringResource(entry.detail), style = MaterialTheme.typography.bodySmall, color = AdaptiveSecondaryText)
                            }
                        }
                    }
                }
                HorizontalDivider(color = AdaptiveBorderColor)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .testTag("safety_alerts_switch"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = AdaptiveCyanAccent, modifier = Modifier.size(22.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.alert_notifications), style = MaterialTheme.typography.bodyLarge, color = AdaptivePrimaryText, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.alert_notifications_summary), style = MaterialTheme.typography.bodySmall, color = AdaptiveSecondaryText)
                    }
                    Switch(checked = alertsEnabled, onCheckedChange = onAlertsEnabledChange)
                }
            }
        }

        // 3. Where official warnings are published
        item {
            SectionCard(title = stringResource(R.string.official_sources)) {
                Text(stringResource(R.string.official_sources_intro), style = MaterialTheme.typography.bodySmall, color = AdaptiveSecondaryText)
                officialSources.forEachIndexed { idx, source ->
                    if (idx > 0) HorizontalDivider(color = AdaptiveBorderColor)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenOfficialUrl(source.url) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(source.name), style = MaterialTheme.typography.bodyLarge, color = AdaptivePrimaryText, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(source.what), style = MaterialTheme.typography.bodySmall, color = AdaptiveSecondaryText)
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = stringResource(R.string.open_website),
                            tint = AdaptiveCyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
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
                contentDescription = stringResource(if (expanded) R.string.hide_details else R.string.show_details),
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
