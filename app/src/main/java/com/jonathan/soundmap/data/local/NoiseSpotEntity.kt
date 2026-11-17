package com.jonathan.soundmap.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jonathan.soundmap.domain.model.NoiseSpot

@Entity(tableName = "noise_spots")
data class NoiseSpotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val averageDb: Double,
    val timestampMillis: Long,
    val latitude: Double,
    val longitude: Double,
)

fun NoiseSpotEntity.toDomain() = NoiseSpot(id, name, averageDb, timestampMillis, latitude, longitude)

fun NoiseSpot.toEntity() = NoiseSpotEntity(id, name, averageDb, timestampMillis, latitude, longitude)
