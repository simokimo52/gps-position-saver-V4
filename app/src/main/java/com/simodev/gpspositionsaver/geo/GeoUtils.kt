package com.simodev.gpspositionsaver.geo

import com.simodev.gpspositionsaver.data.model.CoordinateFormat
import com.simodev.gpspositionsaver.data.model.Place
import com.simodev.gpspositionsaver.data.model.UnitSystem
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure coordinate/geo helpers, ported 1:1 from the TypeScript `geoUtils.ts`.
 * All functions are side-effect free so they can be unit-tested directly.
 */
object GeoUtils {

    private const val EARTH_RADIUS_METERS = 6_371_000.0
    private const val METERS_TO_FEET = 3.28084
    private const val FEET_PER_MILE = 5280.0

    private fun String.fmt(vararg args: Any): String = String.format(Locale.US, this, *args)

    fun toDms(coordinate: Double, isLatitude: Boolean): String {
        val absolute = abs(coordinate)
        val degrees = floor(absolute).toInt()
        val minutesDouble = (absolute - degrees) * 60.0
        val minutes = floor(minutesDouble).toInt()
        val seconds = (minutesDouble - minutes) * 60.0
        val direction = if (isLatitude) {
            if (coordinate >= 0) "N" else "S"
        } else {
            if (coordinate >= 0) "E" else "W"
        }
        val secondsStr = "%.1f".fmt(seconds).padStart(4, '0')
        val minutesStr = minutes.toString().padStart(2, '0')
        return "$degrees° $minutesStr' $secondsStr\" $direction"
    }

    fun toDdm(coordinate: Double, isLatitude: Boolean): String {
        val absolute = abs(coordinate)
        val degrees = floor(absolute).toInt()
        val minutes = (absolute - degrees) * 60.0
        val direction = if (isLatitude) {
            if (coordinate >= 0) "N" else "S"
        } else {
            if (coordinate >= 0) "E" else "W"
        }
        val minutesStr = "%.3f".fmt(minutes).padStart(6, '0')
        return "$degrees° $minutesStr' $direction"
    }

    fun toUtm(lat: Double, lng: Double): String {
        val zone = minOf(60, maxOf(1, floor((lng + 180.0) / 6.0).toInt() + 1))
        val hemisphere = if (lat >= 0) "N" else "S"
        val centralMeridian = -183.0 + zone * 6.0
        val deltaLng = (lng - centralMeridian) * Math.PI / 180.0
        val latRad = lat * Math.PI / 180.0
        val falseEasting = 500_000.0
        val easting = (falseEasting + deltaLng * cos(latRad) * 6_378_137.0).roundToInt()
        val falseNorthing = if (lat < 0) 10_000_000.0 else 0.0
        val northing = (falseNorthing + latRad * 6_378_137.0).roundToInt()
        return "Zone $zone$hemisphere ${easting}m E ${northing}m N"
    }

    fun formatCoordinates(
        lat: Double,
        lng: Double,
        format: CoordinateFormat = CoordinateFormat.DECIMAL_DEGREES,
    ): String = when (format) {
        CoordinateFormat.DECIMAL_DEGREES -> {
            val latDir = if (lat >= 0) "N" else "S"
            val lngDir = if (lng >= 0) "E" else "W"
            "${"%.6f".fmt(abs(lat))}° $latDir, ${"%.6f".fmt(abs(lng))}° $lngDir"
        }
        CoordinateFormat.DEGREES_MINUTES_SECONDS ->
            "${toDms(lat, true)}, ${toDms(lng, false)}"
        CoordinateFormat.DEGREES_DECIMAL_MINUTES ->
            "${toDdm(lat, true)}, ${toDdm(lng, false)}"
        CoordinateFormat.UTM -> toUtm(lat, lng)
    }

    fun formatAccuracy(
        accuracyMeters: Double?,
        unitSystem: UnitSystem = UnitSystem.METRIC,
    ): String {
        if (accuracyMeters == null || accuracyMeters.isNaN()) return "± unknown"
        return if (unitSystem == UnitSystem.METRIC) {
            "±${"%.1f".fmt(accuracyMeters)} m"
        } else {
            "±${"%.1f".fmt(accuracyMeters * METERS_TO_FEET)} ft"
        }
    }

