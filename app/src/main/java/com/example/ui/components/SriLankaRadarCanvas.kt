package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.RadarFrame
import com.example.model.RadarLayerType
import com.example.model.RiverGaugeStation
import com.example.model.SriLankaRegion
import com.example.ui.theme.AdvisoryYellow
import com.example.ui.theme.AlertRed
import com.example.ui.theme.MonsoonTeal
import com.example.ui.theme.NavyAbyss
import com.example.ui.theme.NavyCardBorder
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.WarningAmber
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun SriLankaRadarMapCanvas(
    currentFrame: RadarFrame,
    activeLayers: Set<RadarLayerType>,
    stations: List<RiverGaugeStation>,
    selectedRegion: SriLankaRegion,
    selectedStation: RiverGaugeStation?,
    onStationTapped: (RiverGaugeStation) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_sweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_deg"
    )
    val pulseFactor by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_ring"
    )

    //Normalized Sri Lanka coastline points (x in 0..1, y in 0..1)
    val sriLankaOutline = listOf(
        0.30f to 0.07f, // Point Pedro / Jaffna North
        0.37f to 0.09f, // Jaffna East
        0.41f to 0.16f, // Mullaitivu North
        0.53f to 0.24f, // Mullaitivu Coast
        0.68f to 0.33f, // Trincomalee
        0.75f to 0.46f, // Batticaloa North
        0.81f to 0.57f, // Batticaloa / Kalmunai
        0.82f to 0.70f, // Pottuvil / Arugam Bay
        0.74f to 0.82f, // Yala / Hambantota East
        0.61f to 0.90f, // Hambantota / Tangalle
        0.47f to 0.95f, // Dondra Head / Matara
        0.35f to 0.92f, // Galle
        0.28f to 0.82f, // Kalutara / Bentota
        0.24f to 0.72f, // Colombo
        0.22f to 0.59f, // Negombo / Chilaw
        0.19f to 0.46f, // Kalpitiya Peninsula
        0.24f to 0.36f, // Mannar Bay
        0.22f to 0.27f, // Mannar Island
        0.27f to 0.19f, // Pooneryn
        0.26f to 0.10f  // Jaffna West
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF040D1A), Color(0xFF081B31), Color(0xFF051324))
                )
            )
            .border(1.dp, NavyCardBorder, RoundedCornerShape(22.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("sri_lanka_radar_canvas")
                .pointerInput(stations) {
                    detectTapGestures { tapOffset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val nearest = stations.minByOrNull { st ->
                            hypot(
                                tapOffset.x - st.mapXFraction * w,
                                tapOffset.y - st.mapYFraction * h
                            )
                        }
                        if (nearest != null) {
                            val dist = hypot(
                                tapOffset.x - nearest.mapXFraction * w,
                                tapOffset.y - nearest.mapYFraction * h
                            )
                            if (dist <= 68f) {
                                onStationTapped(nearest)
                            }
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val radarCenter = Offset(w * 0.48f, h * 0.66f) // Central Highlands Radar Dome

            // 1. Polar Radar Range Rings & Lat/Lon Graticule
            val gridStroke = Stroke(
                width = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )
            for (frac in listOf(0.2f, 0.4f, 0.6f, 0.8f)) {
                drawLine(
                    color = RadarCyan.copy(alpha = 0.08f),
                    start = Offset(w * frac, 0f),
                    end = Offset(w * frac, h),
                    strokeWidth = 1f
                )
                drawLine(
                    color = RadarCyan.copy(alpha = 0.08f),
                    start = Offset(0f, h * frac),
                    end = Offset(w, h * frac),
                    strokeWidth = 1f
                )
            }
            for (ringR in listOf(0.18f, 0.34f, 0.50f)) {
                drawCircle(
                    color = RadarCyan.copy(alpha = 0.14f),
                    radius = w * ringR,
                    center = radarCenter,
                    style = gridStroke
                )
            }

            // 2. Build Sri Lanka Island Path
            val islandPath = Path().apply {
                sriLankaOutline.forEachIndexed { index, (nx, ny) ->
                    val px = nx * w
                    val py = ny * h
                    if (index == 0) moveTo(px, py) else lineTo(px, py)
                }
                close()
            }

            // Draw Coastal Glow & Island Landmass
            drawPath(
                path = islandPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF0E2947), Color(0xFF133659), Color(0xFF0B2239)),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
            )
            drawPath(
                path = islandPath,
                color = RadarCyan.copy(alpha = 0.55f),
                style = Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // 3. Central Highlands Topographic Contour & NBRO Landslide Layer
            val highlandsCenter = Offset(w * 0.52f, h * 0.69f)
            drawOval(
                color = Color(0xFF1B456B).copy(alpha = 0.45f),
                topLeft = Offset(highlandsCenter.x - w * 0.16f, highlandsCenter.y - h * 0.13f),
                size = Size(w * 0.32f, h * 0.24f)
            )
            if (activeLayers.contains(RadarLayerType.LANDSLIDE_ZONES)) {
                // Sabaragamuwa & Central Hill Country NBRO Red Hazard Contour
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AlertRed.copy(alpha = 0.42f),
                            WarningAmber.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.49f, h * 0.71f),
                        radius = w * 0.22f
                    ),
                    topLeft = Offset(w * 0.29f, h * 0.56f),
                    size = Size(w * 0.40f, h * 0.30f)
                )
            }

            // 4. Major Sri Lankan River Basins (Kelani, Kalu, Gin, Nilwala, Mahaweli, Malwathu)
            val riverPaths = listOf(
                // Kelani Ganga: Central Hills -> Avissawella -> Hanwella -> Colombo
                listOf(0.51f to 0.69f, 0.38f to 0.71f, 0.33f to 0.73f, 0.25f to 0.72f),
                // Kalu Ganga: Adam's Peak -> Ratnapura -> Putupaula -> Kalutara
                listOf(0.50f to 0.73f, 0.44f to 0.77f, 0.36f to 0.78f, 0.28f to 0.80f),
                // Gin Ganga: Sinharaja -> Baddegama -> Galle
                listOf(0.45f to 0.83f, 0.38f to 0.86f, 0.35f to 0.91f),
                // Nilwala Ganga: Deniyaya -> Panadugama -> Matara
                listOf(0.48f to 0.83f, 0.47f to 0.89f, 0.47f to 0.94f),
                // Mahaweli Ganga: Nuwara Eliya -> Kandy -> Manampitiya -> Trincomalee
                listOf(0.54f to 0.70f, 0.50f to 0.62f, 0.58f to 0.56f, 0.65f to 0.48f, 0.69f to 0.35f),
                // Malwathu Oya: Anuradhapura -> Thanthirimale -> Mannar
                listOf(0.46f to 0.42f, 0.39f to 0.35f, 0.25f to 0.29f)
            )
            riverPaths.forEach { pts ->
                val rPath = Path().apply {
                    pts.forEachIndexed { idx, (rx, ry) ->
                        if (idx == 0) moveTo(rx * w, ry * h) else lineTo(rx * w, ry * h)
                    }
                }
                drawPath(
                    path = rPath,
                    color = RadarCyan.copy(alpha = 0.65f),
                    style = Stroke(width = 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // 5. Live Doppler Precipitation Reflectivity Cells (dBZ)
            if (activeLayers.contains(RadarLayerType.PRECIPITATION_DBZ)) {
                currentFrame.activeCells.forEach { cell ->
                    val cx = cell.centerX * w
                    val cy = cell.centerY * h
                    val rx = cell.radiusX * w
                    val ry = cell.radiusY * h

                    // Outer Light Rain Band (25 dBZ Emerald/Cyan)
                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                SafeEmerald.copy(alpha = 0.34f),
                                RadarCyan.copy(alpha = 0.18f),
                                Color.Transparent
                            ),
                            center = Offset(cx, cy),
                            radius = maxOf(rx, ry) * 1.15f
                        ),
                        topLeft = Offset(cx - rx * 1.1f, cy - ry * 1.1f),
                        size = Size(rx * 2.2f, ry * 2.2f)
                    )

                    // Mid Moderate Rain Band (38 dBZ Yellow/Amber)
                    if (cell.intensityDbz >= 35) {
                        drawOval(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    AdvisoryYellow.copy(alpha = 0.45f),
                                    WarningAmber.copy(alpha = 0.25f),
                                    Color.Transparent
                                ),
                                center = Offset(cx, cy),
                                radius = maxOf(rx, ry) * 0.75f
                            ),
                            topLeft = Offset(cx - rx * 0.72f, cy - ry * 0.72f),
                            size = Size(rx * 1.44f, ry * 1.44f)
                        )
                    }

                    // Inner Heavy Convective Squall Core (50+ dBZ Red/Magenta)
                    if (cell.intensityDbz >= 46) {
                        drawOval(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    AlertRed.copy(alpha = 0.68f),
                                    WarningAmber.copy(alpha = 0.35f),
                                    Color.Transparent
                                ),
                                center = Offset(cx, cy),
                                radius = maxOf(rx, ry) * 0.44f
                            ),
                            topLeft = Offset(cx - rx * 0.42f, cy - ry * 0.42f),
                            size = Size(rx * 0.84f, ry * 0.84f)
                        )
                    }
                }
            }

            // 6. Monsoon Wind Streamlines (SW -> NE Flow)
            if (activeLayers.contains(RadarLayerType.WIND_VECTORS)) {
                val windAnchors = listOf(
                    0.16f to 0.82f, 0.22f to 0.70f, 0.30f to 0.88f,
                    0.38f to 0.74f, 0.46f to 0.62f, 0.56f to 0.50f,
                    0.28f to 0.52f, 0.58f to 0.78f
                )
                val driftFraction = (sweepAngle / 360f)
                windAnchors.forEach { (wx, wy) ->
                    val startX = (wx + driftFraction * 0.05f) * w
                    val startY = (wy - driftFraction * 0.035f) * h
                    val endX = startX + w * 0.075f
                    val endY = startY - h * 0.045f
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.Transparent, MonsoonTeal.copy(alpha = 0.75f)),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY)
                        ),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 2.4f,
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = MonsoonTeal,
                        radius = 3.2f,
                        center = Offset(endX, endY)
                    )
                }
            }

            // 7. Selected District Target Crosshair
            val regPos = Offset(selectedRegion.mapXFraction * w, selectedRegion.mapYFraction * h)
            drawCircle(
                color = RadarCyan.copy(alpha = 0.30f),
                radius = 18f * pulseFactor,
                center = regPos,
                style = Stroke(width = 2f)
            )
            drawCircle(
                color = RadarCyan,
                radius = 5f,
                center = regPos
            )

            // 8. Rotating Doppler Sweep Beam
            rotate(degrees = sweepAngle, pivot = radarCenter) {
                val beamRad = Math.toRadians(0.0)
                val beamEnd = Offset(
                    x = radarCenter.x + (w * 0.50f) * cos(beamRad).toFloat(),
                    y = radarCenter.y + (w * 0.50f) * sin(beamRad).toFloat()
                )
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(RadarCyan.copy(alpha = 0.85f), Color.Transparent),
                        start = radarCenter,
                        end = beamEnd
                    ),
                    start = radarCenter,
                    end = beamEnd,
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
