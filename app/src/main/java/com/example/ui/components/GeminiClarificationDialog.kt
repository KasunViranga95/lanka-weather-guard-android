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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.model.GeminiClarificationMessage
import com.example.model.RegionalWeatherReport
import com.example.model.RiverGaugeStation
import com.example.model.SriLankaRegion
import com.example.model.getDbzMeaningForValue
import com.example.ui.theme.AdaptiveBorderColor
import com.example.ui.theme.AdaptiveCyanAccent
import com.example.ui.theme.AdaptiveElevatedColor
import com.example.ui.theme.AdaptivePrimaryText
import com.example.ui.theme.AdaptiveSecondaryText
import com.example.ui.theme.AdaptiveSurfaceColor
import com.example.ui.theme.AdaptiveTealAccent
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.WarningAmber
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeminiClarificationDialog(
    selectedRegion: SriLankaRegion,
    selectedNearestCity: String,
    regionalReport: RegionalWeatherReport,
    peakDbz: Int,
    primaryStation: RiverGaugeStation?,
    messages: List<GeminiClarificationMessage>,
    isThinking: Boolean,
    onAskQuestion: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var questionInput by remember { mutableStateOf("") }
    val dbzMeaning = getDbzMeaningForValue(peakDbz)

    val quickPrompts = remember(selectedNearestCity, selectedRegion.name, peakDbz, regionalReport, primaryStation) {
        buildList {
            add("Explain $peakDbz dBZ (${dbzMeaning.shortTitle}) radar echo")
            add("What do RiverWatch m³/s flow & flood thresholds mean?")
            add("Explain ${regionalReport.rain24hMm.roundToInt()}mm/24h rain & landslide risk")
            add("Explain ${regionalReport.pressureHpa.roundToInt()} hPa pressure & ${regionalReport.windGustKmh.roundToInt()} km/h gusts")
            add("Safety checklist for $selectedNearestCity (${selectedRegion.name})")
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = AdaptiveSurfaceColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 18.dp)
                .border(1.5.dp, RadarCyan.copy(alpha = 0.65f), RoundedCornerShape(22.dp))
                .testTag("gemini_clarification_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
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
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(RadarCyan.copy(alpha = 0.20f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = AdaptiveCyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "GEMINI METRIC & WEATHER GUIDE",
                                style = MaterialTheme.typography.titleMedium,
                                color = AdaptivePrimaryText,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Clarifying live metrics for $selectedNearestCity, ${selectedRegion.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AdaptiveCyanAccent
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("gemini_dialog_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Gemini Guide",
                            tint = AdaptiveSecondaryText
                        )
                    }
                }

                // Current Live Metrics Snapshot Bar
                Surface(
                    color = AdaptiveElevatedColor,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AdaptiveBorderColor, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "LIVE TELEMETRY CONTEXT ($selectedNearestCity):",
                            style = MaterialTheme.typography.labelSmall,
                            color = AdaptiveTealAccent,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• Radar: $peakDbz dBZ — ${dbzMeaning.shortTitle} (${dbzMeaning.rainRateMmPerHour})",
                            style = MaterialTheme.typography.bodySmall,
                            color = AdaptivePrimaryText,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "• Temp & Rain: ${String.format(Locale.US, "%.1f°C", regionalReport.currentTempC)} • ${String.format(Locale.US, "%.1f mm/h", regionalReport.precipitationMm)} (${regionalReport.rain24hMm.roundToInt()} mm/24h)",
                            style = MaterialTheme.typography.bodySmall,
                            color = AdaptivePrimaryText
                        )
                        if (primaryStation != null) {
                            Text(
                                text = "• River Gauge (${primaryStation.stationName}): ${String.format(Locale.US, "%.2f m", primaryStation.currentLevelM)} (${primaryStation.floodStatus.shortLabel}) • Flow ${primaryStation.dischargeM3s.roundToInt()} m³/s",
                                style = MaterialTheme.typography.bodySmall,
                                color = AdaptiveCyanAccent
                            )
                        }
                    }
                }

                // 1-Tap Metric Clarification Chips
                Text(
                    text = "TAP A METRIC TO CLARIFY INSTANTLY:",
                    style = MaterialTheme.typography.labelSmall,
                    color = AdaptiveSecondaryText,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickPrompts.forEachIndexed { idx, prompt ->
                        Surface(
                            color = RadarCyan.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .border(1.dp, RadarCyan.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                .clickable(enabled = !isThinking) {
                                    onAskQuestion(prompt)
                                }
                                .testTag("gemini_quick_prompt_$idx")
                        ) {
                            Text(
                                text = prompt,
                                style = MaterialTheme.typography.labelSmall,
                                color = AdaptivePrimaryText,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Conversation / Clarification Output Box
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp, max = 250.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AdaptiveElevatedColor)
                        .border(1.dp, AdaptiveBorderColor, RoundedCornerShape(14.dp)),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (msg.isUser) RadarCyan.copy(alpha = 0.16f) else AdaptiveSurfaceColor
                                )
                                .border(
                                    1.dp,
                                    if (msg.isUser) RadarCyan.copy(alpha = 0.45f) else AdaptiveBorderColor,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (msg.isUser) "Your Question" else "Gemini Meteorologist • ${msg.metricTopic}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (msg.isUser) AdaptiveCyanAccent else SafeEmerald,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodySmall,
                                color = AdaptivePrimaryText
                            )
                        }
                    }

                    if (isThinking) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = RadarCyan,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Gemini 3.5 Flash is analyzing Sri Lanka weather & hydrology metrics...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AdaptiveCyanAccent
                                )
                            }
                        }
                    }
                }

                // Custom Question Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = questionInput,
                        onValueChange = { questionInput = it },
                        placeholder = {
                            Text(
                                text = "Ask about dBZ, m³/s flow, rain mm, hPa...",
                                style = MaterialTheme.typography.bodySmall,
                                color = AdaptiveSecondaryText
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RadarCyan,
                            unfocusedBorderColor = AdaptiveBorderColor,
                            focusedTextColor = AdaptivePrimaryText,
                            unfocusedTextColor = AdaptivePrimaryText
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("gemini_question_input")
                    )

                    Button(
                        onClick = {
                            val trimmed = questionInput.trim()
                            if (trimmed.isNotEmpty() && !isThinking) {
                                onAskQuestion(trimmed)
                                questionInput = ""
                            }
                        },
                        enabled = questionInput.isNotBlank() && !isThinking,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RadarCyan,
                            contentColor = Color(0xFF041222)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("gemini_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Ask Gemini",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ask",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "Prototype Security Notice: Configure GEMINI_API_KEY in the AI Studio Secrets panel. Do not share APK files containing personal API keys publicly.",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarningAmber
                )
            }
        }
    }
}
