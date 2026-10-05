package com.blindsail.tacktracker.sensors

import com.blindsail.tacktracker.logic.Confidence
import com.blindsail.tacktracker.logic.angleDiff
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.math.exp

class HeadingFusionTest {
    /** No output smoothing so expected values are exact. */
    private val cfg = HeadingFusion.Config(smoothingSec = 0.0)
    private fun fix(t: Long, cog: Double?, speed: Double = 3.0, acc: Double? = null) = GpsFix(t, cog, speed, acc)
    private fun compass(t: Long, az: Double) = CompassSample(t, az)

    @Test fun noInputGivesNoHeading() {
        val s = HeadingFusion(cfg).current(0)
        assertNull(s.heading)
        assertEquals(HeadingSource.NONE, s.source)
    }

    @Test fun cogIsUsedWhenMoving() {
        val f = HeadingFusion(cfg)
        f.onCompass(compass(0, 90.0))
        val s = f.onGps(fix(0, 100.0))
        assertEquals(100.0, s.heading!!, 1e-9)
        assertEquals(HeadingSource.COG, s.source)
        assertEquals(Confidence.HIGH, s.confidence)
    }

    @Test fun compassPlusOffsetWhenSlow_noJumpOnSwitch() {
        val f = HeadingFusion(cfg)
        f.onCompass(compass(0, 90.0))
        f.onGps(fix(0, 100.0))                       // offset = +10
        val slow = f.onGps(fix(1000, 100.0, speed = 0.3))
        assertEquals(100.0, slow.heading!!, 1e-9)    // same heading right after the switch
        assertEquals(HeadingSource.COMPASS, slow.source)
        val s = f.onCompass(compass(1100, 120.0))
        assertEquals(130.0, s.heading!!, 1e-9)
        assertEquals(Confidence.MEDIUM, s.confidence)
    }

    @Test fun offsetLowPassFollowsTimeConstant() {
        val f = HeadingFusion(cfg)
        f.onCompass(compass(0, 0.0))
        f.onGps(fix(0, 10.0))                        // offset initialised to 10
        var s = f.current(0)
        for (i in 1..20) {
            val t = i * 1000L
            f.onCompass(compass(t, 0.0))
            s = f.onGps(fix(t, 20.0))                // measured offset now 20
        }
        val expected = 20.0 - 10.0 * exp(-1.0)       // 20 s = one time constant
        assertEquals(expected, s.compassOffset!!, 1e-6)
    }

    @Test fun offsetAndHeadingWrapAroundNorth() {
        val f = HeadingFusion(cfg)
        f.onCompass(compass(0, 8.0))
        val s0 = f.onGps(fix(0, 358.0))              // offset = -10
        assertEquals(-10.0, s0.compassOffset!!, 1e-9)
        f.onGps(fix(500, 358.0, speed = 0.2))
        val s = f.onCompass(compass(600, 20.0))
        assertEquals(10.0, s.heading!!, 1e-9)
    }

    @Test fun compassOnlyWithoutOffsetIsLowConfidence() {
        val f = HeadingFusion(cfg)
        f.onCompass(compass(0, 45.0))
        val s = f.current(100)
        assertEquals(45.0, s.heading!!, 1e-9)
        assertEquals(Confidence.LOW, s.confidence)
        assertEquals(HeadingSource.COMPASS, s.source)
        assertNull(s.compassOffset)
    }

    @Test fun oldOffsetGivesLowConfidence() {
        val f = HeadingFusion(cfg)
        f.onCompass(compass(0, 90.0))
        f.onGps(fix(0, 100.0))
        f.onGps(fix(1000, 100.0, speed = 0.2))
        val s = f.onCompass(compass(70_000, 120.0))
        assertEquals(130.0, s.heading!!, 1e-9)
        assertEquals(Confidence.LOW, s.confidence)
    }

    @Test fun poorBearingAccuracyIsRejected() {
        val f = HeadingFusion(cfg)
        f.onCompass(compass(0, 90.0))
        val s = f.onGps(fix(0, 100.0, speed = 3.0, acc = 45.0))
        assertEquals(HeadingSource.COMPASS, s.source)
        assertEquals(90.0, s.heading!!, 1e-9)
        assertEquals(false, s.gpsValid)
    }

    @Test fun missingBearingIsRejected() {
        val f = HeadingFusion(cfg)
        f.onCompass(compass(0, 90.0))
        assertEquals(HeadingSource.COMPASS, f.onGps(fix(0, null)).source)
    }

    @Test fun staleGpsFallsBackToCompass() {
        val f = HeadingFusion(cfg)
        f.onCompass(compass(0, 90.0))
        f.onGps(fix(0, 100.0))
        for (t in 500L..4000L step 500L) f.onCompass(compass(t, 90.0))
        assertEquals(HeadingSource.COG, f.current(2000).source)
        val s = f.current(4000)
        assertEquals(HeadingSource.COMPASS, s.source)
        assertEquals(Confidence.MEDIUM, s.confidence)
        assertEquals(100.0, s.heading!!, 1e-9)
    }

    @Test fun compassInterpolatesBetweenFixes() {
        val f = HeadingFusion(cfg)
        f.onCompass(compass(0, 90.0))
        f.onGps(fix(0, 100.0))
        val s = f.onCompass(compass(500, 120.0))     // boat turned 30 deg since the fix
        assertEquals(130.0, s.heading!!, 1e-9)
        assertEquals(HeadingSource.COG, s.source)
    }

    @Test fun smoothingTakesShortestWayAcrossNorth() {
        val f = HeadingFusion(HeadingFusion.Config(smoothingSec = 1.0))
        f.onCompass(compass(0, 350.0))
        val s = f.onCompass(compass(1000, 10.0))
        val moved = angleDiff(s.heading!!, 350.0)
        assertEquals(20.0 * (1.0 - exp(-1.0)), moved, 1e-3)
    }
}
