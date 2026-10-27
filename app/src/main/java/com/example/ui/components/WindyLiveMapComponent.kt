package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.RegionalWeatherReport
import com.example.model.SriLankaRegion
import com.example.model.ZoomEarthLayer
import com.example.model.getDbzMeaningForValue
import com.example.ui.theme.AlertRed
import com.example.ui.theme.MonsoonTeal
import com.example.ui.theme.NavyAbyss
import com.example.ui.theme.NavyCardBorder
import com.example.ui.theme.NavyElevated
import com.example.ui.theme.NavySurface
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.WarningAmber
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class InAppMapEngine(val label: String) {
    LEAFLET_2D_MAP("2D Live Map"),
    WINDY_IFRAME("Windy Embed"),
    NATIVE_CANVAS("Vector Map")
}

private data class SriLankaCityPin(
    val name: String,
    val district: String,
    val lat: Double,
    val lon: Double,
    val tempC: Double,
    val rainMmH: Double,
    val windKmh: Int,
    val dbz: Int
)

private val ISLAND_OUTLINE_COORDS = listOf(
    0.28f to 0.04f, 0.37f to 0.06f, 0.43f to 0.14f, 0.56f to 0.22f,
    0.72f to 0.32f, 0.80f to 0.45f, 0.86f to 0.57f, 0.87f to 0.70f,
    0.78f to 0.83f, 0.64f to 0.91f, 0.48f to 0.96f, 0.34f to 0.93f,
    0.25f to 0.83f, 0.20f to 0.72f, 0.18f to 0.59f, 0.14f to 0.45f,
    0.20f to 0.35f, 0.18f to 0.25f, 0.24f to 0.17f, 0.23f to 0.08f
)

private fun coordToFraction(lat: Double, lon: Double): Pair<Float, Float> {
    val rawX = ((lon - 79.50) / (81.95 - 79.50) * 0.74 + 0.13).toFloat()
    val rawY = ((9.85 - lat) / (9.85 - 5.85) * 0.90 + 0.05).toFloat()
    return rawX.coerceAtLeast(0.10f).coerceAtMost(0.90f) to
        rawY.coerceAtLeast(0.04f).coerceAtMost(0.96f)
}

