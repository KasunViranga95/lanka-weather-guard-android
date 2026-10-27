package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AdvisoryYellow
import com.example.ui.theme.AlertRed
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.WarningAmber
import java.util.Locale
import kotlin.math.max

enum class AppTab(val route: String, val label: String) {
    ALERTS("alerts", "Alerts"),
    RADAR("radar", "Live Map"),
    RIVERS("rivers", "Rivers"),
    FORECAST("forecast", "Forecast"),
    SAFETY("safety", "Safety Hub")
}

enum class AlertSeverity(
    val label: String,
    val badgeColor: Color,
    val priority: Int
) {
    CRITICAL_RED("RED EVACUATION WARNING", AlertRed, 4),
    MAJOR_AMBER("AMBER FLOOD / STORM WATCH", WarningAmber, 3),
    ADVISORY_YELLOW("YELLOW WEATHER ADVISORY", AdvisoryYellow, 2),
    SAFE_GREEN("NORMAL MONITOR", SafeEmerald, 1)
}

enum class FloodStatus(
    val label: String,
    val shortLabel: String,
    val color: Color,
    val severityRank: Int
) {
    MAJOR_FLOOD("Major Flood", "MAJOR FLOOD", AlertRed, 4),
    MINOR_FLOOD("Minor Flood", "MINOR FLOOD", WarningAmber, 3),
    ALERT("Alert Level", "ALERT", AdvisoryYellow, 2),
    NORMAL("Normal", "NORMAL", SafeEmerald, 1)
}

enum class WaterTrend(val label: String, val symbol: String) {
    RISING_FAST("Rising Rapidly", "▲▲"),
    RISING("Rising", "▲"),
    STABLE("Stable", "●"),
    FALLING("Receding", "▼")
}

enum class HydrographTimeRange(
    val id: String,
    val shortLabel: String,
    val fullTitle: String,
    val xAxisStartLabel: String,
    val xAxisMidLabel: String,
    val xAxisEndLabel: String
) {
    HOURS_6("6h", "6H", "Last 6 Hours (Hourly)", "-6h", "-3h", "Now"),
    HOURS_24("24h", "24H", "Last 24 Hours (3-Hourly)", "-24h", "-12h", "Now"),
    DAYS_3("3d", "3D", "Last 3 Days (72h Trend)", "-3 Days", "-36h", "Now"),
    DAYS_7("7d", "7D", "Last 7 Days (Weekly Basin Curve)", "-7 Days", "-3.5d", "Now")
}

enum class NotificationIntervalOption(
    val id: String,
    val label: String,
    val shortChipLabel: String,
    val intervalMinutes: Int
) {
    MIN_5("5m", "Every 5 min", "5 min", 5),
    MIN_15("15m", "Every 15 min", "15 min", 15),
    MIN_30("30m", "Every 30 min", "30 min", 30),
    HOUR_1("1h", "Every 1 hour", "1 hour", 60),
    HOUR_2("2h", "Every 2 hours", "2 hour", 120),
    HOUR_4("4h", "Every 4 hours", "4 hour", 240);

    val durationMillis: Long
        get() = intervalMinutes * 60_000L
}

enum class RadarLayerType(val label: String) {
    PRECIPITATION_DBZ("Rain Radar (dBZ)"),
    RIVER_GAUGES("RiverWatch Gauges"),
    WIND_VECTORS("Monsoon Wind"),
    LANDSLIDE_ZONES("NBRO Landslide")
}

enum class LiveMapMode(val label: String) {
    INTERACTIVE_LIVE_MAP("Live Weather Map"),
    WINDY_LIVE("Windy.com Live"),
    TACTICAL_DOPPLER_CANVAS("Doppler Radar")
}

