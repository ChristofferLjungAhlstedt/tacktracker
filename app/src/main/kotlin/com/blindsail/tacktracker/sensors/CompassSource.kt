package com.blindsail.tacktracker.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import com.blindsail.tacktracker.logic.wrap360
import com.blindsail.tacktracker.settings.MountOrientation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow

/**
 * Compass from TYPE_ROTATION_VECTOR as a Flow of azimuth samples.
 *
 * [mounting] is read on every event, so it can change while the flow is running.
 * Timestamps use SystemClock.elapsedRealtime(), the same clock as GPS fixes in [LocationSource].
 */
class CompassSource(
    context: Context,
    private val mounting: () -> MountOrientation,
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    val isAvailable: Boolean
        get() = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null

    fun samples(): Flow<CompassSample> = callbackFlow {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (sensor == null) {
            close(IllegalStateException("No rotation vector sensor"))
            return@callbackFlow
        }
        val rotation = FloatArray(9)
        val remapped = FloatArray(9)
        val orientation = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                val m = mounting()
                when (m) {
                    // Phone upright, back of the phone pointing forward: azimuth of the -Z axis.
                    MountOrientation.PORTRAIT_UPRIGHT_FACING_FORWARD ->
                        SensorManager.remapCoordinateSystem(
                            rotation, SensorManager.AXIS_X, SensorManager.AXIS_Z, remapped,
                        )
                    // Flat, screen up: azimuth of the top edge (Y axis).
                    MountOrientation.FLAT_TOP_TO_BOW, MountOrientation.FLAT_TOP_TO_STERN ->
                        SensorManager.remapCoordinateSystem(
                            rotation, SensorManager.AXIS_X, SensorManager.AXIS_Y, remapped,
                        )
                }
                SensorManager.getOrientation(remapped, orientation)
                var azimuth = Math.toDegrees(orientation[0].toDouble())
                if (m == MountOrientation.FLAT_TOP_TO_STERN) azimuth += 180.0
                trySend(CompassSample(SystemClock.elapsedRealtime(), wrap360(azimuth)))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { sensorManager.unregisterListener(listener) }
    }.buffer(Channel.CONFLATED)
}
