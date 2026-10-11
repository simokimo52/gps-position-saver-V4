package com.simodev.gpspositionsaver

import com.simodev.gpspositionsaver.data.model.Track
import com.simodev.gpspositionsaver.data.model.TrackPoint
import com.simodev.gpspositionsaver.geo.GeoUtils
import com.simodev.gpspositionsaver.geo.TrackComputer
import com.simodev.gpspositionsaver.io.ExportCodecs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackComputerTest {

    private fun point(
        lat: Double,
        lng: Double,
        ts: Long,
        alt: Double? = null,
        speed: Double? = null,
    ) = TrackPoint(trackId = 1, latitude = lat, longitude = lng, altitude = alt, speedMs = speed, timestamp = ts)

    @Test
    fun `empty points yield zero stats`() {
        val stats = TrackComputer.computeStats(emptyList())
        assertEquals(0.0, stats.distanceMeters, 0.0001)
        assertEquals(0L, stats.durationMs)
        assertNull(stats.maxSpeedMs)
        assertNull(stats.avgSpeedMs)
        assertEquals(0.0, stats.elevationGainMeters, 0.0001)
    }

    @Test
    fun `single point yields zero stats`() {
        val stats = TrackComputer.computeStats(listOf(point(48.0, 2.0, 1000L)))
        assertEquals(0.0, stats.distanceMeters, 0.0001)
        assertEquals(0L, stats.durationMs)
        assertEquals(0.0, stats.elevationGainMeters, 0.0001)
    }

    @Test
    fun `distance sums the two segments`() {
        // Straight east then straight north, each ~1 degree.
        val points = listOf(
            point(0.0, 0.0, 0L),
            point(0.0, 1.0, 5_000L),
            point(1.0, 1.0, 10_000L),
        )
        val stats = TrackComputer.computeStats(points)
        val expected = GeoUtils.calculateDistance(0.0, 0.0, 0.0, 1.0) +
            GeoUtils.calculateDistance(0.0, 1.0, 1.0, 1.0)
        assertEquals(expected, stats.distanceMeters, 1.0)
    }

    @Test
    fun `duration is first-to-last timestamp delta`() {
        val points = listOf(point(0.0, 0.0, 1_000L), point(0.0, 0.1, 9_000L))
        val stats = TrackComputer.computeStats(points)
        assertEquals(8_000L, stats.durationMs)
    }

    @Test
    fun `max speed picks the largest provided sample`() {
        val points = listOf(
            point(0.0, 0.0, 0L, speed = 1.0),
            point(0.0, 0.0001, 1_000L, speed = 5.0),
            point(0.0, 0.0002, 2_000L, speed = 3.0),
        )
        val stats = TrackComputer.computeStats(points)
        assertEquals(5.0, stats.maxSpeedMs!!, 0.0001)
    }

    @Test
    fun `average speed is distance divided by duration`() {
        // ~111 km east in 1111 seconds ≈ 100 m/s
        val points = listOf(point(0.0, 0.0, 0L), point(0.0, 1.0, 111_100L))
        val stats = TrackComputer.computeStats(points)
        val expectedKm = GeoUtils.calculateDistance(0.0, 0.0, 0.0, 1.0)
        assertEquals(expectedKm / 111.1, stats.avgSpeedMs!!, 1.0)
    }

    @Test
    fun `elevation gain accumulates only uphill steps`() {
        val points = listOf(
            point(0.0, 0.0, 0L, alt = 100.0),
            point(0.0, 0.001, 1_000L, alt = 110.0),
            point(0.0, 0.002, 2_000L, alt = 105.0),
            point(0.0, 0.003, 3_000L, alt = 130.0),
        )
        val stats = TrackComputer.computeStats(points)
        assertEquals(10.0 + 25.0, stats.elevationGainMeters, 0.0001)
    }

    @Test
    fun `trackFromPoints fills summary fields`() {
        val points = listOf(
            point(48.0, 2.0, 1_000L, alt = 100.0),
            point(48.001, 2.001, 5_000L, alt = 110.0),
        )
        val track = TrackComputer.trackFromPoints(id = 7, name = "Run", points = points)
        assertEquals(7L, track.id)
        assertEquals("Run", track.name)
        assertEquals(1_000L, track.startTime)
        assertEquals(5_000L, track.endTime)
        assertEquals(4_000L, track.durationMs)
        assertEquals(48.0, track.startLatitude, 0.0)
        assertEquals(2.0, track.startLongitude, 0.0)
        assertTrue(track.distanceMeters > 100.0)
        assertTrue(track.distanceMeters < 200.0)
        assertNotNull(track.avgSpeedMs)
    }

    @Test
    fun `gpx track export builds trk with points`() {
        val points = listOf(
            point(48.858370, 2.294481, 1_000L, alt = 35.0),
            point(48.858500, 2.294700, 5_000L, alt = 36.0),
        )
        val track = TrackComputer.trackFromPoints(id = 1, name = "Eiffel & Walk", points = points)
        val gpx = ExportCodecs.generateTrackGpx(track, points)

        assertTrue(gpx.contains("<trk>"))
        assertTrue(gpx.contains("<trkseg>"))
        assertTrue(gpx.contains("Eiffel &amp; Walk"))
        assertTrue(gpx.contains("lat=\"48.85837\""))
        assertTrue(gpx.contains("<ele>35.0</ele>"))
        assertTrue(gpx.contains("</trkseg>"))
        assertTrue(gpx.contains("</trk>"))
    }
}