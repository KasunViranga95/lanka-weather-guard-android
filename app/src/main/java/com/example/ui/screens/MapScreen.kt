package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import com.example.R
import com.example.model.CityWeatherSnapshot
import com.example.model.SriLankaRegion
import com.example.model.ZoomEarthLayer
import com.example.ui.theme.AdaptiveBorderColor
import com.example.ui.theme.AdaptiveElevatedColor
import com.example.ui.theme.AdaptivePrimaryText
import com.example.ui.theme.AdaptiveSecondaryText
import com.example.ui.theme.AdaptiveSurfaceColor
import kotlin.math.roundToInt

/**
 * Map tab: the live Windy.com map (rain, radar, clouds, wind, temperature) centred on the chosen place,
 * plus current conditions in towns across the island.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    selectedRegion: SriLankaRegion,
    selectedNearestCity: String,
    selectedLayer: ZoomEarthLayer,
    onLayerSelected: (ZoomEarthLayer) -> Unit,
    islandSnapshots: List<CityWeatherSnapshot>,
    onOpenExternalUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val (lat, lon) = remember(selectedRegion, selectedNearestCity) { selectedRegion.getCoordinatesForCity(selectedNearestCity) }
    val embedUrl = selectedLayer.buildWindyEmbedUrl(lat, lon, zoom = 8)
    var reloadKey by remember { mutableIntStateOf(0) }
    var showTowns by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().testTag("live_radar_screen")) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ZoomEarthLayer.entries.toList(), key = { it.name }) { layer ->
                FilterChip(
                    selected = layer == selectedLayer,
                    onClick = { onLayerSelected(layer) },
                    label = { Text(stringResource(layer.labelRes)) },
                    modifier = Modifier.testTag("map_layer_${layer.name.lowercase()}")
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(AdaptiveSurfaceColor)
        ) {
            WindyMap(url = embedUrl, reloadKey = reloadKey, modifier = Modifier.fillMaxSize())
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (islandSnapshots.isNotEmpty()) {
                FilledTonalButton(onClick = { showTowns = true }, modifier = Modifier.testTag("map_towns_button")) {
                    Icon(Icons.Default.LocationCity, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.towns_now), modifier = Modifier.padding(start = 8.dp))
                }
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { reloadKey++ }) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.reload_map), tint = AdaptiveSecondaryText)
            }
            IconButton(onClick = { onOpenExternalUrl(selectedLayer.buildWindyBrowserUrl(lat, lon, zoom = 8)) }) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = stringResource(R.string.open_full_map), tint = AdaptiveSecondaryText)
            }
        }
    }

    if (showTowns) {
        ModalBottomSheet(
            onDismissRequest = { showTowns = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = AdaptiveSurfaceColor
        ) {
            TownsList(islandSnapshots)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WindyMap(url: String, reloadKey: Int, modifier: Modifier = Modifier) {
    var loadedKey by remember { mutableStateOf<Pair<String, Int>?>(null) }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    // Windy serves its full map to normal mobile browsers; the default WebView agent gets a blank map
                    userAgentString = settings.userAgentString.replace("; wv)", ")").replace(Regex("Version/[0-9.]+ "), "")
                }
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()
            }
        },
        update = { webView ->
            val key = url to reloadKey
            if (loadedKey != key) {
                loadedKey = key
                // Windy's map gets no height when it is the top-level page in a WebView (it stays grey),
                // so it is shown inside a full-size frame, loaded once the view has its real size.
                webView.doOnLayout { v ->
                    if (v.width > 0 && v.height > 0) {
                        (v as WebView).loadDataWithBaseURL("https://embed.windy.com/", windyFrame(url), "text/html", "UTF-8", null)
                    }
                }
            }
        }
    )
}

@Composable
private fun TownsList(towns: List<CityWeatherSnapshot>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.towns_now),
                style = MaterialTheme.typography.titleLarge,
                color = AdaptivePrimaryText,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(towns.sortedByDescending { it.rainTodayMm }, key = { it.name }) { town ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(town.name, style = MaterialTheme.typography.bodyLarge, color = AdaptivePrimaryText)
                    Text(
                        text = stringResource(R.string.gusts_n, town.windGustKmh.roundToInt()),
                        style = MaterialTheme.typography.bodySmall,
                        color = AdaptiveSecondaryText
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFF4285F4), modifier = Modifier.size(14.dp))
                    Text(
                        text = stringResource(R.string.mm_today, town.rainTodayMm.roundToInt()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4285F4)
                    )
                }
                Text(
                    text = "${town.tempC.roundToInt()}°",
                    style = MaterialTheme.typography.titleLarge,
                    color = AdaptivePrimaryText,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(AdaptiveElevatedColor)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            HorizontalDivider(color = AdaptiveBorderColor)
        }
    }
}

/** A page that holds the Windy embed in a frame filling the whole view. */
private fun windyFrame(url: String): String = """
    <!DOCTYPE html>
    <html><head>
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
    <style>html, body { margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden; background: #1d2026; }
    iframe { position: fixed; inset: 0; width: 100%; height: 100%; border: 0; }</style>
    </head><body><iframe src="$url" allowfullscreen></iframe></body></html>
""".trimIndent()
