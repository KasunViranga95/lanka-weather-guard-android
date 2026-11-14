package com.example.data

import com.example.model.FEET_TO_METRES
import com.example.model.GaugeFeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks the built-in list of river gauges: counts, alert levels, units and sources. */
class GaugeDataTest {

    private val stations = SriLankaWeatherRepository().getReferenceStations()

    @Test
    fun `there are 60 gauges with unique ids`() {
        assertEquals(60, stations.size)
        assertEquals(stations.size, stations.map { it.id }.toSet().size)
    }

    @Test
    fun `every gauge has alert below minor below major flood level`() {
        stations.forEach { st ->
            assertTrue("${st.id}: alert must be above 0", st.alertLevelM > 0.0)
            assertTrue("${st.id}: alert < minor", st.alertLevelM < st.minorFloodLevelM)
            assertTrue("${st.id}: minor < major", st.minorFloodLevelM < st.majorFloodLevelM)
        }
    }

    @Test
    fun `every gauge is inside Sri Lanka`() {
        stations.forEach { st ->
            assertTrue("${st.id} latitude", st.latitude in 5.8..9.9)
            assertTrue("${st.id} longitude", st.longitude in 79.5..82.0)
        }
    }

    @Test
    fun `only the eight RiverNet gauges without official levels use estimated levels`() {
        val estimated = stations.filter { it.marksEstimated }.map { it.id }.toSet()
        assertEquals(
            setOf(
                "kalu_kuruvita", "kalu_elapatha", "kalu_ayagama", "kalu_pelmadulla",
                "kalu_kalawana", "kalu_kahawatta", "uruwal_miriswatta", "wattala_kaluela"
            ),
            estimated
        )
        stations.filter { it.marksEstimated }.forEach { assertEquals(GaugeFeed.RIVERNET, it.feed) }
    }

    @Test
    fun `gauges come from the three feeds in the expected numbers`() {
        val byFeed = stations.groupingBy { it.feed }.eachCount()
        assertEquals(40, byFeed[GaugeFeed.IRRIGATION])
        assertEquals(10, byFeed[GaugeFeed.HMIS])
        assertEquals(10, byFeed[GaugeFeed.RIVERNET])
    }

    @Test
    fun `Nagalagam Street is published in feet and its official levels convert to metres`() {
        val nagalagam = stations.single { it.id == "kelani_nagalagam" }
        assertEquals("ft", nagalagam.sourceUnit)
        // Official levels are 4, 5 and 7 ft
        assertEquals(4.0 * FEET_TO_METRES, nagalagam.alertLevelM, 0.01)
        assertEquals(5.0 * FEET_TO_METRES, nagalagam.minorFloodLevelM, 0.01)
        assertEquals(7.0 * FEET_TO_METRES, nagalagam.majorFloodLevelM, 0.01)
    }

    @Test
    fun `every other gauge is published in metres`() {
        stations.filter { it.id != "kelani_nagalagam" }.forEach { assertEquals("${it.id} unit", "m", it.sourceUnit) }
    }

    @Test
    fun `official levels match the Irrigation Department for a few known gauges`() {
        fun levels(id: String) = stations.single { it.id == id }.let { Triple(it.alertLevelM, it.minorFloodLevelM, it.majorFloodLevelM) }
        assertEquals(Triple(6.5, 7.5, 9.5), levels("kelani_hanwella"))
        assertEquals(Triple(5.2, 7.5, 9.5), levels("kalu_ratnapura"))
        assertEquals(Triple(3.5, 4.0, 5.0), levels("gin_baddegama"))
        assertEquals(Triple(1.4, 1.7, 2.8), levels("nilwala_thalgahagoda"))
    }
}
