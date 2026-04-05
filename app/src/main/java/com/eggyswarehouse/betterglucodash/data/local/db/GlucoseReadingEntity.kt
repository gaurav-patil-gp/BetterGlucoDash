package com.eggyswarehouse.betterglucodash.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "glucose_readings")
data class GlucoseReadingEntity(
    @PrimaryKey
    val timestampUtc: Long,
    val valueMgDl: Int,
    val valueDisplay: Double,
    val trendArrow: Int,
    val measurementColor: Int,
    val region: String
)
