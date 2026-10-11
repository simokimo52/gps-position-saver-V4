package com.simodev.gpspositionsaver

import com.simodev.gpspositionsaver.data.model.CoordinateFormat
import com.simodev.gpspositionsaver.data.model.Place
import com.simodev.gpspositionsaver.data.model.UnitSystem
import com.simodev.gpspositionsaver.geo.GeoUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoUtilsTest {

    @Test
    fun `distance between one degree of latitude is about 111 km`() {
        val meters = GeoUtils.calculateDistance(0.0, 0.0, 1.0, 0.0)
        assertEquals(111_194.9, meters, 50.0)
    }

    @Test
    fun `distance is symmetric`() {
        val a = GeoUtils.calculateDistance(48.858370, 2.294481, 51.5074, -0.1278)
        val b = GeoUtils.calculateDistance(51.5074, -0.1278, 48.858370, 2.294481)
        assertEquals(a, b, 0.001)
    }

    @Test
    fun `bearing due east is 90 degrees`() {
        assertEquals(90.0, GeoUtils.calculateBearing(0.0, 0.0, 0.0, 1.0), 0.5)
    }

    @Test
    fun `bearing due north is 0 degrees`() {
        assertEquals(0.0, GeoUtils.calculateBearing(0.0, 0.0, 1.0, 0.0), 0.5)
    }

    @Test
    fun `bearingToCardinal maps to 8 points`() {
        assertEquals("N", GeoUtils.bearingToCardinal(0.0))
        assertEquals("NE", GeoUtils.bearingToCardinal(45.0))
        assertEquals("E", GeoUtils.bearingToCardinal(90.0))
        assertEquals("S", GeoUtils.bearingToCardinal(180.0))
        assertEquals("W", GeoUtils.bearingToCardinal(270.0))
        assertEquals("N", GeoUtils.bearingToCardinal(359.0))
    }

    @Test
    fun `formats decimal degrees`() {
        val text = GeoUtils.formatCoordinates(48.858370, 2.294481, CoordinateFormat.DECIMAL_DEGREES)
        assertEquals("48.858370° N, 2.294481° E", text)
    }

    @Test
    fun `formats southern and western hemispheres`() {
        val text = GeoUtils.formatCoordinates(-33.9249, -18.4241, CoordinateFormat.DECIMAL_DEGREES)
        assertEquals("33.924900° S, 18.424100° W", text)
    }

    @Test
    fun `formats degrees minutes seconds`() {
        val text = GeoUtils.toDms(48.858370, true)
        assertEquals("48° 51' 30.1\" N", text)
    }

    @Test
    fun `formats degrees decimal minutes`() {
        val text = GeoUtils.toDdm(48.858370, true)
        assertEquals("48° 51.502' N", text)
    }

    @Test
    fun `utm contains zone and hemisphere`() {
        val text = GeoUtils.toUtm(48.858370, 2.294481)
        assertTrue("Expected zone 31N in '$text'", text.startsWith("Zone 31N"))
    }

    @Test
    fun `formats distance in metric`() {
        assertEquals("950 m", GeoUtils.formatDistance(950.0, UnitSystem.METRIC))
        assertEquals("1.5 km", GeoUtils.formatDistance(1500.0, UnitSystem.METRIC))
    }

    @Test
    fun `formats distance in imperial`() {
        assertEquals("3281 ft", GeoUtils.formatDistance(1000.0, UnitSystem.IMPERIAL))
        assertEquals("1.9 mi", GeoUtils.formatDistance(3000.0, UnitSystem.IMPERIAL))
    }

    @Test
    fun `formats accuracy with units and unknown fallback`() {
        assertEquals("±5.0 m", GeoUtils.formatAccuracy(5.0, UnitSystem.METRIC))
        assertEquals("±32.8 ft", GeoUtils.formatAccuracy(10.0, UnitSystem.IMPERIAL))
        assertEquals("± unknown", GeoUtils.formatAccuracy(null))
        assertEquals("± unknown", GeoUtils.formatAccuracy(Double.NaN))
    }

    @Test
    fun `proximity tiers follow thresholds`() {
        assertEquals("Right Here", GeoUtils.getProximityTier(10.0).label)
        assertEquals("Very Close", GeoUtils.getProximityTier(100.0).label)
        assertEquals("Walking Distance", GeoUtils.getProximityTier(1000.0).label)
        assertEquals("Nearby", GeoUtils.getProximityTier(5000.0).label)
        assertEquals("Far", GeoUtils.getProximityTier(50_000.0).label)
    }

    @Test
    fun `share text includes name coordinates and note`() {
        val place = Place(
            id = 1, name = "Secret Spot", latitude = 48.858370, longitude = 2.294481,
            note = "Behind the fountain", accuracy = 4.0,
        )
        val text = GeoUtils.createShareText(place, CoordinateFormat.DECIMAL_DEGREES)
        assertTrue(text.contains("Secret Spot"))
        assertTrue(text.contains("48.858370° N"))
        assertTrue(text.contains("Behind the fountain"))
        assertTrue(text.contains("google.com/maps"))
    }

    @Test
    fun `geo uri includes coordinates`() {
        val uri = GeoUtils.getGeoUri(48.858370, 2.294481, "Paris")
        assertTrue(uri.startsWith("geo:48.85837,2.294481"))
        assertTrue(uri.contains("Paris"))
    }
}
