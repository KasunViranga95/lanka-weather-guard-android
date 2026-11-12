package com.example.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.HydrographTimeRange
import com.example.model.RiverGaugeStation
import com.example.ui.theme.AdaptiveElevatedColor
import com.example.ui.theme.AdaptiveSecondaryText
import com.example.ui.theme.AdvisoryYellow
import com.example.ui.theme.AlertRed
import com.example.ui.theme.WarningAmber
import java.util.Locale

private fun m(value: Double) = String.format(Locale.US, "%.2f m", value)

/** Horizontal bar: current level against the alert, minor and major flood marks. */
@Composable
fun RiverThresholdBar(station: RiverGaugeStation, modifier: Modifier = Modifier) {
    val maxScale = (station.majorFloodLevelM * 1.18).coerceAtLeast(station.currentLevelM * 1.05)
    val marks = listOf(
        Triple(station.alertLevelM, AdvisoryYellow, R.string.level_alert),
        Triple(station.minorFloodLevelM, WarningAmber, R.string.level_minor),
        Triple(station.majorFloodLevelM, AlertRed, R.string.level_major)
    )
    val trackColor = AdaptiveElevatedColor
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
        ) {
            drawRoundRect(color = trackColor, size = size, cornerRadius = CornerRadius(6.dp.toPx()))
            val currentFrac = (station.currentLevelM / maxScale).toFloat().coerceIn(0.03f, 1f)
            drawRoundRect(
                color = station.floodStatus.color,
                size = Size(size.width * currentFrac, size.height),
                cornerRadius = CornerRadius(6.dp.toPx())
            )
            marks.forEach { (level, color, _) ->
                val x = size.width * (level / maxScale).toFloat().coerceIn(0f, 1f)
                drawLine(color = color, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 2.5.dp.toPx())
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            marks.forEach { (level, color, label) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Text(
                        text = "${stringResource(label)} ${m(level)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AdaptiveSecondaryText,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun HydrographTimeRangeSelector(
    selectedRange: HydrographTimeRange,
    onRangeSelected: (HydrographTimeRange) -> Unit,
    stationIdTagPrefix: String = "global",
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HydrographTimeRange.entries.forEach { range ->
            FilterChip(
                selected = range == selectedRange,
                onClick = { onRangeSelected(range) },
                label = { Text(stringResource(range.labelRes)) },
                modifier = Modifier.testTag("hydrograph_range_${stationIdTagPrefix}_${range.id}")
            )
        }
    }
}

/** Water level over the chosen period with dashed lines for the flood levels. Only real readings are drawn. */
@Composable
fun RiverHydrographCurve(
    station: RiverGaugeStation,
    timeRange: HydrographTimeRange = HydrographTimeRange.HOURS_24,
    modifier: Modifier = Modifier
) {
    val points = station.getLevelsForTimeRange(timeRange)
    if (points.size < 2) {
        Text(
            text = stringResource(R.string.no_readings_period),
            style = MaterialTheme.typography.bodySmall,
            color = AdaptiveSecondaryText,
            modifier = modifier.padding(vertical = 12.dp)
        )
        return
    }
    val minVal = (minOf(points.min(), station.alertLevelM) * 0.85).coerceAtLeast(0.0)
    val maxVal = maxOf(points.max(), station.majorFloodLevelM) * 1.08
    val range = (maxVal - minVal).coerceAtLeast(0.5)
    val lineColor = station.floodStatus.color
    val labelColor = AdaptiveSecondaryText
    val thresholds = listOf(
        station.majorFloodLevelM to AlertRed,
        station.minorFloodLevelM to WarningAmber,
        station.alertLevelM to AdvisoryYellow
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hydrograph_curve_${station.id}"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(vertical = 8.dp)
        ) {
            val labelWidth = 52.dp.toPx()
            val plotW = size.width - labelWidth
            val h = size.height
            fun y(level: Double) = h - ((level - minVal) / range).toFloat().coerceIn(0f, 1f) * h
            fun x(i: Int) = i.toFloat() / (points.size - 1) * plotW

            val paint = Paint().apply {
                isAntiAlias = true
                textSize = 10.dp.toPx()
            }
            val currentY = y(points.last())
            thresholds.forEach { (level, color) ->
                val ty = y(level)
                drawLine(
                    color = color.copy(alpha = 0.8f),
                    start = Offset(0f, ty),
                    end = Offset(plotW, ty),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )
                // The current level's label wins when the two would overlap
                if (kotlin.math.abs(ty - currentY) >= 14.dp.toPx()) {
                    paint.color = color.toArgb()
                    drawContext.canvas.nativeCanvas.drawText(m(level), plotW + 6.dp.toPx(), ty + 4.dp.toPx(), paint)
                }
            }

            val line = Path()
            val fill = Path()
            points.forEachIndexed { i, level ->
                val px = x(i)
                val py = y(level)
                if (i == 0) {
                    line.moveTo(px, py)
                    fill.moveTo(px, h)
                    fill.lineTo(px, py)
                } else {
                    line.lineTo(px, py)
                    fill.lineTo(px, py)
                }
            }
            fill.lineTo(x(points.lastIndex), h)
            fill.close()
            drawPath(fill, brush = Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.3f), Color.Transparent)))
            drawPath(line, color = lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
            drawCircle(color = lineColor, radius = 4.5.dp.toPx(), center = Offset(x(points.lastIndex), y(points.last())))

            paint.color = labelColor.toArgb()
            drawContext.canvas.nativeCanvas.drawText(m(points.last()), plotW + 6.dp.toPx(), y(points.last()) + 4.dp.toPx(), paint)
        }
        Row(modifier = Modifier.fillMaxWidth().padding(end = 52.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.hours_ago, timeRange.hours), style = MaterialTheme.typography.labelSmall, color = AdaptiveSecondaryText)
            Text(stringResource(R.string.now), style = MaterialTheme.typography.labelSmall, color = AdaptiveSecondaryText)
        }
    }
}
