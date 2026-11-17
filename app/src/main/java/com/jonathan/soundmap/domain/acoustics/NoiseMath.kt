package com.jonathan.soundmap.domain.acoustics

import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Cálculos acústicos puros (sin dependencias de Android), para poder probarlos con JUnit.
 *
 * El micrófono entrega muestras PCM de 16 bits. Primero se calcula el RMS del bloque y luego
 * dB = 20·log10(RMS / Pref). Con Pref = 1 se obtiene un valor relativo al ruido digital mínimo;
 * se usa como aproximación a dB SPL, que es lo habitual en medidores de ruido para teléfonos sin
 * calibrar. El resultado se limita a [MIN_DB, MAX_DB].
 */
object NoiseMath {

    /** Referencia de presión digital: 1 unidad de amplitud PCM. */
    const val P_REF: Double = 1.0

    const val MIN_DB: Double = 0.0
    const val MAX_DB: Double = 120.0

    /** Valor cuadrático medio de las primeras [count] muestras. */
    fun rms(samples: ShortArray, count: Int = samples.size): Double {
        val n = count.coerceIn(0, samples.size)
        if (n == 0) return 0.0
        var sum = 0.0
        for (i in 0 until n) {
            val x = samples[i].toDouble()
            sum += x * x
        }
        return sqrt(sum / n)
    }

    /**
     * Convierte un RMS a decibelios: 20·log10(rms / pRef) + [calibrationOffset].
     * Un RMS de 0 (silencio digital) devuelve [MIN_DB].
     */
    fun rmsToDb(rms: Double, pRef: Double = P_REF, calibrationOffset: Double = 0.0): Double {
        if (rms <= 0.0 || pRef <= 0.0) return MIN_DB
        return (20.0 * log10(rms / pRef) + calibrationOffset).coerceIn(MIN_DB, MAX_DB)
    }

    /** Atajo: decibelios de un bloque de muestras. */
    fun blockDb(samples: ShortArray, count: Int = samples.size, calibrationOffset: Double = 0.0): Double =
        rmsToDb(rms(samples, count), calibrationOffset = calibrationOffset)

    /**
     * Promedio de una ventana de lecturas en dB (media aritmética de los decibelios).
     * Se eligió en vez del nivel energético (Leq) porque Leq se dispara con un solo golpe fuerte:
     * 9,9 s a 40 dB + 0,1 s a 80 dB dan Leq ≈ 60 dB, mientras que la media da ≈ 40 dB. La media
     * refleja mejor si un lugar es tranquilo, que es lo que se busca al medir 10 segundos.
     */
    fun meanDb(levels: List<Double>): Double {
        if (levels.isEmpty()) return MIN_DB
        return levels.average().coerceIn(MIN_DB, MAX_DB)
    }

    /** Suavizado exponencial para que la aguja no salte con cada bloque. */
    fun smooth(previous: Double, current: Double, alpha: Double = 0.25): Double =
        previous + alpha.coerceIn(0.0, 1.0) * (current - previous)
}
