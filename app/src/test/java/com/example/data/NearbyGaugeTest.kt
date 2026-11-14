package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** "River near you": the gauge chosen for a town or a GPS position. */
class NearbyGaugeTest {

    private val repository = SriLankaWeatherRepository()
    private val stations = repository.getReferenceStations().map { it.copy(hasLiveReading = true) }

    private fun nearby(district: String, town: String, exact: Pair<Double, Double>? = null): NearbyGauge? {
        val region = repository.regions.single { it.name == district }
        return repository.findPrimaryStationForRegion(region, town, stations, exact)
    }

    @Test
    fun `a gauge named after the town wins`() {
        assertEquals("kalu_ratnapura", nearby("Ratnapura", "Ratnapura Town")?.station?.id)
        assertEquals("kelani_hanwella", nearby("Colombo", "Hanwella")?.station?.id)
    }

    @Test
    fun `a town listed downstream of a gauge gets that gauge`() {
        assertEquals("kelani_nagalagam", nearby("Colombo", "Wellampitiya")?.station?.id)
    }

    @Test
    fun `partial names no longer match far-away gauges`() {
        // "Ella" used to match "Wellampitiya" and show the Colombo gauge 129 km away
        val ella = nearby("Badulla", "Ella")
        assertEquals("kirindi_wellawaya", ella?.station?.id)
        assertTrue(ella!!.distanceKm < 25.0)
        // "Yakkalamulla" used to match "Yakkala" in Gampaha, 115 km away
        assertTrue(nearby("Galle", "Yakkalamulla")!!.distanceKm < 25.0)
    }

    @Test
    fun `a GPS position right at a gauge picks that gauge`() {
        val gauge = nearby("Ratnapura", "Kuruwita", exact = 6.7960 to 80.3690)
        assertEquals("kalu_kuruvita", gauge?.station?.id)
        assertTrue(gauge!!.distanceKm < 1.0)
    }

    @Test
    fun `places with no gauge within 25 km get none`() {
        assertNull(nearby("Jaffna", "Jaffna City"))
        assertNull(nearby("Trincomalee", "Trincomalee Town"))
    }

    @Test
    fun `every town gets either nothing or a gauge within 40 km`() {
        repository.regions.forEach { region ->
            region.nearestCities.forEach { town ->
                val gauge = repository.findPrimaryStationForRegion(region, town, stations)
                if (gauge != null) assertTrue("$town: ${gauge.station.id} is ${gauge.distanceKm} km away", gauge.distanceKm <= 40.0)
            }
        }
    }
}
