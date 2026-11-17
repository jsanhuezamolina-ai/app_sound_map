package com.jonathan.soundmap.domain.acoustics

import com.jonathan.soundmap.domain.model.NoiseLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoiseSamplerTest {

    @Test
    fun completaALos10SegundosYDevuelveElPromedio() {
        val sampler = NoiseSampler()
        sampler.start(nowMillis = 0)
        for (t in 0L until 10_000L step 100) sampler.add(50.0, t)
        assertFalse(sampler.isComplete(9_999))
        assertEquals(0.5f, sampler.progress(5_000), 1e-6f)
        assertTrue(sampler.isComplete(10_000))
        assertEquals(100, sampler.sampleCount)
        assertEquals(50.0, sampler.finish()!!, 1e-6)
        assertFalse(sampler.isRunning)
    }

    @Test
    fun unRuidoBreve_noDisparaElPromedio() {
        // 9,9 s a 40 dB y un golpe de 0,1 s a 80 dB: el lugar sigue siendo silencioso.
        // Ese es el motivo de medir 10 s y no un instante.
        val sampler = NoiseSampler()
        sampler.start(0)
        for (i in 0 until 99) sampler.add(40.0, i * 100L)
        sampler.add(80.0, 9_900)
        val avg = sampler.finish()!!
        assertEquals(40.4, avg, 1e-6)
        assertEquals(NoiseLevel.QUIET, NoiseLevel.from(avg))
    }

    @Test
    fun sinIniciar_ignoraLecturas() {
        val sampler = NoiseSampler()
        assertEquals(0f, sampler.add(70.0, 100))
        assertNull(sampler.finish())
    }

    @Test
    fun cancelar_descartaLasLecturas() {
        val sampler = NoiseSampler()
        sampler.start(0)
        sampler.add(70.0, 100)
        sampler.cancel()
        assertFalse(sampler.isRunning)
        assertNull(sampler.finish())
    }

    @Test
    fun clasificacionDeNiveles() {
        assertEquals(NoiseLevel.QUIET, NoiseLevel.from(30.0))
        assertEquals(NoiseLevel.QUIET, NoiseLevel.from(44.9))
        assertEquals(NoiseLevel.MODERATE, NoiseLevel.from(45.0))
        assertEquals(NoiseLevel.MODERATE, NoiseLevel.from(65.0))
        assertEquals(NoiseLevel.LOUD, NoiseLevel.from(65.1))
        assertEquals(NoiseLevel.LOUD, NoiseLevel.from(90.0))
    }
}
