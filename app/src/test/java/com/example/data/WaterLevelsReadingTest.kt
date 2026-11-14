package com.example.data

import com.example.model.FEET_TO_METRES
import com.example.network.WaterLevelsPublished
import com.example.network.WaterLevelsRain
import com.example.network.WaterLevelsStation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Readings from waterlevels.lk (HMIS and RiverNet gauges): units, rate of change and stale readings. */
class WaterLevelsReadingTest {

    private val repository = SriLankaWeatherRepository()
    private val karawanella = repository.getReferenceStations().single { it.id == "kelani_karawanella" }

    private fun isoMinutesAgo(minutes: Int): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(System.currentTimeMillis() - minutes * 60_000L))

    private fun reading(
        levelFt: Double? = 9.14,
        published: WaterLevelsPublished? = WaterLevelsPublished(2.786, "m"),
        changeFtPerHour: Double? = -0.3,
        observedAt: String? = isoMinutesAgo(10),
        status: String = "watch",
        rain24h: Double? = null
    ) = WaterLevelsStation(
        station = "Karawanella Bridge", river = "Kelani Ganga", levelFt = levelFt, published = published,
        changePerHourFt = changeFtPerHour, observedAt = observedAt, status = status, rain = WaterLevelsRain(rain24h)
    )

    private fun convert(r: WaterLevelsStation) = with(repository) { karawanella.withWaterLevelsReading(r, discharge = 120.0) }

    @Test
    fun `a gauge published in metres keeps its exact figure`() {
        val st = convert(reading())!!
        assertEquals(2.786, st.currentLevelM, 0.0001)
        assertEquals("m", st.sourceUnit)
        assertTrue(st.hasLiveReading)
    }

    @Test
    fun `the feed's feet figure is converted to metres when that is all there is`() {
        val st = convert(reading(levelFt = 10.0, published = WaterLevelsPublished(10.0, "ft")))!!
        assertEquals(10.0 * FEET_TO_METRES, st.currentLevelM, 0.0001)
        assertEquals("ft", st.sourceUnit)
    }

    @Test
    fun `the hourly rate in feet becomes the change in metres`() {
        val st = convert(reading(changeFtPerHour = -1.0))!!
        assertEquals(-FEET_TO_METRES, st.deltaM, 0.0001)
    }

    @Test
    fun `stale or old readings are not shown as live`() {
        assertNull(convert(reading(status = "stale")))
        assertNull(convert(reading(observedAt = isoMinutesAgo(4 * 60))))
        assertNull(convert(reading(levelFt = null, published = null)))
    }

    @Test
    fun `rain is only counted when the gauge has a rain sensor`() {
        assertFalse(convert(reading(rain24h = null))!!.rainMeasured)
        val withRain = convert(reading(rain24h = 16.0))!!
        assertTrue(withRain.rainMeasured)
        assertEquals(16.0, withRain.catchmentRainMm24h, 0.0)
    }

    @Test
    fun `the gauge keeps its own HMIS alert levels`() {
        val st = convert(reading())!!
        assertEquals(3.0, st.alertLevelM, 0.0)
        assertEquals(4.0, st.minorFloodLevelM, 0.0)
        assertEquals(6.0, st.majorFloodLevelM, 0.0)
    }
}
