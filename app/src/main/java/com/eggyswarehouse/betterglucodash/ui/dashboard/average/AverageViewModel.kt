package com.eggyswarehouse.betterglucodash.ui.dashboard.average

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.eggyswarehouse.betterglucodash.GlucoDashApplication
import com.eggyswarehouse.betterglucodash.data.local.AuthManager
import com.eggyswarehouse.betterglucodash.data.network.Region
import com.eggyswarehouse.betterglucodash.data.repository.LibreRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the "Last 24h Average" card.
 *
 * Combines the rolling 24-hour Room query with the stored region so [AverageCalculator]
 * can produce the correct display unit (mmol/L for CA, mg/dL for US).
 *
 * Uses the same [LibreRepository.getReadingsForWindow] API as other ViewModels
 * to keep the data pipeline consistent.
 */
class AverageViewModel(private val repository: LibreRepository, private val authManager: AuthManager) : ViewModel() {
    val uiState: StateFlow<AverageState> =
        combine(
            repository.getReadingsForWindow(hours = 24),
            authManager.regionFlow
        ) { entities, regionCode ->
            AverageCalculator.compute(entities, isMetric = Region.fromCode(regionCode).isMetric)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AverageState.Calculating
        )

    companion object {
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GlucoDashApplication)
                    AverageViewModel(
                        application.container.libreRepository,
                        application.container.authManager
                    )
                }
            }
    }
}
