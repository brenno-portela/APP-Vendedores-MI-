package com.xateenergia.vendedoresminum.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.xateenergia.vendedoresminum.domain.model.VisitEventDraft
import com.xateenergia.vendedoresminum.domain.model.VisitEventType
import com.xateenergia.vendedoresminum.utils.StateUtils
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Registra eventos append-only para que o historico de uma visita nunca seja
 * substituido pelo proximo feedback. O node tambem ja carrega os metadados que
 * uma futura Cloud Function usara para criar atividades no Odoo com seguranca.
 */
@Singleton
class FirebaseVisitEventRepository @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
    private val firebaseAuth: FirebaseAuth
) {
    suspend fun recordEvent(event: VisitEventDraft): Unit = withContext(Dispatchers.IO) {
        firebaseDatabase.reference.updateChildren(eventUpdates(listOf(event))).await()
    }

    suspend fun eventUpdates(events: List<VisitEventDraft>): Map<String, Any?> = withContext(Dispatchers.IO) {
        val currentUser = firebaseAuth.currentUser
            ?: error("Faca login novamente para registrar a visita.")
        val userSnapshot = firebaseDatabase.getReference("users").child(currentUser.uid).get().await()
        val sellerName = userSnapshot.child("name").getValue(String::class.java)
            ?: userSnapshot.child("displayName").getValue(String::class.java)
            ?: userSnapshot.child("email").getValue(String::class.java)
            ?: currentUser.email
            ?: currentUser.uid
        val sellerState = StateUtils.normalizeUf(userSnapshot.child("state").getValue(String::class.java))

        buildMap {
            events.forEach { event ->
                val eventId = firebaseDatabase.getReference("visitEvents")
                    .child(event.routeId)
                    .child(event.stopId)
                    .push()
                    .key
                    ?: error("Nao foi possivel gerar o evento da visita.")
                val requiresOdooActivity = event.type == VisitEventType.FEEDBACK_SUBMITTED

                put(
                    "visitEvents/${event.routeId}/${event.stopId}/$eventId",
                    mapOf(
                        "id" to eventId,
                        "routeId" to event.routeId,
                        "stopId" to event.stopId,
                        "customerId" to event.customer.id,
                        "customerExternalId" to event.customer.externalId,
                        "customerName" to event.customer.name,
                        "customerCnpjCpf" to event.customer.cnpjCpf,
                        "sellerUid" to currentUser.uid,
                        "sellerName" to sellerName,
                        "sellerEmail" to currentUser.email,
                        "state" to sellerState,
                        "eventType" to event.type.firebaseValue,
                        "attendanceId" to event.attendanceId,
                        "visitStatus" to event.visitStatus,
                        "feedback" to event.feedback,
                        "notVisitedReason" to event.notVisitedReason,
                        "commercialOutcome" to event.commercialOutcome,
                        "nextAction" to event.nextAction,
                        "nextActionDueDate" to event.nextActionDueDate,
                        "location" to mapOf(
                            "latitude" to event.location.latitude,
                            "longitude" to event.location.longitude,
                            "accuracyMeters" to event.locationAccuracyMeters
                        ),
                        "distanceToCustomerMeters" to event.distanceToCustomerMeters,
                        "plannedDistanceMeters" to event.plannedDistanceMeters,
                        "plannedDurationSeconds" to event.plannedDurationSeconds,
                        "actualDistanceMeters" to event.actualDistanceMeters,
                        "actualDurationSeconds" to event.actualDurationSeconds,
                        "movingDurationSeconds" to event.movingDurationSeconds,
                        "stoppedDurationSeconds" to event.stoppedDurationSeconds,
                        "visitDurationSeconds" to event.visitDurationSeconds,
                        "locationSampleCount" to event.locationSampleCount,
                        "locationSource" to event.locationSource,
                        "createdAt" to ServerValue.TIMESTAMP,
                        // Somente feedbacks devem se tornar atividades no Odoo no futuro.
                        "odooSyncStatus" to if (requiresOdooActivity) "pending" else "not_required",
                        "odooOperation" to if (requiresOdooActivity) "create_activity" else null,
                        "odooRetryCount" to 0
                    )
                )
            }
        }
    }
}
