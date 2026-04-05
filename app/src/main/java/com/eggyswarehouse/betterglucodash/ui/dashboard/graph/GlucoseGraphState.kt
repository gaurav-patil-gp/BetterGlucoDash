package com.eggyswarehouse.betterglucodash.ui.dashboard.graph

import com.eggyswarehouse.betterglucodash.data.local.db.GlucoseReadingEntity
import com.eggyswarehouse.betterglucodash.data.network.Region

/**
 * Represents one of the four selectable time windows for the glucose graph (PRD §4.1).
 *
 * Implemented as a sealed class so the compiler can exhaustively check `when` branches.
 * [values] is a lazy getter (not a val) to avoid Kotlin object-initialisation ordering
 * issues that caused a NullPointerException at runtime during the initial composition.
 */
sealed class TimeRange(val hours: Int, val label: String) {
    data object ThreeHour : TimeRange(3, "3h")

    data object SixHour : TimeRange(6, "6h")

    data object TwelveHour : TimeRange(12, "12h")

    data object TwentyFourHour : TimeRange(24, "24h")

    companion object {
        /** Ordered list of all time ranges for the segmented button row. */
        val values get() = listOf(ThreeHour, SixHour, TwelveHour, TwentyFourHour)
    }
}

/**
 * A single sanitised data point fed to [GlucoseGraphRenderer].
 *
 * @param timestamp  UTC epoch millis from [GlucoseReadingEntity.timestampUtc].
 * @param value      Regional display value (mmol/L or mg/dL) from [GlucoseReadingEntity.valueDisplay].
 * @param color      Abbott MeasurementColor: 1=green, 2=yellow, 3=orange(high), 4=red(low).
 * @param region     Region for this reading — replaces the `value < 35` heuristic for
 *                   unit detection in [GlucoseGraphCard].
 */
data class GraphPoint(val timestamp: Long, val value: Double, val color: Int, val region: Region)

/**
 * UI state produced by [GlucoseGraphViewModel] and consumed by [GlucoseGraphCard].
 */
sealed class GlucoseGraphUiState {
    /** Graph data is loading from Room or no readings have arrived yet. */
    data object Loading : GlucoseGraphUiState()

    /**
     * Enough data to render the graph.
     *
     * @param points         Chronologically sorted list of graph points.
     * @param selectedRange  Currently active time-range filter.
     * @param crosshairIndex Index into [points] of the active crosshair, or null.
     * @param animateEntry   True on the first render after a range change; drives the
     *                       left-to-right path draw animation.
     */
    data class Ready(
        val points: List<GraphPoint>,
        val selectedRange: TimeRange,
        val crosshairIndex: Int? = null,
        val animateEntry: Boolean = false
    ) : GlucoseGraphUiState()
}

/**
 * Maps a [GlucoseReadingEntity] from Room to the lightweight [GraphPoint] the
 * renderer needs.
 */
fun GlucoseReadingEntity.toGraphPoint() = GraphPoint(
    timestamp = this.timestampUtc,
    value = this.valueDisplay,
    color = this.measurementColor,
    region = Region.fromCode(this.region)
)
