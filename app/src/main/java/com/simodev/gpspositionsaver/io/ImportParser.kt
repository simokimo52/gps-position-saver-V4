package com.simodev.gpspositionsaver.io

import com.simodev.gpspositionsaver.data.model.Group
import com.simodev.gpspositionsaver.data.model.Place
import org.json.JSONObject

data class ImportResult(
    val places: List<Place>,
    val groups: List<Group> = emptyList(),
)

/**
 * Detects and parses the four supported backup formats, ported from
 * `parseImportContent` in `exportImport.ts`.
 */
object ImportParser {

    fun parse(content: String): ImportResult {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return ImportResult(emptyList())

        if (trimmed.startsWith("{") && trimmed.contains("\"places\"")) return parseJson(trimmed)
        if (trimmed.contains("<gpx")) return parseGpx(trimmed)
        if (trimmed.contains("<kml")) return parseKml(trimmed)
        if (trimmed.startsWith("ID,") || trimmed.contains(",")) return parseCsv(trimmed)
        return ImportResult(emptyList())
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var inQuotes = false
        val current = StringBuilder()
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    current.append('"'); i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result += current.toString().trim(); current.clear()
            } else {
                current.append(c)
            }
            i++
        }
        result += current.toString().trim()
        return result
    }

    private fun parseJson(content: String): ImportResult = try {
        val data = JSONObject(content)
        val placesArr = data.optJSONArray("places")
        val groupsArr = data.optJSONArray("groups")
        val places = mutableListOf<Place>()
        if (placesArr != null) {
            for (i in 0 until placesArr.length()) {
                val o = placesArr.optJSONObject(i) ?: continue
                places += Place(
                    id = o.optLong("id", i + 1L),
                    name = o.optString("name", "Imported Place"),
                    latitude = o.optDouble("latitude", 0.0),
                    longitude = o.optDouble("longitude", 0.0),
                    accuracy = o.optNullableDouble("accuracy"),
                    altitude = o.optNullableDouble("altitude"),
                    address = o.optNullableString("address"),
                    note = o.optNullableString("note"),
                    phone = o.optNullableString("phone"),
                    photoUri = o.optNullableString("photoUri"),
                    groupId = o.optNullableLong("groupId"),
                    isFavorite = o.optBoolean("isFavorite", false),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                )
            }
        }
        val groups = mutableListOf<Group>()
        if (groupsArr != null) {
            for (i in 0 until groupsArr.length()) {
                val o = groupsArr.optJSONObject(i) ?: continue
                groups += Group(
                    id = o.optLong("id", i + 1L),
                    name = o.optString("name", "Group"),
                    colorHex = o.optString("colorHex", "#1E88E5"),
                    iconName = o.optString("iconName", "pin"),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                )
            }
        }
        ImportResult(places, groups)
    } catch (e: Exception) {
        ImportResult(emptyList())
    }

    private fun parseGpx(content: String): ImportResult {
        val places = mutableListOf<Place>()
        val wptRegex = Regex("""<wpt\s+lat="([^"]+)"\s+lon="([^"]+)"[^>]*>([\s\S]*?)</wpt>""")
        for (match in wptRegex.findAll(content)) {
            val lat = match.groupValues[1].toDoubleOrNull() ?: continue
            val lon = match.groupValues[2].toDoubleOrNull() ?: continue
            val inner = match.groupValues[3]
            val name = Regex("""<name>([\s\S]*?)</name>""").find(inner)?.groupValues?.get(1)
            val desc = Regex("""<desc>([\s\S]*?)</desc>""").find(inner)?.groupValues?.get(1)
            val ele = Regex("""<ele>([\s\S]*?)</ele>""").find(inner)?.groupValues?.get(1)
            places += Place(
                id = 0,
                name = name?.let { ExportCodecs.unescapeXml(it) } ?: "Imported Waypoint",
                latitude = lat,
                longitude = lon,
                altitude = ele?.toDoubleOrNull(),
                note = desc?.let { ExportCodecs.unescapeXml(it) },
            )
        }
        return ImportResult(places)
    }

    private fun parseKml(content: String): ImportResult {
        val places = mutableListOf<Place>()
        val pmRegex = Regex("""<Placemark>([\s\S]*?)</Placemark>""")
        val coordRegex = Regex("""<coordinates>\s*([^,\s]+),([^,\s]+)(?:,([^\s<]+))?\s*</coordinates>""")
        for (match in pmRegex.findAll(content)) {
            val inner = match.groupValues[1]
            val coords = coordRegex.find(inner) ?: continue
            val lon = coords.groupValues[1].toDoubleOrNull() ?: continue
            val lat = coords.groupValues[2].toDoubleOrNull() ?: continue
            val alt = coords.groupValues[3].takeIf { it.isNotBlank() }?.toDoubleOrNull()
            val name = Regex("""<name>([\s\S]*?)</name>""").find(inner)?.groupValues?.get(1)
            val desc = Regex("""<description>([\s\S]*?)</description>""").find(inner)?.groupValues?.get(1)
            places += Place(
                id = 0,
                name = name?.let { ExportCodecs.unescapeXml(it) } ?: "Imported KML Point",
                latitude = lat,
                longitude = lon,
                altitude = alt,
                note = desc?.let { ExportCodecs.unescapeXml(it) },
            )
        }
        return ImportResult(places)
    }

    private fun parseCsv(content: String): ImportResult {
        val places = mutableListOf<Place>()
        val lines = content.split("\n").filter { it.isNotBlank() }
        if (lines.isEmpty()) return ImportResult(emptyList())
        val first = lines[0]
        val startIdx = if (first.startsWith("ID,") || first.lowercase().contains("latitude")) 1 else 0
        for (i in startIdx until lines.size) {
            val tokens = parseCsvLine(lines[i])
            if (tokens.size < 4) continue
            val lat = tokens[2].toDoubleOrNull() ?: continue
            val lon = tokens[3].toDoubleOrNull() ?: continue
            places += Place(
                id = 0,
                name = tokens[1].ifBlank { "Imported Place" },
                latitude = lat,
                longitude = lon,
                accuracy = tokens.getOrNull(4)?.toDoubleOrNull(),
                altitude = tokens.getOrNull(5)?.toDoubleOrNull(),
                address = tokens.getOrNull(6)?.ifBlank { null },
                phone = tokens.getOrNull(7)?.ifBlank { null },
                note = tokens.getOrNull(8)?.ifBlank { null },
            )
        }
        return ImportResult(places)
    }

    private fun JSONObject.optNullableString(key: String): String? {
        if (isNull(key)) return null
        val v = optString(key, "")
        return v.ifBlank { null }
    }

    private fun JSONObject.optNullableDouble(key: String): Double? {
        if (isNull(key) || !has(key)) return null
        val d = optDouble(key, Double.NaN)
        return if (d.isNaN()) null else d
    }

    private fun JSONObject.optNullableLong(key: String): Long? {
        if (isNull(key) || !has(key)) return null
        return optLong(key)
    }
}
