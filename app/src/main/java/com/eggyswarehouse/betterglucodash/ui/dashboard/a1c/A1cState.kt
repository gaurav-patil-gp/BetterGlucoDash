package com.eggyswarehouse.betterglucodash.ui.dashboard.a1c

/**
 * UI state for the "Estimated A1C" card.
 *
 * The ADAG study (Nathan et al., 2008) established that a reliable eA1C estimate
 * requires sufficient days of continuous CGM data. The exact thresholds are defined
 * as constants in [A1cCalculator] — see there for the authoritative values.
 *
 * ## Coverage model (ADA/ADAG 2008 — 90-day window)
 * | Days with ≥1 reading                | Result                         |
 * |-------------------------------------|--------------------------------|
 * | ≥ [A1cCalculator.FULL_CONFIDENCE_DAYS] | [Ready] (full confidence)   |
 * | [A1cCalculator.APPROXIMATE_MIN_DAYS]–84 | [Ready] (approximate, ~)  |
 * | < [A1cCalculator.APPROXIMATE_MIN_DAYS]  | [InsufficientData]        |
 */
sealed class A1cState {
    /** Room query in-flight — shown briefly on first launch. */
    data object Calculating : A1cState()

    /**
     * Fewer than [A1cCalculator.APPROXIMATE_MIN_DAYS] of the last 90 days contain glucose data.
     *
     * @param daysWithData How many calendar days (UTC) have ≥ 1 reading so far.
     */
    data class InsufficientData(val daysWithData: Int) : A1cState()

    /**
     * A reliable eA1C estimate is available.
     *
     * The estimate uses the ADAG formula:
     *   **eA1C(%) = (eAG_mg/dL + 46.7) / 28.7**
     *
     * @param a1cPercent    Estimated A1C as a percentage (e.g. 7.2).
     * @param eAGMgDl       Underlying estimated average glucose in mg/dL.
     * @param daysWithData  Calendar days that contributed to the calculation.
     * @param isApproximate True when [A1cCalculator.APPROXIMATE_MIN_DAYS]–84 of 90 days are
     *                      covered (~ prefix shown in the card).
     */
    data class Ready(val a1cPercent: Double, val eAGMgDl: Double, val daysWithData: Int, val isApproximate: Boolean) : A1cState()
}
