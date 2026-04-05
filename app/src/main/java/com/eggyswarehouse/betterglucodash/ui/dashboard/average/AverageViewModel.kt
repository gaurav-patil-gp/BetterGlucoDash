package com.eggyswarehouse.betterglucodash.ui.dashboard.average

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.eggyswarehouse.betterglucodash.data.local.AuthManager
import com.eggyswarehouse.betterglucodash.data.repository.LibreRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.eggyswarehouse.betterglucodash.GlucoDashApplication

class AverageViewModel(
    private val repository: LibreRepository,
    private val authManager: AuthManager
) : ViewModel() {

    // Request exactly 24 hours of data
    val uiState: StateFlow<AverageState> = combine(
        repository.getReadingsForWindow(hours = 24),
        authManager.regionFlow
    ) { entities, region ->
        val isCanada = region == "CA"
        AverageCalculator.compute(entities, isCanada)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AverageState.Calculating
    )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
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
