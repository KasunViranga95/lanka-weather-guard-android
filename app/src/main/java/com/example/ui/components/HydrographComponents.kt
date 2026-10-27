package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.HydrographTimeRange
import com.example.model.RiverGaugeStation
import com.example.ui.theme.AdaptiveBorderColor
import com.example.ui.theme.AdaptiveCyanAccent
import com.example.ui.theme.AdaptiveSecondaryText
import com.example.ui.theme.AdaptiveSurfaceColor
import com.example.ui.theme.AdaptiveTealAccent
import com.example.ui.theme.AdvisoryYellow
import com.example.ui.theme.AlertRed
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.WarningAmber
import java.util.Locale

@Composable
fun RiverThresholdBar(
    station: RiverGaugeStation,
    modifier: Modifier = Modifier
) {
    val maxScale = (station.majorFloodLevelM * 1.18).coerceAtLeast(station.currentLevelM * 1.05)
    val alertFrac = (station.alertLevelM / maxScale).toFloat().coerceIn(0f, 1f)
    val minorFrac = (station.minorFloodLevelM / maxScale).toFloat().coerceIn(0f, 1f)
    val majorFrac = (station.majorFloodLevelM / maxScale).toFloat().coerceIn(0f, 1f)
    val currentFrac = (station.currentLevelM / maxScale).toFloat().coerceIn(0.04f, 1f)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0xFF071426))
        ) {
            val w = size.width
            val h = size.height

            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        RadarCyan,
                        station.floodStatus.color
                    )
                ),
                size = Size(w * currentFrac, h),
                cornerRadius = CornerRadius(9.dp.toPx(), 9.dp.toPx())
            )

            val markers = listOf(
                alertFrac to AdvisoryYellow,
                minorFrac to WarningAmber,
                majorFrac to AlertRed
            )
            markers.forEach { (frac, markerColor) ->
                val x = w * frac
                drawLine(
                    color = markerColor,
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 2.5.dp.toPx()
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThresholdLabel(
                title = "Alert",
                valueM = station.alertLevelM,
                color = AdvisoryYellow,
                modifier = Modifier.weight(1f)
            )
            ThresholdLabel(
                title = "Minor",
                valueM = station.minorFloodLevelM,
                color = WarningAmber,
                modifier = Modifier.weight(1f)
            )
            ThresholdLabel(
                title = "Major",
                valueM = station.majorFloodLevelM,
                color = AlertRed,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ThresholdLabel(
    title: String,
    valueM: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .padding(end = 4.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
                .padding(horizontal = 3.dp)
        )
        Text(
            text = "$title: ${String.format(Locale.US, "%.2fm", valueM)}",
            style = MaterialTheme.typography.labelSmall,
            color = AdaptiveSecondaryText,
            maxLines = 1
        )
    }
}

@Composable
fun HydrographTimeRangeSelector(
    selectedRange: HydrographTimeRange,
    onRangeSelected: (HydrographTimeRange) -> Unit,
    stationIdTagPrefix: String = "global",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HydrographTimeRange.entries.forEach { range ->
            val isSelected = range == selectedRange
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) RadarCyan.copy(alpha = 0.22f) else AdaptiveSurfaceColor
                    )
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) RadarCyan else AdaptiveBorderColor,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onRangeSelected(range) }
                    .padding(vertical = 6.dp, horizontal = 8.dp)
                    .testTag("hydrograph_range_${stationIdTagPrefix}_${range.id}")
            ) {
                Text(
                    text = range.shortLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) AdaptiveCyanAccent else AdaptiveSecondaryText,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

private data class YAxisFloodTick(
    val label: String,
    val levelM: Double,
    val exactY: Float,
    var displayY: Float,
    val color: Color,
    val isFloodThreshold: Boolean
)

@Composable
fun RiverHydrographCurve(
    station: RiverGaugeStation,
    timeRange: HydrographTimeRange = HydrographTimeRange.HOURS_24,
    modifier: Modifier = Modifier
) {
    val points = station.getLevelsForTimeRange(timeRange)
    val minPoint = points.minOrNull() ?: station.previousLevelM
    val maxPoint = points.maxOrNull() ?: station.currentLevelM
    val avgPoint = if (points.isNotEmpty()) points.average() else station.currentLevelM

    val minVal = (minOf(minPoint, station.alertLevelM) * 0.80).coerceAtLeast(0.0)
    val maxVal = (maxOf(maxPoint, station.majorFloodLevelM) * 1.12)
    val range = (maxVal - minVal).coerceAtLeast(0.5)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hydrograph_curve_${station.id}"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Y-Axis: Water Level & Flood Thresholds (m)",
                style = MaterialTheme.typography.labelSmall,
                color = AdaptiveCyanAccent,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Min ${String.format(Locale.US, "%.2fm", minPoint)} • Avg ${String.format(Locale.US, "%.2fm", avgPoint)} • Peak ${String.format(Locale.US, "%.2fm", maxPoint)}",
                style = MaterialTheme.typography.labelSmall,
                color = AdaptiveSecondaryText
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(195.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF071527))
                .border(1.dp, AdaptiveBorderColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 14.dp)
        ) {
            val totalW = size.width
            val h = size.height
            // Reserve left column for the Y-axis flood level labels (Major, Minor, Alert, Base)
            val yAxisWidth = 92.dp.toPx().coerceAtMost(totalW * 0.36f)
            val plotLeft = yAxisWidth + 8.dp.toPx()
            val plotWidth = (totalW - plotLeft - 6.dp.toPx()).coerceAtLeast(40f)

            fun levelToY(level: Double): Float {
                val normalized = ((level - minVal) / range).toFloat().coerceIn(0f, 1f)
                return h - normalized * h
            }

            // Draw Y-Axis vertical spine & X-Axis baseline
            drawLine(
                color = Color(0xFF355276),
                start = Offset(plotLeft, 0f),
                end = Offset(plotLeft, h),
                strokeWidth = 1.5.dp.toPx()
            )
            drawLine(
                color = Color(0xFF355276),
                start = Offset(plotLeft, h),
                end = Offset(plotLeft + plotWidth, h),
                strokeWidth = 1.5.dp.toPx()
            )

            // Build Y-Axis ticks for Flood Levels + Base Level
            val rawTicks = listOf(
                YAxisFloodTick(
                    label = "Major ${String.format(Locale.US, "%.2fm", station.majorFloodLevelM)}",
                    levelM = station.majorFloodLevelM,
                    exactY = levelToY(station.majorFloodLevelM),
                    displayY = levelToY(station.majorFloodLevelM),
                    color = AlertRed,
                    isFloodThreshold = true
                ),
                YAxisFloodTick(
                    label = "Minor ${String.format(Locale.US, "%.2fm", station.minorFloodLevelM)}",
                    levelM = station.minorFloodLevelM,
                    exactY = levelToY(station.minorFloodLevelM),
                    displayY = levelToY(station.minorFloodLevelM),
                    color = WarningAmber,
                    isFloodThreshold = true
                ),
                YAxisFloodTick(
                    label = "Alert ${String.format(Locale.US, "%.2fm", station.alertLevelM)}",
                    levelM = station.alertLevelM,
                    exactY = levelToY(station.alertLevelM),
                    displayY = levelToY(station.alertLevelM),
                    color = AdvisoryYellow,
                    isFloodThreshold = true
                ),
                YAxisFloodTick(
                    label = "Base ${String.format(Locale.US, "%.2fm", minVal)}",
                    levelM = minVal,
                    exactY = h,
                    displayY = h,
                    color = SafeEmerald,
                    isFloodThreshold = false
                )
            ).sortedBy { it.exactY }

            // Ensure Y-Axis text labels never overlap vertically
            val minGapPx = 19.dp.toPx()
            for (i in 1 until rawTicks.size) {
                if (rawTicks[i].displayY - rawTicks[i - 1].displayY < minGapPx) {
                    rawTicks[i].displayY = (rawTicks[i - 1].displayY + minGapPx).coerceAtMost(h)
                }
            }
            for (i in rawTicks.size - 2 downTo 0) {
                if (rawTicks[i + 1].displayY - rawTicks[i].displayY < minGapPx) {
                    rawTicks[i].displayY = (rawTicks[i + 1].displayY - minGapPx).coerceAtLeast(8f)
                }
            }

            val yAxisPaint = Paint().apply {
                isAntiAlias = true
                textSize = 10.5.dp.toPx()
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            // Draw Flood Level horizontal dashed threshold lines & Y-axis labels
            rawTicks.forEach { tick ->
                if (tick.isFloodThreshold) {
                    drawLine(
                        color = tick.color.copy(alpha = 0.72f),
                        start = Offset(plotLeft, tick.exactY),
                        end = Offset(plotLeft + plotWidth, tick.exactY),
                        strokeWidth = 1.6.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                    )
                }

                // Small tick mark on Y-axis connecting to label
                drawLine(
                    color = tick.color,
                    start = Offset(plotLeft - 5.dp.toPx(), tick.exactY),
                    end = Offset(plotLeft, tick.exactY),
                    strokeWidth = 2.dp.toPx()
                )

                // Pill background on the Y-axis for readability
                val pillTop = (tick.displayY - 9.dp.toPx()).coerceIn(0f, h - 16.dp.toPx())
                drawRoundRect(
                    color = tick.color.copy(alpha = 0.16f),
                    topLeft = Offset(0f, pillTop),
                    size = Size(yAxisWidth, 16.dp.toPx()),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                yAxisPaint.color = tick.color.toArgb()
                drawContext.canvas.nativeCanvas.drawText(
                    tick.label,
                    4.dp.toPx(),
                    pillTop + 12.dp.toPx(),
                    yAxisPaint
                )
            }

            // Draw Historical Water Level Curve inside the plot area
            val linePath = Path()
            val fillPath = Path()

            points.forEachIndexed { idx, level ->
                val x = plotLeft + if (points.size > 1) {
                    (idx.toFloat() / (points.size - 1)) * plotWidth
                } else {
                    plotWidth / 2f
                }
                val y = levelToY(level)
                if (idx == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, h)
                    fillPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
                if (idx == points.lastIndex) {
                    fillPath.lineTo(x, h)
                    fillPath.close()
                }
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        station.floodStatus.color.copy(alpha = 0.36f),
                        Color.Transparent
                    )
                )
            )
            drawPath(
                path = linePath,
                color = station.floodStatus.color,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            points.forEachIndexed { idx, level ->
                val x = plotLeft + if (points.size > 1) {
                    (idx.toFloat() / (points.size - 1)) * plotWidth
                } else {
                    plotWidth / 2f
                }
                val y = levelToY(level)
                drawCircle(
                    color = station.floodStatus.color,
                    radius = if (idx == points.lastIndex) 5.5.dp.toPx() else 3.5.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        // X-Axis Time Labels aligned with the plot area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 96.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = timeRange.xAxisStartLabel,
                style = MaterialTheme.typography.bodySmall,
                color = AdaptiveSecondaryText
            )
            Text(
                text = timeRange.xAxisMidLabel,
                style = MaterialTheme.typography.bodySmall,
                color = AdaptiveSecondaryText
            )
            Text(
                text = "${timeRange.xAxisEndLabel} (${String.format(Locale.US, "%.2fm", station.currentLevelM)})",
                style = MaterialTheme.typography.labelMedium,
                color = AdaptiveCyanAccent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
