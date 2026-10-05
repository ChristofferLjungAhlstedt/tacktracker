package com.blindsail.tacktracker.sensors

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * GPS fixes from FusedLocationProviderClient: high accuracy, 1 s interval, at most 2 Hz (spec 5.1).
 *
 * The flow fails with SecurityException if location permission is missing; collectors should catch it.
 * Fix timestamps use Location.elapsedRealtimeNanos, the same clock as SystemClock.elapsedRealtime().
 */
class LocationSource(context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun fixes(): Flow<GpsFix> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) trySend(location.toFix())
            }
        }

        try {
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
                .addOnFailureListener { close(it) }
        } catch (e: SecurityException) {
            close(e)
        }
        awaitClose { client.removeLocationUpdates(callback) }
    }

    private fun Location.toFix() = GpsFix(
        timeMs = elapsedRealtimeNanos / 1_000_000L,
        cog = if (hasBearing()) bearing.toDouble() else null,
        speed = if (hasSpeed()) speed.toDouble() else 0.0,
        bearingAccuracyDeg = if (hasBearingAccuracy()) bearingAccuracyDegrees.toDouble() else null,
        speedAccuracy = if (hasSpeedAccuracy()) speedAccuracyMetersPerSecond.toDouble() else null,
    )
}
