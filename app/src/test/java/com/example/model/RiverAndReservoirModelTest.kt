package com.example.model

import com.example.data.SriLankaWeatherRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Flood status, trend and unit conversions on gauges and reservoirs. */
class RiverAndReservoirModelTest {

    // Hanwella: alert 6.5 m, minor flood 7.5 m, major flood 9.5 m
    private val hanwella = SriLankaWeatherRepository().getReferenceStations().single { it.id == "kelani_hanwella" }

    private fun at(levelM: Double, previousM: Double = levelM, live: Boolean = true) =
        hanwella.copy(currentLevelM = levelM, previousLevelM = previousM, hasLiveReading = live)

    @Test
    fun `flood status follows the official steps`() {
        assertEquals(FloodStatus.NORMAL, at(6.49).floodStatus)
        assertEquals(FloodStatus.ALERT, at(6.5).floodStatus)
        assertEquals(FloodStatus.MINOR_FLOOD, at(7.5).floodStatus)
        assertEquals(FloodStatus.MAJOR_FLOOD, at(9.5).floodStatus)
    }

    @Test
    fun `a gauge without a live reading never reports a flood`() {
        assertEquals(FloodStatus.NORMAL, at(12.0, live = false).floodStatus)
    }

    @Test
    fun `trend comes from the change since the previous reading`() {
        assertEquals(WaterTrend.RISING_FAST, at(7.0, previousM = 6.8).trend)
        assertEquals(WaterTrend.RISING, at(7.0, previousM = 6.95).trend)
        assertEquals(WaterTrend.STABLE, at(7.0, previousM = 6.99).trend)
        assertEquals(WaterTrend.FALLING, at(7.0, previousM = 7.1).trend)
    }

    @Test
    fun `a feet gauge also shows its level in feet`() {
        val feetGauge = at(4.0 * FEET_TO_METRES).copy(sourceUnit = "ft")
        assertEquals("4.0 ft", feetGauge.publishedLevelText)
        assertNull(at(4.0).publishedLevelText)
    }

    private fun reservoir(storage: Double?, cusecs: Double?) = Reservoir(
        id = "test", name = "Test Wewa", river = null, district = null, type = "major",
        status = ReservoirStatus.SPILLING, storagePercent = storage, flowCusecs = cusecs,
        flowKind = "spill", readingDay = "2026-11-12", agency = "Irrigation Department"
    )

    @Test
    fun `cusecs convert to cubic metres a second`() {
        // 1 cusec = 1 cubic foot a second = 0.0283168 m3/s
        assertEquals(76.455, reservoir(83.0, 2700.0).flowM3s!!, 0.01)
        assertNull(reservoir(83.0, null).flowM3s)
    }

    @Test
    fun `nearly full means 90 percent or more`() {
        assertTrue(reservoir(90.0, null).isNearlyFull)
        assertTrue(reservoir(100.0, null).isNearlyFull)
        assertFalse(reservoir(89.9, null).isNearlyFull)
        assertFalse(reservoir(null, null).isNearlyFull)
    }
}
