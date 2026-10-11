package com.simodev.gpspositionsaver.io

import com.simodev.gpspositionsaver.data.model.Group
import com.simodev.gpspositionsaver.data.model.Place
import com.simodev.gpspositionsaver.data.model.Track
import com.simodev.gpspositionsaver.data.model.TrackPoint
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class ExportFormat(val extension: String, val mimeType: String, val label: String) {
    GPX("gpx", "application/gpx+xml", "GPX 1.1"),
    KML("kml", "application/vnd.google-earth.kml+xml", "KML 2.2"),
    CSV("csv", "text/csv", "CSV"),
    JSON("json", "application/json", "JSON Backup"),
}

/** Builders for the four export formats, ported from `exportImport.ts`. */
object ExportCodecs {

    private fun isoTime(epochMillis: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date(epochMillis))
    }

    private fun escapeXml(str: String): String = str
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    fun unescapeXml(str: String): String = str
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&amp;", "&")

    private fun escapeCsv(str: String): String {
        val mustQuote = str.contains(',') || str.contains('"') || str.contains('\n')
        return if (mustQuote) "\"${str.replace("\"", "\"\"")}\"" else str
    }

    fun generateGpx(places: List<Place>): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<gpx version=\"1.1\" creator=\"GPS Position Saver\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
        append("  <metadata>\n    <name>Exported Waypoints</name>\n")
        append("    <time>${isoTime(System.currentTimeMillis())}</time>\n  </metadata>\n")
        for (p in places) {
            append("  <wpt lat=\"${p.latitude}\" lon=\"${p.longitude}\">\n")
            p.altitude?.let { append("    <ele>$it</ele>\n") }
            append("    <time>${isoTime(p.createdAt)}</time>\n")
            append("    <name>${escapeXml(p.name)}</name>\n")
            val desc = mutableListOf<String>()
            p.address?.let { desc += "Address: $it" }
            p.note?.let { desc += "Note: $it" }
            p.phone?.let { desc += "Phone: $it" }
            p.accuracy?.let { desc += "Accuracy: ±${it}m" }
            if (desc.isNotEmpty()) append("    <desc>${escapeXml(desc.joinToString("; "))}</desc>\n")
            append("    <sym>Waypoint</sym>\n  </wpt>\n")
        }
        append("</gpx>")
    }

    fun generateKml(places: List<Place>): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n")
        append("  <Document>\n    <name>GPS Position Saver Places</name>\n")
        for (p in places) {
            append("    <Placemark>\n      <name>${escapeXml(p.name)}</name>\n")
            val desc = mutableListOf<String>()
            p.address?.let { desc += "Address: $it" }
            p.note?.let { desc += "Note: $it" }
            p.phone?.let { desc += "Phone: $it" }
            if (desc.isNotEmpty()) append("      <description>${escapeXml(desc.joinToString("\n"))}</description>\n")
            append("      <Point>\n")
            append("        <coordinates>${p.longitude},${p.latitude},${p.altitude ?: 0.0}</coordinates>\n")
            append("      </Point>\n    </Placemark>\n")
        }
        append("  </Document>\n</kml>")
    }

    fun generateCsv(places: List<Place>, groups: List<Group>): String = buildString {
        val groupMap = groups.associateBy({ it.id }, { it.name })
        append("ID,Name,Latitude,Longitude,Accuracy_Meters,Altitude_Meters,Address,Phone,Note,Category,CreatedAt\n")
        for (p in places) {
            val groupName = p.groupId?.let { groupMap[it] } ?: ""
            append(
                listOf(
                    p.id.toString(),
                    escapeCsv(p.name),
                    p.latitude.toString(),
                    p.longitude.toString(),
                    p.accuracy?.toString() ?: "",
                    p.altitude?.toString() ?: "",
                    escapeCsv(p.address ?: ""),
                    escapeCsv(p.phone ?: ""),
                    escapeCsv(p.note ?: ""),
                    escapeCsv(groupName),
                    p.createdAt.toString(),
                ).joinToString(",")
            )
            append("\n")
        }
    }

    /** GPX track export: a single `<trk>` with one `<trkseg>` of points. */
    fun generateTrackGpx(track: Track, points: List<TrackPoint>): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<gpx version=\"1.1\" creator=\"GPS Position Saver\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
        append("  <metadata>\n    <name>${escapeXml(track.name)}</name>\n")
        append("    <time>${isoTime(track.startTime)}</time>\n  </metadata>\n")
        append("  <trk>\n")
        append("    <name>${escapeXml(track.name)}</name>\n")
        track.endTime?.let { append("    <time>${isoTime(it)}</time>\n") }
        if (track.distanceMeters > 0) {
            append("    <desc>Distance: ${"%.1f".format(track.distanceMeters)} m</desc>\n")
        }
        append("    <trkseg>\n")
        for (p in points) {
            append("      <trkpt lat=\"${p.latitude}\" lon=\"${p.longitude}\">\n")
            p.altitude?.let { append("        <ele>$it</ele>\n") }
            append("        <time>${isoTime(p.timestamp)}</time>\n")
            append("      </trkpt>\n")
        }
        append("    </trkseg>\n")
        append("  </trk>\n")
        append("</gpx>")
    }

    fun generateJson(places: List<Place>, groups: List<Group>): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("groups", JSONArray().apply { groups.forEach { put(groupToJson(it)) } })
        root.put("places", JSONArray().apply { places.forEach { put(placeToJson(it)) } })
        return root.toString(2)
    }

    private fun groupToJson(g: Group) = JSONObject().apply {
        put("id", g.id)
        put("name", g.name)
        put("colorHex", g.colorHex)
        put("iconName", g.iconName)
        put("createdAt", g.createdAt)
    }

    private fun placeToJson(p: Place) = JSONObject().apply {
        put("id", p.id)
        put("name", p.name)
        put("latitude", p.latitude)
        put("longitude", p.longitude)
        put("accuracy", p.accuracy ?: JSONObject.NULL)
        put("altitude", p.altitude ?: JSONObject.NULL)
        put("address", p.address ?: JSONObject.NULL)
        put("note", p.note ?: JSONObject.NULL)
        put("phone", p.phone ?: JSONObject.NULL)
        put("photoUri", p.photoUri ?: JSONObject.NULL)
        put("groupId", p.groupId ?: JSONObject.NULL)
        put("isFavorite", p.isFavorite)
        put("createdAt", p.createdAt)
        put("updatedAt", p.updatedAt)
    }
}
