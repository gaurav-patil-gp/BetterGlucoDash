package com.eggyswarehouse.betterglucodash.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a single CGM glucose reading persisted to the local database.
 *
 * **Upsert pattern:** The primary key is [timestampUtc]. All inserts use
 * `OnConflictStrategy.REPLACE`, so writing the same timestamp twice updates the row
 * in-place rather than creating a duplicate. This makes API back-fills self-healing.
 *
 * **Two-value storage:** Abbott provides both a raw mg/dL integer ([valueMgDl]) and a
 * region-converted display value ([valueDisplay]). We store both:
 *  - [valueMgDl]     is used for medical calculations (A1C, average) — always mg/dL.
 *  - [valueDisplay]  is used for rendering — already converted to mmol/L by Abbott for CA users.
 */
@Entity(tableName = "glucose_readings")
data class GlucoseReadingEntity(
    /** UTC epoch millis. Primary key — INSERT OR REPLACE performs an upsert. */
    @PrimaryKey
    val timestampUtc: Long,
    /** Raw mg/dL integer from Abbott's API. Always present regardless of region. */
    val valueMgDl: Int,
    /**
     * Regional display value: mmol/L for CA/EU, mg/dL for US.
     * Abbott converts server-side — no client-side math needed.
     */
    val valueDisplay: Double,
    /** TrendArrow integer: 1=↓↓ 2=↓ 3=→ 4=↑ 5=↑↑. */
    val trendArrow: Int,
    /** Abbott MeasurementColor: 1=green (in-range), 2=yellow (slightly high), 3=orange (high), 4=red (low). */
    val measurementColor: Int,
    /** Region code ("US"/"CA") of the session that wrote this row. Used for unit display in graph. */
    val region: String
)