/**
 * Full-Window In-App Live Weather & Radar Map for Sri Lanka.
 * - Compact single-line header + single-line horizontal scrollable chip strip so the map fills the
 *   entire window without any vertical header stretching or empty dark space.
 * - Uses keyless Esri World Topo/Street Map + OpenStreetMap tiles + RainViewer Live Weather Radar
 *   tiles + 2D HTML5 Canvas animated wind streamlines + properly sized `iconSize: null` location
 *   badges for all Sri Lankan cities.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ZoomEarthLiveWebMap(
    selectedRegion: SriLankaRegion,
    nearestCity: String,
    selectedLayer: ZoomEarthLayer,
    regionalReport: RegionalWeatherReport,
    onOpenInExternalBrowser: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val (cityLat, cityLon) = remember(selectedRegion.id, nearestCity) {
        selectedRegion.getCoordinatesForCity(nearestCity)
    }

    var currentZoom by remember(selectedRegion.id, nearestCity) {
        mutableIntStateOf(8)
    }
    var mapEngine by remember {
        mutableStateOf(InAppMapEngine.LEAFLET_2D_MAP)
    }
    var reloadNonce by remember { mutableIntStateOf(0) }
    var isWebLoading by remember { mutableStateOf(true) }

    val estimatedDbz = when {
        regionalReport.precipitationMm >= 8.0 || regionalReport.rain24hMm >= 180.0 -> 55
        regionalReport.precipitationMm >= 5.0 || regionalReport.rain24hMm >= 120.0 -> 48
        regionalReport.precipitationMm >= 2.5 || regionalReport.rain24hMm >= 70.0 -> 39
        regionalReport.precipitationMm >= 1.0 -> 30
        else -> 22
    }
    val dbzMeaning = getDbzMeaningForValue(estimatedDbz)

    val allSriLankaCityPins = remember(selectedRegion.id, nearestCity, regionalReport, cityLat, cityLon, estimatedDbz) {
        val base = mutableListOf(
            SriLankaCityPin("Colombo", "Colombo", 6.9344, 79.8428, 28.4, 5.2, 58, 46),
            SriLankaCityPin("Ratnapura", "Ratnapura", 6.6828, 80.3992, 26.2, 8.6, 62, 56),
            SriLankaCityPin("Galle", "Galle", 6.0535, 80.2210, 27.8, 4.8, 64, 44),
            SriLankaCityPin("Matara", "Matara", 5.9549, 80.5550, 28.1, 4.4, 60, 42),
            SriLankaCityPin("Kandy", "Kandy", 7.2906, 80.6337, 23.6, 3.8, 44, 38),
            SriLankaCityPin("Nuwara Eliya", "Nuwara Eliya", 6.9497, 80.7891, 16.4, 4.5, 52, 41),
            SriLankaCityPin("Kalutara", "Kalutara", 6.5854, 79.9607, 27.9, 6.1, 56, 49),
            SriLankaCityPin("Gampaha", "Gampaha", 7.0873, 80.0144, 28.2, 4.9, 52, 43),
            SriLankaCityPin("Anuradhapura", "Anuradhapura", 8.3114, 80.4037, 30.8, 1.4, 36, 24),
            SriLankaCityPin("Jaffna", "Jaffna", 9.6615, 80.0255, 31.4, 0.8, 42, 19),
            SriLankaCityPin("Trincomalee", "Trincomalee", 8.5874, 81.2152, 31.2, 1.1, 38, 21),
            SriLankaCityPin("Batticaloa", "Batticaloa", 7.7310, 81.6747, 30.6, 1.6, 40, 25),
            SriLankaCityPin("Hambantota", "Hambantota", 6.1246, 81.1185, 29.8, 1.9, 54, 27)
        )
        // Remove duplicate or same-district pin so the user's residing city badge never collides with its district capital
        base.removeAll {
            it.name.equals(nearestCity, ignoreCase = true) ||
                it.district.equals(selectedRegion.name, ignoreCase = true) ||
                (abs(it.lat - cityLat) < 0.12 && abs(it.lon - cityLon) < 0.12)
        }
        base.add(
            0,
            SriLankaCityPin(
                name = nearestCity,
                district = selectedRegion.name,
                lat = cityLat,
                lon = cityLon,
                tempC = regionalReport.currentTempC,
                rainMmH = regionalReport.precipitationMm,
                windKmh = regionalReport.windGustKmh.roundToInt(),
                dbz = estimatedDbz
            )
        )
        base
    }

    val windyBrowserUrl = remember(selectedLayer, cityLat, cityLon, currentZoom) {
        selectedLayer.buildWindyBrowserUrl(
            lat = cityLat,
            lon = cityLon,
            zoom = currentZoom
        )
    }

    val leaflet2dHtml = remember(selectedRegion.id, nearestCity, selectedLayer, currentZoom, regionalReport, reloadNonce) {
        buildLeaflet2dWeatherHtml(
            pinnedCity = nearestCity,
            centerLat = cityLat,
            centerLon = cityLon,
            zoom = currentZoom,
            layer = selectedLayer,
            pins = allSriLankaCityPins,
            pressureHpa = regionalReport.pressureHpa.roundToInt()
        )
    }

    val windyEmbedHtml = remember(selectedRegion.id, nearestCity, selectedLayer, currentZoom, reloadNonce) {
        val embedUrl = selectedLayer.buildWindyEmbedUrl(
            lat = cityLat,
            lon = cityLon,
            zoom = currentZoom
        )
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <style>
                html, body { margin: 0; padding: 0; width: 100vw; height: 100vh; background: #06162B; overflow: hidden; }
                iframe { width: 100vw; height: 100vh; border: 0; display: block; }
            </style>
        </head>
        <body>
            <iframe src="$embedUrl" allowfullscreen></iframe>
        </body>
        </html>
        """.trimIndent()
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(NavyAbyss)
            .border(1.dp, RadarCyan.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            .testTag("zoom_earth_live_web_map")
    ) {
        // 1. Compact Top Header (Never wraps vertically — leaves maximum window area for the map)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(NavySurface)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = RadarCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "📍 $nearestCity, ${selectedRegion.name} • ${selectedLayer.label}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { reloadNonce++ },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("zoom_earth_reload_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload Map",
                            tint = RadarCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Surface(
                        color = RadarCyan.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clickable { onOpenInExternalBrowser(windyBrowserUrl) }
                            .testTag("zoom_earth_open_browser_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open Windy.com in Browser",
                                tint = RadarCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Windy Web",
                                style = MaterialTheme.typography.labelSmall,
                                color = RadarCyan,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Single-Line Scrollable Control Strip (Guaranteed 1 line, zero vertical stretching)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(vertical = 1.dp)
            ) {
                item {
                    Surface(
                        color = if (currentZoom >= 10) RadarCyan.copy(alpha = 0.24f) else NavyElevated,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clickable { currentZoom = 11 }
                            .testTag("zoom_earth_focus_city_pill")
                    ) {
                        Text(
                            text = "📍 $nearestCity (11x)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (currentZoom >= 10) RadarCyan else Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                item {
                    Surface(
                        color = if (currentZoom in 8..9) RadarCyan.copy(alpha = 0.24f) else NavyElevated,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clickable { currentZoom = 8 }
                            .testTag("windy_district_zoom_pill")
                    ) {
                        Text(
                            text = "🗺 District (8x)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (currentZoom in 8..9) RadarCyan else Color.White,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                item {
                    Surface(
                        color = if (currentZoom <= 7) RadarCyan.copy(alpha = 0.24f) else NavyElevated,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clickable { currentZoom = 7 }
                            .testTag("windy_embed_pill")
                    ) {
                        Text(
                            text = "🇱🇰 Whole Island (7x)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (currentZoom <= 7) RadarCyan else MonsoonTeal,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                items(InAppMapEngine.entries.toList(), key = { it.name }) { engine ->
                    val active = mapEngine == engine
                    Surface(
                        color = if (active) MonsoonTeal.copy(alpha = 0.26f) else NavyElevated,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { mapEngine = engine }
                    ) {
                        Text(
                            text = engine.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) MonsoonTeal else Color.White.copy(alpha = 0.80f),
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // 2. Full-Window Map Viewport (Fills 100% of the window space between header and footer)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (mapEngine == InAppMapEngine.NATIVE_CANVAS) {
                NativeMultiCityWeatherCanvas(
                    pins = allSriLankaCityPins,
                    selectedLayer = selectedLayer,
                    currentZoom = currentZoom,
                    pinLat = cityLat,
                    pinLon = cityLon
                )
            } else {
                val htmlPayload = if (mapEngine == InAppMapEngine.LEAFLET_2D_MAP) {
                    leaflet2dHtml
                } else {
                    windyEmbedHtml
                }

                key(mapEngine, selectedRegion.id, nearestCity, selectedLayer, currentZoom, reloadNonce) {
                    var webViewRef by remember { mutableStateOf<WebView?>(null) }

                    DisposableEffect(Unit) {
                        onDispose {
                            try {
                                webViewRef?.stopLoading()
                                webViewRef?.destroy()
                            } catch (_: Exception) {
                            }
                        }
                    }

                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewRef = this
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setBackgroundColor(android.graphics.Color.parseColor("#071C33"))
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    loadsImagesAutomatically = true
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                    userAgentString =
                                        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                                }
                                webChromeClient = WebChromeClient()
                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(
                                        view: WebView?,
                                        url: String?,
                                        favicon: Bitmap?
                                    ) {
                                        isWebLoading = true
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isWebLoading = false
                                        view?.evaluateJavascript(
                                            "if (window.slMap) { window.slMap.invalidateSize(); }",
                                            null
                                        )
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        if (request?.isForMainFrame == true) {
                                            isWebLoading = false
                                            mapEngine = InAppMapEngine.NATIVE_CANVAS
                                        }
                                    }

                                    override fun onRenderProcessGone(
                                        view: WebView?,
                                        detail: RenderProcessGoneDetail?
                                    ): Boolean {
                                        try {
                                            (view?.parent as? ViewGroup)?.removeView(view)
                                            view?.destroy()
                                        } catch (_: Exception) {
                                        }
                                        isWebLoading = false
                                        mapEngine = if (mapEngine == InAppMapEngine.WINDY_IFRAME) {
                                            InAppMapEngine.LEAFLET_2D_MAP
                                        } else {
                                            InAppMapEngine.NATIVE_CANVAS
                                        }
                                        return true
                                    }
                                }

                                loadDataWithBaseURL(
                                    "https://openstreetmap.org/",
                                    htmlPayload,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            }
                        },
                        update = { view ->
                            view.evaluateJavascript(
                                "if (window.slMap) { window.slMap.invalidateSize(); }",
                                null
                            )
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isWebLoading) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x8806162B))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(NavySurface)
                                    .border(1.dp, RadarCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = RadarCyan,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Loading Sri Lanka Live Weather Map...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Compact Telemetry & dBZ Meaning Bar Below Map
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(NavySurface)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📍 $nearestCity (${selectedRegion.name}) • $estimatedDbz dBZ — ${dbzMeaning.shortTitle}",
                    style = MaterialTheme.typography.labelMedium,
                    color = dbzMeaning.badgeColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = dbzMeaning.rainRateMmPerHour,
                    style = MaterialTheme.typography.labelSmall,
                    color = RadarCyan,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "🌡 ${String.format(Locale.US, "%.1f°C", regionalReport.currentTempC)} (Feels ${regionalReport.feelsLikeC.roundToInt()}°C) • 🌧 ${String.format(Locale.US, "%.1f mm/h", regionalReport.precipitationMm)} • 💨 Gusts ${regionalReport.windGustKmh.roundToInt()} km/h • ${regionalReport.pressureHpa.roundToInt()} hPa",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Builds a full-window 100% WebGL-free interactive 2D geographic weather map of Sri Lanka using
 * Leaflet.js with keyless Esri World Street/Topo Map + OpenStreetMap tiles, RainViewer live radar
 * tiles, 2D HTML5 Canvas animated monsoon wind streamlines, and properly sized `iconSize: null`
 * badges for all Sri Lankan cities.
 */
