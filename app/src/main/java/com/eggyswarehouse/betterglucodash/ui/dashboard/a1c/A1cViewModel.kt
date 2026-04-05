package com.eggyswarehouse.betterglucodash.ui.dashboard.a1c

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.eggyswarehouse.betterglucodash.GlucoDashApplication
import com.eggyswarehouse.betterglucodash.data.repository.LibreRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the "Estimated A1C" card.
 *
 * Queries the past 90 days of Room data (maximum retention window) and delegates
 * all calculation to [A1cCalculator]. The ADAG formula requires a
 * [A1cCalculator.APPROXIMATE_MIN_DAYS]-day minimum; the extra days provide a larger
 * sample for improved accuracy once funded.
 *
 * Uses the same [LibreRepository.getReadingsForWindow] API as other ViewModels
 * to keep the data pipeline consistent.
 */
class A1cViewModel(private val repository: LibreRepository) : ViewModel() {
    val uiState: StateFlow<A1cState> =
        repository
            .getReadingsForWindow(hours = NINETY_DAYS_HOURS)
            .map { entities -> A1cCalculator.compute(entities) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = A1cState.Calculating
            )

    companion object {
        /** 90 days expressed as hours for [LibreRepository.getReadingsForWindow]. */
        private const val NINETY_DAYS_HOURS = 90 * 24

        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GlucoDashApplication
                    A1cViewModel(app.container.libreRepository)
                }
            }
    }
}
