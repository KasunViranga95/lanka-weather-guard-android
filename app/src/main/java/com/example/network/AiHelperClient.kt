package com.example.network

import com.example.BuildConfig
import com.example.model.RegionalWeatherReport
import com.example.model.RiverGaugeStation
import com.example.model.SriLankaRegion
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

private data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig = GeminiGenerationConfig()
)

private data class GeminiContent(
    @Json(name = "role") val role: String? = null,
    @Json(name = "parts") val parts: List<GeminiPart>
)

private data class GeminiPart(@Json(name = "text") val text: String? = null)

private data class GeminiGenerationConfig(
    @Json(name = "temperature") val temperature: Float = 0.35f,
    @Json(name = "topP") val topP: Float = 0.9f
)

private data class GeminiResponse(@Json(name = "candidates") val candidates: List<GeminiCandidate>?)

private data class GeminiCandidate(@Json(name = "content") val content: GeminiContent?)

/** Body sent to the app's own proxy (see server/ai-proxy), which adds the Gemini key on the server. */
private data class ProxyRequest(
    @Json(name = "question") val question: String,
    @Json(name = "context") val context: String
)

private data class ProxyResponse(@Json(name = "answer") val answer: String?)

/**
 * Answers weather questions with Gemini. Release builds only talk to the proxy in AI_PROXY_URL, so no
 * API key is ever inside the app. Debug builds may call Gemini directly with a key from .env for testing.
 * With neither configured the helper is hidden.
 */
object AiHelperClient {

    private const val GEMINI_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val proxyUrl: String = BuildConfig.AI_PROXY_URL.trim()
    private val debugKey: String = BuildConfig.GEMINI_API_KEY.trim()
        .takeIf { BuildConfig.DEBUG && it.isNotEmpty() && it != "MY_GEMINI_API_KEY" }
        .orEmpty()

    val isAvailable: Boolean get() = proxyUrl.isNotEmpty() || debugKey.isNotEmpty()

    private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val json = "application/json; charset=utf-8".toMediaType()

    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /** The answer, or null if the helper could not be reached. */
    suspend fun ask(
        question: String,
        region: SriLankaRegion,
        nearestCity: String,
        report: RegionalWeatherReport,
        station: RiverGaugeStation?,
        languageTag: String
    ): String? = withContext(Dispatchers.IO) {
        val context = buildContext(region, nearestCity, report, station, languageTag)
        try {
            when {
                proxyUrl.isNotEmpty() -> askProxy(question, context)
                debugKey.isNotEmpty() -> askGemini(question, context)
                else -> null
            }?.trim()?.takeIf { it.isNotEmpty() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private fun askProxy(question: String, context: String): String? {
        val body = moshi.adapter(ProxyRequest::class.java).toJson(ProxyRequest(question.take(500), context))
        val request = Request.Builder().url(proxyUrl).post(body.toRequestBody(json)).build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return response.body?.string()?.let { moshi.adapter(ProxyResponse::class.java).fromJson(it)?.answer }
        }
    }

    private fun askGemini(question: String, context: String): String? {
        val payload = GeminiRequest(
            contents = listOf(GeminiContent(role = "user", parts = listOf(GeminiPart(question.take(500))))),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(context)))
        )
        val request = Request.Builder()
            .url(GEMINI_URL)
            .header("x-goog-api-key", debugKey)
            .post(moshi.adapter(GeminiRequest::class.java).toJson(payload).toRequestBody(json))
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return response.body?.string()
                ?.let { moshi.adapter(GeminiResponse::class.java).fromJson(it) }
                ?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        }
    }

    private fun buildContext(
        region: SriLankaRegion,
        nearestCity: String,
        report: RegionalWeatherReport,
        station: RiverGaugeStation?,
        languageTag: String
    ): String = buildString {
        val language = when (languageTag) {
            "si" -> "Sinhala"
            "ta" -> "Tamil"
            else -> "English"
        }
        appendLine("You help people in Sri Lanka understand the weather and river levels where they live.")
        appendLine("Answer in $language, in plain words, in 3 to 5 short bullet points. Include practical safety advice when relevant.")
        appendLine("Only use the data below; if something is not in it, say you don't know. For emergencies tell people to call 117 (DMC).")
        appendLine("Place: $nearestCity, ${region.name} District, ${region.province} Province.")
        if (report.isLiveApiData) {
            appendLine(
                "Now: ${report.currentTempC.roundToInt()}°C (feels like ${report.feelsLikeC.roundToInt()}°C), humidity ${report.humidityPercent}%, " +
                    "wind ${report.windSpeedKmh.roundToInt()} km/h gusting ${report.windGustKmh.roundToInt()} km/h, rain today ${report.rain24hMm.roundToInt()} mm" +
                    (report.uvIndex?.let { ", UV index ${it.roundToInt()}" } ?: "") + "."
            )
            appendLine(
                "Next hours: " + report.hourly.take(12).joinToString("; ") {
                    "${it.timeLabel} ${it.tempC.roundToInt()}°C rain ${it.rainProbability}% ${String.format(Locale.US, "%.1f", it.rainMm)} mm"
                }
            )
            appendLine(
                "Next days: " + report.daily.joinToString("; ") {
                    "${it.dateLabel} ${it.minTempC.roundToInt()}–${it.maxTempC.roundToInt()}°C rain ${it.maxRainProb}% ${it.totalRainMm.roundToInt()} mm"
                }
            )
        }
        if (station != null) {
            appendLine(
                "Nearest river gauge: ${station.riverName} at ${station.stationName}, ${String.format(Locale.US, "%.2f", station.currentLevelM)} m " +
                    "(${station.floodStatus.label}, ${station.trend.label.lowercase(Locale.US)}). Alert level ${station.alertLevelM} m, " +
                    "minor flood ${station.minorFloodLevelM} m, major flood ${station.majorFloodLevelM} m."
            )
        }
    }
}
