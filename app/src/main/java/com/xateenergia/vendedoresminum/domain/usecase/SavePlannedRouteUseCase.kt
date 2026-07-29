package com.xateenergia.vendedoresminum.domain.usecase

import com.xateenergia.vendedoresminum.data.entities.PlannedRouteEntity
import com.xateenergia.vendedoresminum.data.entities.PlannedRouteStopEntity
import com.xateenergia.vendedoresminum.data.repository.PlannedRouteRepository
import com.xateenergia.vendedoresminum.domain.model.Coordinate
import com.xateenergia.vendedoresminum.domain.model.NearbyCustomer
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SavePlannedRouteUseCase @Inject constructor(
    private val plannedRouteRepository: PlannedRouteRepository
) {
    suspend operator fun invoke(
        name: String,
        mainCustomerName: String?,
        origin: Coordinate,
        radiusKm: Double,
        orderedStops: List<NearbyCustomer>,
        routeDistanceMeters: Double? = null,
        routeDurationSeconds: Double? = null,
        startLocation: Coordinate? = null
    ): Long = withContext(Dispatchers.IO) {
        val route = PlannedRouteEntity(
            name = name,
            mainCustomerName = mainCustomerName,
            mainLatitude = origin.latitude,
            mainLongitude = origin.longitude,
            radiusKm = radiusKm
        )
        val stops = orderedStops.mapIndexed { index, stop ->
            PlannedRouteStopEntity(
                routeId = 0,
                customerId = stop.customer.id,
                orderIndex = index + 1,
                distanceMeters = stop.distanceMeters
            )
        }
        val routeId = plannedRouteRepository.saveRoute(route, stops)
        plannedRouteRepository.saveRouteToFirebase(
            localRouteId = routeId,
            route = route.copy(id = routeId),
            orderedStops = orderedStops,
            distanceMeters = routeDistanceMeters,
            durationSeconds = routeDurationSeconds,
            startLatitude = startLocation?.latitude,
            startLongitude = startLocation?.longitude
        )
        routeId
    }
}

