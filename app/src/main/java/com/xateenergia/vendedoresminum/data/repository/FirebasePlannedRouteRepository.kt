package com.xateenergia.vendedoresminum.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.xateenergia.vendedoresminum.data.entities.PlannedRouteEntity
import com.xateenergia.vendedoresminum.domain.model.Coordinate
import com.xateenergia.vendedoresminum.domain.model.Customer
import com.xateenergia.vendedoresminum.domain.model.FirebaseRouteStopSummary
import com.xateenergia.vendedoresminum.domain.model.FirebaseRouteSummary
import com.xateenergia.vendedoresminum.domain.model.NearbyCustomer
import com.xateenergia.vendedoresminum.domain.model.VisitEventDraft
import com.xateenergia.vendedoresminum.domain.model.VisitEventType
import com.xateenergia.vendedoresminum.utils.StateUtils
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Singleton
class FirebasePlannedRouteRepository @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
    private val firebaseAuth: FirebaseAuth,
    private val firebaseVisitEventRepository: FirebaseVisitEventRepository
) {
    /**
     * Observa somente as rotas do vendedor logado. A lista nao e espelhada do
     * Room para a tela: alteracoes e exclusoes feitas no backoffice chegam
     * imediatamente por este listener do Realtime Database.
     */
    fun observeRouteSummaries(): Flow<List<FirebaseRouteSummary>> = callbackFlow {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val query = firebaseDatabase.getReference("plannedRoutes")
            .orderByChild("sellerUid")
            .equalTo(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(
                    snapshot.children
                        .mapNotNull { it.toFirebaseRouteSummary() }
                        .sortedByDescending { it.createdAt }
                )
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    /** Observa as paradas de uma rota remota ja visivel para o vendedor. */
    fun observeStopSummaries(routeId: String): Flow<List<FirebaseRouteStopSummary>> = callbackFlow {
        if (routeId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val reference = firebaseDatabase.getReference("plannedRouteStops").child(routeId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(
                    snapshot.children
                        .mapNotNull { it.toFirebaseRouteStopSummary(routeId) }
                        .sortedBy { it.orderIndex }
                )
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        reference.addValueEventListener(listener)
        awaitClose { reference.removeEventListener(listener) }
    }

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

    /** Atualiza uma rota pelo identificador remoto usado pelo historico unificado. */
    suspend fun updateRemoteRouteCompletionStatus(
        routeId: String,
        isCompleted: Boolean,
        reason: String?
    ): Unit = withContext(Dispatchers.IO) {
        if (routeId.isBlank()) return@withContext
        val status = if (isCompleted) "completed" else "not_completed"
        firebaseDatabase.reference.updateChildren(
            mapOf(
                "plannedRoutes/$routeId/isCompleted" to isCompleted,
                "plannedRoutes/$routeId/status" to status,
                "plannedRoutes/$routeId/notCompletedReason" to reason,
                "plannedRoutes/$routeId/completedAt" to if (isCompleted) ServerValue.TIMESTAMP else null,
                "plannedRoutes/$routeId/updatedAt" to ServerValue.TIMESTAMP
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

private fun DataSnapshot.toFirebaseRouteSummary(): FirebaseRouteSummary? {
    val routeId = child("id").stringValue() ?: key ?: return null
    val status = child("status").stringValue() ?: "planned"
    return FirebaseRouteSummary(
        id = routeId,
        name = child("name").stringValue() ?: "Rota sem nome",
        mainCustomerName = child("mainCustomerName").stringValue(),
        radiusKm = child("radiusKm").doubleValue() ?: 0.0,
        createdAt = child("createdAtTimestamp").longValue()
            ?: child("createdAt").longValue()
            ?: 0L,
        isCompleted = child("isCompleted").booleanValue() ?: status == "completed",
        notCompletedReason = child("notCompletedReason").stringValue(),
        stopCount = child("stopCount").longValue()?.toInt() ?: 0,
        status = status,
        distanceMeters = child("distanceMeters").doubleValue()
            ?: child("estimatedDistanceMeters").doubleValue(),
        durationSeconds = child("durationSeconds").doubleValue()
            ?: child("estimatedDurationSeconds").doubleValue()
    )
}

private fun DataSnapshot.toFirebaseRouteStopSummary(routeId: String): FirebaseRouteStopSummary? {
    val stopId = child("id").stringValue() ?: key ?: return null
    val feedbackLocation = child("feedbackLocation")
    return FirebaseRouteStopSummary(
        routeId = routeId,
        customerId = child("customerId").stringValue() ?: stopId,
        orderIndex = child("order").longValue()?.toInt() ?: 0,
        distanceMeters = child("distanceMeters").doubleValue() ?: 0.0,
        visitStatus = child("status").stringValue()
            ?: child("result").stringValue()
            ?: "pending",
        feedback = child("feedback").stringValue(),
        feedbackAt = child("feedbackAt").longValue()
            ?: child("visitedAt").longValue(),
        feedbackLatitude = child("feedbackLatitude").doubleValue()
            ?: feedbackLocation.child("latitude").doubleValue(),
        feedbackLongitude = child("feedbackLongitude").doubleValue()
            ?: feedbackLocation.child("longitude").doubleValue(),
        customerName = child("customerName").stringValue(),
        companyName = child("clientName").stringValue(),
        phone = child("phone").stringValue()
    )
}

private fun DataSnapshot.stringValue(): String? = when (val raw = value) {
    is String -> raw.trim().takeIf { it.isNotBlank() }
    is Number, is Boolean -> raw.toString()
    else -> null
}

private fun DataSnapshot.doubleValue(): Double? = when (val raw = value) {
    is Number -> raw.toDouble()
    is String -> raw.replace(",", ".").toDoubleOrNull()
    else -> null
}

private fun DataSnapshot.longValue(): Long? = when (val raw = value) {
    is Number -> raw.toLong()
    is String -> raw.toLongOrNull()
    else -> null
}

private fun DataSnapshot.booleanValue(): Boolean? = when (val raw = value) {
    is Boolean -> raw
    is String -> raw.equals("true", ignoreCase = true)
    else -> null
}
