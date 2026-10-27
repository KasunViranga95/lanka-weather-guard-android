package com.example.network

import com.example.BuildConfig
import com.example.model.RegionalWeatherReport
import com.example.model.RiverGaugeStation
import com.example.model.SriLankaRegion
import com.example.model.getDbzMeaningForValue
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

data class GeminiGenerateContentRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null
)

data class GeminiContent(
    @Json(name = "role") val role: String? = null,
    @Json(name = "parts") val parts: List<GeminiPart>
)

data class GeminiPart(
    @Json(name = "text") val text: String? = null
)

data class GeminiGenerationConfig(
    @Json(name = "temperature") val temperature: Float = 0.4f,
    @Json(name = "topP") val topP: Float = 0.9f
)

data class GeminiGenerateContentResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateContentRequest
    ): GeminiGenerateContentResponse
}

object GeminiClarificationClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // 60-second timeouts as required by gemini-api guidelines
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun clarifyWeatherOrMetric(
        userQuestion: String,
        region: SriLankaRegion,
        nearestCity: String,
        report: RegionalWeatherReport,
        peakDbz: Int,
        primaryStation: RiverGaugeStation?
    ): String = withContext(Dispatchers.IO) {
        val apiKey = runCatching {
            BuildConfig::class.java.getDeclaredField("GEMINI_API_KEY").get(null) as? String
        }.getOrNull().orEmpty()
        val dbzMeaning = getDbzMeaningForValue(peakDbz)

        val systemContext = buildString {
            appendLine("You are the Lanka Weather Guard Meteorologist & Hydrologist AI Assistant for Sri Lanka.")
            appendLine("Explain weather, radar (dBZ), river hydrology (m, m³/s, catchment rain mm), and DMC safety metrics in simple, clear, actionable terms for citizens.")
            appendLine("Current User Location: $nearestCity, ${region.name} District (${region.province} Province).")
            appendLine("Primary River Basin: ${region.primaryRiverBasin}.")
            appendLine("Live Weather Telemetry: Temp ${String.format(Locale.US, "%.1f°C", report.currentTempC)} (Feels like ${report.feelsLikeC.roundToInt()}°C), Hourly Rain ${String.format(Locale.US, "%.1f mm/h", report.precipitationMm)}, 24h Rain ${report.rain24hMm.roundToInt()} mm, Wind Gusts ${report.windGustKmh.roundToInt()} km/h, Pressure ${report.pressureHpa.roundToInt()} hPa, Humidity ${report.humidityPercent}%.")
            appendLine("Radar Echo Peak: $peakDbz dBZ — ${dbzMeaning.shortTitle} (${dbzMeaning.rainRateMmPerHour}): ${dbzMeaning.weatherMeaning}")
            if (primaryStation != null) {
                appendLine("Nearest RiverWatch Gauge: ${primaryStation.stationName} on ${primaryStation.riverName} (${primaryStation.basinCode}) at ${String.format(Locale.US, "%.2f m", primaryStation.currentLevelM)} [${primaryStation.floodStatus.label}], Alert: ${primaryStation.alertLevelM}m, Minor Flood: ${primaryStation.minorFloodLevelM}m, Major Flood: ${primaryStation.majorFloodLevelM}m, Discharge: ${primaryStation.dischargeM3s.roundToInt()} m³/s.")
            }
            appendLine("Keep responses concise (3-5 short bullet points), easy to read, and include practical safety guidance.")
        }

        val isKeyConfigured = apiKey.isNotBlank() &&
            apiKey != "MY_GEMINI_API_KEY" &&
            !apiKey.startsWith("YOUR_")

        if (isKeyConfigured) {
            try {
                val request = GeminiGenerateContentRequest(
                    contents = listOf(
                        GeminiContent(
                            role = "user",
                            parts = listOf(GeminiPart(text = userQuestion))
                        )
                    ),
                    systemInstruction = GeminiContent(
                        parts = listOf(GeminiPart(text = systemContext))
                    ),
                    generationConfig = GeminiGenerationConfig(
                        temperature = 0.35f,
                        topP = 0.9f
                    )
                )
                val response = service.generateContent(apiKey = apiKey, request = request)
                val answer = response.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull()
                    ?.text
                    ?.trim()
                if (!answer.isNullOrBlank()) {
                    return@withContext answer
                }
            } catch (_: Exception) {
                // Fall back to rich telemetry-backed domain explanation if network/key fails during adverse weather
            }
        }

        buildInstantTelemetryExplanation(
            question = userQuestion,
            region = region,
            nearestCity = nearestCity,
            report = report,
            peakDbz = peakDbz,
            primaryStation = primaryStation
        )
    }

    private fun buildInstantTelemetryExplanation(
        question: String,
        region: SriLankaRegion,
        nearestCity: String,
        report: RegionalWeatherReport,
        peakDbz: Int,
        primaryStation: RiverGaugeStation?
    ): String {
        val q = question.lowercase(Locale.US)
        val dbzMeaning = getDbzMeaningForValue(peakDbz)
        return when {
            q.contains("dbz") || q.contains("radar") || q.contains("echo") -> buildString {
                appendLine("📡 Radar Reflectivity ($peakDbz dBZ — ${dbzMeaning.shortTitle}) Explained:")
                appendLine("• What dBZ Measures: Decibels of Z (dBZ) measures how strongly raindrops reflect weather radar pulses over $nearestCity (${region.name}).")
                appendLine("• Current Reading ($peakDbz dBZ → ${dbzMeaning.shortTitle}): Indicates ${dbzMeaning.rainRateMmPerHour} rainfall rate. ${dbzMeaning.weatherMeaning}")
                appendLine("• Scale Reference: 15–25 dBZ = Light Drizzle | 26–35 dBZ = Moderate Rain | 36–45 dBZ = Heavy Downpour | 46–52 dBZ = Torrential Squall | 53+ dBZ = Extreme Cloudburst.")
                appendLine("• Recommended Action: ${dbzMeaning.citizenAction}")
            }

            q.contains("river") || q.contains("m³/s") || q.contains("flow") || q.contains("flood") || q.contains("gauge") -> {
                val st = primaryStation
                buildString {
                    if (st != null) {
                        appendLine("🌊 RiverWatch Hydrology (${st.stationName} — ${st.riverName}) Explained:")
                        appendLine("• Water Level (${String.format(Locale.US, "%.2f m", st.currentLevelM)} → ${st.floodStatus.label}): Measured at the Irrigation Dept gauge relative to Alert (${st.alertLevelM}m), Minor Flood (${st.minorFloodLevelM}m), and Major Flood (${st.majorFloodLevelM}m).")
                        appendLine("• Discharge Flow (${st.dischargeM3s.roundToInt()} m³/s): Cubic meters per second passing the gauge. 1 m³/s equals 1,000 liters of water flowing every second.")
                        appendLine("• Catchment Rain (${st.catchmentRainMm24h.roundToInt()} mm/24h): Upstream rainfall feeding ${st.riverName}; high catchment rain causes water levels to rise 3–6 hours later downstream in ${st.downstreamTowns.joinToString(", ")}.")
                    } else {
                        appendLine("🌊 River Hydrology & Discharge (m³/s) Explained:")
                        appendLine("• Gauge Level (m): Current river depth compared against Irrigation Department Alert, Minor, and Major flood thresholds.")
                        appendLine("• Flow Rate (m³/s): Volume of water (in cubic meters) flowing every second—higher values indicate rapid downstream surge.")
                    }
                }
            }

            q.contains("rain") || q.contains("precip") || q.contains("mm") || q.contains("landslide") -> buildString {
                appendLine("🌧 Precipitation (${String.format(Locale.US, "%.1f mm/h", report.precipitationMm)} • ${report.rain24hMm.roundToInt()} mm/24h) Explained:")
                appendLine("• 1 mm of Rain = 1 Liter of water per square meter of ground.")
                appendLine("• Hourly Rate (${String.format(Locale.US, "%.1f mm/h", report.precipitationMm)}): Current intensity in $nearestCity. Rates above 15 mm/h quickly flood urban drains.")
                appendLine("• 24h Accumulation (${report.rain24hMm.roundToInt()} mm): NBRO issues Yellow Landslide Watch at >75 mm/24h, Amber at >100 mm/24h, and Red Evacuation Warning at >150 mm/24h in slope-prone districts (${if (region.isLandslideProne) "${region.name} is landslide-prone" else "monitor low-lying drainage"}). ")
            }

            q.contains("pressure") || q.contains("hpa") || q.contains("wind") || q.contains("gust") -> buildString {
                appendLine("💨 Atmospheric Pressure (${report.pressureHpa.roundToInt()} hPa) & Wind Gusts (${report.windGustKmh.roundToInt()} km/h) Explained:")
                appendLine("• Surface Pressure (${report.pressureHpa.roundToInt()} hPa): Normal tropical sea-level pressure in Sri Lanka is ~1010–1012 hPa. Values below 1007 hPa indicate active monsoon troughs or Bay of Bengal depressions drawing in moisture.")
                appendLine("• Sustained Wind vs. Gusts: Sustained speed is ${report.windSpeedKmh.roundToInt()} km/h, while sudden squall gusts reach ${report.windGustKmh.roundToInt()} km/h. Gusts above 55 km/h can snap branches and roughen coastal seas.")
            }

            else -> buildString {
                appendLine("🇱🇰 Live Telemetry Clarification for $nearestCity (${region.name}):")
                appendLine("• Temperature & Feel: ${String.format(Locale.US, "%.1f°C", report.currentTempC)} (Feels like ${report.feelsLikeC.roundToInt()}°C at ${report.humidityPercent}% humidity).")
                appendLine("• Rainfall & Radar: ${String.format(Locale.US, "%.1f mm/h", report.precipitationMm)} (${report.rain24hMm.roundToInt()} mm in 24h) with $peakDbz dBZ (${dbzMeaning.shortTitle}).")
                if (primaryStation != null) {
                    appendLine("• Local River Basin (${primaryStation.riverName} at ${primaryStation.stationName}): ${String.format(Locale.US, "%.2f m", primaryStation.currentLevelM)} — ${primaryStation.floodStatus.label} (Flow: ${primaryStation.dischargeM3s.roundToInt()} m³/s).")
                }
                appendLine("• Safety Guidance: ${dbzMeaning.citizenAction} Keep DMC emergency hotline 117 accessible.")
            }
        }.trimEnd()
    }
}
