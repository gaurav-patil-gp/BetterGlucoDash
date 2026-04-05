package com.eggyswarehouse.betterglucodash.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for BetterGlucoDash.
 *
 * Schema is exported to `app/schemas/` (version-controlled) so migration diffs are
 * visible in code review. See `ksp { arg("room.schemaLocation", ...) }` in app/build.gradle.kts.
 *
 * KNOWN TRADE-OFF: [fallbackToDestructiveMigration] is used for V1 because this is a
 * cache-only database (all data is re-fetched from Abbott's API on next poll). There is
 * no user-generated data that would be lost. V3 should add explicit migrations if the
 * schema stabilises and long-term offline retention becomes a product requirement.
 */
@Database(entities = [GlucoseReadingEntity::class], version = 1, exportSchema = true)
abstract class GlucoseDatabase : RoomDatabase() {
    abstract fun glucoseDao(): GlucoseDao

    companion object {
        @Volatile
        private var instance: GlucoseDatabase? = null

        fun getDatabase(context: Context): GlucoseDatabase = instance ?: synchronized(this) {
            Room
                .databaseBuilder(context, GlucoseDatabase::class.java, "glucose_database")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                .also { instance = it }
        }
    }
}
