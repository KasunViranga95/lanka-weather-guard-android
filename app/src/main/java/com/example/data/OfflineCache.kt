package com.example.data

import android.content.Context
import com.example.model.RegionalWeatherReport
import com.example.model.Reservoir
import com.example.model.RiverGaugeStation
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

/**
 * Keeps the last good forecast for each place and the last live river readings on the phone, so the
 * app, the widget and background checks still have something to show when the network is down.
 */
object OfflineCache {

    @Volatile
    private var dir: File? = null

    private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val reportAdapter: JsonAdapter<RegionalWeatherReport> by lazy { moshi.adapter(RegionalWeatherReport::class.java) }
    private val stationsAdapter: JsonAdapter<List<RiverGaugeStation>> by lazy {
        moshi.adapter(Types.newParameterizedType(List::class.java, RiverGaugeStation::class.java))
    }

    private val reservoirsAdapter: JsonAdapter<List<Reservoir>> by lazy {
        moshi.adapter(Types.newParameterizedType(List::class.java, Reservoir::class.java))
    }

    fun init(context: Context) {
        if (dir == null) dir = File(context.applicationContext.filesDir, "offline").apply { mkdirs() }
    }

    private fun reportFile(placeKey: String): File? =
        dir?.let { File(it, "report_" + placeKey.lowercase().replace(Regex("[^a-z0-9]+"), "_") + ".json") }

    fun saveReport(placeKey: String, report: RegionalWeatherReport) {
        if (!report.isLiveApiData) return
        runCatching { reportFile(placeKey)?.writeText(reportAdapter.toJson(report)) }
    }

    fun loadReport(placeKey: String): RegionalWeatherReport? =
        runCatching { reportFile(placeKey)?.takeIf { it.exists() }?.readText()?.let(reportAdapter::fromJson) }.getOrNull()

    fun saveStations(stations: List<RiverGaugeStation>) {
        if (stations.none { it.hasLiveReading }) return
        runCatching { dir?.let { File(it, "stations.json").writeText(stationsAdapter.toJson(stations)) } }
    }

    fun loadStations(): List<RiverGaugeStation>? =
        runCatching { dir?.let { File(it, "stations.json") }?.takeIf { it.exists() }?.readText()?.let(stationsAdapter::fromJson) }.getOrNull()

    fun saveReservoirs(reservoirs: List<Reservoir>) {
        if (reservoirs.isEmpty()) return
        runCatching { dir?.let { File(it, "reservoirs.json").writeText(reservoirsAdapter.toJson(reservoirs)) } }
    }

    fun loadReservoirs(): List<Reservoir>? =
        runCatching { dir?.let { File(it, "reservoirs.json") }?.takeIf { it.exists() }?.readText()?.let(reservoirsAdapter::fromJson) }.getOrNull()
}
