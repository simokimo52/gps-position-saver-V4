package com.simodev.gpspositionsaver.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A saved GPS waypoint. Mirrors the TypeScript `Place` interface. */
@Entity(tableName = "places")
data class Place(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double? = null,
    val altitude: Double? = null,
    val address: String? = null,
    val note: String? = null,
    val phone: String? = null,
    val photoUri: String? = null,
    val groupId: Long? = null,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/** A user-defined category. Mirrors the TypeScript `Group` interface. */
@Entity(tableName = "groups")
data class Group(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val name: String,
    val colorHex: String,
    val iconName: String = "pin",
    val createdAt: Long = System.currentTimeMillis(),
)

/** A time-limited live-share session. Mirrors the TypeScript `ShareSession`. */
@Entity(tableName = "sessions")
data class ShareSession(
    @PrimaryKey
    val sessionId: String,
    val startTime: Long,
    val durationMinutes: Int,
    val expiresAt: Long,
    val isActive: Boolean,
    val shareUrl: String,
    val lastLatitude: Double? = null,
    val lastLongitude: Double? = null,
    val lastAccuracy: Double? = null,
    val lastUpdated: Long? = null,
)

/** A recorded GPS track (path captured over time). */
@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val name: String,
    val startTime: Long,
    val endTime: Long? = null,
    val distanceMeters: Double = 0.0,
    val durationMs: Long = 0L,
    val maxSpeedMs: Double? = null,
    val avgSpeedMs: Double? = null,
    val startLatitude: Double,
    val startLongitude: Double,
)

/** A single GPS sample belonging to a Track. */
@Entity(tableName = "track_points", primaryKeys = ["trackId", "timestamp"])
data class TrackPoint(
    val trackId: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val accuracy: Double? = null,
    val speedMs: Double? = null,
    val timestamp: Long,
)

enum class CoordinateFormat(val displayName: String, val example: String) {
    DECIMAL_DEGREES("Decimal Degrees (DD)", "48.858370° N, 2.294481° E"),
    DEGREES_MINUTES_SECONDS("Degrees, Minutes, Seconds (DMS)", "48° 51' 30.1\" N"),
    DEGREES_DECIMAL_MINUTES("Degrees, Decimal Minutes (DDM)", "48° 51.503' N"),
    UTM("Universal Transverse Mercator (UTM)", "Zone 31U 448m E 5411m N"),
}

enum class UnitSystem(val displayName: String, val caption: String) {
    METRIC("Metric", "Meters, kilometers"),
    IMPERIAL("Imperial", "Feet, miles"),
}

data class AppSettings(
    val coordinateFormat: CoordinateFormat = CoordinateFormat.DECIMAL_DEGREES,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val defaultGroupId: Long? = null,
)

/** Bottom-navigation destinations. */
enum class ScreenTab { HOME, PLACES, MAP, LIVE, SETTINGS, TRACK }

/** Base map style in the map view. */
enum class MapLayerType { STANDARD, SATELLITE }

/** Live GPS fix exposed to the UI. */
data class LocationState(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double,
    val altitude: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isSimulated: Boolean = false,
    val statusText: String = "Waiting for GPS...",
)
