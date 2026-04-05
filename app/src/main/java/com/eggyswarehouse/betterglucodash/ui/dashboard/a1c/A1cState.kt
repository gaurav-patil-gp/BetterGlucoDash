package com.eggyswarehouse.betterglucodash.ui.dashboard.a1c

/**
 * UI state for the "Estimated A1C" card.
 *
 * The ADAG study (Nathan et al., 2008) established that a reliable eA1C estimate
 * requires at least 60 days of continuous CGM data. Below that threshold the
 * result is too strongly weighted toward recent readings to be clinically useful.
 *
 * ## Coverage model (ADA/ADAG 2008 — 90-day window)
 * | Days with ≥1 reading | Result                         |
 * |----------------------|--------------------------------|
 * | ≥ 85/90              | [Ready] (full confidence)      |
 * | 70–84/90             | [Ready] (approximate, ~)       |
 * | < 70/90              | [InsufficientData]             |
 */
sealed class A1cState {

    /** Room query in-flight — shown briefly on first launch. */
    object Calculating : A1cState()

    /**
     * Fewer than 70 of the last 90 days contain glucose data.
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
     * @param isApproximate True when 50–56 of 60 days are covered (~ prefix shown).
     */
    data class Ready(
        val a1cPercent:    Double,
        val eAGMgDl:       Double,
        val daysWithData:  Int,
        val isApproximate: Boolean
    ) : A1cState()
}
