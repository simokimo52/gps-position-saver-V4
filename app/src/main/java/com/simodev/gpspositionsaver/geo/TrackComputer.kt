package com.simodev.gpspositionsaver.geo

import com.simodev.gpspositionsaver.data.model.Track
import com.simodev.gpspositionsaver.data.model.TrackPoint

/** Pure functions that derive track statistics from recorded GPS samples. */
object TrackComputer {

    data class Stats(
        val distanceMeters: Double,
        val durationMs: Long,
        val maxSpeedMs: Double?,
        val avgSpeedMs: Double?,
        val elevationGainMeters: Double,
    )

    fun computeStats(points: List<TrackPoint>): Stats {
        var distance = 0.0
        var maxSpeed: Double? = null
        var elevationGain = 0.0
        var prev: TrackPoint? = null

        for (point in points) {
            prev?.let { p ->
                val d = GeoUtils.calculateDistance(
                    p.latitude, p.longitude, point.latitude, point.longitude,
                )
                distance += d
                point.speedMs?.let { maxSpeed = maxOf(maxSpeed ?: 0.0, it) }
                val prevAlt = p.altitude
                val alt = point.altitude
                if (prevAlt != null && alt != null && alt > prevAlt) {
                    elevationGain += alt - prevAlt
                }
            }
            prev = point
        }

        val duration = if (points.size >= 2) {
            points.last().timestamp - points.first().timestamp
        } else {
            0L
        }
        val avg = if (duration > 0) (distance / (duration / 1000.0)) else null

        return Stats(
            distanceMeters = distance,
            durationMs = duration,
            maxSpeedMs = maxSpeed,
            avgSpeedMs = avg,
            elevationGainMeters = elevationGain,
        )
    }

    fun trackFromPoints(
        id: Long,
        name: String,
        points: List<TrackPoint>,
    ): Track {
        val stats = computeStats(points)
        val first = points.firstOrNull()
        return Track(
            id = id,
            name = name,
            startTime = first?.timestamp ?: System.currentTimeMillis(),
            endTime = points.lastOrNull()?.timestamp,
            distanceMeters = stats.distanceMeters,
            durationMs = stats.durationMs,
            maxSpeedMs = stats.maxSpeedMs,
            avgSpeedMs = stats.avgSpeedMs,
            startLatitude = first?.latitude ?: 0.0,
            startLongitude = first?.longitude ?: 0.0,
        )
    }

    /** Distance in meters between consecutive points that moved at least this far. */
    fun sampleDistance(points: List<TrackPoint>): Double {
        var total = 0.0
        var prev: TrackPoint? = null
        for (p in points) {
            prev?.let { total += GeoUtils.calculateDistance(it.latitude, it.longitude, p.latitude, p.longitude) }
            prev = p
        }
        return total
    }
}