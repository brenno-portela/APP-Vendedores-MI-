package com.xateenergia.vendedoresminum.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.xateenergia.vendedoresminum.data.entities.PlannedRouteEntity
import com.xateenergia.vendedoresminum.domain.model.Coordinate
import com.xateenergia.vendedoresminum.domain.model.Customer
import com.xateenergia.vendedoresminum.domain.model.NearbyCustomer
import com.xateenergia.vendedoresminum.domain.model.VisitEventDraft
import com.xateenergia.vendedoresminum.domain.model.VisitEventType
import com.xateenergia.vendedoresminum.utils.StateUtils
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Singleton
class FirebasePlannedRouteRepository @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
    private val firebaseAuth: FirebaseAuth,
    private val firebaseVisitEventRepository: FirebaseVisitEventRepository
) {
    suspend fun savePlannedRoute(
        localRouteId: Long,
        route: PlannedRouteEntity,
        orderedStops: List<NearbyCustomer>,
        distanceMeters: Double?,
        durationSeconds: Double?,
        startLatitude: Double?,
        startLongitude: Double?
    ): Unit = withContext(Dispatchers.IO) {
        val currentUser = firebaseAuth.currentUser ?: return@withContext
        val userSnapshot = firebaseDatabase.getReference("users").child(currentUser.uid).get().await()
        val sellerName = userSnapshot.child("name").getValue(String::class.java)
            ?: userSnapshot.child("email").getValue(String::class.java)
            ?: currentUser.email
            ?: currentUser.uid
        val sellerState = StateUtils.normalizeUf(userSnapshot.child("state").getValue(String::class.java))

        val firebaseRouteId = firebaseRouteId(currentUser.uid, localRouteId)
        val updates = mutableMapOf<String, Any?>()

        updates["plannedRoutes/$firebaseRouteId"] = mapOf(
            "id" to firebaseRouteId,
            "localRouteId" to localRouteId,
            "name" to route.name,
            "mainCustomerName" to route.mainCustomerName,
            "sellerUid" to currentUser.uid,
            "sellerName" to sellerName,
            "sellerEmail" to currentUser.email,
            "state" to sellerState,
            "status" to "planned",
            "isCompleted" to false,
            "notCompletedReason" to null,
            "stopCount" to orderedStops.size,
            "radiusKm" to route.radiusKm,
            "distanceMeters" to distanceMeters,
            "durationSeconds" to durationSeconds,
            "origin" to mapOf(
                "latitude" to route.mainLatitude,
                "longitude" to route.mainLongitude
            ),
            "startLocation" to mapOf(
                "latitude" to startLatitude,
                "longitude" to startLongitude
            ),
            "createdAt" to route.createdAt,
            "createdAtTimestamp" to ServerValue.TIMESTAMP,
            "updatedAt" to ServerValue.TIMESTAMP
        )

        orderedStops.forEachIndexed { index, stop ->
            val stopId = stop.customer.externalId
                ?.takeIf { it.isNotBlank() }
                ?: stop.customer.id.toString()

            updates["plannedRouteStops/$firebaseRouteId/$stopId"] = mapOf(
                "id" to stopId,
                "routeId" to firebaseRouteId,
                "customerId" to stop.customer.id,
                "customerExternalId" to stop.customer.externalId,
                "customerName" to stop.customer.name,
                "order" to index + 1,
                "distanceMeters" to stop.distanceMeters,
                "status" to "planned",
                "state" to stop.customer.state,
                "city" to stop.customer.city,
                "latitude" to stop.customer.latitude,
                "longitude" to stop.customer.longitude,
                "timestamp" to ServerValue.TIMESTAMP
            )
        }

        firebaseDatabase.reference.updateChildren(updates).await()
    }

    suspend fun updateRouteCompletionStatus(
        localRouteId: Long,
        isCompleted: Boolean,
        reason: String?
    ): Unit = withContext(Dispatchers.IO) {
        val uid = firebaseAuth.currentUser?.uid ?: return@withContext
        val firebaseRouteId = firebaseRouteId(uid, localRouteId)
        val status = if (isCompleted) "completed" else "not_completed"
        firebaseDatabase.reference.updateChildren(
            mapOf(
                "plannedRoutes/$firebaseRouteId/isCompleted" to isCompleted,
                "plannedRoutes/$firebaseRouteId/status" to status,
                "plannedRoutes/$firebaseRouteId/notCompletedReason" to reason,
                "plannedRoutes/$firebaseRouteId/completedAt" to if (isCompleted) ServerValue.TIMESTAMP else null,
                "plannedRoutes/$firebaseRouteId/updatedAt" to ServerValue.TIMESTAMP
            )
        ).await()
    }

    suspend fun saveStopFeedback(
        localRouteId: Long,
        customer: Customer,
        wasVisited: Boolean,
        feedback: String,
        location: Coordinate,
        locationAccuracyMeters: Float?,
        distanceToCustomerMeters: Double,
        notVisitedReason: String?,
        commercialOutcome: String?,
        nextAction: String?,
        nextActionDueDate: String?
    ): Unit = withContext(Dispatchers.IO) {
        val uid = firebaseAuth.currentUser?.uid ?: return@withContext
        val firebaseRouteId = firebaseRouteId(uid, localRouteId)
        val stopId = customer.firebaseStopId()
        val stopPath = "plannedRouteStops/$firebaseRouteId/$stopId"
        val visitStatus = if (wasVisited) "visited" else "not_visited"

        // O snapshot da parada e os eventos sao gravados no mesmo update atomico.
        val updates = mutableMapOf<String, Any?>(
                "$stopPath/status" to visitStatus,
                "$stopPath/result" to visitStatus,
                "$stopPath/wasVisited" to wasVisited,
                "$stopPath/feedback" to feedback,
                "$stopPath/notVisitedReason" to notVisitedReason,
                "$stopPath/commercialOutcome" to commercialOutcome,
                "$stopPath/nextAction" to nextAction,
                "$stopPath/nextActionDueDate" to nextActionDueDate,
                "$stopPath/feedbackAt" to ServerValue.TIMESTAMP,
                "$stopPath/visitedAt" to ServerValue.TIMESTAMP,
                "$stopPath/feedbackLatitude" to location.latitude,
                "$stopPath/feedbackLongitude" to location.longitude,
                "$stopPath/feedbackAccuracyMeters" to locationAccuracyMeters,
                "$stopPath/feedbackDistanceToCustomerMeters" to distanceToCustomerMeters,
                "$stopPath/feedbackLocation" to mapOf(
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "accuracyMeters" to locationAccuracyMeters
                ),
                "$stopPath/checkOutAt" to if (wasVisited) ServerValue.TIMESTAMP else null,
                "$stopPath/updatedAt" to ServerValue.TIMESTAMP,
                "plannedRoutes/$firebaseRouteId/updatedAt" to ServerValue.TIMESTAMP
            )
        val feedbackEvent = VisitEventDraft(
            routeId = firebaseRouteId,
            stopId = stopId,
            customer = customer,
            type = VisitEventType.FEEDBACK_SUBMITTED,
            visitStatus = visitStatus,
            feedback = feedback,
            notVisitedReason = notVisitedReason,
            commercialOutcome = commercialOutcome,
            nextAction = nextAction,
            nextActionDueDate = nextActionDueDate,
            location = location,
            locationAccuracyMeters = locationAccuracyMeters,
            distanceToCustomerMeters = distanceToCustomerMeters
        )
        val events = buildList {
            add(feedbackEvent)
            if (wasVisited) add(feedbackEvent.copy(type = VisitEventType.CHECK_OUT))
        }
        updates.putAll(firebaseVisitEventRepository.eventUpdates(events))
        firebaseDatabase.reference.updateChildren(updates).await()
    }

    suspend fun recordStopCheckIn(
        localRouteId: Long,
        customer: Customer,
        location: Coordinate,
        locationAccuracyMeters: Float?,
        distanceToCustomerMeters: Double
    ): Unit = withContext(Dispatchers.IO) {
        val uid = firebaseAuth.currentUser?.uid ?: error("Faca login novamente para iniciar a visita.")
        val firebaseRouteId = firebaseRouteId(uid, localRouteId)
        val stopId = customer.firebaseStopId()
        val stopPath = "plannedRouteStops/$firebaseRouteId/$stopId"
        val event = VisitEventDraft(
            routeId = firebaseRouteId,
            stopId = stopId,
            customer = customer,
            type = VisitEventType.CHECK_IN,
            visitStatus = "in_progress",
            location = location,
            locationAccuracyMeters = locationAccuracyMeters,
            distanceToCustomerMeters = distanceToCustomerMeters
        )
        val updates = mutableMapOf<String, Any?>(
            "$stopPath/checkInAt" to ServerValue.TIMESTAMP,
            "$stopPath/checkInLocation" to mapOf(
                "latitude" to location.latitude,
                "longitude" to location.longitude,
                "accuracyMeters" to locationAccuracyMeters
            ),
            "$stopPath/checkInDistanceToCustomerMeters" to distanceToCustomerMeters,
            "$stopPath/updatedAt" to ServerValue.TIMESTAMP,
            "plannedRoutes/$firebaseRouteId/updatedAt" to ServerValue.TIMESTAMP
        )
        updates.putAll(firebaseVisitEventRepository.eventUpdates(listOf(event)))
        firebaseDatabase.reference.updateChildren(updates).await()
    }

    suspend fun updateRouteNavigationStatus(
        localRouteId: Long,
        status: String,
        isCompleted: Boolean = false,
        reason: String? = null
    ): Unit = withContext(Dispatchers.IO) {
        val uid = firebaseAuth.currentUser?.uid ?: return@withContext
        val firebaseRouteId = firebaseRouteId(uid, localRouteId)
        val updates = mutableMapOf<String, Any?>(
            "plannedRoutes/$firebaseRouteId/status" to status,
            "plannedRoutes/$firebaseRouteId/isCompleted" to isCompleted,
            "plannedRoutes/$firebaseRouteId/notCompletedReason" to reason,
            "plannedRoutes/$firebaseRouteId/updatedAt" to ServerValue.TIMESTAMP
        )

        if (status == "in_progress" || status == "em andamento") {
            updates["plannedRoutes/$firebaseRouteId/startedAt"] = ServerValue.TIMESTAMP
        }
        if (isCompleted) {
            updates["plannedRoutes/$firebaseRouteId/completedAt"] = ServerValue.TIMESTAMP
        }

        firebaseDatabase.reference.updateChildren(updates).await()
    }

    private fun firebaseRouteId(uid: String, localRouteId: Long): String {
        return "${uid}_$localRouteId"
    }

    private fun Customer.firebaseStopId(): String {
        return externalId?.takeIf { it.isNotBlank() } ?: id.toString()
    }
}
