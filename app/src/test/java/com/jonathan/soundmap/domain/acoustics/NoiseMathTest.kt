package com.jonathan.soundmap.domain.acoustics

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.log10
import kotlin.math.sqrt

class NoiseMathTest {

    private val eps = 1e-6

    @Test
    fun rms_deUnaSenalConstante_esSuAmplitud() {
        assertEquals(1000.0, NoiseMath.rms(ShortArray(512) { 1000 }), eps)
        assertEquals(1000.0, NoiseMath.rms(ShortArray(512) { if (it % 2 == 0) 1000 else -1000 }), eps)
    }

    @Test
    fun rms_soloUsaLasMuestrasLeidas() {
        val buffer = ShortArray(10) { if (it < 4) 300 else 0 }
        assertEquals(300.0, NoiseMath.rms(buffer, count = 4), eps)
        assertEquals(0.0, NoiseMath.rms(buffer, count = 0), eps)
    }

    @Test
    fun rms_deUnaSenoidal_esAmplitudSobreRaizDeDos() {
        val amp = 10_000.0
        val samples = ShortArray(44_100) { (amp * kotlin.math.sin(2 * Math.PI * 440 * it / 44_100)).toInt().toShort() }
        assertEquals(amp / sqrt(2.0), NoiseMath.rms(samples), 5.0)
    }

    @Test
    fun rmsToDb_aplicaLaFormula20Log10() {
        assertEquals(20 * log10(1000.0), NoiseMath.rmsToDb(1000.0), eps) // 60 dB
        assertEquals(40.0, NoiseMath.rmsToDb(100.0), eps)
        assertEquals(46.0, NoiseMath.rmsToDb(100.0, calibrationOffset = 6.0), eps)
    }

    @Test
    fun rmsToDb_silencioYLimites() {
        assertEquals(NoiseMath.MIN_DB, NoiseMath.rmsToDb(0.0), eps)
        assertEquals(NoiseMath.MIN_DB, NoiseMath.rmsToDb(0.5), eps) // negativo → se recorta a 0
        assertEquals(NoiseMath.MAX_DB, NoiseMath.rmsToDb(1e9), eps)
    }

    @Test
    fun duplicarLaAmplitud_sumaSeisDecibeles() {
        val diff = NoiseMath.rmsToDb(2000.0) - NoiseMath.rmsToDb(1000.0)
        assertEquals(6.0206, diff, 1e-3)
    }

    @Test
    fun meanDb_deNivelesIguales_esEseNivel() {
        assertEquals(50.0, NoiseMath.meanDb(listOf(50.0, 50.0, 50.0)), eps)
    }

    @Test
    fun meanDb_esLaMediaAritmetica() {
        assertEquals(50.0, NoiseMath.meanDb(listOf(40.0, 60.0)), eps)
    }

    @Test
    fun meanDb_listaVacia_esCero() {
        assertEquals(NoiseMath.MIN_DB, NoiseMath.meanDb(emptyList()), eps)
    }

    @Test
    fun smooth_seAcercaGradualmente() {
        assertEquals(25.0, NoiseMath.smooth(0.0, 100.0, alpha = 0.25), eps)
        assertEquals(100.0, NoiseMath.smooth(0.0, 100.0, alpha = 1.0), eps)
    }
}
