package com.xateenergia.vendedoresminum.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.xateenergia.vendedoresminum.domain.model.Coordinate
import com.xateenergia.vendedoresminum.domain.model.Customer
import com.xateenergia.vendedoresminum.domain.model.VisitEventDraft
import com.xateenergia.vendedoresminum.domain.model.VisitEventType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Persiste a telemetria de uma navegacao ativa sem alterar o fluxo comercial.
 * A rota guarda os agregados e visitEvents preserva uma linha do tempo auditavel.
 */
@Singleton
class FirebaseRouteTelemetryRepository @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
    private val firebaseAuth: FirebaseAuth,
    private val firebaseVisitEventRepository: FirebaseVisitEventRepository
) {
    /** Identificador remoto usado pelas rotas criadas diretamente no aplicativo. */
    fun ownedRouteId(localRouteId: Long): String? {
        return firebaseAuth.currentUser?.uid?.let { uid -> "${uid}_$localRouteId" }
    }

    suspend fun startSession(
        routeId: String,
        routeName: String,
        location: Coordinate,
        locationAccuracyMeters: Float?,
        plannedDistanceMeters: Double?,
        plannedDurationSeconds: Double?,
        stopCount: Int,
        startedAtClient: Long
    ): Unit = withContext(Dispatchers.IO) {
        val updates = mutableMapOf<String, Any?>(
            "plannedRoutes/$routeId/telemetryStatus" to "active",
            "plannedRoutes/$routeId/telemetryStartedAt" to ServerValue.TIMESTAMP,
            "plannedRoutes/$routeId/telemetryStartedAtClient" to startedAtClient,
            "plannedRoutes/$routeId/plannedDistanceMeters" to plannedDistanceMeters,
            "plannedRoutes/$routeId/plannedDurationSeconds" to plannedDurationSeconds,
            // Mantem o tamanho original da rota para que os indicadores possam
            // distinguir uma rota sem paradas de uma rota que ainda nao foi executada.
            "plannedRoutes/$routeId/telemetryStopCount" to stopCount,
            "plannedRoutes/$routeId/telemetryVersion" to 1,
            "plannedRoutes/$routeId/actualDistanceMeters" to 0.0,
            "plannedRoutes/$routeId/actualDurationSeconds" to 0,
            "plannedRoutes/$routeId/movingDurationSeconds" to 0,
            "plannedRoutes/$routeId/stoppedDurationSeconds" to 0,
            "plannedRoutes/$routeId/locationSampleCount" to 1,
            "plannedRoutes/$routeId/lastLocation" to location.toFirebaseLocation(locationAccuracyMeters),
            "plannedRoutes/$routeId/lastLocationAt" to ServerValue.TIMESTAMP,
            "plannedRoutes/$routeId/updatedAt" to ServerValue.TIMESTAMP
        )
        updates.putAll(
            firebaseVisitEventRepository.eventUpdates(
                listOf(
                    routeEvent(
                        routeId = routeId,
                        routeName = routeName,
                        type = VisitEventType.ROUTE_STARTED,
                        location = location,
                        locationAccuracyMeters = locationAccuracyMeters,
                        plannedDistanceMeters = plannedDistanceMeters,
                        plannedDurationSeconds = plannedDurationSeconds,
                        actualDistanceMeters = 0.0,
                        actualDurationSeconds = 0,
                        movingDurationSeconds = 0,
                        stoppedDurationSeconds = 0,
                        locationSampleCount = 1
                    )
                )
            )
        )
        firebaseDatabase.reference.updateChildren(updates).await()
    }

    suspend fun recordProgress(
        routeId: String,
        routeName: String,
        location: Coordinate,
        locationAccuracyMeters: Float?,
        actualDistanceMeters: Double,
        actualDurationSeconds: Long,
        movingDurationSeconds: Long,
        stoppedDurationSeconds: Long,
        locationSampleCount: Int,
        capturedAtClient: Long
    ): Unit = withContext(Dispatchers.IO) {
        val updates = mutableMapOf<String, Any?>(
            "plannedRoutes/$routeId/actualDistanceMeters" to actualDistanceMeters,
            "plannedRoutes/$routeId/actualDurationSeconds" to actualDurationSeconds,
            "plannedRoutes/$routeId/movingDurationSeconds" to movingDurationSeconds,
            "plannedRoutes/$routeId/stoppedDurationSeconds" to stoppedDurationSeconds,
            "plannedRoutes/$routeId/locationSampleCount" to locationSampleCount,
            "plannedRoutes/$routeId/lastLocation" to location.toFirebaseLocation(locationAccuracyMeters),
            "plannedRoutes/$routeId/lastLocationAt" to ServerValue.TIMESTAMP,
            "plannedRoutes/$routeId/lastLocationAtClient" to capturedAtClient,
            "plannedRoutes/$routeId/updatedAt" to ServerValue.TIMESTAMP
        )
        updates.putAll(
            firebaseVisitEventRepository.eventUpdates(
                listOf(
                    routeEvent(
                        routeId = routeId,
                        routeName = routeName,
                        type = VisitEventType.ROUTE_PROGRESS,
                        location = location,
                        locationAccuracyMeters = locationAccuracyMeters,
                        actualDistanceMeters = actualDistanceMeters,
                        actualDurationSeconds = actualDurationSeconds,
                        movingDurationSeconds = movingDurationSeconds,
                        stoppedDurationSeconds = stoppedDurationSeconds,
                        locationSampleCount = locationSampleCount
                    )
                )
            )
        )
        firebaseDatabase.reference.updateChildren(updates).await()
    }

    suspend fun recordStopArrival(
        routeId: String,
        stopId: String,
        customer: Customer,
        location: Coordinate,
        locationAccuracyMeters: Float?,
        distanceToCustomerMeters: Double,
        arrivedAtClient: Long
    ): Unit = withContext(Dispatchers.IO) {
        val stopPath = "plannedRouteStops/$routeId/$stopId"
        val updates = mutableMapOf<String, Any?>(
            "$stopPath/arrivedAt" to ServerValue.TIMESTAMP,
            "$stopPath/arrivedAtClient" to arrivedAtClient,
            "$stopPath/arrivalLocation" to location.toFirebaseLocation(locationAccuracyMeters),
            "$stopPath/arrivalAccuracyMeters" to locationAccuracyMeters,
            "$stopPath/arrivalDistanceToCustomerMeters" to distanceToCustomerMeters,
            "$stopPath/updatedAt" to ServerValue.TIMESTAMP,
            "plannedRoutes/$routeId/updatedAt" to ServerValue.TIMESTAMP
        )
        updates.putAll(
            firebaseVisitEventRepository.eventUpdates(
                listOf(
                    VisitEventDraft(
                        routeId = routeId,
                        stopId = stopId,
                        customer = customer,
                        type = VisitEventType.STOP_ARRIVED,
                        visitStatus = "arrived",
                        location = location,
                        locationAccuracyMeters = locationAccuracyMeters,
                        distanceToCustomerMeters = distanceToCustomerMeters,
                        locationSource = "gps"
                    )
                )
            )
        )
        firebaseDatabase.reference.updateChildren(updates).await()
    }

    suspend fun recordStopDeparture(
        routeId: String,
        stopId: String,
        customer: Customer,
        location: Coordinate,
        locationAccuracyMeters: Float?,
        distanceToCustomerMeters: Double,
        visitDurationSeconds: Long,
        departedAtClient: Long
    ): Unit = withContext(Dispatchers.IO) {
        val stopPath = "plannedRouteStops/$routeId/$stopId"
        val updates = mutableMapOf<String, Any?>(
            "$stopPath/departedAt" to ServerValue.TIMESTAMP,
            "$stopPath/departedAtClient" to departedAtClient,
            "$stopPath/departureLocation" to location.toFirebaseLocation(locationAccuracyMeters),
            "$stopPath/departureAccuracyMeters" to locationAccuracyMeters,
            "$stopPath/departureDistanceToCustomerMeters" to distanceToCustomerMeters,
            "$stopPath/visitDurationSeconds" to visitDurationSeconds,
            "$stopPath/updatedAt" to ServerValue.TIMESTAMP,
            "plannedRoutes/$routeId/updatedAt" to ServerValue.TIMESTAMP
        )
        updates.putAll(
            firebaseVisitEventRepository.eventUpdates(
                listOf(
                    VisitEventDraft(
                        routeId = routeId,
                        stopId = stopId,
                        customer = customer,
                        type = VisitEventType.STOP_DEPARTED,
                        visitStatus = "departed",
                        location = location,
                        locationAccuracyMeters = locationAccuracyMeters,
                        distanceToCustomerMeters = distanceToCustomerMeters,
                        visitDurationSeconds = visitDurationSeconds,
                        locationSource = "gps"
                    )
                )
            )
        )
        firebaseDatabase.reference.updateChildren(updates).await()
    }

    suspend fun finishSession(
        routeId: String,
        routeName: String,
        location: Coordinate?,
        locationAccuracyMeters: Float?,
        actualDistanceMeters: Double,
        actualDurationSeconds: Long,
        movingDurationSeconds: Long,
        stoppedDurationSeconds: Long,
        locationSampleCount: Int,
        completionPercent: Int,
        finishedAtClient: Long
    ): Unit = withContext(Dispatchers.IO) {
        val updates = mutableMapOf<String, Any?>(
            "plannedRoutes/$routeId/telemetryStatus" to "ended",
            "plannedRoutes/$routeId/telemetryFinishedAt" to ServerValue.TIMESTAMP,
            "plannedRoutes/$routeId/telemetryFinishedAtClient" to finishedAtClient,
            "plannedRoutes/$routeId/actualDistanceMeters" to actualDistanceMeters,
            "plannedRoutes/$routeId/actualDurationSeconds" to actualDurationSeconds,
            "plannedRoutes/$routeId/movingDurationSeconds" to movingDurationSeconds,
            "plannedRoutes/$routeId/stoppedDurationSeconds" to stoppedDurationSeconds,
            "plannedRoutes/$routeId/locationSampleCount" to locationSampleCount,
            "plannedRoutes/$routeId/completionPercent" to completionPercent,
            "plannedRoutes/$routeId/updatedAt" to ServerValue.TIMESTAMP
        )
        location?.let { currentLocation ->
            updates["plannedRoutes/$routeId/lastLocation"] = currentLocation.toFirebaseLocation(locationAccuracyMeters)
            updates["plannedRoutes/$routeId/lastLocationAt"] = ServerValue.TIMESTAMP
            updates.putAll(
                firebaseVisitEventRepository.eventUpdates(
                    listOf(
                        routeEvent(
                            routeId = routeId,
                            routeName = routeName,
                            type = VisitEventType.ROUTE_FINISHED,
                            location = currentLocation,
                            locationAccuracyMeters = locationAccuracyMeters,
                            actualDistanceMeters = actualDistanceMeters,
                            actualDurationSeconds = actualDurationSeconds,
                            movingDurationSeconds = movingDurationSeconds,
                            stoppedDurationSeconds = stoppedDurationSeconds,
                            locationSampleCount = locationSampleCount
                        )
                    )
                )
            )
        }
        firebaseDatabase.reference.updateChildren(updates).await()
    }

    private fun routeEvent(
        routeId: String,
        routeName: String,
        type: VisitEventType,
        location: Coordinate,
        locationAccuracyMeters: Float?,
        plannedDistanceMeters: Double? = null,
        plannedDurationSeconds: Double? = null,
        actualDistanceMeters: Double? = null,
        actualDurationSeconds: Long? = null,
        movingDurationSeconds: Long? = null,
        stoppedDurationSeconds: Long? = null,
        locationSampleCount: Int? = null
    ): VisitEventDraft {
        return VisitEventDraft(
            routeId = routeId,
            stopId = ROUTE_EVENT_STOP_ID,
            customer = Customer(
                id = 0L,
                name = routeName.ifBlank { "Rota" },
                latitude = location.latitude,
                longitude = location.longitude
            ),
            type = type,
            location = location,
            locationAccuracyMeters = locationAccuracyMeters,
            plannedDistanceMeters = plannedDistanceMeters,
            plannedDurationSeconds = plannedDurationSeconds,
            actualDistanceMeters = actualDistanceMeters,
            actualDurationSeconds = actualDurationSeconds,
            movingDurationSeconds = movingDurationSeconds,
            stoppedDurationSeconds = stoppedDurationSeconds,
            locationSampleCount = locationSampleCount,
            locationSource = "gps"
        )
    }

    private fun Coordinate.toFirebaseLocation(accuracyMeters: Float?): Map<String, Any?> {
        return mapOf(
            "latitude" to latitude,
            "longitude" to longitude,
            "accuracyMeters" to accuracyMeters
        )
    }

    private companion object {
        const val ROUTE_EVENT_STOP_ID = "route_session"
    }
}
