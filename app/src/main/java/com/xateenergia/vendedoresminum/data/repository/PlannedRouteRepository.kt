package com.xateenergia.vendedoresminum.data.repository

import com.xateenergia.vendedoresminum.data.dao.PlannedRouteDao
import com.xateenergia.vendedoresminum.data.entities.PlannedRouteEntity
import com.xateenergia.vendedoresminum.data.entities.PlannedRouteStopEntity
import com.xateenergia.vendedoresminum.domain.model.PlannedRouteSummary
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class PlannedRouteRepository @Inject constructor(
    private val plannedRouteDao: PlannedRouteDao,
    private val firebasePlannedRouteRepository: FirebasePlannedRouteRepository
) {
    fun observeSummaries(): Flow<List<PlannedRouteSummary>> = plannedRouteDao.observeSummaries()

    suspend fun saveRoute(route: PlannedRouteEntity, stops: List<PlannedRouteStopEntity>): Long {
        return plannedRouteDao.saveRoute(route, stops)
    }

    suspend fun saveRouteToFirebase(
        localRouteId: Long,
        route: PlannedRouteEntity,
        orderedStops: List<com.xateenergia.vendedoresminum.domain.model.NearbyCustomer>,
        distanceMeters: Double?,
        durationSeconds: Double?,
        startLatitude: Double?,
        startLongitude: Double?
    ) {
        firebasePlannedRouteRepository.savePlannedRoute(
            localRouteId = localRouteId,
            route = route,
            orderedStops = orderedStops,
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
            startLatitude = startLatitude,
            startLongitude = startLongitude
        )
    }

    suspend fun updateRouteCompletionStatus(routeId: Long, isCompleted: Boolean, reason: String?) {
        plannedRouteDao.updateRouteCompletionStatus(routeId, isCompleted, reason)
        firebasePlannedRouteRepository.updateRouteCompletionStatus(routeId, isCompleted, reason)
    }

    suspend fun updateRouteNavigationStatus(
        routeId: Long,
        status: String,
        isCompleted: Boolean = false,
        reason: String? = null
    ) {
        if (isCompleted || status == "completed" || status == "not_completed") {
            plannedRouteDao.updateRouteCompletionStatus(routeId, isCompleted, reason)
        }
        firebasePlannedRouteRepository.updateRouteNavigationStatus(routeId, status, isCompleted, reason)
    }

    suspend fun deleteAll() {
        plannedRouteDao.deleteAll()
    }
}