enum class ZoomEarthLayer(val label: String, val windyOverlay: String) {
    LIVE_HOME("Rain & Thunder", "rain"),
    PRECIPITATION("Rain Accumulation", "rainAccu"),
    RADAR("Weather Radar (dBZ)", "radar"),
    SATELLITE("Satellite Clouds", "clouds"),
    WIND_SPEED("Wind & Gusts", "wind"),
    TEMPERATURE("Live Temperature", "temp"),
    PRESSURE("Pressure (hPa)", "pressure");

    fun buildWindyEmbedUrl(lat: Double, lon: Double, zoom: Int = 10): String {
        val formattedLat = String.format(Locale.US, "%.4f", lat)
        val formattedLon = String.format(Locale.US, "%.4f", lon)
        return "https://embed.windy.com/embed2.html?lat=$formattedLat&lon=$formattedLon&detailLat=$formattedLat&detailLon=$formattedLon&width=650&height=450&zoom=$zoom&level=surface&overlay=$windyOverlay&product=ecmwf&menu=&message=true&marker=true&calendar=now&pressure=true&type=map&location=coordinates&detail=&metricWind=km%2Fh&metricTemp=%C2%B0C&radarRange=-1"
    }

    fun buildWindyBrowserUrl(lat: Double, lon: Double, zoom: Int = 10): String {
        val formattedLat = String.format(Locale.US, "%.4f", lat)
        val formattedLon = String.format(Locale.US, "%.4f", lon)
        return "https://www.windy.com/$formattedLat/$formattedLon?-$windyOverlay,$formattedLat,$formattedLon,$zoom,m:eI5adTl"
    }
}

enum class LiveMapOverlayMetric(val label: String, val code: String) {
    RADAR_PRECIP("Live Rain & Temp", "radar"),
    TEMP_HUMIDITY("Temperature (°C)", "temp"),
    PRECIP_24H("Precipitation (mm)", "precip"),
    RIVER_GAUGES("RiverWatch Gauges", "rivers"),
    WIND_PRESSURE("Wind & Pressure", "wind")
}

data class DbzValueMeaning(
    val dbzRange: String,
    val shortTitle: String,
    val rainRateMmPerHour: String,
    val weatherMeaning: String,
    val citizenAction: String,
    val badgeColor: Color,
    val minDbz: Int,
    val maxDbz: Int
)

val RadarDbzMeaningList: List<DbzValueMeaning> = listOf(
    DbzValueMeaning(
        dbzRange = "15 – 25 dBZ",
        shortTitle = "Light Rain / Drizzle",
        rainRateMmPerHour = "0.5 – 2.5 mm/h",
        weatherMeaning = "Small water droplets or light passing monsoon drizzle.",
        citizenAction = "Safe for normal outdoor activities; carry an umbrella.",
        badgeColor = SafeEmerald,
        minDbz = 15,
        maxDbz = 25
    ),
    DbzValueMeaning(
        dbzRange = "26 – 35 dBZ",
        shortTitle = "Moderate Steady Rain",
        rainRateMmPerHour = "2.5 – 10 mm/h",
        weatherMeaning = "Steady regional rainfall wetting roads and feeding river tributaries.",
        citizenAction = "Drive carefully on wet roads; monitor local river gauges.",
        badgeColor = RadarCyan,
        minDbz = 26,
        maxDbz = 35
    ),
    DbzValueMeaning(
        dbzRange = "36 – 45 dBZ",
        shortTitle = "Heavy Downpour",
        rainRateMmPerHour = "10 – 30 mm/h",
        weatherMeaning = "Dense monsoon rain cells causing rapid surface runoff and rising drains.",
        citizenAction = "Avoid low-lying floodplains; secure outdoor items.",
        badgeColor = AdvisoryYellow,
        minDbz = 36,
        maxDbz = 45
    ),
    DbzValueMeaning(
        dbzRange = "46 – 52 dBZ",
        shortTitle = "Torrential Squall",
        rainRateMmPerHour = "30 – 65 mm/h",
        weatherMeaning = "Intense convective thunderstorm cell with strong wind gusts & flash flooding.",
        citizenAction = "Postpone travel; high risk of urban road flooding & slope instability.",
        badgeColor = WarningAmber,
        minDbz = 46,
        maxDbz = 52
    ),
    DbzValueMeaning(
        dbzRange = "53 – 65+ dBZ",
        shortTitle = "Extreme Cloudburst",
        rainRateMmPerHour = "65 – 150+ mm/h",
        weatherMeaning = "Severe tropical storm core capable of rapid river overflow & landslides.",
        citizenAction = "IMMEDIATE DANGER: Move to high ground & follow DMC 117 evacuation orders!",
        badgeColor = AlertRed,
        minDbz = 53,
        maxDbz = 99
    )
)

