package com.jonathan.soundmap.domain.acoustics

/**
 * Acumula lecturas de dB durante una ventana de tiempo (10 s por defecto) y entrega el promedio
 * (media de los dB) al terminar. Es puro: el tiempo se pasa como parámetro, para probarlo sin esperar.
 */
class NoiseSampler(private val durationMillis: Long = DEFAULT_DURATION_MS) {

    private val levels = mutableListOf<Double>()
    private var startMillis: Long = -1

    val isRunning: Boolean get() = startMillis >= 0

    fun start(nowMillis: Long) {
        levels.clear()
        startMillis = nowMillis
    }

    /** Agrega una lectura. Devuelve el progreso de 0 a 1. */
    fun add(db: Double, nowMillis: Long): Float {
        if (!isRunning) return 0f
        levels += db
        return progress(nowMillis)
    }

    fun progress(nowMillis: Long): Float {
        if (!isRunning) return 0f
        return ((nowMillis - startMillis).toFloat() / durationMillis).coerceIn(0f, 1f)
    }

    fun isComplete(nowMillis: Long): Boolean = isRunning && nowMillis - startMillis >= durationMillis

    val sampleCount: Int get() = levels.size

    /** Termina la medición y devuelve el promedio, o null si no hubo lecturas. */
    fun finish(): Double? {
        val result = if (levels.isEmpty()) null else NoiseMath.meanDb(levels)
        cancel()
        return result
    }

    fun cancel() {
        startMillis = -1
        levels.clear()
    }

    companion object {
        const val DEFAULT_DURATION_MS = 10_000L
    }
}
