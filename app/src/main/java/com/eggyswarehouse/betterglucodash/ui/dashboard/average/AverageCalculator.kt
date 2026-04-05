package com.eggyswarehouse.betterglucodash.ui.dashboard.average

import com.eggyswarehouse.betterglucodash.data.local.db.GlucoseReadingEntity
import java.util.Calendar
import java.util.TimeZone

/**
 * Computes the rolling 24-hour glucose average using an hourly-coverage model.
 *
 * ## Methodology
 * We divide the 24h window into 24 one-hour buckets (identified by their
 * calendar-hour in UTC). A bucket is "covered" when it contains ≥ 1 reading.
 *
 * This mirrors clinical CGM consensus: time-in-range and average calculations
 * require sufficient temporal distribution, not just total sample count.
 *
 * ## Coverage thresholds
 * | Covered hours | Result                        |
 * |---------------|-------------------------------|
 * | 24/24         | [AverageState.Ready] (exact)  |
 * | 22–23/24      | [AverageState.Ready] (~)      |
 * | < 22/24       | [AverageState.InsufficientData] |
 *
 * A single contiguous gap > 2h → [AverageState.IncompleteData] regardless of total coverage
 * (sensor disconnection biases the average unpredictably).
 *
 * Has no Android framework dependencies — fully unit-testable in plain JVM tests.
 */
object AverageCalculator {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Total hours in the coverage window. */
    private const val TOTAL_HOURS = 24

    /**
     * Minimum number of distinct hours that must have ≥ 1 reading for a
     * medically meaningful average. 22/24 = ~92% hourly coverage.
     */
    private const val MIN_COVERED_HOURS = 22

    /**
     * Maximum contiguous data gap before declaring the window [AverageState.IncompleteData].
     * 2 hours is a pragmatic threshold accounting for sensor warm-up / brief disconnects.
     */
    private const val MAX_GAP_MS = 2L * 60 * 60 * 1_000

    /** IFCC-aligned mg/dL → mmol/L conversion factor. */
    private const val MGDL_TO_MMOL = 18.01559

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Evaluates [readings] (already filtered to the last 24h) and returns
     * the appropriate [AverageState].
     *
     * @param readings   All entities from the past 24h, in any order.
     * @param isCanada   True when the user's region is "CA" (display in mmol/L).
     */
    fun compute(readings: List<GlucoseReadingEntity>, isCanada: Boolean): AverageState {
        if (readings.isEmpty()) return AverageState.InsufficientData(hoursWithData = 0)

        val sorted = readings.sortedBy { it.timestampUtc }

        // ── Contiguous-gap check ───────────────────────────────────────────────
        for (i in 0 until sorted.size - 1) {
            if (sorted[i + 1].timestampUtc - sorted[i].timestampUtc > MAX_GAP_MS) {
                return AverageState.IncompleteData
            }
        }

        // ── Hourly-bucket coverage ─────────────────────────────────────────────
        // Map each reading to a "YYYY-MM-DD-HH" UTC key so distinct hours are counted.
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val coveredHourKeys = sorted.map { entity ->
            utcCal.timeInMillis = entity.timestampUtc
            val y  = utcCal.get(Calendar.YEAR)
            val mo = utcCal.get(Calendar.MONTH)
            val d  = utcCal.get(Calendar.DAY_OF_MONTH)
            val h  = utcCal.get(Calendar.HOUR_OF_DAY)
            "$y-$mo-$d-$h"
        }.toSet()

        val hoursWithData = coveredHourKeys.size.coerceAtMost(TOTAL_HOURS)

        if (hoursWithData < MIN_COVERED_HOURS) {
            return AverageState.InsufficientData(hoursWithData = hoursWithData)
        }

        // ── Compute average ────────────────────────────────────────────────────
        val avgMgDl  = sorted.sumOf { it.valueMgDl.toLong() }.toDouble() / sorted.size
        val display  = if (isCanada) avgMgDl / MGDL_TO_MMOL else avgMgDl

        return AverageState.Ready(
            averageMgDl    = avgMgDl,
            displayAverage = display,
            hoursWithData  = hoursWithData,
            isApproximate  = hoursWithData < TOTAL_HOURS,
            isMmol         = isCanada
        )
    }
}
