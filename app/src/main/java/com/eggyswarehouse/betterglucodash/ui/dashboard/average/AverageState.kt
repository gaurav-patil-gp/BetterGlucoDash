package com.eggyswarehouse.betterglucodash.ui.dashboard.average

/**
 * UI state for the "Last 24h Average" card.
 *
 * The sealed hierarchy enforces the medical coverage rules implemented in [AverageCalculator]:
 *
 *  Coverage model:
 *   - We split the rolling 24h window into 24 one-hour buckets.
 *   - A bucket counts as "covered" when it has ≥ 1 glucose reading.
 *   - A reliable 24h average requires all 24 buckets covered.
 *   - ≥ 22/24 buckets: [Ready] (accurate, or approximate with ~ prefix if < 24).
 *   - < 22/24 buckets: [InsufficientData] — not enough hourly coverage.
 *   - Any contiguous gap > 2h: [IncompleteData] — sensor interruption.
 */
sealed class AverageState {
    /** Room query in-flight — shown briefly on first launch. */
    data object Calculating : AverageState()

    /**
     * Fewer than 22 of the 24 one-hour buckets contain at least one reading.
     *
     * @param hoursWithData  Number of hours (0–23) that have ≥ 1 reading.
     */
    data class InsufficientData(val hoursWithData: Int) : AverageState()

    /**
     * A contiguous gap > 2 hours exists in the window — sensor interruption.
     * The average cannot be trusted medically; display an alert.
     */
    data object IncompleteData : AverageState()

    /**
     * Sufficient coverage — a reliable 24h average is ready.
     *
     * @param averageMgDl    Arithmetic mean of all readings in mg/dL.
     * @param displayAverage Regional display value (mmol/L or mg/dL).
     * @param hoursWithData  Hours that contributed (out of 24) — shown in detail.
     * @param isApproximate  True when 22–23 of 24 hours covered. Card shows "~" prefix.
     * @param isMmol         True for CA region (display in mmol/L).
     */
    data class Ready(
        val averageMgDl: Double,
        val displayAverage: Double,
        val hoursWithData: Int,
        val isApproximate: Boolean,
        val isMmol: Boolean
    ) : AverageState()
}
