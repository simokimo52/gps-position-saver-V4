package com.simodev.gpspositionsaver.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.simodev.gpspositionsaver.data.model.LocationState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Wraps FusedLocationProviderClient and exposes GPS fixes as a Flow,
 * replacing the browser `watchPosition` / `getCurrentPosition` calls.
 */
class LocationRepository(private val context: Context) {

    private val client: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun locationUpdates(): Flow<LocationState> = callbackFlow {
        if (!hasPermission()) {
            close()
            return@callbackFlow
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
            .setWaitForAccurateLocation(true)
            .setMinUpdateIntervalMillis(2_000L)
            .setMaxUpdateDelayMillis(5_000L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it.toState()) }
            }
        }

        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(callback) }
    }

    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): LocationState? {
        if (!hasPermission()) return null
        return suspendCancellableCoroutine { cont ->
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc -> cont.resume(loc?.toState()) }
                .addOnFailureListener { cont.resume(null) }
        }
    }

    private fun Location.toState() = LocationState(
        latitude = latitude,
        longitude = longitude,
        accuracy = if (hasAccuracy()) accuracy.toDouble() else 4.0,
        altitude = if (hasAltitude()) altitude else null,
        timestamp = time,
        isSimulated = false,
        statusText = "Live GPS Active",
    )
}
