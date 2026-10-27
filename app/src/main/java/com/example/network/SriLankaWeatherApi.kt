package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

data class OpenMeteoResponse(
    @Json(name = "current") val current: OpenMeteoCurrent?,
    @Json(name = "hourly") val hourly: OpenMeteoHourly?,
    @Json(name = "daily") val daily: OpenMeteoDaily?
)

data class OpenMeteoCurrent(
    @Json(name = "time") val time: String?,
    @Json(name = "temperature_2m") val temperature2m: Double?,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: Int?,
    @Json(name = "apparent_temperature") val apparentTemperature: Double?,
    @Json(name = "precipitation") val precipitation: Double?,
    @Json(name = "rain") val rain: Double?,
    @Json(name = "weather_code") val weatherCode: Int?,
    @Json(name = "surface_pressure") val surfacePressure: Double?,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double?,
    @Json(name = "wind_gusts_10m") val windGusts10m: Double?
)

data class OpenMeteoHourly(
    @Json(name = "time") val time: List<String>?,
    @Json(name = "temperature_2m") val temperature2m: List<Double>?,
    @Json(name = "precipitation_probability") val precipitationProbability: List<Int>?,
    @Json(name = "precipitation") val precipitation: List<Double>?,
    @Json(name = "wind_gusts_10m") val windGusts10m: List<Double>?,
    @Json(name = "weather_code") val weatherCode: List<Int>?
)

data class OpenMeteoDaily(
    @Json(name = "time") val time: List<String>?,
    @Json(name = "weather_code") val weatherCode: List<Int>?,
    @Json(name = "temperature_2m_max") val temperature2mMax: List<Double>?,
    @Json(name = "temperature_2m_min") val temperature2mMin: List<Double>?,
    @Json(name = "precipitation_sum") val precipitationSum: List<Double>?,
    @Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Int>?,
    @Json(name = "wind_gusts_10m_max") val windGusts10mMax: List<Double>?
)

data class OpenMeteoFloodResponse(
    @Json(name = "daily") val daily: OpenMeteoFloodDaily?
)

data class OpenMeteoFloodDaily(
    @Json(name = "time") val time: List<String>?,
    @Json(name = "river_discharge") val riverDischarge: List<Double>?
)

// Sri Lanka Irrigation Department hydrology gauge feed (ArcGIS FeatureServer, ~hourly readings)
data class IrrigationGaugeQueryResponse(
    @Json(name = "features") val features: List<IrrigationGaugeFeature>?,
    @Json(name = "exceededTransferLimit") val exceededTransferLimit: Boolean?
)

data class IrrigationGaugeFeature(
    @Json(name = "attributes") val attributes: IrrigationGaugeReading?
)

data class IrrigationGaugeReading(
    @Json(name = "basin") val basin: String?,
    @Json(name = "gauge") val gauge: String?,
    @Json(name = "water_level") val waterLevelM: Double?,
    @Json(name = "rain_fall") val rainFallMm: Double?,
    @Json(name = "CreationDate") val creationDateMillis: Long?,
    @Json(name = "alertpull") val alertLevelM: Double?,
    @Json(name = "minorpull") val minorFloodLevelM: Double?,
    @Json(name = "majorpull") val majorFloodLevelM: Double?
)

interface IrrigationGaugeService {
    @GET("query")
    suspend fun queryReadings(
        @Query("where") where: String = "CreationDate >= CURRENT_TIMESTAMP - INTERVAL '24' HOUR",
        @Query("outFields") outFields: String = "basin,gauge,water_level,rain_fall,CreationDate,alertpull,minorpull,majorpull",
        @Query("orderByFields") orderBy: String = "CreationDate DESC",
        @Query("returnGeometry") returnGeometry: Boolean = false,
        @Query("resultOffset") resultOffset: Int = 0,
        @Query("resultRecordCount") resultRecordCount: Int = 1000,
        @Query("f") format: String = "json"
    ): IrrigationGaugeQueryResponse

