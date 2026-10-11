package com.simodev.gpspositionsaver

import com.simodev.gpspositionsaver.data.model.Group
import com.simodev.gpspositionsaver.data.model.Place
import com.simodev.gpspositionsaver.io.ExportCodecs
import com.simodev.gpspositionsaver.io.ImportParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CodecsTest {

    private val groups = listOf(
        Group(1, "Home", "#4CAF50", "home", createdAt = 1000L),
        Group(2, "Travel", "#00ACC1", "compass", createdAt = 2000L),
    )

    private val places = listOf(
        Place(
            id = 1, name = "Eiffel Tower", latitude = 48.858370, longitude = 2.294481,
            accuracy = 4.2, altitude = 35.0, address = "Champ de Mars, Paris",
            note = "Sunset spot", phone = "+33 892", groupId = 2, isFavorite = true,
            createdAt = 1_600_000_000_000L, updatedAt = 1_600_000_000_000L,
        ),
        Place(
            id = 2, name = "Comma, Street", latitude = 40.0, longitude = -73.0,
            note = "Has \"quotes\" and, commas", createdAt = 1_600_000_001_000L,
            updatedAt = 1_600_000_001_000L,
        ),
    )

    @Test
    fun `gpx export escapes xml and round trips`() {
        val gpx = ExportCodecs.generateGpx(places)
        assertTrue(gpx.contains("<?xml"))
        assertTrue(gpx.contains("lat=\"48.85837\""))
        val parsed = ImportParser.parse(gpx)
        assertEquals(2, parsed.places.size)
        assertEquals("Eiffel Tower", parsed.places[0].name)
        assertEquals(48.858370, parsed.places[0].latitude!!, 0.000001)
        assertEquals(35.0, parsed.places[0].altitude!!, 0.0001)
    }

    @Test
    fun `gpx unescapes special characters in names`() {
        val xml = ExportCodecs.generateGpx(places.subList(1, 2))
        val parsed = ImportParser.parse(xml)
        assertEquals("Comma, Street", parsed.places[0].name)
    }

    @Test
    fun `kml export round trips lon lat order`() {
        val kml = ExportCodecs.generateKml(places)
        val parsed = ImportParser.parse(kml)
        assertEquals(2, parsed.places.size)
        // KML stores lon,lat - parser must swap back correctly.
        assertEquals(48.858370, parsed.places[0].latitude!!, 0.000001)
        assertEquals(2.294481, parsed.places[0].longitude!!, 0.000001)
    }

    @Test
    fun `csv export includes header and round trips quoted values`() {
        val csv = ExportCodecs.generateCsv(places, groups)
        assertTrue(csv.startsWith("ID,Name,"))
        val parsed = ImportParser.parse(csv)
        assertEquals(2, parsed.places.size)
        assertEquals("Comma, Street", parsed.places[1].name)
        assertEquals("Travel", groups.first { it.id == 2L }.name)
    }

    @Test
    fun `json export round trips places and groups`() {
        val json = ExportCodecs.generateJson(places, groups)
        val parsed = ImportParser.parse(json)
        assertEquals(2, parsed.places.size)
        assertEquals(2, parsed.groups.size)
        assertEquals("Eiffel Tower", parsed.places[0].name)
        assertTrue(parsed.places[0].isFavorite)
        assertEquals(2L, parsed.places[0].groupId)
        assertEquals("#4CAF50", parsed.groups[0].colorHex)
    }

    @Test
    fun `import of unknown content yields no places`() {
        assertTrue(ImportParser.parse("hello world").places.isEmpty())
        assertTrue(ImportParser.parse("").places.isEmpty())
    }

    @Test
    fun `xml escaping is reversible`() {
        val original = "A & B <tag> \"q\" 'a'"
        val escaped = ExportCodecs.generateGpx(
            listOf(Place(id = 1, name = original, latitude = 1.0, longitude = 2.0))
        )
        val parsed = ImportParser.parse(escaped)
        assertEquals(original, parsed.places[0].name)
    }
}