private fun buildLeaflet2dWeatherHtml(
    pinnedCity: String,
    centerLat: Double,
    centerLon: Double,
    zoom: Int,
    layer: ZoomEarthLayer,
    pins: List<SriLankaCityPin>,
    pressureHpa: Int
): String {
    val pinsJsonArray = pins.joinToString(prefix = "[", postfix = "]") { pin ->
        val metricLabel = when (layer) {
            ZoomEarthLayer.RADAR ->
                "${pin.dbz} dBZ • ${String.format(Locale.US, "%.1f", pin.rainMmH)}mm/h"
            ZoomEarthLayer.TEMPERATURE ->
                "${String.format(Locale.US, "%.1f°C", pin.tempC)}"
            ZoomEarthLayer.WIND_SPEED ->
                "${pin.windKmh}km/h • ${pin.tempC.roundToInt()}°C"
            ZoomEarthLayer.PRESSURE ->
                "$pressureHpa hPa • ${pin.tempC.roundToInt()}°C"
            else ->
                "${pin.tempC.roundToInt()}°C • ${String.format(Locale.US, "%.1f", pin.rainMmH)}mm/h • ${pin.dbz} dBZ"
        }
        val hexColor = when {
            pin.dbz >= 52 -> "#FF3B5C"
            pin.dbz >= 45 -> "#FFAA00"
            pin.dbz >= 35 -> "#FFD600"
            else -> "#00E5FF"
        }
        """
        {
          "name": "${pin.name.replace("\"", "")}",
          "lat": ${String.format(Locale.US, "%.4f", pin.lat)},
          "lon": ${String.format(Locale.US, "%.4f", pin.lon)},
          "metric": "${metricLabel.replace("\"", "")}",
          "color": "$hexColor",
          "isUser": ${pin.name.equals(pinnedCity, ignoreCase = true)}
        }
        """.trimIndent()
    }

    val tileUrl = if (layer == ZoomEarthLayer.SATELLITE) {
        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
    } else {
        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}"
    }

    return """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<style>
  * { box-sizing: border-box; }
  html, body {
    margin: 0;
    padding: 0;
    width: 100vw;
    height: 100vh;
    background: #071C33;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
    overflow: hidden;
  }
  #map {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    width: 100vw;
    height: 100vh;
    z-index: 1;
    background: #071C33;
  }
  #windCanvas {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    width: 100vw;
    height: 100vh;
    z-index: 400;
    pointer-events: none;
  }
  .sl-badge-wrap {
    display: inline-block;
    transform: translate(-50%, -50%);
    pointer-events: auto;
  }
  .sl-city-label {
    display: inline-block;
    background: rgba(6, 20, 39, 0.92);
    border: 1.5px solid #00E5FF;
    border-radius: 8px;
    color: #FFFFFF;
    padding: 3px 7px;
    font-size: 11px;
    line-height: 1.25;
    font-weight: 700;
    white-space: nowrap;
    box-shadow: 0 2px 6px rgba(0,0,0,0.65);
  }
  .sl-user-label {
    display: inline-block;
    background: rgba(12, 34, 64, 0.96);
    border: 2px solid #FF3B5C;
    border-radius: 9px;
    color: #FFFFFF;
    padding: 4px 8px;
    font-size: 11.5px;
    line-height: 1.25;
    font-weight: 800;
    white-space: nowrap;
    box-shadow: 0 0 12px rgba(255,59,92,0.55);
  }
  .sl-sub {
    color: #00E5FF;
    font-size: 10px;
    font-weight: 700;
    display: block;
    margin-top: 1px;
  }
  .leaflet-control-attribution { display: none !important; }
</style>
</head>
<body>
<div id="map"></div>
<canvas id="windCanvas"></canvas>
<script>
  const centerLat = ${String.format(Locale.US, "%.4f", centerLat)};
  const centerLon = ${String.format(Locale.US, "%.4f", centerLon)};
  const initZoom = $zoom;
  const cities = $pinsJsonArray;

  try {
    const map = L.map('map', {
      center: [centerLat, centerLon],
      zoom: initZoom,
      zoomControl: true,
      preferCanvas: true
    });
    window.slMap = map;

    // Keyless Esri World Street / Satellite Map of Sri Lanka (Zero API Key watermark)
    L.tileLayer('$tileUrl', {
      maxZoom: 18
    }).addTo(map);

    // Fetch live RainViewer Weather Radar tile layer for Sri Lanka
    fetch('https://api.rainviewer.com/public/weather-maps.json')
      .then(r => r.json())
      .then(data => {
        if (data && data.radar && data.radar.past && data.radar.past.length > 0) {
          const latest = data.radar.past[data.radar.past.length - 1];
          L.tileLayer(data.host + latest.path + '/256/{z}/{x}/{y}/2/1_1.png', {
            opacity: 0.65,
            zIndex: 250
          }).addTo(map);
        }
      })
      .catch(() => {});

    // Render all Sri Lankan City Markers & Properly Sized Location Name Badges
    cities.forEach(c => {
      L.circle([c.lat, c.lon], {
        color: c.color,
        fillColor: c.color,
        fillOpacity: c.isUser ? 0.28 : 0.16,
        weight: c.isUser ? 2 : 1.2,
        radius: c.isUser ? 12000 : 8500
      }).addTo(map);

      const iconHtml = '<div class="sl-badge-wrap"><div class="' + (c.isUser ? 'sl-user-label' : 'sl-city-label') + '">' +
        (c.isUser ? '📍 ' : '') + c.name +
        '<span class="sl-sub">' + c.metric + '</span></div></div>';

      const customIcon = L.divIcon({
        className: '',
        iconSize: null,
        html: iconHtml
      });

      L.marker([c.lat, c.lon], { icon: customIcon }).addTo(map);
    });

    setTimeout(() => { map.invalidateSize(); }, 120);
    setTimeout(() => { map.invalidateSize(); }, 450);
    window.addEventListener('resize', () => { map.invalidateSize(); });
  } catch (e) {}

  // 2D HTML5 Canvas Wind Streamlines (Zero WebGL)
  const canvas = document.getElementById('windCanvas');
  const ctx = canvas.getContext('2d');
  function resizeCanvas() {
    canvas.width = window.innerWidth || document.documentElement.clientWidth || 360;
    canvas.height = window.innerHeight || document.documentElement.clientHeight || 420;
  }
  window.addEventListener('resize', resizeCanvas);
  resizeCanvas();

  const particles = Array.from({ length: 40 }, (_, i) => ({
    x: (i * 41) % Math.max(1, canvas.width),
    y: (i * 59) % Math.max(1, canvas.height),
    len: 18 + (i % 12),
    speed: 1.15 + (i % 3) * 0.4
  }));

  function animateWind() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.strokeStyle = 'rgba(0, 200, 255, 0.55)';
    ctx.lineWidth = 2.0;
    ctx.lineCap = 'round';
    particles.forEach(p => {
      ctx.beginPath();
      ctx.moveTo(p.x, p.y);
      ctx.lineTo(p.x + p.len, p.y - p.len * 0.55);
      ctx.stroke();
      p.x += p.speed;
      p.y -= p.speed * 0.55;
      if (p.x > canvas.width) p.x = 0;
      if (p.y < 0) p.y = canvas.height;
    });
    requestAnimationFrame(animateWind);
  }
  animateWind();
</script>
</body>
</html>
    """.trimIndent()
}