fun getDbzMeaningForValue(dbz: Int): DbzValueMeaning {
    return RadarDbzMeaningList.firstOrNull { dbz in it.minDbz..it.maxDbz }
        ?: if (dbz < 15) RadarDbzMeaningList.first() else RadarDbzMeaningList.last()
}

data class GeminiClarificationMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val metricTopic: String = "Weather & Flood Telemetry",
    val timestampText: String = "Just now"
)

data class SriLankaRegion(
    val id: String,
    val name: String,
    val province: String,
    val latitude: Double,
    val longitude: Double,
    val mapXFraction: Float, // 0f..1f on Sri Lanka island canvas
    val mapYFraction: Float, // 0f..1f on Sri Lanka island canvas
    val primaryRiverBasin: String,
    val isLandslideProne: Boolean,
    val isCoastalZone: Boolean,
    val elevationMeters: Int,
    val nearestCities: List<String> = listOf(name)
) {
    fun getCoordinatesForCity(cityName: String): Pair<Double, Double> {
        val exactCoord = SRI_LANKA_CITY_COORDINATES[cityName.trim().lowercase(Locale.US)]
        if (exactCoord != null) return exactCoord
        val idx = nearestCities.indexOfFirst { it.equals(cityName, ignoreCase = true) }
        if (idx <= 0) return latitude to longitude
        val latOffset = ((idx % 3) - 1) * 0.028
        val lonOffset = ((idx / 2) - 1) * 0.028
        return (latitude + latOffset) to (longitude + lonOffset)
    }
}

