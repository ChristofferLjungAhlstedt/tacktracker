package com.blindsail.tacktracker.sensors

import com.blindsail.tacktracker.logic.Confidence
import com.blindsail.tacktracker.logic.angleDiff
import com.blindsail.tacktracker.logic.wrap360
import com.blindsail.tacktracker.logic.wrapSigned
import com.blindsail.tacktracker.settings.Settings
import kotlin.math.exp

/*
 * PURE KOTLIN (no Android imports) so it can be tested on a laptop with fake inputs.
 * All timestamps are milliseconds on ONE monotonic clock (SystemClock.elapsedRealtime on Android).
 */

/** One GPS fix. [cog] is course over ground in degrees, null when the receiver has no bearing. */
data class GpsFix(
    val timeMs: Long,
    val cog: Double?,
    val speed: Double,
    val bearingAccuracyDeg: Double? = null,
    val speedAccuracy: Double? = null,
)

/** One compass reading: azimuth in degrees [0, 360), already corrected for phone mounting. */
data class CompassSample(val timeMs: Long, val azimuth: Double)

enum class HeadingSource { COG, COMPASS, NONE }

data class HeadingState(
    val timeMs: Long,
    /** Fused, smoothed heading in the COG frame. Null when no usable input exists. */
    val heading: Double?,
    val confidence: Confidence,
    val source: HeadingSource,
    /** Speed from the latest GPS fix, m/s. */
    val speed: Double,
    /** COG of the latest GPS fix (may be rejected as unusable), null if none. */
    val cog: Double?,
    /** Latest compass azimuth if fresh, else null. */
    val compassAzimuth: Double?,
    /** Slowly filtered COG - compass offset, degrees in [-180, 180). Null until first valid fix. */
    val compassOffset: Double?,
    val gpsValid: Boolean,
)

/**
 * Combines GPS course and compass into one heading in one reference frame (spec section 5.2).
 *
 *  - Valid COG (speed >= gpsMinSpeed, bearing accuracy ok, fix fresh): COG is the anchor. Between 1 Hz
 *    fixes the compass turn since the last fix is added, so the output follows the boat smoothly
 *    but equals COG at every fix.
 *  - Otherwise: compass + compassOffset, where the offset is a slow circular low-pass of COG - compass.
 *  - The result is smoothed with a short circular low-pass.
 *
 * Not thread-safe: call from one thread.
 */
class HeadingFusion(private val config: Config = Config()) {

    data class Config(
        val gpsMinSpeed: Double = 1.0,
        val maxBearingAccuracyDeg: Double = 20.0,
        val offsetTimeConstantSec: Double = 20.0,
        val smoothingSec: Double = 0.75,
        /** A valid fix older than this is no longer trusted ("GPS lost"). */
        val gpsStaleSec: Double = 3.0,
        val compassStaleSec: Double = 2.0,
        /** Offset older than this gives LOW confidence in compass mode. */
        val offsetMaxAgeSec: Double = 60.0,
    ) {
        companion object {
            fun from(s: Settings) = Config(
                gpsMinSpeed = s.gpsMinSpeed,
                offsetTimeConstantSec = s.compassOffsetTimeConstantSeconds,
                smoothingSec = s.headingSmoothingSeconds,
            )
        }
    }

    private var compass: CompassSample? = null
    private var lastSpeed = 0.0
    private var lastCog: Double? = null

    private var anchorCog: Double? = null
    private var anchorTimeMs = 0L
    private var anchorCompass: Double? = null

    private var offset: Double? = null
    private var offsetTimeMs = 0L

    private var smoothed: Double? = null
    private var smoothTimeMs = 0L

    fun onCompass(sample: CompassSample): HeadingState {
        compass = sample
        return compute(sample.timeMs)
    }

    fun onGps(fix: GpsFix): HeadingState {
        lastSpeed = fix.speed
        lastCog = fix.cog
        val cog = fix.cog
        if (cog != null && isUsable(fix)) {
            anchorCog = cog
            anchorTimeMs = fix.timeMs
            val c = freshCompass(fix.timeMs)
            anchorCompass = c?.azimuth
            if (c != null) updateOffset(fix.timeMs, wrapSigned(cog - c.azimuth))
        } else {
            // Too slow or unreliable: switch to compass immediately instead of waiting for staleness.
            anchorCog = null
            anchorCompass = null
        }
        return compute(fix.timeMs)
    }

    /** Re-evaluate at [nowMs], e.g. from a timer, so that GPS loss is noticed without new data. */
    fun current(nowMs: Long): HeadingState = compute(nowMs)

    /** COG is only trustworthy when the boat is moving and the receiver reports a good bearing. */
    private fun isUsable(f: GpsFix): Boolean {
        if (f.speed < config.gpsMinSpeed) return false
        val acc = f.bearingAccuracyDeg
        return acc == null || acc <= config.maxBearingAccuracyDeg
    }

    private fun freshCompass(nowMs: Long): CompassSample? =
        compass?.takeIf { nowMs - it.timeMs <= config.compassStaleSec * 1000 }

    private fun updateOffset(timeMs: Long, sample: Double) {
        val current = offset
        offset = if (current == null) {
            sample
        } else {
            val dt = ((timeMs - offsetTimeMs) / 1000.0).coerceAtLeast(0.0)
            val a = smoothingFactor(dt, config.offsetTimeConstantSec)
            wrapSigned(current + a * angleDiff(sample, current))
        }
        offsetTimeMs = timeMs
    }

    private fun smoothingFactor(dtSec: Double, tauSec: Double): Double =
        if (tauSec <= 0.0) 1.0 else 1.0 - exp(-dtSec / tauSec)

    private fun compute(nowMs: Long): HeadingState {
        val c = freshCompass(nowMs)
        val anchor = anchorCog
        val cogValid = anchor != null && nowMs - anchorTimeMs <= config.gpsStaleSec * 1000

        val raw: Double?
        val source: HeadingSource
        val confidence: Confidence
        if (cogValid && anchor != null) {
            val ac = anchorCompass
            raw = if (c != null && ac != null) wrap360(anchor + angleDiff(c.azimuth, ac)) else anchor
            source = HeadingSource.COG
            confidence = Confidence.HIGH
        } else if (c != null) {
            val off = offset
            raw = if (off != null) wrap360(c.azimuth + off) else c.azimuth
            source = HeadingSource.COMPASS
            val recent = off != null && nowMs - offsetTimeMs <= config.offsetMaxAgeSec * 1000
            confidence = if (recent) Confidence.MEDIUM else Confidence.LOW
        } else {
            raw = null
            source = HeadingSource.NONE
            confidence = Confidence.LOW
        }

        val heading = smooth(raw, nowMs)
        return HeadingState(
            timeMs = nowMs,
            heading = heading,
            confidence = confidence,
            source = source,
            speed = lastSpeed,
            cog = lastCog,
            compassAzimuth = c?.azimuth,
            compassOffset = offset,
            gpsValid = cogValid,
        )
    }

    private fun smooth(raw: Double?, nowMs: Long): Double? {
        if (raw == null) {
            smoothed = null
            return null
        }
        val prev = smoothed
        val out = if (prev == null) {
            raw
        } else {
            val dt = ((nowMs - smoothTimeMs) / 1000.0).coerceAtLeast(0.0)
            wrap360(prev + smoothingFactor(dt, config.smoothingSec) * angleDiff(raw, prev))
        }
        smoothed = out
        smoothTimeMs = nowMs
        return out
    }
}
