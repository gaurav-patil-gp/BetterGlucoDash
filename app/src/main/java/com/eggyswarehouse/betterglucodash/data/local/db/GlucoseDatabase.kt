package com.eggyswarehouse.betterglucodash.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [GlucoseReadingEntity::class], version = 1, exportSchema = false)
abstract class GlucoseDatabase : RoomDatabase() {
    abstract fun glucoseDao(): GlucoseDao

    companion object {
        @Volatile
        private var Instance: GlucoseDatabase? = null

        fun getDatabase(context: Context): GlucoseDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, GlucoseDatabase::class.java, "glucose_database")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