    // Official station list; its "Unit" column says whether a gauge reports in metres or feet
    @GET
    suspend fun queryStationUnits(
        @Url url: String = "https://services3.arcgis.com/J7ZFXmR8rSmQ3FGf/arcgis/rest/services/hydrostations/FeatureServer/0/query",
        @Query("where") where: String = "1=1",
        @Query("outFields") outFields: String = "station,Unit",
        @Query("returnGeometry") returnGeometry: Boolean = false,
        @Query("f") format: String = "json"
    ): HydroStationQueryResponse
}

data class HydroStationQueryResponse(
    @Json(name = "features") val features: List<HydroStationFeature>?
)

data class HydroStationFeature(
    @Json(name = "attributes") val attributes: HydroStationAttributes?
)

data class HydroStationAttributes(
    @Json(name = "station") val station: String?,
    @Json(name = "Unit") val unit: String?
)

interface OpenMeteoService {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,rain,weather_code,surface_pressure,wind_speed_10m,wind_gusts_10m",
        @Query("hourly") hourly: String = "temperature_2m,precipitation_probability,precipitation,wind_gusts_10m,weather_code",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max,wind_gusts_10m_max",
        @Query("timezone") timezone: String = "Asia/Colombo",
        @Query("forecast_days") forecastDays: Int = 7,
        // Hourly series starts at the current hour rather than midnight
        @Query("forecast_hours") forecastHours: Int = 24
    ): OpenMeteoResponse

    // Same location from several global models; keys come back suffixed, e.g. "temperature_2m_gfs_seamless"
    @GET("v1/forecast")
    suspend fun getMultiModelTemperatures(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String = "temperature_2m,apparent_temperature,relative_humidity_2m",
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min",
        @Query("models") models: String = TEMPERATURE_ENSEMBLE_MODELS.joinToString(","),
        @Query("timezone") timezone: String = "Asia/Colombo",
        @Query("forecast_days") forecastDays: Int = 7,
        @Query("forecast_hours") forecastHours: Int = 24
    ): OpenMeteoMultiModelResponse

    // Comma-separated coordinates → one response object per location (JSON array)
    @GET("v1/forecast")
    suspend fun getCurrentForLocations(
        @Query("latitude") latitudes: String,
        @Query("longitude") longitudes: String,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,precipitation,wind_gusts_10m,weather_code",
        @Query("daily") daily: String = "precipitation_sum",
        @Query("forecast_days") forecastDays: Int = 1,
        @Query("timezone") timezone: String = "Asia/Colombo"
    ): List<OpenMeteoResponse>
}

/**
 * Models blended (median) for temperature. Scored against Sri Lankan airport METAR observations
 * (Katunayake, Ratmalana, Mattala, Jaffna): this median had the lowest bias and tied for the lowest
 * error, and stays robust when any single model has a bad run.
 */
val TEMPERATURE_ENSEMBLE_MODELS = listOf("ecmwf_ifs", "gfs_seamless", "ukmo_seamless", "icon_seamless")

data class OpenMeteoMultiModelResponse(
    @Json(name = "hourly") val hourly: Map<String, Any?>?,
    @Json(name = "daily") val daily: Map<String, Any?>?
)

interface OpenMeteoFloodService {
    @GET("v1/flood")
    suspend fun getRiverDischarge(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("daily") daily: String = "river_discharge",
        @Query("forecast_days") forecastDays: Int = 3
    ): OpenMeteoFloodResponse

    // Comma-separated coordinates return one response object per location (JSON array)
    @GET("v1/flood")
    suspend fun getRiverDischargeForLocations(
        @Query("latitude") latitudes: String,
        @Query("longitude") longitudes: String,
        @Query("daily") daily: String = "river_discharge",
        @Query("forecast_days") forecastDays: Int = 1
    ): List<OpenMeteoFloodResponse>
}

object WeatherNetworkClient {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    val openMeteoService: OpenMeteoService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoService::class.java)
    }

    val floodService: OpenMeteoFloodService by lazy {
        Retrofit.Builder()
            .baseUrl("https://flood-api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoFloodService::class.java)
    }

    val irrigationGaugeService: IrrigationGaugeService by lazy {
        Retrofit.Builder()
            .baseUrl("https://services3.arcgis.com/J7ZFXmR8rSmQ3FGf/arcgis/rest/services/gauges_2_view/FeatureServer/0/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(IrrigationGaugeService::class.java)
    }
}
