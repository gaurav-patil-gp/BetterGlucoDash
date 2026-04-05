package com.eggyswarehouse.betterglucodash.ui.dashboard.a1c

import com.eggyswarehouse.betterglucodash.data.local.db.GlucoseReadingEntity
import java.time.Instant
import java.time.ZoneOffset

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
 * The ADAG study used 90-day data windows. A [APPROXIMATE_MIN_DAYS]-day minimum is applied here:
 *  - Red blood cells live ~120 days; HbA1c is a time-weighted integral.
 *  - With fewer days the estimate is skewed toward recent readings.
 *  - We count **distinct calendar days** (UTC) with ≥ 1 reading.
 *
 * ## Coverage thresholds
 * | Days with data              | Result                      |
 * |-----------------------------|-----------------------------|
 * | ≥ [FULL_CONFIDENCE_DAYS]    | [A1cState.Ready] exact       |
 * | [APPROXIMATE_MIN_DAYS]–84   | [A1cState.Ready] ~           |
 * | < [APPROXIMATE_MIN_DAYS]    | [A1cState.InsufficientData]  |
 *
 * Has no Android framework dependencies — fully unit-testable in plain JVM tests.
 */
object A1cCalculator {
    /** Target calendar days for a clinically meaningful eA1C estimate (ADA/ADAG 2008). */
    const val TARGET_DAYS = 90

    /** Days with data for full-confidence estimate (94% of 90-day window). */
    const val FULL_CONFIDENCE_DAYS = 85

    /** Minimum days for an approximate estimate (~). */
    const val APPROXIMATE_MIN_DAYS = 70

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Computes [A1cState] from [readings] spanning up to 90 days.
     * Caller should query Room for the past 90 × 24 = 2160 hours.
     *
     * @param readings All [GlucoseReadingEntity] rows from the retention window.
     */
    fun compute(readings: List<GlucoseReadingEntity>): A1cState {
        if (readings.isEmpty()) return A1cState.InsufficientData(daysWithData = 0)

        // Count distinct UTC calendar days using immutable java.time (thread-safe).
        val dayKeys =
            readings
                .map { entity ->
                    Instant
                        .ofEpochMilli(entity.timestampUtc)
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate()
                        .toString() // "YYYY-MM-DD" — unique per calendar day
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
            a1cPercent = a1cPercent,
            eAGMgDl = eAGMgDl,
            daysWithData = daysWithData,
            isApproximate = daysWithData < FULL_CONFIDENCE_DAYS
        )
    }
}
