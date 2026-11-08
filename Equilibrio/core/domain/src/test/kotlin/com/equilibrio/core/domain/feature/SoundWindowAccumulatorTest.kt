package com.equilibrio.core.domain.feature

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class SoundWindowAccumulatorTest {

    private fun sine(amplitude: Int, frequency: Double, samples: Int): ShortArray =
        ShortArray(samples) { i ->
            (amplitude * sin(2.0 * PI * frequency * i / 16_000.0)).toInt().toShort()
        }

    @Test
    fun `silence produces low level and zero overload`() {
        val acc = SoundWindowAccumulator()
        acc.feed(ShortArray(16_000), 16_000)
        val features = acc.finish()
        assertTrue(features.dbMean < 5f)
        assertEquals(0, features.impulsivePeaks)
        assertEquals(0f, features.overloadIndex, 0.01f)
    }

    @Test
    fun `sine energy is concentrated in the mid band`() {
        val acc = SoundWindowAccumulator()
        val buffer = sine(8_000, 440.0, 16_000)
        acc.feed(buffer, buffer.size)
        val features = acc.finish()
        assertTrue(features.dbMean > 60f)
        assertTrue(features.midBandRatio > features.lowBandRatio)
        assertTrue(features.midBandRatio > features.highBandRatio)
        val total = features.lowBandRatio + features.midBandRatio + features.highBandRatio
        assertEquals(1f, total, 0.01f)
    }

    @Test
    fun `sudden loud burst counts as impulsive peak`() {
        val acc = SoundWindowAccumulator()
        val quiet = sine(120, 220.0, 16_000)
        val loud = sine(14_000, 220.0, 4_096)
        acc.feed(quiet, quiet.size)
        acc.feed(loud, loud.size)
        val features = acc.finish()
        assertTrue(features.impulsivePeaks >= 1)
        assertTrue(features.dbMax > features.dbMean)
    }

    @Test
    fun `overload index stays within bounds`() {
        val acc = SoundWindowAccumulator()
        repeat(4) {
            val loud = sine(20_000, 3_000.0, 4_096)
            acc.feed(loud, loud.size)
        }
        val features = acc.finish()
        assertTrue(features.overloadIndex in 0f..100f)
        assertTrue(features.overloadIndex > 30f)
    }
}
