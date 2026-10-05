package com.blindsail.tacktracker.logic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AngleMathTest {
    private val eps = 1e-9

    @Test fun wrap360_basics() {
        assertEquals(0.0, wrap360(360.0), eps)
        assertEquals(350.0, wrap360(-10.0), eps)
        assertEquals(10.0, wrap360(370.0), eps)
        assertEquals(0.0, wrap360(-1e-15), eps)
        assertTrue(wrap360(-1e-15) < 360.0)
    }

    @Test fun wrapSigned_basics() {
        assertEquals(-180.0, wrapSigned(180.0), eps)
        assertEquals(-170.0, wrapSigned(190.0), eps)
        assertEquals(170.0, wrapSigned(-190.0), eps)
        assertEquals(0.0, wrapSigned(720.0), eps)
        assertEquals(-10.0, wrapSigned(350.0), eps)
    }

    @Test fun angleDiff_acrossNorth() {
        assertEquals(20.0, angleDiff(10.0, 350.0), eps)
        assertEquals(-20.0, angleDiff(350.0, 10.0), eps)
    }

    @Test fun circularMean_aroundNorthIsZeroNot180() {
        assertEquals(0.0, angleDiff(circularMean(listOf(350.0, 10.0)), 0.0), 1e-9)
        assertEquals(5.0, circularMean(listOf(355.0, 15.0)), 1e-9)
    }

    @Test fun circularMean_weights() {
        val m = circularMean(listOf(0.0, 90.0), listOf(3.0, 1.0))
        assertTrue(m > 0.0 && m < 45.0)
        assertEquals(0.0, angleDiff(circularMean(listOf(10.0, 20.0), listOf(1.0, 0.0)), 10.0), 1e-9)
    }

    @Test fun circularStdDev_steadyVsSpread() {
        assertEquals(0.0, circularStdDev(listOf(45.0, 45.0, 45.0)), eps)
        assertEquals(0.0, circularStdDev(listOf(45.0)), eps)
        assertTrue(circularStdDev(listOf(358.0, 0.0, 2.0, 1.0, 359.0)) < 3.0)
        assertTrue(circularStdDev(listOf(0.0, 40.0, 80.0, 120.0)) > 20.0)
        // Same spread must give the same answer across the 0/360 boundary.
        val a = circularStdDev(listOf(-2.0, 0.0, 2.0))
        val b = circularStdDev(listOf(358.0, 0.0, 2.0))
        assertEquals(a, b, 1e-9)
    }

    @Test fun bisector_specExample() {
        // Wind 0, starboard close-hauled 315, port close-hauled 45.
        assertEquals(0.0, bisector(315.0, 45.0), eps)
    }

    @Test fun bisector_variousWinds() {
        assertEquals(90.0, bisector(45.0, 135.0), eps)
        assertEquals(200.0, bisector(155.0, 245.0), eps)
        assertEquals(340.0, bisector(295.0, 25.0), eps)
    }

    @Test fun relativeWind_signConvention() {
        assertEquals(45.0, relativeWind(0.0, 315.0), eps)   // starboard
        assertEquals(-45.0, relativeWind(0.0, 45.0), eps)   // port
        assertEquals(10.0, relativeWind(5.0, 355.0), eps)   // across north
    }
}
