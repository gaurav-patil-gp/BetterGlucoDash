package com.eggyswarehouse.betterglucodash.ui.dashboard.a1c

import com.eggyswarehouse.betterglucodash.data.local.db.GlucoseReadingEntity
import java.util.Calendar
import java.util.TimeZone

/**
 * Calculates an estimated HbA1c (eA1C) from Room glucose readings.
 *
 * ## Clinical Formula
 * Source: Nathan et al., "Translating the A1C Assay into Estimated Average Glucose
 * Values." *Diabetes Care* 31(8):1473–1478, 2008 (the ADAG study).
 * Endorsed by the ADA, ACE, and IFCC.
 *
 * ```
 * eA1C (%) = (eAG_mg/dL + 46.7) / 28.7
 * ```
 *
 * Equivalently, given an average glucose:
 * ```
 * eAG_mg/dL  = arithmetic mean of all mg/dL readings
 * eA1C (%)   = (eAG_mg/dL + 46.7) / 28.7
 * ```
 *
 * ## Minimum Data Requirement
 * The ADAG study used 90-day data windows. A 60-day minimum is applied here:
 *  - Red blood cells live ~120 days; HbA1c is a time-weighted integral.
 *  - With < 60 days the estimate is skewed toward recent readings.
 *  - We count **distinct calendar days** (UTC) with ≥ 1 reading.
 *
 * ## Coverage thresholds
 * | Days with data | Result  |
 * |----------------|---------|
 * | ≥ 57           | [A1cState.Ready] exact  |
 * | 50–56          | [A1cState.Ready] ~      |
 * | < 50           | [A1cState.InsufficientData] |
 *
 * Has no Android framework dependencies — fully unit-testable in plain JVM tests.
 */
object A1cCalculator {

    /** Target calendar days for a clinically meaningful eA1C estimate (ADA/ADAG 2008). */
    private const val TARGET_DAYS = 90

    /** Days with data for full-confidence estimate (94% of 90-day window). */
    private const val FULL_CONFIDENCE_DAYS = 85

    /** Minimum days for an approximate estimate (~). */
    private const val APPROXIMATE_MIN_DAYS = 70

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Computes [A1cState] from [readings] spanning up to 90 days.
     * Caller should query Room for the past 90 × 24 = 2160 hours.
     *
     * @param readings All [GlucoseReadingEntity] rows from the retention window.
     */
    fun compute(readings: List<GlucoseReadingEntity>): A1cState {
        if (readings.isEmpty()) return A1cState.InsufficientData(daysWithData = 0)

        // Count distinct UTC calendar days
        val utcCal  = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val dayKeys = readings.map { entity ->
            utcCal.timeInMillis = entity.timestampUtc
            val y = utcCal.get(Calendar.YEAR)
            val m = utcCal.get(Calendar.MONTH)
            val d = utcCal.get(Calendar.DAY_OF_MONTH)
            "$y-$m-$d"
        }.toSet()

        val daysWithData = dayKeys.size.coerceAtMost(TARGET_DAYS)

        if (daysWithData < APPROXIMATE_MIN_DAYS) {
            return A1cState.InsufficientData(daysWithData = daysWithData)
        }

        // Arithmetic mean in mg/dL
        val eAGMgDl = readings.sumOf { it.valueMgDl.toLong() }.toDouble() / readings.size

        // ADAG formula (Nathan et al. 2008)
        val a1cPercent = (eAGMgDl + 46.7) / 28.7

        return A1cState.Ready(
            a1cPercent    = a1cPercent,
            eAGMgDl       = eAGMgDl,
            daysWithData  = daysWithData,
            isApproximate = daysWithData < FULL_CONFIDENCE_DAYS
        )
    }
}