private val SRI_LANKA_CITY_COORDINATES: Map<String, Pair<Double, Double>> = mapOf(
    // Colombo District
    "colombo fort" to (6.9344 to 79.8428),
    "wellampitiya" to (6.9369 to 79.9019),
    "kaduwela" to (6.9333 to 79.9833),
    "kolonnawa" to (6.9328 to 79.8889),
    "hanwella" to (6.9011 to 80.0853),
    "avissawella" to (6.9522 to 80.2117),
    "dehiwala" to (6.8511 to 79.8630),
    "moratuwa" to (6.7730 to 79.8816),
    "maharagama" to (6.8480 to 79.9265),
    "homagama" to (6.8440 to 80.0024),
    // Gampaha District
    "gampaha" to (7.0873 to 80.0144),
    "negombo" to (7.2008 to 79.8737),
    "ja-ela" to (7.0747 to 79.8919),
    "wattala" to (6.9892 to 79.8917),
    "kelaniya" to (6.9553 to 79.9220),
    "biyagama" to (6.9469 to 80.0050),
    "veyangoda" to (7.1558 to 80.0569),
    "mirigama" to (7.2458 to 80.1258),
    "minuwangoda" to (7.1667 to 79.9500),
    // Kalutara District
    "kalutara" to (6.5854 to 79.9607),
    "panadura" to (6.7132 to 79.9026),
    "horana" to (6.7158 to 80.0625),
    "bulathsinhala" to (6.6531 to 80.1764),
    "palindanuwara" to (6.5269 to 80.1942),
    "matugama" to (6.5219 to 80.1144),
    "aluthgama" to (6.4342 to 79.9989),
    "bandaragama" to (6.7147 to 79.9889),
    // Ratnapura District
    "ratnapura" to (6.6828 to 80.3992),
    "kuruwita" to (6.7772 to 80.3653),
    "eheliyagoda" to (6.8489 to 80.2619),
    "pelmadulla" to (6.6258 to 80.5364),
    "kalawana" to (6.5333 to 80.4000),
    "elapatha" to (6.6528 to 80.3472),
    "ayagama" to (6.6500 to 80.3000),
    "balangoda" to (6.6508 to 80.6986),
    "embilipitiya" to (6.3333 to 80.8500),
    // Kegalle District
    "kegalle" to (7.2513 to 80.3464),
    "kitulgala" to (6.9897 to 80.4178),
    "deraniyagala" to (6.9247 to 80.3389),
    "dehiowita" to (6.9694 to 80.2647),
    "ruwanwella" to (7.0422 to 80.2528),
    "warakapola" to (7.2269 to 80.1969),
    "mawanella" to (7.2525 to 80.4464),
    "rambukkana" to (7.3228 to 80.3911),
    // Galle District
    "galle" to (6.0535 to 80.2210),
    "baddegama" to (6.1833 to 80.1833),
    "thawalama" to (6.3389 to 80.3264),
    "neluwa" to (6.3833 to 80.3667),
    "nagoda" to (6.2167 to 80.2667),
    "hikkaduwa" to (6.1395 to 80.1063),
    "ambalangoda" to (6.2355 to 80.0538),
    "elpitiya" to (6.2922 to 80.1636),
    // Matara District
    "matara" to (5.9549 to 80.5550),
    "akuressa" to (6.0969 to 80.4758),
    "pitabeddara" to (6.2089 to 80.4647),
    "athuraliya" to (6.0539 to 80.5250),
    "malimbada" to (6.0167 to 80.5167),
    "weligama" to (5.9740 to 80.4294),
    "deniyaya" to (6.3428 to 80.5578),
    // Hambantota District
    "hambantota" to (6.1246 to 81.1185),
    "tissamaharama" to (6.2833 to 81.2833),
    "tangalle" to (6.0240 to 80.7942),
    "ambalantota" to (6.1228 to 81.0258),
    // Kandy District
    "kandy" to (7.2906 to 80.6337),
    "peradeniya" to (7.2631 to 80.5928),
    "gampola" to (7.1644 to 80.5767),
    "nawalapitiya" to (7.0544 to 80.5342),
    "katugastota" to (7.3264 to 80.6231),
    // Nuwara Eliya District
    "nuwara eliya" to (6.9497 to 80.7891),
    "hatton" to (6.8917 to 80.5956),
    "talawakele" to (6.9372 to 80.6581),
    "walapane" to (7.0956 to 80.8617),
    // Badulla, Monaragala, Matale, Kurunegala, Puttalam, Anuradhapura, Polonnaruwa, Jaffna, Trincomalee, Batticaloa, Ampara
    "badulla" to (6.9934 to 81.0550),
    "bandarawela" to (6.8259 to 80.9982),
    "monaragala" to (6.8728 to 81.3506),
    "kataragama" to (6.4136 to 81.3325),
    "matale" to (7.4675 to 80.6234),
    "dambulla" to (7.8742 to 80.6511),
    "kurunegala" to (7.4818 to 80.3609),
    "chilaw" to (7.5758 to 79.7953),
    "puttalam" to (8.0362 to 79.8283),
    "anuradhapura" to (8.3114 to 80.4037),
    "polonnaruwa" to (7.9403 to 81.0188),
    "jaffna" to (9.6615 to 80.0255),
    "kilinochchi" to (9.3803 to 80.3770),
    "mullaitivu" to (9.2671 to 80.8142),
    "vavuniya" to (8.7542 to 80.4982),
    "mannar" to (8.9810 to 79.9044),
    "trincomalee" to (8.5874 to 81.2152),
    "batticaloa" to (7.7310 to 81.6747),
    "ampara" to (7.2975 to 81.6820),
    "kalmunai" to (7.4090 to 81.8347)
)

