package com.equilibrio.core.domain.signal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class FftTest {

    @Test
    fun `dominant frequency of a pure sine`() {
        val sampleRate = 1024.0
        val frequency = 64.0
        val samples = DoubleArray(1024) { i ->
            sin(2.0 * PI * frequency * i / sampleRate)
        }
        val dominant = Fft.dominantFrequencyHz(samples, sampleRate)
        assertEquals(frequency, dominant, 1.0)
    }

    @Test
    fun `spectral energy grows with amplitude`() {
        val quiet = DoubleArray(512) { i -> 0.1 * sin(2.0 * PI * 20.0 * i / 512.0) }
        val loud = DoubleArray(512) { i -> 1.0 * sin(2.0 * PI * 20.0 * i / 512.0) }
        assertTrue(Fft.spectralEnergy(loud) > Fft.spectralEnergy(quiet) * 50)
    }

    @Test
    fun `band ratio isolates the sine band`() {
        val sampleRate = 1024.0
        val samples = DoubleArray(1024) { i ->
            sin(2.0 * PI * 100.0 * i / sampleRate)
        }
        val mags = Fft.hannMagnitudes(samples)
        val mid = Fft.bandRatio(mags, sampleRate, 20.0, 250.0)
        val high = Fft.bandRatio(mags, sampleRate, 2000.0, 8000.0)
        assertTrue(mid > 0.9)
        assertTrue(high < 0.05)
    }
}
