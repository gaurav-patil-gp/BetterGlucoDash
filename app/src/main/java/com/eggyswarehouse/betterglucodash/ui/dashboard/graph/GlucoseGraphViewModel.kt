package com.eggyswarehouse.betterglucodash.ui.dashboard.graph

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.eggyswarehouse.betterglucodash.data.repository.LibreRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.eggyswarehouse.betterglucodash.GlucoDashApplication

class GlucoseGraphViewModel(
    private val repository: LibreRepository
) : ViewModel() {

    private val _selectedRange = MutableStateFlow<TimeRange>(TimeRange.ThreeHour)

    private val _crosshairIndex = MutableStateFlow<Int?>(null)

    private var hasLoadedInitialData = false

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<GlucoseGraphUiState> = _selectedRange
        .flatMapLatest { range ->
            repository.getReadingsForWindow(range.hours).map { entities ->
                Pair(range, entities)
            }
        }
        .combine(_crosshairIndex) { (range, entities), crosshair ->
            if (entities.isEmpty()) {
                GlucoseGraphUiState.Loading
            } else {
                val animate = !hasLoadedInitialData
                hasLoadedInitialData = true
                GlucoseGraphUiState.Ready(
                    points = entities.map { it.toGraphPoint() },
                    selectedRange = range,
                    crosshairIndex = crosshair,
                    animateEntry = animate
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GlucoseGraphUiState.Loading
        )

    fun selectRange(range: TimeRange) {
        _selectedRange.value = range
        _crosshairIndex.value = null
    }

    fun updateCrosshair(index: Int?) {
        _crosshairIndex.value = index
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GlucoDashApplication)
                GlucoseGraphViewModel(application.container.libreRepository)
            }
        }
    }
}
