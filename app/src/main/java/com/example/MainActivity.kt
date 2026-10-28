package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Water
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.UserPreferencesStore
import com.example.model.AppTab
import com.example.model.DispatchedNotificationRecord
import com.example.model.FloodStatus
import com.example.ui.components.GeminiClarificationDialog
import com.example.ui.components.ResidingLocationDialog
import com.example.ui.screens.AlertsDashboardScreen
import com.example.ui.screens.LiveRadarScreen
import com.example.ui.screens.RiverWaterLevelsScreen
import com.example.ui.screens.SafetyHubScreen
import com.example.ui.theme.AdaptiveAppBackground
import com.example.ui.theme.AdaptiveCyanAccent
import com.example.ui.theme.AdaptiveElevatedColor
import com.example.ui.theme.AdaptivePrimaryText
import com.example.ui.theme.AdaptiveSecondaryText
import com.example.ui.theme.AdaptiveSurfaceColor
import com.example.ui.theme.AlertRed
import com.example.ui.theme.LankaWeatherGuardTheme
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.WarningAmber
import com.example.util.WeatherNotificationHelper
import com.example.viewmodel.WeatherViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var latestNotificationIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UserPreferencesStore.init(applicationContext)
        WeatherNotificationHelper.createNotificationChannel(this)
        latestNotificationIntent = intent
        enableEdgeToEdge()
        setContent {
            LankaWeatherGuardApp(
                notificationIntent = latestNotificationIntent,
                onNotificationIntentConsumed = { latestNotificationIntent = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        latestNotificationIntent = intent
    }
}

@Composable
fun LankaWeatherGuardApp(
    weatherViewModel: WeatherViewModel = viewModel(),
    notificationIntent: Intent? = null,
    onNotificationIntentConsumed: () -> Unit = {}
) {
    val uiState by weatherViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingNotificationRecord by remember { mutableStateOf<DispatchedNotificationRecord?>(null) }

    LankaWeatherGuardTheme(darkTheme = uiState.isDarkTheme) {
        val notificationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { _ ->
            val pending = pendingNotificationRecord
            if (pending != null) {
                weatherViewModel.selectAndPostGeneratedNotification(context, pending)
                pendingNotificationRecord = null
            } else {
                weatherViewModel.triggerNotificationForCurrentArea(context)
            }
        }

        val locationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                resolveDeviceLocation(
                    context = context,
                    onCoordinates = { lat, lon ->
                        weatherViewModel.onCoordinatesDetectedAndNotify(context, lat, lon)
                    },
                    onFallback = {
                        weatherViewModel.openResidingLocationDialog(permissionDenied = true)
                    }
                )
            } else {
                weatherViewModel.openResidingLocationDialog(permissionDenied = true)
            }
        }

        // Auto-sync weather metrics & river gauges every 10 min while the app is on screen
        val lifecycleOwner = LocalLifecycleOwner.current
        LaunchedEffect(lifecycleOwner) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                weatherViewModel.runAutoRefreshLoop()
            }
        }

        LaunchedEffect(Unit) {
            UserPreferencesStore.init(context.applicationContext)
            weatherViewModel.syncSavedPreferencesIfNeeded()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            weatherViewModel.triggerNotificationForCurrentArea(context)
        }

        LaunchedEffect(notificationIntent) {
            val activeIntent = notificationIntent ?: return@LaunchedEffect
            val fromNotification = activeIntent.getBooleanExtra(
                WeatherNotificationHelper.EXTRA_FROM_NOTIFICATION,
                false
            )
            if (fromNotification) {
                val regionId = activeIntent.getStringExtra(
                    WeatherNotificationHelper.EXTRA_NOTIFICATION_REGION_ID
                )
                val city = activeIntent.getStringExtra(
                    WeatherNotificationHelper.EXTRA_NOTIFICATION_CITY
                )
                weatherViewModel.handleNotificationClicked(
                    context = context,
                    regionId = regionId,
                    nearestCity = city
                )
                onNotificationIntentConsumed()
            }
        }

        val requestLocationAction = {
            val hasFine = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (hasFine || hasCoarse) {
                resolveDeviceLocation(
                    context = context,
                    onCoordinates = { lat, lon ->
                        weatherViewModel.onCoordinatesDetectedAndNotify(context, lat, lon)
                    },
                    onFallback = {
                        weatherViewModel.openResidingLocationDialog(permissionDenied = false)
                    }
                )
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }

        val sendNotificationForCurrentAreaAction = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                weatherViewModel.triggerNotificationForCurrentArea(context)
            }
        }

        if (uiState.showResidingLocationDialog) {
            ResidingLocationDialog(
                regions = uiState.regions,
                initialRegion = uiState.selectedRegion,
                initialCity = uiState.selectedNearestCity,
                initialInterval = uiState.selectedNotificationInterval,
                wasPermissionDenied = uiState.wasLocationPermissionDenied,
                onDismiss = { weatherViewModel.dismissResidingLocationDialog() },
                onConfirmSelection = { chosenRegion, chosenCity, chosenInterval ->
                    weatherViewModel.confirmResidingLocationAndNotify(
                        context = context,
                        region = chosenRegion,
                        nearestCity = chosenCity,
                        fromGps = false,
                        intervalOption = chosenInterval
                    )
                }
            )
        }

        if (uiState.showGeminiClarifierDialog) {
            val currentPeakDbz = uiState.radarFrames.getOrElse(uiState.currentRadarFrameIndex) {
                uiState.radarFrames.firstOrNull()
            }?.peakDbz ?: 48
            val primaryRiverStation = uiState.riverStations.firstOrNull {
                it.district.equals(uiState.selectedRegion.name, ignoreCase = true)
            } ?: uiState.riverStations.maxByOrNull { it.floodStatus.severityRank }

            GeminiClarificationDialog(
                selectedRegion = uiState.selectedRegion,
                selectedNearestCity = uiState.selectedNearestCity,
                regionalReport = uiState.regionalReport,
                peakDbz = currentPeakDbz,
                primaryStation = primaryRiverStation,
                messages = uiState.geminiMessages,
                isThinking = uiState.isGeminiThinking,
                onAskQuestion = { question -> weatherViewModel.askGeminiClarification(question) },
                onDismiss = { weatherViewModel.dismissGeminiClarifier() }
            )
        }

        if (uiState.currentTab != AppTab.ALERTS) {
            BackHandler {
                weatherViewModel.selectTab(AppTab.ALERTS)
            }
        }

        var showOverflowMenu by remember { mutableStateOf(false) }
        val floodStationCount = uiState.riverStations.count {
            it.floodStatus == FloodStatus.MAJOR_FLOOD || it.floodStatus == FloodStatus.MINOR_FLOOD
        }

        val primaryStationForArea = remember(uiState.riverStations, uiState.selectedRegion, uiState.selectedNearestCity) {
            weatherViewModel.primaryStationForCurrentArea()
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = AdaptiveAppBackground,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AdaptiveAppBackground)
                        .statusBarsPadding()
                ) {
                    // One clean row: tappable location on the left, three icon actions on the right
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { weatherViewModel.openResidingLocationDialog(permissionDenied = false) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("top_bar_residing_area_chip"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = AdaptivePrimaryText,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = uiState.selectedNearestCity,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = AdaptivePrimaryText,
                                    fontWeight = FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (uiState.isGpsDetected) {
                                        "${uiState.selectedRegion.name} · from GPS"
                                    } else {
                                        "${uiState.selectedRegion.name} District"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AdaptiveSecondaryText,
                                    maxLines = 1
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = "Change area",
                                tint = AdaptiveSecondaryText,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { showOverflowMenu = true },
                                modifier = Modifier.testTag("top_bar_more_button")
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = AdaptiveSecondaryText)
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Ask the AI helper") },
                                    leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        weatherViewModel.openGeminiClarifier()
                                    },
                                    modifier = Modifier.testTag("top_bar_ask_gemini_button")
                                )
                                DropdownMenuItem(
                                    text = { Text("Refresh now") },
                                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                    enabled = !uiState.isLoading,
                                    onClick = {
                                        showOverflowMenu = false
                                        weatherViewModel.refreshAllTelemetry()
                                    },
                                    modifier = Modifier.testTag("top_bar_refresh_button")
                                )
                                DropdownMenuItem(
                                    text = { Text(if (uiState.isDarkTheme) "Light theme" else "Dark theme") },
                                    leadingIcon = {
                                        Icon(
                                            if (uiState.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        weatherViewModel.setDarkTheme(!uiState.isDarkTheme)
                                    },
                                    modifier = Modifier.testTag("theme_mode_toggle_button")
                                )
                            }
                        }
                    }

                    if (uiState.isLoading && uiState.currentTab != AppTab.ALERTS) {
                        LinearProgressIndicator(
                            color = RadarCyan,
                            trackColor = Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = AdaptiveSurfaceColor,
                    tonalElevation = 0.dp
                ) {
                    AppTab.entries.forEach { tab ->
                        val selected = uiState.currentTab == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = { weatherViewModel.selectTab(tab) },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (tab == AppTab.RIVERS && floodStationCount > 0) {
                                            Badge(containerColor = AlertRed) {
                                                Text(floodStationCount.toString(), color = Color.White)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = when (tab) {
                                            AppTab.ALERTS -> if (selected) Icons.Filled.WbSunny else Icons.Outlined.WbSunny
                                            AppTab.RADAR -> if (selected) Icons.Filled.Map else Icons.Outlined.Map
                                            AppTab.RIVERS -> if (selected) Icons.Filled.Water else Icons.Outlined.Water
                                            AppTab.SAFETY -> if (selected) Icons.Filled.HealthAndSafety else Icons.Outlined.HealthAndSafety
                                        },
                                        contentDescription = tab.label
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AdaptivePrimaryText,
                                selectedTextColor = AdaptivePrimaryText,
                                indicatorColor = AdaptiveCyanAccent.copy(alpha = 0.22f),
                                unselectedIconColor = AdaptiveSecondaryText,
                                unselectedTextColor = AdaptiveSecondaryText
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.route}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (uiState.currentTab) {
                    AppTab.ALERTS -> AlertsDashboardScreen(
                        selectedNearestCity = uiState.selectedNearestCity,
                        alerts = uiState.alerts,
                        regionalReport = uiState.regionalReport,
                        riverStations = uiState.riverStations,
                        primaryStation = primaryStationForArea,
                        isSyncing = uiState.isLoading,
                        onRefresh = { weatherViewModel.refreshAllTelemetry() },
                        onSendAreaNotificationNow = sendNotificationForCurrentAreaAction,
                        onOpenRiversTab = { weatherViewModel.selectTab(AppTab.RIVERS) },
                        onDialEmergencyDmc = { dialEmergencyNumber(context, "117") }
                    )

                    AppTab.RADAR -> LiveRadarScreen(
                        liveMapMode = uiState.liveMapMode,
                        islandSnapshots = uiState.islandSnapshots,
                        selectedZoomEarthLayer = uiState.selectedZoomEarthLayer,
                        liveMapOverlayMetric = uiState.liveMapOverlayMetric,
                        radarFrames = uiState.radarFrames,
                        currentFrameIndex = uiState.currentRadarFrameIndex,
                        isRadarPlaying = uiState.isRadarPlaying,
                        activeLayers = uiState.activeRadarLayers,
                        stations = uiState.riverStations,
                        regions = uiState.regions,
                        selectedRegion = uiState.selectedRegion,
                        selectedNearestCity = uiState.selectedNearestCity,
                        regionalReport = uiState.regionalReport,
                        selectedStation = uiState.selectedStationDetail,
                        onSelectMapMode = { weatherViewModel.setLiveMapMode(it) },
                        onSelectZoomEarthLayer = { weatherViewModel.setZoomEarthLayer(it) },
                        onSelectMapOverlayMetric = { weatherViewModel.setLiveMapOverlayMetric(it) },
                        onTogglePlayPause = { weatherViewModel.toggleRadarPlayPause() },
                        onFrameIndexChanged = { weatherViewModel.setRadarFrameIndex(it) },
                        onToggleLayer = { weatherViewModel.toggleRadarLayer(it) },
                        onStationSelected = { weatherViewModel.selectStationDetail(it) },
                        onSelectRegionAndCity = { region, city ->
                            weatherViewModel.confirmResidingLocationAndNotify(
                                context = context,
                                region = region,
                                nearestCity = city,
                                fromGps = false
                            )
                        },
                        onOpenExternalUrl = { url -> openExternalGovernmentUrl(context, url) },
                        onOpenGeminiClarifier = { prompt -> weatherViewModel.openGeminiClarifier(prompt) }
                    )

                    AppTab.RIVERS -> RiverWaterLevelsScreen(
                        stations = uiState.riverStations,
                        selectedBasinFilter = uiState.selectedBasinFilter,
                        searchQuery = uiState.riverSearchQuery,
                        selectedHydrographTimeRange = uiState.selectedHydrographTimeRange,
                        onBasinFilterSelected = { weatherViewModel.setBasinFilter(it) },
                        onSearchQueryChanged = { weatherViewModel.setRiverSearchQuery(it) },
                        onHydrographTimeRangeSelected = { weatherViewModel.setHydrographTimeRange(it) }
                    )

                    AppTab.SAFETY -> SafetyHubScreen(
                        selectedRegion = uiState.selectedRegion,
                        selectedNearestCity = uiState.selectedNearestCity,
                        selectedNotificationInterval = uiState.selectedNotificationInterval,
                        governmentAnnouncements = uiState.governmentAnnouncements,
                        selectedGovCategoryFilter = uiState.selectedGovCategoryFilter,
                        shelters = uiState.shelters,
                        checklist = uiState.checklist,
                        onSelectGovCategoryFilter = { weatherViewModel.setGovCategoryFilter(it) },
                        onSelectNotificationInterval = { interval ->
                            weatherViewModel.selectNotificationInterval(context, interval)
                        },
                        onOpenOfficialUrl = { url -> openExternalGovernmentUrl(context, url) },
                        onToggleChecklistItem = { weatherViewModel.toggleChecklistItem(it) },
                        onDialNumber = { dialEmergencyNumber(context, it) },
                        onShareSafetyReport = {
                            val topRiver = uiState.riverStations.maxByOrNull { it.floodStatus.severityRank }
                            val shareText = buildString {
                                appendLine("🇱🇰 Lanka Weather Guard — Official Situation Report")
                                appendLine("Residing Area: ${uiState.selectedNearestCity}, ${uiState.selectedRegion.name} (${uiState.selectedRegion.primaryRiverBasin})")
                                appendLine("Auto-Notification Interval: ${uiState.selectedNotificationInterval.label}")
                                appendLine("Weather: ${uiState.regionalReport.weatherDescription}, ${String.format(Locale.US, "%.1f", uiState.regionalReport.rain24hMm)}mm/24h, Gusts ${uiState.regionalReport.windGustKmh.toInt()} km/h")
                                if (topRiver != null) {
                                    appendLine("Critical River Gauge: ${topRiver.riverName} at ${topRiver.stationName} = ${String.format(Locale.US, "%.2fm", topRiver.currentLevelM)} (${topRiver.floodStatus.label})")
                                }
                                appendLine("Official Portals: https://www.dmc.gov.lk | https://riverwatch.lk | https://www.news.lk")
                                appendLine("Emergency DMC Hotline: 117 | Suwa Seriya: 1990")
                            }
                            shareSituationText(context, shareText)
                        }
                    )
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun resolveDeviceLocation(
    context: Context,
    onCoordinates: (Double, Double) -> Unit,
    onFallback: () -> Unit
) {
    try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val gpsLoc = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val netLoc = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        val best = gpsLoc ?: netLoc
        if (best != null) {
            onCoordinates(best.latitude, best.longitude)
        } else {
            onFallback()
        }
    } catch (_: Exception) {
        onFallback()
    }
}

private fun openExternalGovernmentUrl(context: Context, url: String) {
    try {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(browserIntent)
    } catch (_: Exception) {
        // Ignore if no browser activity on headless test device
    }
}

private fun dialEmergencyNumber(context: Context, phone: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        // Ignore if no dialer activity on headless test device
    }
}

private fun shareSituationText(context: Context, message: String) {
    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        val chooser = Intent.createChooser(sendIntent, "Share Weather Situation Report").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (_: Exception) {
        // Ignore if no chooser activity on headless test device
    }
}
