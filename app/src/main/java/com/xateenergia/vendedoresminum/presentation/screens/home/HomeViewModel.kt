package com.xateenergia.vendedoresminum.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xateenergia.vendedoresminum.data.repository.FirebaseCustomerRepository
import com.xateenergia.vendedoresminum.data.repository.FirebaseSharedRouteRepository
import com.xateenergia.vendedoresminum.data.repository.FirebaseUserRepository
import com.xateenergia.vendedoresminum.data.repository.PlannedRouteRepository
import com.xateenergia.vendedoresminum.data.repository.SellerIdentity
import com.xateenergia.vendedoresminum.domain.model.SharedRouteAssignment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
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
    private val plannedRouteRepository: PlannedRouteRepository,
    private val firebaseSharedRouteRepository: FirebaseSharedRouteRepository
) : ViewModel() {

    // Guarda a identidade usada para consultar somente a carteira do vendedor logado.
    private val sellerIdentity = MutableStateFlow<SellerIdentity?>(null)

    // Troca automaticamente a consulta em tempo real quando o perfil do vendedor mudar.
    private val customerCount = sellerIdentity.flatMapLatest { seller ->
        if (seller?.hasAssignmentIdentifier() == true) {
            firebaseCustomerRepository.observeCustomersForSeller(seller)
                .map { it.size }
        } else {
            flowOf(0)
        }
    }.catch { emit(0) }

    // A agenda do vendedor vem da caixa privada de rotas atribuidas pelo
    // backoffice. O Firebase permanece a fonte de verdade dessa tela.
    private val assignedRoutes = firebaseSharedRouteRepository.observeAssignedRoutes()
        .catch { emit(emptyList()) }

    // A Home usa o Firebase como fonte de verdade para nao mostrar rotas que
    // ja foram removidas pelo administrador no backoffice.
    val state: StateFlow<HomeUiState> = combine(
        customerCount,
        plannedRouteRepository.observeFirebaseSummaries()
            .map { it.size }
            .catch { emit(0) },
        assignedRoutes
    ) { customerCount, routeCount, sharedRoutes ->
        HomeUiState(
            customerCount = customerCount,
            plannedRoutesCount = routeCount,
            sharedRoutes = sharedRoutes
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    init {
        viewModelScope.launch {
            // Carrega nome e e-mail do perfil para liberar somente os clientes atribuidos.
            sellerIdentity.value = runCatching { firebaseUserRepository.getCurrentSellerIdentity() }
                .getOrNull()
        }
    }
}

data class HomeUiState(
    val customerCount: Int = 0,
    val plannedRoutesCount: Int = 0,
    val sharedRoutes: List<SharedRouteAssignment> = emptyList(),
    val isSyncingCustomers: Boolean = false,
    val syncMessage: String? = null
) {
    /** Rotas que ainda precisam ser cumpridas ou retomadas pelo vendedor. */
    val activeSharedRoutes: List<SharedRouteAssignment>
        get() = sharedRoutes.filter { route ->
            route.status.lowercase() !in setOf("completed", "concluida", "not_completed", "nao_concluida")
        }

    val assignedStopsCount: Int
        get() = activeSharedRoutes.sumOf { it.stops.size }
}