@Composable
private fun NativeMultiCityWeatherCanvas(
    pins: List<SriLankaCityPin>,
    selectedLayer: ZoomEarthLayer,
    currentZoom: Int,
    pinLat: Double,
    pinLon: Double
) {
    val (pinXFrac, pinYFrac) = remember(pinLat, pinLon) {
        coordToFraction(pinLat, pinLon)
    }
    var userPanOffset by remember { mutableStateOf(Offset.Zero) }
    val canvasZoomScale = when {
        currentZoom >= 10 -> 1.75f
        currentZoom >= 8 -> 1.25f
        else -> 1.0f
    }

    val infiniteTransition = rememberInfiniteTransition(label = "native_windy_loop")
    val windStreamPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "native_wind_phase"
    )
    val pinPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "native_pin_pulse"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val nextX = (userPanOffset.x + dragAmount.x).coerceAtLeast(-220f).coerceAtMost(220f)
                    val nextY = (userPanOffset.y + dragAmount.y).coerceAtLeast(-220f).coerceAtMost(220f)
                    userPanOffset = Offset(nextX, nextY)
                }
            }
    ) {
        val w = size.width
        val h = size.height
        if (w <= 16f || h <= 16f) return@Canvas

        val focusX = if (currentZoom >= 8) pinXFrac else 0.50f
        val focusY = if (currentZoom >= 8) pinYFrac else 0.50f

        fun sx(nx: Float): Float =
            (nx - focusX) * w * canvasZoomScale + w * 0.5f + userPanOffset.x

        fun sy(ny: Float): Float =
            (ny - focusY) * h * canvasZoomScale + h * 0.5f + userPanOffset.y

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF06162B), Color(0xFF0A2442), Color(0xFF051326))
            )
        )

        val islandPath = Path().apply {
            ISLAND_OUTLINE_COORDS.forEachIndexed { idx, (nx, ny) ->
                val px = sx(nx)
                val py = sy(ny)
                if (idx == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        }

        drawPath(
            path = islandPath,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF103052), Color(0xFF17406A), Color(0xFF0D2744)),
                start = Offset(sx(0.2f), sy(0.05f)),
                end = Offset(sx(0.8f), sy(0.95f))
            )
        )
        drawPath(
            path = islandPath,
            color = RadarCyan.copy(alpha = 0.80f),
            style = Stroke(width = 2.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        for (row in 0..4) {
            for (col in 0..3) {
                val seedIdx = row * 4 + col
                val baseNx = 0.16f + col * 0.18f
                val baseNy = 0.14f + row * 0.18f
                val phase = (windStreamPhase + seedIdx * 0.07f) % 1f
                val angleRad = Math.toRadians(235.0)
                val moveDist = 0.06f * phase
                val curNx = baseNx - (cos(angleRad) * moveDist).toFloat()
                val curNy = baseNy + (sin(angleRad) * moveDist).toFloat()
                val x0 = sx(curNx)
                val y0 = sy(curNy)
                drawLine(
                    color = RadarCyan.copy(alpha = 0.55f),
                    start = Offset(x0, y0),
                    end = Offset(x0 + 22f, y0 - 12f),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )
            }
        }

        val namePaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val metricPaint = Paint().apply {
            isAntiAlias = true
            color = RadarCyan.toArgb()
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        pins.forEachIndexed { index, pin ->
            val (nx, ny) = coordToFraction(pin.lat, pin.lon)
            val cx = sx(nx)
            val cy = sy(ny)
            val isUser = index == 0
            val dbzInfo = getDbzMeaningForValue(pin.dbz)

            val rad = (w * 0.12f * canvasZoomScale).coerceAtLeast(10f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(dbzInfo.badgeColor.copy(alpha = 0.32f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = rad
                ),
                radius = rad,
                center = Offset(cx, cy)
            )

            if (isUser) {
                drawCircle(
                    color = RadarCyan.copy(alpha = 0.35f),
                    radius = 20f * pinPulse,
                    center = Offset(cx, cy)
                )
            }

            drawCircle(
                color = if (isUser) AlertRed else dbzInfo.badgeColor,
                radius = if (isUser) 7.5f else 5.5f,
                center = Offset(cx, cy)
            )

            val titleText = pin.name
            val subText = when (selectedLayer) {
                ZoomEarthLayer.RADAR -> "${pin.dbz} dBZ"
                ZoomEarthLayer.TEMPERATURE -> "${pin.tempC.roundToInt()}°C"
                ZoomEarthLayer.WIND_SPEED -> "${pin.windKmh}km/h"
                else -> "${pin.tempC.roundToInt()}°C • ${pin.dbz} dBZ"
            }

            val pillW = maxOf(namePaint.measureText(titleText), metricPaint.measureText(subText)) + 18f
            val pillH = 46f
            val maxLeft = (w - pillW - 6f).coerceAtLeast(6f)
            val maxTop = (h - pillH - 6f).coerceAtLeast(6f)
            val pLeft = (cx + 9f).coerceAtLeast(6f).coerceAtMost(maxLeft)
            val pTop = (cy - pillH / 2f).coerceAtLeast(6f).coerceAtMost(maxTop)

            drawRoundRect(
                color = if (isUser) Color(0xEE082442) else Color(0xCC061527),
                topLeft = Offset(pLeft, pTop),
                size = Size(pillW, pillH),
                cornerRadius = CornerRadius(10f, 10f)
            )
            drawRoundRect(
                color = if (isUser) RadarCyan else NavyCardBorder,
                topLeft = Offset(pLeft, pTop),
                size = Size(pillW, pillH),
                cornerRadius = CornerRadius(10f, 10f),
                style = Stroke(width = if (isUser) 2f else 1f)
            )

            drawContext.canvas.nativeCanvas.apply {
                drawText(titleText, pLeft + 9f, pTop + 19f, namePaint)
                drawText(subText, pLeft + 9f, pTop + 39f, metricPaint)
            }
        }
    }
}
