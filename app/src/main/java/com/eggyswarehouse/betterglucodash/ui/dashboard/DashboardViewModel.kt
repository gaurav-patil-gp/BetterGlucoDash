package com.eggyswarehouse.betterglucodash.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.eggyswarehouse.betterglucodash.GlucoDashApplication
import com.eggyswarehouse.betterglucodash.data.local.AuthManager
import com.eggyswarehouse.betterglucodash.data.local.LIBRE_TIMESTAMP_FORMATTER
import com.eggyswarehouse.betterglucodash.data.network.Region
import com.eggyswarehouse.betterglucodash.data.repository.GlucoseFlowState
import com.eggyswarehouse.betterglucodash.data.repository.LibreRepository
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI state for the Dashboard screen.
 *
 * @property currentGlucose  Formatted glucose string ready for display (e.g. "10.1" or "182").
 * @property unit            Unit label locked to the login region ("mmol/L" or "mg/dL").
 * @property trendArrow      Unicode trend arrow (↓↓ / ↓ / → / ↑ / ↑↑).
 * @property timestamp       Local-time string of the last reading from the API.
 * @property lastReadingMs   UTC epoch millis of the last reading (for "X min ago" display).
 * @property glucoseColor    Abbott MeasurementColor: 1=green, 2=yellow, 3=orange, 4=red.
 *                           Drives the hero card's subtle background tint.
 * @property isLoading       True until the first successful reading arrives.
 * @property error           Non-null when a recoverable error has occurred.
 * @property isSessionExpired True when Abbott's API has rejected the JWT (HTTP 401).
 */
data class DashboardUiState(
    val currentGlucose: String = "--",
    val unit: String = "mmol/L",
    val trendArrow: String = "→",
    /** Raw TrendArrow integer from the API: 1=↓↓ 2=↓ 3=→ 4=↑ 5=↑↑. Drives trend description text. */
    val trendCode: Int = 3,
    val timestamp: String = "",
    val lastReadingMs: Long = 0L,
    val glucoseColor: Int = 1,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isSessionExpired: Boolean = false
)

class DashboardViewModel(private val repository: LibreRepository, private val authManager: AuthManager) : ViewModel() {
    companion object {
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GlucoDashApplication
                    DashboardViewModel(app.container.libreRepository, app.container.authManager)
                }
            }

        /** Maps TrendArrow integer (1–5) from the LibreLinkUp API to a Unicode symbol. */
        fun trendArrowSymbol(arrow: Int): String = when (arrow) {
            1 -> "↓↓" // SingleDown — falling fast
            2 -> "↓" // FortyFiveDown — falling
            3 -> "→" // Flat — stable
            4 -> "↑" // FortyFiveUp — rising
            5 -> "↑↑" // SingleUp — rising fast
            else -> "?"
        }
    }

    private var isLoggedOut = false

    val uiState: StateFlow<DashboardUiState> =
        combine(
            repository.glucoseFlow,
            authManager.regionFlow
        ) { flowState, regionCode ->
            val region = Region.fromCode(regionCode)
            val unitString = region.unitLabel

            when (flowState) {
                is GlucoseFlowState.Loading ->
                    DashboardUiState(
                        unit = unitString,
                        isLoading = true
                    )
                is GlucoseFlowState.SessionExpired ->
                    DashboardUiState(
                        unit = unitString,
                        isLoading = false,
                        isSessionExpired = true
                    )
                is GlucoseFlowState.Error ->
                    DashboardUiState(
                        unit = unitString,
                        isLoading = false,
                        error = flowState.message
                    )
                is GlucoseFlowState.Success -> {
                    val m = flowState.measurement
                    // Abbott pre-converts Value to the regional unit — no client conversion needed.
                    val valueString =
                        if (region.isMetric) {
                            String.format(java.util.Locale.getDefault(), "%.1f", m.Value)
                        } else {
                            m.ValueInMgPerDl.toString()
                        }
                    // Parse FactoryTimestamp (UTC) to epoch millis for "X min ago" display.
                    val readingMs =
                        try {
                            LocalDateTime
                                .parse(m.FactoryTimestamp, LIBRE_TIMESTAMP_FORMATTER)
                                .toInstant(ZoneOffset.UTC)
                                .toEpochMilli()
                        } catch (_: Exception) {
                            0L
                        }

                    DashboardUiState(
                        currentGlucose = valueString,
                        unit = unitString,
                        trendArrow = trendArrowSymbol(m.TrendArrow),
                        trendCode = m.TrendArrow,
                        timestamp = m.Timestamp,
                        lastReadingMs = readingMs,
                        glucoseColor = m.MeasurementColor,
                        isLoading = false
                    )
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState()
        )

    /** Idempotent — safe to call from both the session-expiry handler and the logout button. */
    fun logout() {
        if (isLoggedOut) return
        isLoggedOut = true
        viewModelScope.launch { repository.logout() }
    }

    fun clearDatabase() {
        viewModelScope.launch { repository.clearDatabase() }
    }
}
