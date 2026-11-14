package com.example.data

import android.app.Application
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.example.model.AlertSeverity
import com.example.model.RiverGaugeStation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * River alerts worked out by the app, in English. Runs with a plain Application so the test doesn't
 * start the background alert scheduler.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class AlertEngineTest {

    private val res: Resources = ApplicationProvider.getApplicationContext<Application>().resources
    private val repository = SriLankaWeatherRepository()
    private val reference = repository.getReferenceStations()
    private val galle = repository.regions.single { it.name == "Galle" }
    // Not live weather, so only river alerts are produced
    private val report = repository.getPlaceholderRegionalReport(galle, "Baddegama")

    private fun gauge(id: String, levelM: Double): RiverGaugeStation =
        reference.single { it.id == id }.copy(currentLevelM = levelM, previousLevelM = levelM, hasLiveReading = true)

    private fun alerts(vararg stations: RiverGaugeStation) =
        AlertEngine.build(res, galle, "Baddegama", report, stations.toList(), nearStation = null)

    @Test
    fun `a river in minor flood raises an amber alert listing the areas at risk`() {
        // Baddegama: alert 3.5 m, minor flood 4.0 m
        val alert = alerts(gauge("gin_baddegama", 4.12)).single()
        assertEquals(AlertSeverity.MAJOR_AMBER, alert.severity)
        assertTrue(alert.title, alert.title.contains("Gin Ganga"))
        assertTrue(alert.summary, alert.summary.contains("Areas at risk: Baddegama, Bope-Poddala, Wakwella, Gintota"))
        assertTrue("Galle district gauge is local to Galle", alert.isUserDistrictMatch)
    }

    @Test
    fun `a river in major flood raises a red alert`() {
        assertEquals(AlertSeverity.CRITICAL_RED, alerts(gauge("gin_baddegama", 5.2)).single().severity)
    }

    @Test
    fun `rivers at normal levels raise no alert`() {
        assertTrue(alerts(gauge("gin_baddegama", 2.0), gauge("kelani_hanwella", 4.0)).isEmpty())
    }

    @Test
    fun `alerts from estimated levels say so`() {
        // Kuruvita (estimated): alert 4.1 m, minor 5.1 m
        val alert = alerts(gauge("kalu_kuruvita", 5.3)).single()
        assertTrue(alert.summary, alert.summary.contains("(estimated levels)"))
    }

    @Test
    fun `gauges on the same river are grouped into one alert`() {
        val alert = alerts(gauge("kalu_ratnapura", 8.0), gauge("kalu_ellagawa", 10.8)).single()
        assertTrue(alert.summary.contains("Ratnapura"))
        assertTrue(alert.summary.contains("Ellagawa"))
        assertFalse(alert.summary.contains("Gin Ganga"))
    }
}
