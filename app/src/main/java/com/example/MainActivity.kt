package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Water
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AlertEngine
import com.example.data.UserPreferencesStore
import com.example.model.AppTab
import com.example.model.FloodStatus
import com.example.ui.components.AiHelperSheet
import com.example.ui.components.PlacePickerDialog
import com.example.ui.components.SettingsSheet
import com.example.ui.screens.MapScreen
import com.example.ui.screens.RiverWaterLevelsScreen
import com.example.ui.screens.SafetyHubScreen
import com.example.ui.screens.WeatherScreen
import com.example.ui.theme.AdaptiveAppBackground
import com.example.ui.theme.AdaptiveCyanAccent
import com.example.ui.theme.AdaptivePrimaryText
import com.example.ui.theme.AdaptiveSecondaryText
import com.example.ui.theme.AdaptiveSurfaceColor
import com.example.ui.theme.AlertRed
import com.example.ui.theme.LankaWeatherGuardTheme
import com.example.ui.theme.RadarCyan
import com.example.util.AlertNotifier
import com.example.util.LocaleHelper
import com.example.util.WeatherFormat
import com.example.viewmodel.WeatherViewModel
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private var latestNotificationIntent by mutableStateOf<Intent?>(null)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    val configuration = LocalConfiguration.current
    val language = remember(configuration) { LocaleHelper.current(context) }

    LankaWeatherGuardTheme(darkTheme = uiState.isDarkTheme) {
        val notificationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { }

        val locationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                resolveDeviceLocation(
                    context = context,
                    onCoordinates = { lat, lon -> weatherViewModel.onCoordinatesDetected(lat, lon) },
                    onFallback = { weatherViewModel.openPlacePicker(permissionDenied = true) }
                )
            } else {
                weatherViewModel.openPlacePicker(permissionDenied = true)
            }
        }

        fun requestNotificationsIfNeeded() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !AlertNotifier.canPost(context)) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val setAlertsEnabled: (Boolean) -> Unit = { enabled ->
            weatherViewModel.setAlertsEnabled(enabled)
            if (enabled) requestNotificationsIfNeeded()
        }

        // Keep weather and river gauges fresh while the app is on screen
        val lifecycleOwner = LocalLifecycleOwner.current
        LaunchedEffect(lifecycleOwner) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                weatherViewModel.runAutoRefreshLoop()
            }
        }

        LaunchedEffect(Unit) {
            if (uiState.alertsEnabled) requestNotificationsIfNeeded()
        }

        LaunchedEffect(notificationIntent) {
            val activeIntent = notificationIntent ?: return@LaunchedEffect
            if (activeIntent.getBooleanExtra(AlertNotifier.EXTRA_FROM_NOTIFICATION, false)) {
                weatherViewModel.handleNotificationClicked(
                    regionId = activeIntent.getStringExtra(AlertNotifier.EXTRA_NOTIFICATION_REGION_ID),
                    nearestCity = activeIntent.getStringExtra(AlertNotifier.EXTRA_NOTIFICATION_CITY)
                )
                onNotificationIntentConsumed()
            }
        }

        val requestLocationAction = {
            val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                resolveDeviceLocation(
                    context = context,
                    onCoordinates = { lat, lon -> weatherViewModel.onCoordinatesDetected(lat, lon) },
                    onFallback = { weatherViewModel.openPlacePicker(permissionDenied = true) }
                )
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                )
            }
        }

        val primaryStationForArea = remember(uiState.riverStations, uiState.selectedRegion, uiState.selectedNearestCity) {
            weatherViewModel.primaryStationForCurrentArea()
        }

        // Alerts are worked out from the data on screen, in the app language
        val alerts = remember(uiState.regionalReport, uiState.riverStations, primaryStationForArea, configuration) {
            AlertEngine.build(
                res = context.resources,
                region = uiState.selectedRegion,
                city = uiState.selectedNearestCity,
                report = uiState.regionalReport,
                stations = uiState.riverStations,
                nearStation = primaryStationForArea
            )
        }

        if (uiState.showPlacePicker) {
            PlacePickerDialog(
                regions = uiState.regions,
                initialRegion = uiState.selectedRegion,
                initialCity = uiState.selectedNearestCity,
                wasPermissionDenied = uiState.wasLocationPermissionDenied,
                onUseMyLocation = {
                    weatherViewModel.dismissPlacePicker()
                    requestLocationAction()
                },
                onDismiss = { weatherViewModel.dismissPlacePicker() },
                onConfirm = { region, city -> weatherViewModel.selectPlace(region, city) }
            )
        }

        if (uiState.showSettings) {
            SettingsSheet(
                language = language,
                onLanguageChange = { tag ->
                    weatherViewModel.dismissSettings()
                    (context as? Activity)?.let { LocaleHelper.set(it, tag) }
                },
                isDarkTheme = uiState.isDarkTheme,
                onDarkThemeChange = { weatherViewModel.setDarkTheme(it) },
                alertsEnabled = uiState.alertsEnabled,
                onAlertsEnabledChange = setAlertsEnabled,
                onOpenUrl = { openUrl(context, it) },
                onDismiss = { weatherViewModel.dismissSettings() }
            )
        }

        if (uiState.showAiHelper) {
            AiHelperSheet(
                cityName = uiState.selectedNearestCity,
                messages = uiState.aiMessages,
                isThinking = uiState.isAiThinking,
                onAsk = { weatherViewModel.askAiHelper(it, language.ifEmpty { configuration.locales[0].language }) },
                onDismiss = { weatherViewModel.dismissAiHelper() }
            )
        }

        if (uiState.currentTab != AppTab.ALERTS) {
            BackHandler { weatherViewModel.selectTab(AppTab.ALERTS) }
        }

        val floodStationCount = uiState.riverStations.count {
            it.floodStatus == FloodStatus.MAJOR_FLOOD || it.floodStatus == FloodStatus.MINOR_FLOOD
        }
        var showPlacesMenu by remember { mutableStateOf(false) }
        var showOverflowMenu by remember { mutableStateOf(false) }

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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Place name: tap to switch between saved places, use GPS or add a place
                        Box(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showPlacesMenu = true }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("top_bar_residing_area_chip"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (uiState.isGpsDetected) Icons.Default.MyLocation else Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = AdaptivePrimaryText,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = uiState.selectedNearestCity,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = AdaptivePrimaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = stringResource(R.string.district_name, uiState.selectedRegion.name),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AdaptiveSecondaryText,
                                        maxLines = 1
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = stringResource(R.string.change_place),
                                    tint = AdaptiveSecondaryText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            DropdownMenu(expanded = showPlacesMenu, onDismissRequest = { showPlacesMenu = false }) {
                                uiState.savedPlaces.forEach { place ->
                                    val isCurrent = place.regionId == uiState.selectedRegion.id &&
                                        place.city.equals(uiState.selectedNearestCity, ignoreCase = true)
                                    val placeRegion = uiState.regions.firstOrNull { it.id == place.regionId }
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(place.city, fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal)
                                                placeRegion?.let {
                                                    Text(
                                                        stringResource(R.string.district_name, it.name),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = AdaptiveSecondaryText
                                                    )
                                                }
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(if (isCurrent) Icons.Default.Check else Icons.Default.Place, contentDescription = null)
                                        },
                                        trailingIcon = if (uiState.savedPlaces.size > 1) {
                                            {
                                                IconButton(onClick = { weatherViewModel.removePlace(place) }) {
                                                    Icon(
                                                        Icons.Default.Close,
                                                        contentDescription = stringResource(R.string.remove_place, place.city),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        } else null,
                                        onClick = {
                                            showPlacesMenu = false
                                            if (!isCurrent) weatherViewModel.selectSavedPlace(place)
                                        },
                                        modifier = Modifier.testTag("saved_place_${place.city.lowercase(Locale.US).replace(' ', '_')}")
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.use_my_location)) },
                                    leadingIcon = { Icon(Icons.Default.MyLocation, contentDescription = null) },
                                    onClick = {
                                        showPlacesMenu = false
                                        requestLocationAction()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.add_place)) },
                                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                    enabled = uiState.savedPlaces.size < UserPreferencesStore.MAX_PLACES,
                                    onClick = {
                                        showPlacesMenu = false
                                        weatherViewModel.openPlacePicker()
                                    },
                                    modifier = Modifier.testTag("add_place_button")
                                )
                            }
                        }

                        Box {
                            IconButton(
                                onClick = { showOverflowMenu = true },
                                modifier = Modifier.testTag("top_bar_more_button")
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options), tint = AdaptiveSecondaryText)
                            }
                            DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
                                if (weatherViewModel.isAiHelperAvailable) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.ai_title)) },
                                        leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            weatherViewModel.openAiHelper()
                                        },
                                        modifier = Modifier.testTag("top_bar_ask_gemini_button")
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.refresh_now)) },
                                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                    enabled = !uiState.isLoading,
                                    onClick = {
                                        showOverflowMenu = false
                                        weatherViewModel.refreshAllTelemetry()
                                    },
                                    modifier = Modifier.testTag("top_bar_refresh_button")
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.settings)) },
                                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        weatherViewModel.openSettings()
                                    },
                                    modifier = Modifier.testTag("top_bar_settings_button")
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
                NavigationBar(containerColor = AdaptiveSurfaceColor, tonalElevation = 0.dp) {
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
                                        contentDescription = null
                                    )
                                }
                            },
                            label = { Text(text = stringResource(tab.labelRes), style = MaterialTheme.typography.labelMedium, maxLines = 1) },
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
                    AppTab.ALERTS -> WeatherScreen(
                        selectedNearestCity = uiState.selectedNearestCity,
                        alerts = alerts,
                        alertsEnabled = uiState.alertsEnabled,
                        onAlertsEnabledChange = setAlertsEnabled,
                        regionalReport = uiState.regionalReport,
                        riverStations = uiState.riverStations,
                        primaryStation = primaryStationForArea,
                        isSyncing = uiState.isLoading,
                        onRefresh = { weatherViewModel.refreshAllTelemetry() },
                        onOpenRiversTab = { weatherViewModel.selectTab(AppTab.RIVERS) },
                        onDialEmergencyDmc = { dialNumber(context, "117") }
                    )

                    AppTab.RADAR -> MapScreen(
                        selectedRegion = uiState.selectedRegion,
                        selectedNearestCity = uiState.selectedNearestCity,
                        selectedLayer = uiState.selectedMapLayer,
                        onLayerSelected = { weatherViewModel.setMapLayer(it) },
                        islandSnapshots = uiState.islandSnapshots,
                        onOpenExternalUrl = { openUrl(context, it) }
                    )

                    AppTab.RIVERS -> RiverWaterLevelsScreen(
                        stations = uiState.riverStations,
                        reservoirs = uiState.reservoirs,
                        selectedBasinFilter = uiState.selectedBasinFilter,
                        searchQuery = uiState.riverSearchQuery,
                        selectedHydrographTimeRange = uiState.selectedHydrographTimeRange,
                        onBasinFilterSelected = { weatherViewModel.setBasinFilter(it) },
                        onSearchQueryChanged = { weatherViewModel.setRiverSearchQuery(it) },
                        onHydrographTimeRangeSelected = { weatherViewModel.setHydrographTimeRange(it) }
                    )

                    AppTab.SAFETY -> SafetyHubScreen(
                        checkedChecklistIds = uiState.checkedChecklistIds,
                        alertsEnabled = uiState.alertsEnabled,
                        onAlertsEnabledChange = setAlertsEnabled,
                        onOpenOfficialUrl = { openUrl(context, it) },
                        onToggleChecklistItem = { weatherViewModel.toggleChecklistItem(it) },
                        onDialNumber = { dialNumber(context, it) },
                        onShareSafetyReport = {
                            val res = context.resources
                            val report = uiState.regionalReport
                            val shareText = buildString {
                                appendLine(res.getString(R.string.share_title, uiState.selectedNearestCity, uiState.selectedRegion.name))
                                if (report.isLiveApiData) {
                                    appendLine(
                                        res.getString(
                                            R.string.share_weather,
                                            res.getString(WeatherFormat.conditionRes(report.weatherCode)),
                                            report.currentTempC.roundToInt(),
                                            report.rain24hMm.roundToInt(),
                                            report.windGustKmh.roundToInt()
                                        )
                                    )
                                }
                                primaryStationForArea?.takeIf { it.hasLiveReading }?.let { st ->
                                    appendLine(
                                        res.getString(
                                            R.string.share_river,
                                            st.riverName,
                                            st.stationName,
                                            String.format(Locale.US, "%.2f", st.currentLevelM),
                                            res.getString(st.floodStatus.labelRes)
                                        )
                                    )
                                }
                                alerts.firstOrNull { it.isUserDistrictMatch }?.let { appendLine("⚠ ${it.title}") }
                                append(res.getString(R.string.share_hotlines))
                            }
                            shareText(context, shareText, res.getString(R.string.share))
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
        val best = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        if (best != null) onCoordinates(best.latitude, best.longitude) else onFallback()
    } catch (_: Exception) {
        onFallback()
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        // No browser installed
    }
}

private fun dialNumber(context: Context, phone: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        // No dialer on this device
    }
}

private fun shareText(context: Context, message: String, title: String) {
    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(sendIntent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        // No app to share with
    }
}
