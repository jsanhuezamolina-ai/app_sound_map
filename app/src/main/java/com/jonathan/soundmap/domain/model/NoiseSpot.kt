package com.jonathan.soundmap.domain.model

/** Lugar medido y guardado por el usuario. */
data class NoiseSpot(
    val id: Long = 0,
    val name: String,
    val averageDb: Double,
    val timestampMillis: Long,
    val latitude: Double,
    val longitude: Double,
) {
    val level: NoiseLevel get() = NoiseLevel.from(averageDb)
}
