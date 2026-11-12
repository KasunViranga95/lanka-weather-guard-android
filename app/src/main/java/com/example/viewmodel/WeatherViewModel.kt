package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SavedPlace
import com.example.data.NearbyGauge
import com.example.data.SriLankaWeatherRepository
import com.example.data.UserPreferencesStore
import com.example.model.AiChatMessage
import com.example.model.AppTab
import com.example.model.CityWeatherSnapshot
import com.example.model.HydrographTimeRange
import com.example.model.RegionalWeatherReport
import com.example.model.RiverGaugeStation
import com.example.model.SriLankaRegion
import com.example.model.ZoomEarthLayer
import com.example.network.AiHelperClient
import com.example.widget.WeatherWidget
import com.example.work.WeatherCheckWorker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WeatherUiState(
    val currentTab: AppTab = AppTab.ALERTS,
    val isDarkTheme: Boolean = true,
    val regions: List<SriLankaRegion> = emptyList(),
    val selectedRegion: SriLankaRegion,
    val selectedNearestCity: String,
    val isGpsDetected: Boolean = false,
    // The phone's position when the place came from "Use my location" (used to find the nearest gauge)
    val gpsLocation: Pair<Double, Double>? = null,
    val savedPlaces: List<SavedPlace> = emptyList(),
    val showPlacePicker: Boolean = false,
    val wasLocationPermissionDenied: Boolean = false,
    val showSettings: Boolean = false,
    val alertsEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val riverStations: List<RiverGaugeStation> = emptyList(),
    val selectedBasinFilter: String = "",
    val riverSearchQuery: String = "",
    val selectedHydrographTimeRange: HydrographTimeRange = HydrographTimeRange.HOURS_24,
    val regionalReport: RegionalWeatherReport,
    val selectedMapLayer: ZoomEarthLayer = ZoomEarthLayer.LIVE_HOME,
    val checkedChecklistIds: Set<String> = emptySet(),
    val showAiHelper: Boolean = false,
    val isAiThinking: Boolean = false,
    val aiMessages: List<AiChatMessage> = emptyList(),
    // Live conditions for towns across the island (Map tab)
    val islandSnapshots: List<CityWeatherSnapshot> = emptyList()
)

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SriLankaWeatherRepository()

    private val savedPrefs = UserPreferencesStore.loadPreferences()

    private val initialRegion: SriLankaRegion =
        savedPrefs.regionId?.let { repository.regionById(it) } ?: repository.regions.first()

    private val initialCity: String = savedPrefs.nearestCity?.takeIf { city ->
        city.isNotBlank() && initialRegion.nearestCities.any { it.equals(city, ignoreCase = true) }
    } ?: initialRegion.nearestCities.getOrElse(1) { initialRegion.name }

    private val _uiState = MutableStateFlow(
        WeatherUiState(
            isDarkTheme = savedPrefs.isDarkTheme,
            regions = repository.regions,
            selectedRegion = initialRegion,
            selectedNearestCity = initialCity,
            isGpsDetected = savedPrefs.isFromGps,
            gpsLocation = savedPrefs.gpsLocation,
            savedPlaces = UserPreferencesStore.loadPlaces(),
            alertsEnabled = UserPreferencesStore.alertsEnabled(),
            // Show what was saved on the phone straight away; live data replaces it when it arrives
            riverStations = repository.cachedStations() ?: repository.getReferenceStations(),
            regionalReport = repository.cachedReport(initialRegion, initialCity)
                ?: repository.getPlaceholderRegionalReport(initialRegion, initialCity),
            checkedChecklistIds = UserPreferencesStore.loadChecklist()
        )
    )
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var telemetryRefreshJob: Job? = null
    @Volatile
    private var lastTelemetrySyncMillis: Long = 0L

    init {
        if (_uiState.value.savedPlaces.isEmpty()) {
            UserPreferencesStore.addPlace(SavedPlace(initialRegion.id, initialCity))
            _uiState.update { it.copy(savedPlaces = UserPreferencesStore.loadPlaces()) }
        }
        refreshAllTelemetry(initialRegion, initialCity)
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun setDarkTheme(dark: Boolean) {
        UserPreferencesStore.saveDarkTheme(dark)
        _uiState.update { it.copy(isDarkTheme = dark) }
    }

    // ---- Places ----

    fun openPlacePicker(permissionDenied: Boolean = false) {
        _uiState.update { it.copy(showPlacePicker = true, wasLocationPermissionDenied = permissionDenied) }
    }

    fun dismissPlacePicker() {
        _uiState.update { it.copy(showPlacePicker = false) }
    }

    /** Shows [nearestCity] in [region], adding it to the saved places. */
    fun selectPlace(region: SriLankaRegion, nearestCity: String, fromGps: Boolean = false, gpsLocation: Pair<Double, Double>? = null) {
        UserPreferencesStore.saveLocation(region, nearestCity, fromGps, gpsLocation)
        _uiState.update {
            it.copy(
                selectedRegion = region,
                selectedNearestCity = nearestCity,
                isGpsDetected = fromGps,
                gpsLocation = gpsLocation?.takeIf { fromGps },
                showPlacePicker = false,
                savedPlaces = UserPreferencesStore.loadPlaces(),
                regionalReport = repository.cachedReport(region, nearestCity)
                    ?: repository.getPlaceholderRegionalReport(region, nearestCity)
            )
        }
        refreshAllTelemetry(region, nearestCity)
    }

    fun selectSavedPlace(place: SavedPlace) {
        val region = repository.regionById(place.regionId) ?: return
        selectPlace(region, place.city)
    }

    fun removePlace(place: SavedPlace) {
        UserPreferencesStore.removePlace(place)
        val remaining = UserPreferencesStore.loadPlaces()
        _uiState.update { it.copy(savedPlaces = remaining) }
        val state = _uiState.value
        val wasCurrent = place.regionId == state.selectedRegion.id && place.city.equals(state.selectedNearestCity, ignoreCase = true)
        if (wasCurrent) remaining.firstOrNull()?.let { selectSavedPlace(it) }
    }

    fun onCoordinatesDetected(latitude: Double, longitude: Double) {
        val (nearest, city) = repository.findNearestCity(latitude, longitude)
        selectPlace(nearest, city, fromGps = true, gpsLocation = latitude to longitude)
    }

    /** Opens the app on the place a notification was about. */
    fun handleNotificationClicked(regionId: String?, nearestCity: String?) {
        val region = regionId?.let { repository.regionById(it) } ?: return
        val city = nearestCity?.takeIf { it.isNotBlank() } ?: region.nearestCities.firstOrNull() ?: region.name
        _uiState.update { it.copy(currentTab = AppTab.ALERTS) }
        selectPlace(region, city)
    }

    // ---- Settings ----

    fun openSettings() = _uiState.update { it.copy(showSettings = true) }

    fun dismissSettings() = _uiState.update { it.copy(showSettings = false) }

    fun setAlertsEnabled(enabled: Boolean) {
        UserPreferencesStore.setAlertsEnabled(enabled)
        _uiState.update { it.copy(alertsEnabled = enabled) }
        if (enabled) WeatherCheckWorker.runNow(getApplication())
    }

    // ---- Data ----

    /**
     * Keeps weather and river gauges fresh while the app is on screen: refreshes immediately if the last
     * sync is older than [AUTO_REFRESH_INTERVAL_MS], then every 10 minutes. Call from
     * repeatOnLifecycle(STARTED) so it pauses in the background (the background worker takes over).
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
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    islandSnapshots = snapshots.ifEmpty { state.islandSnapshots },
                    regionalReport = report,
                    riverStations = stations
                )
            }
            if (report.isLiveApiData && !report.isFromCache) {
                lastTelemetrySyncMillis = System.currentTimeMillis()
                WeatherWidget.updateAll(getApplication())
            }
        }.also { telemetryRefreshJob = it }
    }

    /** The river gauge most relevant to the selected place, or null when none is close enough. */
    fun primaryStationForCurrentArea(): NearbyGauge? {
        val state = _uiState.value
        return repository.findPrimaryStationForRegion(state.selectedRegion, state.selectedNearestCity, state.riverStations, state.gpsLocation)
    }

    // ---- Rivers ----

    fun setBasinFilter(basin: String) {
        _uiState.update { it.copy(selectedBasinFilter = basin) }
    }

    fun setRiverSearchQuery(query: String) {
        _uiState.update { it.copy(riverSearchQuery = query, selectedBasinFilter = if (query.isNotBlank()) "" else it.selectedBasinFilter) }
    }

    fun setHydrographTimeRange(range: HydrographTimeRange) {
        _uiState.update { it.copy(selectedHydrographTimeRange = range) }
    }

    // ---- Map ----

    fun setMapLayer(layer: ZoomEarthLayer) {
        _uiState.update { it.copy(selectedMapLayer = layer) }
    }

    // ---- Safety ----

    fun toggleChecklistItem(itemId: String) {
        _uiState.update { state ->
            val updated = if (itemId in state.checkedChecklistIds) state.checkedChecklistIds - itemId else state.checkedChecklistIds + itemId
            UserPreferencesStore.saveChecklist(updated)
            state.copy(checkedChecklistIds = updated)
        }
    }

    // ---- AI helper ----

    val isAiHelperAvailable: Boolean get() = AiHelperClient.isAvailable

    fun openAiHelper() = _uiState.update { it.copy(showAiHelper = true) }

    fun dismissAiHelper() = _uiState.update { it.copy(showAiHelper = false) }

    fun askAiHelper(question: String, languageTag: String) {
        val trimmed = question.trim()
        if (trimmed.isEmpty()) return
        _uiState.update { state ->
            state.copy(
                isAiThinking = true,
                aiMessages = state.aiMessages + AiChatMessage(id = "user-${System.currentTimeMillis()}", isUser = true, text = trimmed)
            )
        }
        viewModelScope.launch {
            val state = _uiState.value
            val answer = AiHelperClient.ask(
                question = trimmed,
                region = state.selectedRegion,
                nearestCity = state.selectedNearestCity,
                report = state.regionalReport,
                station = primaryStationForCurrentArea()?.station?.takeIf { it.hasLiveReading },
                languageTag = languageTag
            )
            _uiState.update { current ->
                current.copy(
                    isAiThinking = false,
                    aiMessages = current.aiMessages + AiChatMessage(
                        id = "ai-${System.currentTimeMillis()}",
                        isUser = false,
                        text = answer.orEmpty(),
                        failed = answer == null
                    )
                )
            }
        }
    }

    companion object {
        const val AUTO_REFRESH_INTERVAL_MS: Long = 10 * 60 * 1000L
    }
}
