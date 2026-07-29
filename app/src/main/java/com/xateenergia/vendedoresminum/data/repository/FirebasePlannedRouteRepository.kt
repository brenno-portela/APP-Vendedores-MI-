package com.xateenergia.vendedoresminum.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.xateenergia.vendedoresminum.data.entities.PlannedRouteEntity
import com.xateenergia.vendedoresminum.domain.model.NearbyCustomer
import com.xateenergia.vendedoresminum.utils.StateUtils
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Singleton
class FirebasePlannedRouteRepository @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
    private val firebaseAuth: FirebaseAuth
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
}
