package com.xateenergia.vendedoresminum.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xateenergia.vendedoresminum.data.repository.FirebaseCustomerRepository
import com.xateenergia.vendedoresminum.data.repository.FirebaseUserRepository
import com.xateenergia.vendedoresminum.data.repository.PlannedRouteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val firebaseCustomerRepository: FirebaseCustomerRepository,
    private val firebaseUserRepository: FirebaseUserRepository,
    private val plannedRouteRepository: PlannedRouteRepository
) : ViewModel() {

    // Guarda o estado do vendedor logado; sem esse dado a Home nao consulta clientes.
    private val userState = MutableStateFlow<String?>(null)

    // Troca automaticamente a consulta em tempo real sempre que o estado do vendedor mudar.
    private val customerCount = userState.flatMapLatest { state ->
        if (state != null) {
            firebaseCustomerRepository.observeCustomersForState(state)
                .map { it.size }
        } else {
            flowOf(0)
        }
    }

    // Combina a quantidade de clientes do Firebase com as rotas planejadas ainda salvas localmente.
    val state: StateFlow<HomeUiState> = combine(
        customerCount,
        plannedRouteRepository.observeSummaries().map { it.size }
    ) { customerCount, routeCount ->
        HomeUiState(customerCount = customerCount, plannedRoutesCount = routeCount)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    init {
        viewModelScope.launch {
            // Carrega o estado autorizado em users/{uid}/state e libera a consulta dos clientes.
            val state = firebaseUserRepository.getCurrentUserState()
            userState.value = state
        }
    }
}

data class HomeUiState(
    val customerCount: Int = 0,
    val plannedRoutesCount: Int = 0,
    val isSyncingCustomers: Boolean = false,
    val syncMessage: String? = null
)
