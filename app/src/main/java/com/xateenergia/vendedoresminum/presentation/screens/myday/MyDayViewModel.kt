package com.xateenergia.vendedoresminum.presentation.screens.myday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xateenergia.vendedoresminum.data.repository.FirebaseSharedRouteRepository
import com.xateenergia.vendedoresminum.domain.model.Customer
import com.xateenergia.vendedoresminum.domain.model.SharedRouteAssignment
import com.xateenergia.vendedoresminum.domain.model.SharedRouteStop
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Concentra a agenda operacional do vendedor. A caixa privada de rotas no
 * Firebase continua sendo a fonte de verdade para rotas e retornos agendados.
 */
@HiltViewModel
class MyDayViewModel @Inject constructor(
    firebaseSharedRouteRepository: FirebaseSharedRouteRepository
) : ViewModel() {

    val state: StateFlow<MyDayUiState> = firebaseSharedRouteRepository.observeAssignedRoutes()
        .map { routes ->
            MyDayUiState(
                routes = routes,
                today = LocalDate.now().toString(),
                isLoading = false
            )
        }
        .catch {
            emit(
                MyDayUiState(
                    today = LocalDate.now().toString(),
                    isLoading = false,
                    error = "Nao foi possivel atualizar sua agenda agora. Verifique a conexao e tente novamente."
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MyDayUiState(today = LocalDate.now().toString())
        )
}

data class MyDayUiState(
    val routes: List<SharedRouteAssignment> = emptyList(),
    val today: String,
    val isLoading: Boolean = true,
    val error: String? = null
) {
    /** Todas as rotas atribuídas, ordenadas pela urgencia da operacao. */
    val agendaRoutes: List<SharedRouteAssignment>
        get() = routes.sortedWith(
            compareBy<SharedRouteAssignment> { routePriority(it, today) }
                .thenBy { it.dueDate.orEmpty() }
                .thenBy { it.name.lowercase(Locale.getDefault()) }
        )

    val activeRoutes: List<SharedRouteAssignment>
        get() = agendaRoutes.filterNot { it.status.isFinishedRoute() }

    val priorityRoute: SharedRouteAssignment?
        get() = agendaRoutes.firstOrNull { it.status.isRouteInProgress() }
            ?: activeRoutes.firstOrNull()

    val totalStops: Int
        get() = activeRoutes.sumOf { it.stops.size }

    /** Inclui visita realizada e tentativa com feedback, pois ambas foram registradas. */
    val completedStops: Int
        get() = activeRoutes.sumOf { route ->
            route.stops.count { stop -> stop.status.hasRegisteredOutcome() }
        }

    val remainingStops: Int
        get() = (totalStops - completedStops).coerceAtLeast(0)

    /** Retornos de hoje e atrasados ficam juntos para nada importante se perder. */
    val revisitsDueToday: List<MyDayRevisit>
        get() = routes.flatMap { route ->
            route.stops.mapNotNull { stop ->
                val dueDate = stop.nextActionDueDate?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val nextAction = stop.nextAction?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                if (dueDate > today) return@mapNotNull null

                MyDayRevisit(
                    routeId = route.id,
                    routeName = route.name,
                    customer = stop.customer,
                    nextAction = nextAction,
                    dueDate = dueDate,
                    isOverdue = dueDate < today
                )
            }
        }.sortedWith(compareBy<MyDayRevisit> { it.dueDate }.thenBy { it.customer.name })

    fun timeInRouteSeconds(nowMillis: Long): Long {
        val route = priorityRoute ?: return 0L
        if (!route.status.isRouteInProgress()) return 0L
        val startedAt = route.startedAt ?: return 0L
        return ((nowMillis - startedAt) / 1_000L).coerceAtLeast(0L)
    }
}

data class MyDayRevisit(
    val routeId: String,
    val routeName: String,
    val customer: Customer,
    val nextAction: String,
    val dueDate: String,
    val isOverdue: Boolean
)

private fun routePriority(route: SharedRouteAssignment, today: String): Int = when {
    route.status.isRouteInProgress() -> 0
    !route.status.isFinishedRoute() && route.dueDate?.let { it < today } == true -> 1
    !route.status.isFinishedRoute() && route.dueDate == today -> 2
    !route.status.isFinishedRoute() -> 3
    else -> 4
}

private fun String.isRouteInProgress(): Boolean =
    lowercase(Locale.ROOT) == "in_progress"

private fun String.isFinishedRoute(): Boolean =
    lowercase(Locale.ROOT) in setOf("completed", "concluida", "not_completed", "nao_concluida")

private fun String.hasRegisteredOutcome(): Boolean =
    lowercase(Locale.ROOT) in setOf("visited", "not_visited")
