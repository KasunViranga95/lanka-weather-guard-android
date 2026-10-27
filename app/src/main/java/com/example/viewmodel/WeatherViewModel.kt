package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GovernmentAnnouncementsProvider
import com.example.data.SriLankaWeatherRepository
import com.example.data.UserPreferencesStore
import com.example.model.AlertSeverity
import com.example.model.AppTab
import com.example.model.CityWeatherSnapshot
import com.example.model.DispatchedNotificationRecord
import com.example.model.EmergencyShelter
import com.example.model.GeminiClarificationMessage
import com.example.model.GovernmentAnnouncement
import com.example.model.HydrographTimeRange
import com.example.model.LiveMapMode
import com.example.model.LiveMapOverlayMetric
import com.example.model.NotificationIntervalOption
import com.example.model.RadarFrame
import com.example.model.RadarLayerType
import com.example.model.RegionalWeatherReport
import com.example.model.RiverGaugeStation
import com.example.model.SafetyChecklistItem
import com.example.model.SriLankaRegion
import com.example.model.WeatherAlert
import com.example.model.ZoomEarthLayer
import com.example.model.getDbzMeaningForValue
import com.example.network.GeminiClarificationClient
import com.example.util.WeatherNotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class WeatherUiState(
    val currentTab: AppTab = AppTab.ALERTS,
    val isDarkTheme: Boolean = true,
    val regions: List<SriLankaRegion> = emptyList(),
    val selectedRegion: SriLankaRegion,
    val selectedNearestCity: String,
    val selectedNotificationInterval: NotificationIntervalOption = NotificationIntervalOption.MIN_15,
    val isGpsDetected: Boolean = false,
    val showResidingLocationDialog: Boolean = false,
    val wasLocationPermissionDenied: Boolean = false,
    val gpsStatusMessage: String = "Saved Area: Wellampitiya, Colombo • Auto-Alert Every 15 min",
    val latestNotification: DispatchedNotificationRecord? = null,
    val notificationHistory: List<DispatchedNotificationRecord> = emptyList(),
    val isLoading: Boolean = false,
    val alerts: List<WeatherAlert> = emptyList(),
    val selectedAlertSeverityFilter: AlertSeverity? = null,
    val riverStations: List<RiverGaugeStation> = emptyList(),
    val selectedBasinFilter: String = "All Basins",
    val riverSearchQuery: String = "",
    val selectedHydrographTimeRange: HydrographTimeRange = HydrographTimeRange.HOURS_24,
    val selectedStationDetail: RiverGaugeStation? = null,
    val regionalReport: RegionalWeatherReport,
    val radarFrames: List<RadarFrame> = emptyList(),
    val currentRadarFrameIndex: Int = 3,
    val isRadarPlaying: Boolean = true,
    val liveMapMode: LiveMapMode = LiveMapMode.INTERACTIVE_LIVE_MAP,
    val selectedZoomEarthLayer: ZoomEarthLayer = ZoomEarthLayer.LIVE_HOME,
    val liveMapOverlayMetric: LiveMapOverlayMetric = LiveMapOverlayMetric.RADAR_PRECIP,
    val activeRadarLayers: Set<RadarLayerType> = setOf(
        RadarLayerType.PRECIPITATION_DBZ,
        RadarLayerType.WIND_VECTORS
    ),
    val governmentAnnouncements: List<GovernmentAnnouncement> = emptyList(),
    val selectedGovCategoryFilter: String = "All Official",
    val shelters: List<EmergencyShelter> = emptyList(),
    val checklist: List<SafetyChecklistItem> = emptyList(),
    val showGeminiClarifierDialog: Boolean = false,
    val isGeminiThinking: Boolean = false,
    val geminiMessages: List<GeminiClarificationMessage> = emptyList(),
    // Live conditions for the island overview map towns
    val islandSnapshots: List<CityWeatherSnapshot> = emptyList()
)

