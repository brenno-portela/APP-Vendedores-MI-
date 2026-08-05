package com.xateenergia.vendedoresminum.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.xateenergia.vendedoresminum.domain.model.Coordinate
import com.xateenergia.vendedoresminum.domain.model.Customer
import com.xateenergia.vendedoresminum.domain.model.SharedRouteAssignment
import com.xateenergia.vendedoresminum.domain.model.SharedRouteStop
import com.xateenergia.vendedoresminum.domain.model.VisitEventDraft
import com.xateenergia.vendedoresminum.domain.model.VisitEventType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Mantem a caixa de entrada de rotas que o administrador atribuiu ao vendedor.
 * A copia em sharedRoutesBySeller permite leitura restrita por usuario, enquanto
 * plannedRoutes e plannedRouteStops continuam sendo a fonte do historico do backoffice.
 */
@Singleton
class FirebaseSharedRouteRepository @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
    private val firebaseAuth: FirebaseAuth,
    private val firebaseVisitEventRepository: FirebaseVisitEventRepository
) {
    fun observeAssignedRoutes(): Flow<List<SharedRouteAssignment>> = callbackFlow {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val reference = firebaseDatabase.getReference("sharedRoutesBySeller").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(
                    snapshot.children
                        .mapNotNull { it.toSharedRouteAssignment() }
                        .sortedWith(compareBy<SharedRouteAssignment> { it.dueDate.orEmpty() }.thenBy { it.name })
                )
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        reference.addValueEventListener(listener)
        awaitClose { reference.removeEventListener(listener) }
    }

    suspend fun getAssignedRoute(routeId: String): SharedRouteAssignment? = withContext(Dispatchers.IO) {
        val uid = firebaseAuth.currentUser?.uid ?: return@withContext null
        firebaseDatabase.getReference("sharedRoutesBySeller").child(uid).child(routeId).get().await()
            .toSharedRouteAssignment()
    }

    suspend fun markNavigationStarted(routeId: String): Unit = updateRouteStatus(
        routeId = routeId,
        status = "in_progress",
        isCompleted = false,
        reason = null
    )

    suspend fun completeRoute(routeId: String): Unit = updateRouteStatus(
        routeId = routeId,
        status = "completed",
        isCompleted = true,
        reason = null
    )

    suspend fun saveStopFeedback(
        routeId: String,
        stopId: String,
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
        val uid = firebaseAuth.currentUser?.uid ?: error("Faca login novamente para registrar a visita.")
        val status = if (wasVisited) "visited" else "not_visited"
        val canonicalStopPath = "plannedRouteStops/$routeId/$stopId"
        val sellerStopPath = "sharedRoutesBySeller/$uid/$routeId/stops/$stopId"

        val updates = mutableMapOf<String, Any?>(
                "$canonicalStopPath/status" to status,
                "$canonicalStopPath/result" to status,
                "$canonicalStopPath/wasVisited" to wasVisited,
                "$canonicalStopPath/feedback" to feedback,
                "$canonicalStopPath/notVisitedReason" to notVisitedReason,
                "$canonicalStopPath/commercialOutcome" to commercialOutcome,
                "$canonicalStopPath/nextAction" to nextAction,
                "$canonicalStopPath/nextActionDueDate" to nextActionDueDate,
                "$canonicalStopPath/feedbackAt" to ServerValue.TIMESTAMP,
                "$canonicalStopPath/visitedAt" to ServerValue.TIMESTAMP,
                "$canonicalStopPath/feedbackLatitude" to location.latitude,
                "$canonicalStopPath/feedbackLongitude" to location.longitude,
                "$canonicalStopPath/feedbackAccuracyMeters" to locationAccuracyMeters,
                "$canonicalStopPath/feedbackDistanceToCustomerMeters" to distanceToCustomerMeters,
                "$canonicalStopPath/feedbackLocation" to mapOf(
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "accuracyMeters" to locationAccuracyMeters
                ),
                "$canonicalStopPath/checkOutAt" to if (wasVisited) ServerValue.TIMESTAMP else null,
                "$canonicalStopPath/updatedAt" to ServerValue.TIMESTAMP,
                "$sellerStopPath/status" to status,
                "$sellerStopPath/result" to status,
                "$sellerStopPath/wasVisited" to wasVisited,
                "$sellerStopPath/feedback" to feedback,
                "$sellerStopPath/notVisitedReason" to notVisitedReason,
                "$sellerStopPath/commercialOutcome" to commercialOutcome,
                "$sellerStopPath/nextAction" to nextAction,
                "$sellerStopPath/nextActionDueDate" to nextActionDueDate,
                "$sellerStopPath/feedbackAt" to ServerValue.TIMESTAMP,
                "$sellerStopPath/visitedAt" to ServerValue.TIMESTAMP,
                "$sellerStopPath/feedbackLatitude" to location.latitude,
                "$sellerStopPath/feedbackLongitude" to location.longitude,
                "$sellerStopPath/feedbackAccuracyMeters" to locationAccuracyMeters,
                "$sellerStopPath/feedbackDistanceToCustomerMeters" to distanceToCustomerMeters,
                "$sellerStopPath/feedbackLocation" to mapOf(
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "accuracyMeters" to locationAccuracyMeters
                ),
                "$sellerStopPath/checkOutAt" to if (wasVisited) ServerValue.TIMESTAMP else null,
                "$sellerStopPath/updatedAt" to ServerValue.TIMESTAMP,
                "plannedRoutes/$routeId/updatedAt" to ServerValue.TIMESTAMP,
                "sharedRoutesBySeller/$uid/$routeId/updatedAt" to ServerValue.TIMESTAMP
            )
        val feedbackEvent = VisitEventDraft(
            routeId = routeId,
            stopId = stopId,
            customer = customer,
            type = VisitEventType.FEEDBACK_SUBMITTED,
            visitStatus = status,
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
        routeId: String,
        stopId: String,
        customer: Customer,
        location: Coordinate,
        locationAccuracyMeters: Float?,
        distanceToCustomerMeters: Double
    ): Unit = withContext(Dispatchers.IO) {
        val uid = firebaseAuth.currentUser?.uid ?: error("Faca login novamente para iniciar a visita.")
        val canonicalStopPath = "plannedRouteStops/$routeId/$stopId"
        val sellerStopPath = "sharedRoutesBySeller/$uid/$routeId/stops/$stopId"
        val event = VisitEventDraft(
            routeId = routeId,
            stopId = stopId,
            customer = customer,
            type = VisitEventType.CHECK_IN,
            visitStatus = "in_progress",
            location = location,
            locationAccuracyMeters = locationAccuracyMeters,
            distanceToCustomerMeters = distanceToCustomerMeters
        )
        val updates = mutableMapOf<String, Any?>(
            "$canonicalStopPath/checkInAt" to ServerValue.TIMESTAMP,
            "$canonicalStopPath/checkInLocation" to mapOf(
                "latitude" to location.latitude,
                "longitude" to location.longitude,
                "accuracyMeters" to locationAccuracyMeters
            ),
            "$canonicalStopPath/checkInDistanceToCustomerMeters" to distanceToCustomerMeters,
            "$canonicalStopPath/updatedAt" to ServerValue.TIMESTAMP,
            "$sellerStopPath/checkInAt" to ServerValue.TIMESTAMP,
            "$sellerStopPath/checkInLocation" to mapOf(
                "latitude" to location.latitude,
                "longitude" to location.longitude,
                "accuracyMeters" to locationAccuracyMeters
            ),
            "$sellerStopPath/checkInDistanceToCustomerMeters" to distanceToCustomerMeters,
            "$sellerStopPath/updatedAt" to ServerValue.TIMESTAMP,
            "plannedRoutes/$routeId/updatedAt" to ServerValue.TIMESTAMP,
            "sharedRoutesBySeller/$uid/$routeId/updatedAt" to ServerValue.TIMESTAMP
        )
        updates.putAll(firebaseVisitEventRepository.eventUpdates(listOf(event)))
        firebaseDatabase.reference.updateChildren(updates).await()
    }

    private suspend fun updateRouteStatus(
        routeId: String,
        status: String,
        isCompleted: Boolean,
        reason: String?
    ): Unit = withContext(Dispatchers.IO) {
        val uid = firebaseAuth.currentUser?.uid ?: error("Faca login novamente para iniciar a rota.")
        val updates = mutableMapOf<String, Any?>(
            "plannedRoutes/$routeId/status" to status,
            "plannedRoutes/$routeId/isCompleted" to isCompleted,
            "plannedRoutes/$routeId/notCompletedReason" to reason,
            "plannedRoutes/$routeId/updatedAt" to ServerValue.TIMESTAMP,
            "sharedRoutesBySeller/$uid/$routeId/status" to status,
            "sharedRoutesBySeller/$uid/$routeId/isCompleted" to isCompleted,
            "sharedRoutesBySeller/$uid/$routeId/updatedAt" to ServerValue.TIMESTAMP
        )
        if (status == "in_progress") {
            updates["plannedRoutes/$routeId/startedAt"] = ServerValue.TIMESTAMP
            updates["sharedRoutesBySeller/$uid/$routeId/startedAt"] = ServerValue.TIMESTAMP
        }
        if (isCompleted) {
            updates["plannedRoutes/$routeId/completedAt"] = ServerValue.TIMESTAMP
            updates["sharedRoutesBySeller/$uid/$routeId/completedAt"] = ServerValue.TIMESTAMP
        }
        firebaseDatabase.reference.updateChildren(updates).await()
    }
}

private fun DataSnapshot.toSharedRouteAssignment(): SharedRouteAssignment? {
    val routeId = child("id").stringValue() ?: key ?: return null
    val stops = child("stops").children
        .mapNotNull { it.toSharedRouteStop() }
        .sortedBy { it.order }

    return SharedRouteAssignment(
        id = routeId,
        name = child("name").stringValue() ?: "Rota compartilhada",
        sellerUid = child("sellerUid").stringValue().orEmpty(),
        sellerName = child("sellerName").stringValue(),
        state = child("state").stringValue(),
        dueDate = child("dueDate").stringValue(),
        targetCompletionPercent = child("targetCompletionPercent").intValue() ?: 90,
        notes = child("assignmentNotes").stringValue() ?: child("notes").stringValue(),
        estimatedDistanceMeters = child("estimatedDistanceMeters").doubleValue(),
        estimatedDurationSeconds = child("estimatedDurationSeconds").doubleValue(),
        status = child("status").stringValue() ?: "assigned",
        stops = stops
    )
}

private fun DataSnapshot.toSharedRouteStop(): SharedRouteStop? {
    val latitude = child("latitude").doubleValue() ?: return null
    val longitude = child("longitude").doubleValue() ?: return null
    val stopId = child("id").stringValue() ?: key ?: return null
    val externalId = child("customerExternalId").stringValue()
        ?: child("customerId").stringValue()
        ?: stopId

    return SharedRouteStop(
        id = stopId,
        order = child("order").intValue() ?: 0,
        status = child("status").stringValue() ?: "assigned",
        customer = Customer(
            id = child("customerId").longValue() ?: stableCustomerId(externalId),
            name = child("customerName").stringValue() ?: "Cliente sem nome",
            address = child("address").stringValue() ?: child("dealAddress").stringValue(),
            city = child("city").stringValue(),
            state = child("state").stringValue(),
            latitude = latitude,
            longitude = longitude,
            phone = child("phone").stringValue(),
            segment = child("segment").stringValue(),
            status = child("pipelineStage").stringValue() ?: child("status").stringValue(),
            notes = child("notes").stringValue(),
            opportunity = child("opportunity").stringValue(),
            cnpjCpf = child("cnpjCpf").stringValue() ?: child("cpfCnpj").stringValue(),
            externalId = externalId,
            email = child("email").stringValue(),
            responsavel = child("responsavel").stringValue(),
            expectedRevenue = child("expectedRevenue").stringValue(),
            clientName = child("clientName").stringValue()
        )
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

private fun DataSnapshot.intValue(): Int? = longValue()?.toInt()

private fun stableCustomerId(value: String): Long {
    var hash = 1125899906842597L
    value.forEach { hash = 31 * hash + it.code }
    return if (hash == Long.MIN_VALUE) 0L else kotlin.math.abs(hash)
}
