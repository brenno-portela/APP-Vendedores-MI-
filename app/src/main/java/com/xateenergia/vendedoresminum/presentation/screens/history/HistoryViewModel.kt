package com.xateenergia.vendedoresminum.presentation.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xateenergia.vendedoresminum.data.repository.PlannedRouteRepository
import com.xateenergia.vendedoresminum.domain.model.PlannedRouteSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val plannedRouteRepository: PlannedRouteRepository
) : ViewModel() {
    val state: StateFlow<HistoryUiState> = plannedRouteRepository.observeSummaries()
        .map { HistoryUiState(routes = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState()
        )

    fun updateRouteStatus(routeId: Long, isCompleted: Boolean, reason: String?) {
        viewModelScope.launch {
            plannedRouteRepository.updateRouteCompletionStatus(
                routeId = routeId,
                isCompleted = isCompleted,
                reason = if (isCompleted) null else reason
            )
        }
    }
}

data class HistoryUiState(
    val routes: List<PlannedRouteSummary> = emptyList()
)

