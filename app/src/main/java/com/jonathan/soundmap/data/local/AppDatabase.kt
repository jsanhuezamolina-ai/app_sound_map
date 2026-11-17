package com.jonathan.soundmap.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [NoiseSpotEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noiseSpotDao(): NoiseSpotDao

    companion object {
        const val NAME = "soundmap.db"
    }
}
