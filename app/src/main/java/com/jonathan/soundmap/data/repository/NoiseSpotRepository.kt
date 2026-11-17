package com.jonathan.soundmap.data.repository

import com.jonathan.soundmap.data.local.NoiseSpotDao
import com.jonathan.soundmap.data.local.toDomain
import com.jonathan.soundmap.data.local.toEntity
import com.jonathan.soundmap.domain.model.NoiseSpot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Punto único de acceso a los lugares guardados (base de datos local Room, sin red). */
@Singleton
class NoiseSpotRepository @Inject constructor(private val dao: NoiseSpotDao) {

    fun observeSpots(): Flow<List<NoiseSpot>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun save(spot: NoiseSpot): Long = dao.insert(spot.toEntity())

    suspend fun delete(id: Long) = dao.deleteById(id)
}
