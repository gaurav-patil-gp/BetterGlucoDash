package com.eggyswarehouse.betterglucodash.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GlucoseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadings(readings: List<GlucoseReadingEntity>)

    @Query("SELECT * FROM glucose_readings WHERE timestampUtc >= :since ORDER BY timestampUtc ASC")
    fun getReadingsSince(since: Long): Flow<List<GlucoseReadingEntity>>

    @Query("DELETE FROM glucose_readings WHERE timestampUtc < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    /** Wipes all glucose readings (used by "Clear Local Data" in the dashboard). */
    @Query("DELETE FROM glucose_readings")
    suspend fun deleteAll()
}
