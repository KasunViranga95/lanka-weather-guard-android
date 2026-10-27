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
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.Flood
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.outlined.CompassCalibration
import androidx.compose.material.icons.outlined.Flood
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.UserPreferencesStore
import com.example.model.AlertSeverity
import com.example.model.AppTab
import com.example.model.DispatchedNotificationRecord
import com.example.model.FloodStatus
import com.example.ui.components.GeminiClarificationDialog
import com.example.ui.components.ResidingLocationDialog
import com.example.ui.screens.AlertsDashboardScreen
import com.example.ui.screens.LiveRadarScreen
import com.example.ui.screens.RegionalForecastScreen
import com.example.ui.screens.RiverWaterLevelsScreen
import com.example.ui.screens.SafetyHubScreen
import com.example.ui.theme.AdaptiveAppBackground
import com.example.ui.theme.AdaptiveBorderColor
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

        val selectGeneratedNotificationAction: (DispatchedNotificationRecord) -> Unit = { record ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                pendingNotificationRecord = record
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                weatherViewModel.selectAndPostGeneratedNotification(context, record)
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

        val criticalAlertCount = uiState.alerts.count { it.severity == AlertSeverity.CRITICAL_RED }
        val floodStationCount = uiState.riverStations.count {
            it.floodStatus == FloodStatus.MAJOR_FLOOD || it.floodStatus == FloodStatus.MINOR_FLOOD
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = AdaptiveAppBackground,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AdaptiveSurfaceColor)
                        .statusBarsPadding()
                ) {
                    // Row 1: Clean App Title + Ask Gemini Button (with statusBarsPadding so it never overlaps the clock/status bar)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.header_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = AdaptiveCyanAccent,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )

                        Surface(
                            color = AdaptiveElevatedColor,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .border(1.dp, AdaptiveBorderColor, RoundedCornerShape(18.dp))
                                .clickable { weatherViewModel.openGeminiClarifier() }
                                .testTag("top_bar_ask_gemini_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Ask Gemini",
                                    tint = AdaptiveCyanAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Ask Gemini",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AdaptivePrimaryText,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Row 2: Location Chip + Explicit [Light | Dark] Mode Buttons (Single clean bar, no duplicate top tab menu)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, bottom = 10.dp)
                            .testTag("top_bar_live_temp_precip_strip"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = AdaptiveElevatedColor,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                                .border(1.dp, AdaptiveBorderColor, RoundedCornerShape(10.dp))
                                .clickable {
                                    weatherViewModel.openResidingLocationDialog(permissionDenied = false)
                                }
                                .testTag("top_bar_residing_area_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(SafeEmerald)
                                    )
                                    Text(
                                        text = "${uiState.selectedNearestCity} • ${
                                            String.format(Locale.US, "%.1f°C", uiState.regionalReport.currentTempC)
                                        }",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AdaptivePrimaryText,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.EditLocationAlt,
                                    contentDescription = "Change Location",
                                    tint = AdaptiveCyanAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        // Side-by-Side Light & Dark Mode Buttons
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(AdaptiveElevatedColor)
                                .border(1.dp, AdaptiveBorderColor, RoundedCornerShape(10.dp))
                                .padding(2.dp)
                                .testTag("theme_mode_toggle_button"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Surface(
                                color = if (!uiState.isDarkTheme) RadarCyan else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .clickable { weatherViewModel.setDarkTheme(false) }
                                    .testTag("top_light_mode_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LightMode,
                                        contentDescription = "Light Mode",
                                        tint = if (!uiState.isDarkTheme) Color.White else WarningAmber,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Light",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (!uiState.isDarkTheme) Color.White else AdaptivePrimaryText,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                color = if (uiState.isDarkTheme) RadarCyan else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .clickable { weatherViewModel.setDarkTheme(true) }
                                    .testTag("top_dark_mode_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DarkMode,
                                        contentDescription = "Dark Mode",
                                        tint = if (uiState.isDarkTheme) Color.White else AdaptiveCyanAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Dark",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (uiState.isDarkTheme) Color.White else AdaptivePrimaryText,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = AdaptiveBorderColor)

                    if (uiState.isLoading) {
                        LinearProgressIndicator(
                            color = RadarCyan,
                            trackColor = AdaptiveElevatedColor,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = AdaptiveSurfaceColor,
                    tonalElevation = 4.dp
                ) {
                    AppTab.entries.forEach { tab ->
                        val selected = uiState.currentTab == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = { weatherViewModel.selectTab(tab) },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (tab == AppTab.ALERTS && criticalAlertCount > 0) {
                                            Badge(containerColor = AlertRed) {
                                                Text(criticalAlertCount.toString(), color = Color.White)
                                            }
                                        } else if (tab == AppTab.RIVERS && floodStationCount > 0) {
                                            Badge(containerColor = AlertRed) {
                                                Text(floodStationCount.toString(), color = Color.White)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = when (tab) {
                                            AppTab.ALERTS -> if (selected) Icons.Filled.NotificationsActive else Icons.Outlined.NotificationsActive
                                            AppTab.RADAR -> if (selected) Icons.Filled.Radar else Icons.Outlined.Radar
                                            AppTab.RIVERS -> if (selected) Icons.Filled.Flood else Icons.Outlined.Flood
                                            AppTab.FORECAST -> if (selected) Icons.Filled.CompassCalibration else Icons.Outlined.CompassCalibration
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
                                selectedIconColor = AdaptiveCyanAccent,
                                selectedTextColor = AdaptiveCyanAccent,
                                indicatorColor = RadarCyan.copy(alpha = 0.14f),
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
                        regions = uiState.regions,
                        selectedRegion = uiState.selectedRegion,
                        selectedNearestCity = uiState.selectedNearestCity,
                        selectedNotificationInterval = uiState.selectedNotificationInterval,
                        gpsStatusMessage = uiState.gpsStatusMessage,
                        latestNotification = uiState.latestNotification,
                        notificationHistory = uiState.notificationHistory,
                        residingGovAnnouncement = uiState.governmentAnnouncements.firstOrNull(),
                        alerts = uiState.alerts,
                        selectedSeverityFilter = uiState.selectedAlertSeverityFilter,
                        regionalReport = uiState.regionalReport,
                        riverStations = uiState.riverStations,
                        onRegionSelected = { region ->
                            weatherViewModel.confirmResidingLocationAndNotify(
                                context = context,
                                region = region,
                                nearestCity = region.nearestCities.firstOrNull() ?: region.name,
                                fromGps = false
                            )
                        },
                        onSelectNotificationInterval = { interval ->
                            weatherViewModel.selectNotificationInterval(context, interval)
                        },
                        onRequestGpsLocation = requestLocationAction,
                        onOpenResidingLocationPicker = {
                            weatherViewModel.openResidingLocationDialog(permissionDenied = false)
                        },
                        onSendAreaNotificationNow = sendNotificationForCurrentAreaAction,
                        onSelectGeneratedNotification = selectGeneratedNotificationAction,
                        onRefreshTelemetry = { weatherViewModel.refreshAllTelemetry() },
                        onSeverityFilterSelected = { weatherViewModel.setAlertSeverityFilter(it) },
                        onOpenRadarTab = { weatherViewModel.selectTab(AppTab.RADAR) },
                        onOpenRiversTab = { weatherViewModel.selectTab(AppTab.RIVERS) },
                        onOpenSafetyTab = { weatherViewModel.selectTab(AppTab.SAFETY) },
                        onOpenOfficialUrl = { url -> openExternalGovernmentUrl(context, url) },
                        onDialEmergencyDmc = { dialEmergencyNumber(context, "117") }
                    )

                    AppTab.RADAR -> LiveRadarScreen(
                        liveMapMode = uiState.liveMapMode,
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

                    AppTab.FORECAST -> RegionalForecastScreen(
                        regions = uiState.regions,
                        selectedRegion = uiState.selectedRegion,
                        report = uiState.regionalReport,
                        onRegionSelected = { region ->
                            weatherViewModel.selectRegion(region)
                        }
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
