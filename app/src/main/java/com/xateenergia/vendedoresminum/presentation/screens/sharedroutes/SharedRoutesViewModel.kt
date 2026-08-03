package com.xateenergia.vendedoresminum.presentation.screens.sharedroutes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xateenergia.vendedoresminum.data.repository.FirebaseSharedRouteRepository
import com.xateenergia.vendedoresminum.domain.model.SharedRouteAssignment
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class SharedRoutesViewModel @Inject constructor(
    firebaseSharedRouteRepository: FirebaseSharedRouteRepository
) : ViewModel() {
    val state: StateFlow<SharedRoutesUiState> = firebaseSharedRouteRepository.observeAssignedRoutes()
        .map { routes -> SharedRoutesUiState(routes = routes, isLoading = false) }
        .catch { throwable -> emit(SharedRoutesUiState(isLoading = false, error = throwable.message)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SharedRoutesUiState()
        )
}

data class SharedRoutesUiState(
    val routes: List<SharedRouteAssignment> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