data class DispatchedNotificationRecord(
    val id: String,
    val title: String,
    val body: String,
    val district: String,
    val nearestCity: String,
    val riverBasin: String,
    val severity: AlertSeverity,
    val timestampText: String,
    val sourceMethod: String,
    val intervalLabel: String = "Every 15 min",
    val officialPortalUrl: String = "https://www.dmc.gov.lk",
    val temperatureC: Double = 28.0,
    val feelsLikeC: Double = 31.0,
    val hourlyPrecipitationMm: Double = 4.2,
    val rain24hMm: Double = 142.0,
    val currentWeatherSummary: String = "Heavy Monsoon Showers • Gusts 58 km/h",
    val forecastSummary: String = "Next 24h: Heavy Rain (90% prob, 25°C–30°C)",
    val govAnnouncementHeadline: String = "DMC Flood & Weather Directive Active",
    val govReferenceCode: String = "DMC/SL/2025"
)

data class GovernmentAnnouncement(
    val id: String,
    val headline: String,
    val issuingMinistryOrAgency: String,
    val referenceCode: String,
    val category: String,
    val publishedTimeText: String,
    val targetDistricts: List<String>,
    val targetCities: List<String>,
    val summary: String,
    val keyDirectives: List<String>,
    val officialUrl: String,
    val officialPortalName: String,
    val severity: AlertSeverity,
    val isResidingAreaMatch: Boolean = false
)

data class WeatherAlert(
    val id: String,
    val title: String,
    val issuingAgency: String,
    val district: String,
    val riverBasin: String?,
    val severity: AlertSeverity,
    val summary: String,
    val recommendedActions: List<String>,
    val issuedTimeText: String,
    val validUntilText: String,
    val rainfallMm24h: Double,
    val windGustKmh: Double,
    val isUserDistrictMatch: Boolean = false
)

data class RiverGaugeStation(
    val id: String,
    val stationName: String,
    val riverName: String,
    val basinCode: String,
    val district: String,
    val latitude: Double,
    val longitude: Double,
    val mapXFraction: Float,
    val mapYFraction: Float,
    val currentLevelM: Double,
    val previousLevelM: Double,
    val alertLevelM: Double,
    val minorFloodLevelM: Double,
    val majorFloodLevelM: Double,
    val catchmentRainMm24h: Double,
    val dischargeM3s: Double,
    val lastUpdatedText: String,
    val hourlyLevelsM: List<Double>,
    val downstreamTowns: List<String>,
    val tributaryOf: String? = null
) {
    val floodStatus: FloodStatus
        get() = when {
            currentLevelM >= majorFloodLevelM -> FloodStatus.MAJOR_FLOOD
            currentLevelM >= minorFloodLevelM -> FloodStatus.MINOR_FLOOD
            currentLevelM >= alertLevelM -> FloodStatus.ALERT
            else -> FloodStatus.NORMAL
        }

    val deltaM: Double
        get() = currentLevelM - previousLevelM

    val trend: WaterTrend
        get() = when {
            deltaM >= 0.18 -> WaterTrend.RISING_FAST
            deltaM > 0.03 -> WaterTrend.RISING
            deltaM < -0.03 -> WaterTrend.FALLING
            else -> WaterTrend.STABLE
        }

    val fillRatioToMajor: Float
        get() = (currentLevelM / (majorFloodLevelM * 1.15)).toFloat().coerceIn(0.08f, 1f)

    fun getLevelsForTimeRange(range: HydrographTimeRange): List<Double> {
        val base6h = hourlyLevelsM.ifEmpty { listOf(previousLevelM, currentLevelM) }
        val earliest6h = base6h.first()
        val normalBaseline = max(0.45, alertLevelM * 0.62)
        return when (range) {
            HydrographTimeRange.HOURS_6 -> base6h
            HydrographTimeRange.HOURS_24 -> listOf(
                normalBaseline + (earliest6h - normalBaseline) * 0.25,
                normalBaseline + (earliest6h - normalBaseline) * 0.42,
                normalBaseline + (earliest6h - normalBaseline) * 0.60,
                normalBaseline + (earliest6h - normalBaseline) * 0.78,
                earliest6h,
                base6h.getOrElse(base6h.size / 3) { previousLevelM },
                base6h.getOrElse((base6h.size * 2) / 3) { previousLevelM },
                currentLevelM
            )
            HydrographTimeRange.DAYS_3 -> listOf(
                normalBaseline * 0.90,
                normalBaseline * 0.96,
                normalBaseline * 1.08,
                normalBaseline + (earliest6h - normalBaseline) * 0.35,
                normalBaseline + (earliest6h - normalBaseline) * 0.68,
                earliest6h,
                previousLevelM,
                currentLevelM
            )
            HydrographTimeRange.DAYS_7 -> listOf(
                normalBaseline * 0.82,
                normalBaseline * 0.88,
                normalBaseline * 0.94,
                normalBaseline * 0.91,
                normalBaseline * 1.12,
                normalBaseline + (earliest6h - normalBaseline) * 0.52,
                earliest6h,
                currentLevelM
            )
        }
    }
}

