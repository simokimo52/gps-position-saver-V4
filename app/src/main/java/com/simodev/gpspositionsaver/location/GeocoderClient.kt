package com.simodev.gpspositionsaver.location

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Reverse geocoding via OpenStreetMap Nominatim, ported from `geocoder.ts`.
 * Returns null silently on network failure so the app keeps working offline.
 */
object GeocoderClient {

    suspend fun reverseGeocode(lat: Double, lng: Double): String? = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL(
                "https://nominatim.openstreetmap.org/reverse?format=jsonv2" +
                    "&lat=$lat&lon=$lng&zoom=18&addressdetails=1"
            )
            conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
                setRequestProperty("Accept-Language", "en")
                setRequestProperty("User-Agent", "GpsPositionSaver/2.0")
            }
            if (conn.responseCode != 200) return@withContext null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val address = json.optJSONObject("address")
            val parts = mutableListOf<String>()
            if (address != null) {
                listOf("road", "pedestrian", "street").firstNotNullOfOrNull { address.optNonBlank(it) }?.let { parts += it }
                listOf("suburb", "neighbourhood", "quarter").firstNotNullOfOrNull { address.optNonBlank(it) }?.let { parts += it }
                listOf("city", "town", "village", "county").firstNotNullOfOrNull { address.optNonBlank(it) }?.let { parts += it }
                address.optNonBlank("country")?.let { parts += it }
            }
            when {
                parts.isNotEmpty() -> parts.joinToString(", ")
                json.has("display_name") -> json.optString("display_name")
                else -> null
            }
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun JSONObject.optNonBlank(key: String): String? =
        optString(key, "").ifBlank { null }
}
