package com.blindsail.tacktracker.logic

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * Angle helpers. PURE KOTLIN: no Android imports.
 * Sign convention used everywhere: rel > 0 = starboard tack, rel < 0 = port tack.
 */

private const val DEG2RAD = PI / 180.0
private const val RAD2DEG = 180.0 / PI

/** Normalise to [0, 360). */
fun wrap360(a: Double): Double {
    val r = ((a % 360.0) + 360.0) % 360.0
    // Guards against tiny negative inputs producing exactly 360.0 after the addition.
    return if (r >= 360.0) 0.0 else r
}

/** Normalise to [-180, 180). */
fun wrapSigned(a: Double): Double {
    val w = wrap360(a)
    return if (w >= 180.0) w - 360.0 else w
}

/** Signed shortest difference a - b, in [-180, 180). */
fun angleDiff(a: Double, b: Double): Double = wrapSigned(a - b)

/**
 * Circular (vector) mean in degrees, [0, 360). Optional weights must match [angles] in size.
 * If the vectors cancel exactly (e.g. 0 and 180 equally weighted) the result is arbitrary (0.0).
 */
fun circularMean(angles: List<Double>, weights: List<Double>? = null): Double {
    require(angles.isNotEmpty()) { "angles must not be empty" }
    require(weights == null || weights.size == angles.size) { "weights size must match angles size" }
    var x = 0.0
    var y = 0.0
    for (i in angles.indices) {
        val w = weights?.get(i) ?: 1.0
        x += w * cos(angles[i] * DEG2RAD)
        y += w * sin(angles[i] * DEG2RAD)
    }
    if (abs(x) < 1e-12 && abs(y) < 1e-12) return 0.0
    return wrap360(atan2(y, x) * RAD2DEG)
}

/**
 * Circular standard deviation in degrees: sqrt(-2 ln R) with R the mean resultant length.
 * Returns 0.0 for a single sample or identical angles and +Infinity when the angles cancel out.
 */
fun circularStdDev(angles: List<Double>): Double {
    if (angles.size < 2) return 0.0
    var x = 0.0
    var y = 0.0
    for (a in angles) {
        x += cos(a * DEG2RAD)
        y += sin(a * DEG2RAD)
    }
    val r = sqrt(x * x + y * y) / angles.size
    if (r >= 1.0 - 1e-12) return 0.0
    if (r <= 1e-12) return Double.POSITIVE_INFINITY
    return sqrt(-2.0 * ln(r)) * RAD2DEG
}

/**
 * Wind direction (windFrom) from the two close-hauled headings.
 * = wrap360(starboardHeading + wrapSigned(portHeading - starboardHeading) / 2)
 */
fun bisector(starboardHeading: Double, portHeading: Double): Double =
    wrap360(starboardHeading + wrapSigned(portHeading - starboardHeading) / 2.0)

/** rel = wrapSigned(windFrom - heading). rel > 0: starboard tack, rel < 0: port tack. */
fun relativeWind(windFrom: Double, heading: Double): Double = wrapSigned(windFrom - heading)
