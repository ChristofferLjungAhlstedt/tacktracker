package com.blindsail.tacktracker.settings

/** Plain data, no Android imports, so the logic layer and tests can use it directly. */

enum class OutputMode { EVENT_PLUS_PERIODIC, CONTINUOUS_TONE, TONES_ONLY, SPEECH_ONLY }
enum class MountOrientation { FLAT_TOP_TO_BOW, PORTRAIT_UPRIGHT_FACING_FORWARD, FLAT_TOP_TO_STERN }
enum class SpeedUnit { KNOTS, METERS_PER_SECOND, KMH }

/** Defaults and ranges follow section 16 of the specification; other defaults come from sections 5-13. */
data class Settings(
    // Output
    val outputMode: OutputMode = OutputMode.EVENT_PLUS_PERIODIC,
    val speechEnabled: Boolean = true,
    val tonesEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val speechRate: Float = 1.0f,
    /** 0 = off, otherwise 5..60. */
    val repeatSeconds: Int = 10,
    /** 0 = off, otherwise 15..120. */
    val heartbeatSeconds: Int = 30,
    val starboardToneHz: Int = 880,
    val portToneHz: Int = 440,
    val stereoPanning: Boolean = false,

    // Tack detection
    val confirmSeconds: Double = 1.5,
    val headToWindZone: Double = 15.0,
    val runningZone: Double = 10.0,
    val minSpeedForTack: Double = 0.3,

    // Heading fusion
    val gpsMinSpeed: Double = 1.0,
    val compassOffsetTimeConstantSeconds: Double = 20.0,
    val headingSmoothingSeconds: Double = 0.75,
    val mounting: MountOrientation = MountOrientation.FLAT_TOP_TO_BOW,

    // Calibration
    val firstTackSeconds: Int = 20,
    val secondTackSeconds: Int = 8,
    val settleSeconds: Int = 4,
    val steadySeconds: Int = 3,
    val steadyThreshold: Double = 3.0,
    val minTackChange: Double = 50.0,
    val tackTimeoutSeconds: Int = 30,
    val tackAngleMin: Double = 60.0,
    val tackAngleMax: Double = 120.0,
    val minSamples: Int = 5,

    // Wind estimation
    val windAlpha: Double = 0.3,
    val maxHeadingAge: Int = 120,
    val closeHauledMin: Double = 25.0,
    val closeHauledMax: Double = 65.0,
    val shiftAnnounceThreshold: Double = 5.0,
    val outlierShift: Double = 25.0,

    // Leg tracking
    val minUpwindSeconds: Int = 60,
    val downwindEnterAngle: Double = 110.0,
    val upwindEnterAngle: Double = 70.0,
    val roundingConfirmSeconds: Int = 5,
    val manualOverridePauseSeconds: Int = 30,

    // Other
    val startTimerMinutes: Int = 4,
    val speedUnit: SpeedUnit = SpeedUnit.KNOTS,
    val loggingEnabled: Boolean = true,
) {
    /** Returns a copy with every value clamped to its allowed range. */
    fun coerced(): Settings = copy(
        speechRate = speechRate.coerceIn(0.5f, 2.0f),
        repeatSeconds = if (repeatSeconds == 0) 0 else repeatSeconds.coerceIn(5, 60),
        heartbeatSeconds = if (heartbeatSeconds == 0) 0 else heartbeatSeconds.coerceIn(15, 120),
        starboardToneHz = starboardToneHz.coerceIn(200, 2000),
        portToneHz = portToneHz.coerceIn(200, 2000),
        confirmSeconds = confirmSeconds.coerceIn(0.5, 4.0),
        headToWindZone = headToWindZone.coerceIn(5.0, 30.0),
        runningZone = runningZone.coerceIn(5.0, 20.0),
        gpsMinSpeed = gpsMinSpeed.coerceIn(0.3, 2.0),
        windAlpha = windAlpha.coerceIn(0.1, 0.6),
        startTimerMinutes = if (startTimerMinutes in listOf(3, 4, 5)) startTimerMinutes else 4,
    )
}