    fun formatDistance(
        meters: Double,
        unitSystem: UnitSystem = UnitSystem.METRIC,
    ): String = if (unitSystem == UnitSystem.METRIC) {
        if (meters < 1000) "${meters.roundToInt()} m"
        else "${"%.1f".fmt(meters / 1000.0)} km"
    } else {
        val feet = meters * METERS_TO_FEET
        if (feet < FEET_PER_MILE) "${feet.roundToInt()} ft"
        else "${"%.1f".fmt(feet / FEET_PER_MILE)} mi"
    }

    /** Haversine great-circle distance in meters. */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val phi1 = lat1 * Math.PI / 180.0
        val phi2 = lat2 * Math.PI / 180.0
        val dPhi = (lat2 - lat1) * Math.PI / 180.0
        val dLambda = (lon2 - lon1) * Math.PI / 180.0
        val a = sin(dPhi / 2) * sin(dPhi / 2) +
            cos(phi1) * cos(phi2) * sin(dLambda / 2) * sin(dLambda / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    /** Compass bearing in degrees (0..360) from point 1 to point 2. */
    fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val phi1 = lat1 * Math.PI / 180.0
        val phi2 = lat2 * Math.PI / 180.0
        val dLambda = (lon2 - lon1) * Math.PI / 180.0
        val y = sin(dLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLambda)
        return ((atan2(y, x) * 180.0 / Math.PI) + 360.0) % 360.0
    }

    fun bearingToCardinal(bearing: Double): String {
        val cardinals = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val index = (bearing / 45.0).roundToInt() % 8
        return cardinals[if (index < 0) index + 8 else index]
    }

    data class ProximityTier(val label: String, val argb: Long)

    fun getProximityTier(meters: Double): ProximityTier = when {
        meters < 50 -> ProximityTier("Right Here", 0xFFD1FAE5)
        meters < 500 -> ProximityTier("Very Close", 0xFFCCFBF1)
        meters < 2_000 -> ProximityTier("Walking Distance", 0xFFDBEAFE)
        meters < 10_000 -> ProximityTier("Nearby", 0xFFE0E7FF)
        else -> ProximityTier("Far", 0xFFE2E8F0)
    }

    fun getGoogleMapsUrl(lat: Double, lng: Double): String =
        "https://www.google.com/maps/search/?api=1&query=$lat,$lng"

    fun getOpenStreetMapUrl(lat: Double, lng: Double): String =
        "https://www.openstreetmap.org/?mlat=$lat&mlon=$lng#map=17/$lat/$lng"

    /** Geo URI understood by Android map apps. */
    fun getGeoUri(lat: Double, lng: Double, label: String? = null): String {
        val q = if (label.isNullOrBlank()) {
            "$lat,$lng"
        } else {
            val encoded = java.net.URLEncoder.encode(label, "UTF-8").replace("+", "%20")
            "$lat,$lng($encoded)"
        }
        return "geo:$lat,$lng?q=$q"
    }

    fun createShareText(place: Place, coordFormat: CoordinateFormat): String {
        val parts = mutableListOf<String>()
        parts += "📍 ${place.name}"
        if (!place.address.isNullOrBlank()) parts += "🏠 ${place.address}"
        parts += "🌐 Coordinates: ${formatCoordinates(place.latitude, place.longitude, coordFormat)}"
        place.accuracy?.let { parts += "🎯 Accuracy: ${formatAccuracy(it)}" }
        if (!place.phone.isNullOrBlank()) parts += "📞 Phone: ${place.phone}"
        if (!place.note.isNullOrBlank()) parts += "📝 Note: ${place.note}"
        parts += "🔗 Open in Maps: ${getGoogleMapsUrl(place.latitude, place.longitude)}"
        return parts.joinToString("\n")
    }
}