class WeatherViewModel(
    private val repository: SriLankaWeatherRepository = SriLankaWeatherRepository()
) : ViewModel() {

    private val savedPrefs = UserPreferencesStore.loadPreferences()

    private val initialRegion: SriLankaRegion = repository.regions.firstOrNull {
        it.id.equals(savedPrefs.regionId, ignoreCase = true) ||
            it.name.equals(savedPrefs.regionId, ignoreCase = true)
    } ?: repository.regions.first()

    private val initialCity: String = savedPrefs.nearestCity?.takeIf { city ->
        city.isNotBlank() && initialRegion.nearestCities.any { it.equals(city, ignoreCase = true) }
    } ?: initialRegion.nearestCities.getOrElse(1) { initialRegion.name }

    private val initialInterval: NotificationIntervalOption =
        NotificationIntervalOption.entries.firstOrNull { it.id == savedPrefs.intervalId }
            ?: NotificationIntervalOption.MIN_15

    private val initialFrames = repository.getRadarFrames()
    // Placeholders only — live data is loaded by refreshAllTelemetry() in init (never block the main thread)
    private val initialStations = repository.getReferenceStations()
    private val initialReport = repository.getPlaceholderRegionalReport(initialRegion, initialCity)
    private val initialPrimaryStation = repository.findPrimaryStationForRegion(
        initialRegion,
        initialCity,
        initialStations
    )

    private val _uiState = MutableStateFlow(
        WeatherUiState(
            isDarkTheme = savedPrefs.isDarkTheme,
            regions = repository.regions,
            selectedRegion = initialRegion,
            selectedNearestCity = initialCity,
            selectedNotificationInterval = initialInterval,
            isGpsDetected = savedPrefs.isFromGps,
            gpsStatusMessage = "Saved Area: $initialCity, ${initialRegion.name} • Auto-Alert ${initialInterval.label}",
            alerts = repository.getAlertsForRegion(
                selectedRegion = initialRegion,
                nearestCity = initialCity,
                stations = initialStations,
                report = initialReport
            ),
            riverStations = initialStations,
            regionalReport = initialReport,
            radarFrames = initialFrames,
            governmentAnnouncements = GovernmentAnnouncementsProvider.getAnnouncementsForArea(
                selectedRegion = initialRegion,
                nearestCity = initialCity,
                primaryStation = initialPrimaryStation,
                allRegions = repository.regions,
                report = initialReport
            ),
            shelters = repository.getEmergencyShelters(),
            checklist = repository.getInitialChecklist()
        )
    )
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var radarLoopJob: Job? = null
    private var notificationSchedulerJob: Job? = null
    private var telemetryRefreshJob: Job? = null
    @Volatile
    private var lastTelemetrySyncMillis: Long = 0L

    init {
        refreshAllTelemetry(initialRegion, initialCity)
        startRadarAnimationLoop()
    }

    /**
     * Synchronizes the ViewModel with SharedPreferences if UserPreferencesStore was initialized
     * after ViewModel construction.
     */
    fun syncSavedPreferencesIfNeeded() {
        val prefs = UserPreferencesStore.loadPreferences()
        val savedRegion = prefs.regionId?.let { id ->
            repository.regions.firstOrNull {
                it.id.equals(id, ignoreCase = true) || it.name.equals(id, ignoreCase = true)
            }
        }
        if (savedRegion != null) {
            val savedCity = prefs.nearestCity?.takeIf { it.isNotBlank() }
                ?: savedRegion.nearestCities.firstOrNull()
                ?: savedRegion.name
            val savedInterval = NotificationIntervalOption.entries.firstOrNull { it.id == prefs.intervalId }
                ?: _uiState.value.selectedNotificationInterval
            val current = _uiState.value
            if (current.selectedRegion.id != savedRegion.id ||
                !current.selectedNearestCity.equals(savedCity, ignoreCase = true) ||
                current.selectedNotificationInterval != savedInterval ||
                current.isDarkTheme != prefs.isDarkTheme
            ) {
                _uiState.update {
                    it.copy(
                        selectedRegion = savedRegion,
                        selectedNearestCity = savedCity,
                        selectedNotificationInterval = savedInterval,
                        isGpsDetected = prefs.isFromGps,
                        isDarkTheme = prefs.isDarkTheme,
                        gpsStatusMessage = "Saved Area: $savedCity, ${savedRegion.name} • Auto-Alert ${savedInterval.label}"
                    )
                }
                refreshAllTelemetry(savedRegion, savedCity)
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun toggleDarkTheme() {
        val nextDark = !_uiState.value.isDarkTheme
        UserPreferencesStore.saveDarkTheme(nextDark)
        _uiState.update { it.copy(isDarkTheme = nextDark) }
    }

    fun setDarkTheme(dark: Boolean) {
        UserPreferencesStore.saveDarkTheme(dark)
        _uiState.update { it.copy(isDarkTheme = dark) }
    }

    fun openResidingLocationDialog(permissionDenied: Boolean = false) {
        _uiState.update {
            it.copy(
                showResidingLocationDialog = true,
                wasLocationPermissionDenied = permissionDenied
            )
        }
    }

    fun dismissResidingLocationDialog() {
        _uiState.update { it.copy(showResidingLocationDialog = false) }
    }

    fun selectNotificationInterval(
        context: Context,
        intervalOption: NotificationIntervalOption
    ) {
        UserPreferencesStore.saveNotificationInterval(intervalOption)
        _uiState.update { state ->
            state.copy(
                selectedNotificationInterval = intervalOption,
                gpsStatusMessage = "Saved Area: ${state.selectedNearestCity}, ${state.selectedRegion.name} • Auto-Alert ${intervalOption.label}"
            )
        }
        triggerNotificationForCurrentArea(context, intervalOption)
    }

    fun setHydrographTimeRange(range: HydrographTimeRange) {
        _uiState.update { it.copy(selectedHydrographTimeRange = range) }
    }

    fun setGovCategoryFilter(category: String) {
        _uiState.update { it.copy(selectedGovCategoryFilter = category) }
    }

    fun selectRegion(
        region: SriLankaRegion,
        nearestCity: String = region.nearestCities.firstOrNull() ?: region.name,
        fromGps: Boolean = false
    ) {
        UserPreferencesStore.saveLocation(
            region = region,
            nearestCity = nearestCity,
            isFromGps = fromGps,
            intervalOption = _uiState.value.selectedNotificationInterval
        )
        _uiState.update {
            it.copy(
                selectedRegion = region,
                selectedNearestCity = nearestCity,
                isGpsDetected = fromGps,
                showResidingLocationDialog = false,
                gpsStatusMessage = if (fromGps) {
                    "GPS Saved: $nearestCity, ${region.name} • Auto-Alert ${it.selectedNotificationInterval.label}"
                } else {
                    "Saved Area: $nearestCity, ${region.name} • Auto-Alert ${it.selectedNotificationInterval.label}"
                }
            )
        }
        refreshAllTelemetry(region, nearestCity)
    }

    fun confirmResidingLocationAndNotify(
        context: Context,
        region: SriLankaRegion,
        nearestCity: String,
        fromGps: Boolean,
        intervalOption: NotificationIntervalOption = _uiState.value.selectedNotificationInterval
    ) {
        val appContext = context.applicationContext
        UserPreferencesStore.saveLocation(
            region = region,
            nearestCity = nearestCity,
            isFromGps = fromGps,
            intervalOption = intervalOption
        )
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedRegion = region,
                    selectedNearestCity = nearestCity,
                    selectedNotificationInterval = intervalOption,
                    isGpsDetected = fromGps,
                    showResidingLocationDialog = false,
                    isLoading = true,
                    gpsStatusMessage = if (fromGps) {
                        "GPS Saved: $nearestCity, ${region.name} • Auto-Alert ${intervalOption.label}"
                    } else {
                        "Saved Area: $nearestCity, ${region.name} • Auto-Alert ${intervalOption.label}"
                    }
                )
            }
            val report = repository.fetchRegionalReport(region, nearestCity)
            val stations = repository.fetchLiveRiverStations(region)
            val alerts = repository.getAlertsForRegion(region, nearestCity, stations, report)
            val primaryStation = repository.findPrimaryStationForRegion(region, nearestCity, stations)
            val govAnnouncements = GovernmentAnnouncementsProvider.getAnnouncementsForArea(
                selectedRegion = region,
                nearestCity = nearestCity,
                primaryStation = primaryStation,
                allRegions = repository.regions,
                report = report
            )

            val notificationRecord = WeatherNotificationHelper.buildAndDispatchAreaNotification(
                context = appContext,
                region = region,
                nearestCity = nearestCity,
                primaryStation = primaryStation,
                report = report,
                isFromGps = fromGps,
                intervalOption = intervalOption,
                govAnnouncement = govAnnouncements.firstOrNull()
            )

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    regionalReport = report,
                    riverStations = stations,
                    alerts = alerts,
                    governmentAnnouncements = govAnnouncements,
                    latestNotification = notificationRecord,
                    notificationHistory = (listOf(notificationRecord) + state.notificationHistory)
                        .distinctBy { it.id }
                        .take(8),
                    selectedStationDetail = state.selectedStationDetail?.let { prev ->
                        stations.find { it.id == prev.id }
                    }
                )
            }
            lastTelemetrySyncMillis = System.currentTimeMillis()

            restartPeriodicNotificationJob(appContext, intervalOption)
        }
    }

    /**
     * Called when the user selects a generated notification in the app: updates latestNotification
     * and immediately dispatches/shows it in the Android device's notification shade.
     */
    fun selectAndPostGeneratedNotification(
        context: Context,
        record: DispatchedNotificationRecord
    ) {
        val matchedRegion = repository.regions.firstOrNull {
            it.name.equals(record.district, ignoreCase = true)
        } ?: _uiState.value.selectedRegion

        _uiState.update { state ->
            state.copy(latestNotification = record)
        }

        WeatherNotificationHelper.postRecordToDeviceNotifications(
            context = context.applicationContext,
            record = record,
            regionId = matchedRegion.id
        )
    }

    /**
     * Called when the user clicks a notification in the Android system notification shade:
     * navigates to the Alerts tab and restores/saves the notification's location.
     */
    fun handleNotificationClicked(
        context: Context,
        regionId: String?,
        nearestCity: String?
    ) {
        val targetRegion = regionId?.let { id ->
            repository.regions.firstOrNull {
                it.id.equals(id, ignoreCase = true) || it.name.equals(id, ignoreCase = true)
            }
        } ?: _uiState.value.selectedRegion

        val targetCity = nearestCity?.takeIf { it.isNotBlank() }
            ?: if (targetRegion.id == _uiState.value.selectedRegion.id) {
                _uiState.value.selectedNearestCity
            } else {
                targetRegion.nearestCities.firstOrNull() ?: targetRegion.name
            }

        UserPreferencesStore.saveLocation(
            region = targetRegion,
            nearestCity = targetCity,
            isFromGps = _uiState.value.isGpsDetected,
            intervalOption = _uiState.value.selectedNotificationInterval
        )

        _uiState.update { state ->
            state.copy(
                currentTab = AppTab.ALERTS,
                selectedRegion = targetRegion,
                selectedNearestCity = targetCity,
                showResidingLocationDialog = false,
                gpsStatusMessage = "Saved Area: $targetCity, ${targetRegion.name} • Auto-Alert ${state.selectedNotificationInterval.label}"
            )
        }
        refreshAllTelemetry(targetRegion, targetCity)
    }

    fun onCoordinatesDetectedAndNotify(context: Context, latitude: Double, longitude: Double) {
        val (nearest, defaultCity) = repository.findNearestCity(latitude, longitude)
        confirmResidingLocationAndNotify(
            context = context,
            region = nearest,
            nearestCity = defaultCity,
            fromGps = true
        )
    }

    fun triggerNotificationForCurrentArea(
        context: Context,
        intervalOption: NotificationIntervalOption = _uiState.value.selectedNotificationInterval
    ) {
        val state = _uiState.value
        confirmResidingLocationAndNotify(
            context = context,
            region = state.selectedRegion,
            nearestCity = state.selectedNearestCity,
            fromGps = state.isGpsDetected,
            intervalOption = intervalOption
        )
    }

    private fun restartPeriodicNotificationJob(
        context: Context,
        intervalOption: NotificationIntervalOption
    ) {
        val appContext = context.applicationContext
        notificationSchedulerJob?.cancel()
        notificationSchedulerJob = viewModelScope.launch {
            while (isActive) {
                delay(intervalOption.durationMillis)
                val state = _uiState.value
                val report = repository.fetchRegionalReport(state.selectedRegion, state.selectedNearestCity)
                val stations = repository.fetchLiveRiverStations(state.selectedRegion)
                val primaryStation = repository.findPrimaryStationForRegion(
                    state.selectedRegion,
                    state.selectedNearestCity,
                    stations
                )
                val govAnnouncements = GovernmentAnnouncementsProvider.getAnnouncementsForArea(
                    selectedRegion = state.selectedRegion,
                    nearestCity = state.selectedNearestCity,
                    primaryStation = primaryStation,
                    allRegions = repository.regions,
                    report = report
                )
                val record = WeatherNotificationHelper.buildAndDispatchAreaNotification(
                    context = appContext,
                    region = state.selectedRegion,
                    nearestCity = state.selectedNearestCity,
                    primaryStation = primaryStation,
                    report = report,
                    isFromGps = state.isGpsDetected,
                    intervalOption = state.selectedNotificationInterval,
                    govAnnouncement = govAnnouncements.firstOrNull()
                )
                val alerts = repository.getAlertsForRegion(
                    state.selectedRegion,
                    state.selectedNearestCity,
                    stations,
                    report
                )
                lastTelemetrySyncMillis = System.currentTimeMillis()
                _uiState.update { current ->
                    current.copy(
                        regionalReport = report,
                        riverStations = stations,
                        alerts = alerts,
                        selectedStationDetail = current.selectedStationDetail?.let { prev ->
                            stations.find { it.id == prev.id }
                        },
                        governmentAnnouncements = govAnnouncements,
                        latestNotification = record,
                        notificationHistory = (listOf(record) + current.notificationHistory)
                            .distinctBy { it.id }
                            .take(8)
                    )
                }
            }
        }
    }

    /**
     * Keeps weather metrics and river gauges in sync while the app is visible: refreshes immediately
     * if the last sync is older than [AUTO_REFRESH_INTERVAL_MS], then every 10 minutes. Call from
     * repeatOnLifecycle(STARTED) so it pauses in the background and resumes when reopened.
     */
    suspend fun runAutoRefreshLoop() {
        while (true) {
            val sinceLastSync = System.currentTimeMillis() - lastTelemetrySyncMillis
            if (sinceLastSync >= AUTO_REFRESH_INTERVAL_MS) {
                // Reuse a sync already in flight (e.g. the one started in init) instead of restarting it
                val inFlight = telemetryRefreshJob?.takeIf { it.isActive }
                (inFlight ?: refreshAllTelemetry()).join()
                if (System.currentTimeMillis() - lastTelemetrySyncMillis >= AUTO_REFRESH_INTERVAL_MS) {
                    delay(30_000L) // sync didn't complete; back off briefly before retrying
                }
            } else {
                delay(AUTO_REFRESH_INTERVAL_MS - sinceLastSync)
            }
        }
    }

    fun refreshAllTelemetry(
        region: SriLankaRegion = _uiState.value.selectedRegion,
        nearestCity: String = _uiState.value.selectedNearestCity
    ): Job {
        // Cancel any in-flight sync so a slow response for a previous area can't overwrite newer data
        telemetryRefreshJob?.cancel()
        return viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val snapshots = repository.fetchIslandSnapshots()
            val report = repository.fetchRegionalReport(region, nearestCity)
            val stations = repository.fetchLiveRiverStations(region)
            val alerts = repository.getAlertsForRegion(region, nearestCity, stations, report)
            val primaryStation = repository.findPrimaryStationForRegion(region, nearestCity, stations)
            val govAnnouncements = GovernmentAnnouncementsProvider.getAnnouncementsForArea(
                selectedRegion = region,
                nearestCity = nearestCity,
                primaryStation = primaryStation,
                allRegions = repository.regions,
                report = report
            )
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    islandSnapshots = snapshots.ifEmpty { state.islandSnapshots },
                    regionalReport = report,
                    riverStations = stations,
                    alerts = alerts,
                    governmentAnnouncements = govAnnouncements,
                    selectedStationDetail = state.selectedStationDetail?.let { prev ->
                        stations.find { it.id == prev.id }
                    }
                )
            }
            lastTelemetrySyncMillis = System.currentTimeMillis()
        }.also { telemetryRefreshJob = it }
    }

    /** The river gauge most relevant to the selected town (null until stations have loaded). */
    fun primaryStationForCurrentArea(): RiverGaugeStation? {
        val state = _uiState.value
        if (state.riverStations.isEmpty()) return null
        return repository.findPrimaryStationForRegion(state.selectedRegion, state.selectedNearestCity, state.riverStations)
    }

    fun setAlertSeverityFilter(severity: AlertSeverity?) {
        _uiState.update { it.copy(selectedAlertSeverityFilter = severity) }
    }

    fun setBasinFilter(basin: String) {
        _uiState.update { it.copy(selectedBasinFilter = basin) }
    }

    fun setRiverSearchQuery(query: String) {
        _uiState.update { state ->
            val trimmed = query.trim()
            val shouldResetBasin = trimmed.isNotEmpty() &&
                state.selectedBasinFilter != "All Basins" &&
                state.riverStations.none { st ->
                    st.riverName.contains(state.selectedBasinFilter, ignoreCase = true) &&
                        (st.stationName.contains(trimmed, ignoreCase = true) ||
                            st.riverName.contains(trimmed, ignoreCase = true) ||
                            st.basinCode.contains(trimmed, ignoreCase = true) ||
                            st.district.contains(trimmed, ignoreCase = true) ||
                            (st.tributaryOf?.contains(trimmed, ignoreCase = true) == true) ||
                            st.downstreamTowns.any { town -> town.contains(trimmed, ignoreCase = true) })
                }
            state.copy(
                riverSearchQuery = query,
                selectedBasinFilter = if (shouldResetBasin) "All Basins" else state.selectedBasinFilter
            )
        }
    }

    fun selectStationDetail(station: RiverGaugeStation?) {
        _uiState.update { it.copy(selectedStationDetail = station) }
    }

    fun setLiveMapMode(mode: LiveMapMode) {
        _uiState.update { it.copy(liveMapMode = mode) }
    }

    fun setZoomEarthLayer(layer: ZoomEarthLayer) {
        _uiState.update { it.copy(selectedZoomEarthLayer = layer) }
    }

    fun setLiveMapOverlayMetric(metric: LiveMapOverlayMetric) {
        _uiState.update { it.copy(liveMapOverlayMetric = metric) }
    }

    fun setRadarFrameIndex(index: Int) {
        val maxIdx = (_uiState.value.radarFrames.size - 1).coerceAtLeast(0)
        _uiState.update { it.copy(currentRadarFrameIndex = index.coerceIn(0, maxIdx)) }
    }

    fun toggleRadarPlayPause() {
        val nextPlaying = !_uiState.value.isRadarPlaying
        _uiState.update { it.copy(isRadarPlaying = nextPlaying) }
        if (nextPlaying) {
            startRadarAnimationLoop()
        } else {
            radarLoopJob?.cancel()
        }
    }

    fun toggleRadarLayer(layer: RadarLayerType) {
        _uiState.update { state ->
            val updated = state.activeRadarLayers.toMutableSet()
            if (updated.contains(layer)) {
                updated.remove(layer)
            } else {
                updated.add(layer)
            }
            state.copy(activeRadarLayers = updated)
        }
    }

    fun toggleChecklistItem(itemId: String) {
        _uiState.update { state ->
            state.copy(
                checklist = state.checklist.map { item ->
                    if (item.id == itemId) item.copy(isChecked = !item.isChecked) else item
                }
            )
        }
    }

    fun openGeminiClarifier(initialQuestion: String? = null) {
        val currentState = _uiState.value
        val peakDbz = currentState.radarFrames.getOrElse(currentState.currentRadarFrameIndex) {
            currentState.radarFrames.firstOrNull()
        }?.peakDbz ?: 48
        val dbzMeaning = getDbzMeaningForValue(peakDbz)
        val defaultIntro = if (currentState.geminiMessages.isEmpty()) {
            listOf(
                GeminiClarificationMessage(
                    id = "welcome-gemini",
                    isUser = false,
                    metricTopic = "Sri Lanka Weather & Hydrology",
                    text = "Ask me to clarify any weather or river metric for ${currentState.selectedNearestCity} (${currentState.selectedRegion.name})—such as Radar Reflectivity ($peakDbz dBZ — ${dbzMeaning.shortTitle}), RiverWatch Discharge (m³/s), 24h Catchment Rain (mm), Surface Pressure (hPa), or Flood Alert levels."
                )
            )
        } else {
            currentState.geminiMessages
        }

        _uiState.update {
            it.copy(
                showGeminiClarifierDialog = true,
                geminiMessages = defaultIntro
            )
        }

        if (!initialQuestion.isNullOrBlank()) {
            askGeminiClarification(initialQuestion)
        }
    }

    fun dismissGeminiClarifier() {
        _uiState.update { it.copy(showGeminiClarifierDialog = false) }
    }

    fun askGeminiClarification(question: String) {
        val trimmed = question.trim()
        if (trimmed.isEmpty()) return

        val userMsg = GeminiClarificationMessage(
            id = "user-${System.currentTimeMillis()}",
            isUser = true,
            text = trimmed
        )

        _uiState.update { state ->
            state.copy(
                isGeminiThinking = true,
                geminiMessages = state.geminiMessages + userMsg
            )
        }

        viewModelScope.launch {
            val state = _uiState.value
            val peakDbz = state.radarFrames.getOrElse(state.currentRadarFrameIndex) {
                state.radarFrames.firstOrNull()
            }?.peakDbz ?: 48
            val primaryStation = repository.findPrimaryStationForRegion(
                state.selectedRegion,
                state.selectedNearestCity,
                state.riverStations
            )

            val explanation = GeminiClarificationClient.clarifyWeatherOrMetric(
                userQuestion = trimmed,
                region = state.selectedRegion,
                nearestCity = state.selectedNearestCity,
                report = state.regionalReport,
                peakDbz = peakDbz,
                primaryStation = primaryStation
            )

            val aiMsg = GeminiClarificationMessage(
                id = "gemini-${System.currentTimeMillis()}",
                isUser = false,
                metricTopic = "${state.selectedNearestCity}, ${state.selectedRegion.name}",
                text = explanation
            )

            _uiState.update { current ->
                current.copy(
                    isGeminiThinking = false,
                    geminiMessages = current.geminiMessages + aiMsg
                )
            }
        }
    }

    companion object {
        const val AUTO_REFRESH_INTERVAL_MS: Long = 10 * 60 * 1000L
    }

    private fun startRadarAnimationLoop() {
        radarLoopJob?.cancel()
        radarLoopJob = viewModelScope.launch {
            while (isActive && _uiState.value.isRadarPlaying) {
                delay(1400L)
                _uiState.update { state ->
                    if (!state.isRadarPlaying || state.radarFrames.isEmpty()) {
                        state
                    } else {
                        val nextIdx = (state.currentRadarFrameIndex + 1) % state.radarFrames.size
                        state.copy(currentRadarFrameIndex = nextIdx)
                    }
                }
            }
        }
    }
}
