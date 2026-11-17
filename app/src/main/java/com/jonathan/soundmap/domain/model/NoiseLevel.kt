package com.jonathan.soundmap.domain.model

/** Clasificación del ruido ambiental según el promedio en dB. */
enum class NoiseLevel(val label: String, val description: String) {
    QUIET("Silencioso", "Ideal para estudiar o leer"),
    MODERATE("Moderado", "Cafetería tranquila o conversación suave"),
    LOUD("Ruidoso", "Tráfico o ambiente agitado");

    companion object {
        const val QUIET_LIMIT = 45.0
        const val LOUD_LIMIT = 65.0

        /** < 45 dB silencioso · 45–65 dB moderado · > 65 dB ruidoso. */
        fun from(db: Double): NoiseLevel = when {
            db < QUIET_LIMIT -> QUIET
            db <= LOUD_LIMIT -> MODERATE
            else -> LOUD
        }
    }
}