data class HourlyForecastItem(
    val timeLabel: String,
    val tempC: Double,
    val rainMm: Double,
    val rainProbability: Int,
    val windGustKmh: Double,
    val weatherCode: Int
)

data class DailyForecastItem(
    val dayLabel: String,
    val dateLabel: String,
    val maxTempC: Double,
    val minTempC: Double,
    val totalRainMm: Double,
    val maxRainProb: Int,
    val maxWindKmh: Double,
    val weatherSummary: String,
    val riskLevel: AlertSeverity
)

data class ActivitySafetyAssessment(
    val activityName: String,
    val category: String,
    val statusLabel: String,
    val statusColor: Color,
    val recommendation: String,
    val safetyScore: Int // 0..100
)

data class RegionalWeatherReport(
    val region: SriLankaRegion,
    val currentTempC: Double,
    val feelsLikeC: Double,
    val humidityPercent: Int,
    val precipitationMm: Double,
    val rain24hMm: Double,
    val windSpeedKmh: Double,
    val windGustKmh: Double,
    val weatherDescription: String,
    val pressureHpa: Double,
    val hourly: List<HourlyForecastItem>,
    val daily: List<DailyForecastItem>,
    val activityAssessments: List<ActivitySafetyAssessment>,
    val isLiveApiData: Boolean,
    val syncedTimestamp: String
)

data class RadarFrame(
    val index: Int,
    val timeOffsetMinutes: Int,
    val label: String,
    val isNowcastForecast: Boolean,
    val peakDbz: Int,
    val activeCells: List<RadarStormCell>
)

data class RadarStormCell(
    val id: String,
    val centerX: Float,
    val centerY: Float,
    val radiusX: Float,
    val radiusY: Float,
    val intensityDbz: Int, // 20..62 dBZ
    val movementDeg: Float,
    val label: String
) {
    val color: Color
        get() = when {
            intensityDbz >= 52 -> AlertRed
            intensityDbz >= 44 -> WarningAmber
            intensityDbz >= 35 -> AdvisoryYellow
            intensityDbz >= 26 -> RadarCyan
            else -> SafeEmerald
        }
}

data class EmergencyShelter(
    val id: String,
    val name: String,
    val district: String,
    val elevationMeters: Int,
    val capacityPeople: Int,
    val contactPhone: String,
    val distanceKmText: String,
    val facilities: List<String>
)

data class SafetyChecklistItem(
    val id: String,
    val title: String,
    val detail: String,
    val category: String,
    val isChecked: Boolean = false
)
