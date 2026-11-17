package com.jonathan.soundmap.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoiseSpotDao {

    /** Todos los lugares, del más reciente al más antiguo. Se actualiza solo al cambiar la tabla. */
    @Query("SELECT * FROM noise_spots ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<NoiseSpotEntity>>

    @Insert
    suspend fun insert(spot: NoiseSpotEntity): Long

    @Query("DELETE FROM noise_spots WHERE id = :id")
    suspend fun deleteById(id: Long)
}
