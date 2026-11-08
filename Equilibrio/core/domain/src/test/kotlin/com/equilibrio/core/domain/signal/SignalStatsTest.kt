package com.equilibrio.core.domain.signal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalStatsTest {

    @Test
    fun `mean std and rms of a constant signal`() {
        val stats = SignalStats.of(DoubleArray(100) { 2.0 })
        assertEquals(2.0, stats.mean, 1e-9)
        assertEquals(0.0, stats.stdDev, 1e-9)
        assertEquals(2.0, stats.rms, 1e-9)
        assertEquals(0, stats.peakCount)
    }

    @Test
    fun `peaks are counted above two sigmas`() {
        val values = doubleArrayOf(1.0, 1.0, 20.0, 1.0, 1.0, 1.0, 20.0, 1.0, 1.0, 1.0)
        val stats = SignalStats.of(values, sigma = 1.5)
        assertEquals(2, stats.peakCount)
        assertEquals(20.0, stats.max, 1e-9)
    }

    @Test
    fun `agitation index is bounded and monotonic`() {
        val calm = SignalStats.agitationIndex(0.01, 0.01, 0, 15.0, 0.1)
        val moving = SignalStats.agitationIndex(1.0, 0.8, 12, 15.0, 30.0)
        val intense = SignalStats.agitationIndex(4.0, 5.0, 60, 15.0, 200.0)
        assertTrue(calm < 5.0)
        assertTrue(moving in 5.0..100.0)
        assertTrue(intense > moving)
        assertTrue(intense <= 100.0)
    }

    @Test
    fun `z score handles zero variance`() {
        assertEquals(2.5, SignalStats.zScore(5.0, 0.0, 2.0), 1e-9)
        assertEquals(5.0, SignalStats.zScore(5.0, 0.0, 0.0), 1e-9)
        assertEquals(6.0, SignalStats.zScore(100.0, 0.0, 1.0), 1e-9)
    }
}
