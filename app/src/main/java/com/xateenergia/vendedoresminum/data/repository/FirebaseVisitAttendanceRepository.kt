package com.xateenergia.vendedoresminum.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.xateenergia.vendedoresminum.domain.model.Coordinate
import com.xateenergia.vendedoresminum.domain.model.Customer
import com.xateenergia.vendedoresminum.domain.model.VisitAttendance
import com.xateenergia.vendedoresminum.domain.model.VisitAttendanceStatus
import com.xateenergia.vendedoresminum.domain.model.VisitEventDraft
import com.xateenergia.vendedoresminum.domain.model.VisitEventType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Grava cada tentativa de atendimento em um filho proprio da parada. Os campos
 * antigos da parada continuam recebendo o ultimo resultado para compatibilidade
 * com historicos e relatorios que ja existem no backoffice.
 */
@Singleton
class FirebaseVisitAttendanceRepository @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
    private val firebaseAuth: FirebaseAuth,
    private val firebaseVisitEventRepository: FirebaseVisitEventRepository
) {
    suspend fun start(
        attendance: VisitAttendance,
        customer: Customer,
        mirrorToSharedRoute: Boolean
    ) = withContext(Dispatchers.IO) {
        val updates = baseUpdates(attendance, customer, mirrorToSharedRoute).apply {
            putAll(
                firebaseVisitEventRepository.eventUpdates(
                    listOf(attendance.toEvent(customer, VisitEventType.CHECK_IN))
                )
            )
        }
        firebaseDatabase.reference.updateChildren(updates).await()
    }

    suspend fun recordCheckout(
        attendance: VisitAttendance,
        customer: Customer,
        mirrorToSharedRoute: Boolean
    ) = withContext(Dispatchers.IO) {
        require(attendance.status == VisitAttendanceStatus.AWAITING_FEEDBACK) {
            "O checkout precisa ser registrado antes do resultado da visita."
        }
        val updates = baseUpdates(attendance, customer, mirrorToSharedRoute).apply {
            putAll(
                firebaseVisitEventRepository.eventUpdates(
                    listOf(attendance.toEvent(customer, VisitEventType.CHECK_OUT))
                )
            )
        }
        firebaseDatabase.reference.updateChildren(updates).await()
    }

    suspend fun complete(
        attendance: VisitAttendance,
        customer: Customer,
        mirrorToSharedRoute: Boolean
    ) = withContext(Dispatchers.IO) {
        require(
            attendance.status == VisitAttendanceStatus.VISITED ||
                attendance.status == VisitAttendanceStatus.NOT_VISITED
        ) { "Informe o resultado do atendimento antes de salvar." }

        val uid = firebaseAuth.currentUser?.uid ?: error("Faca login novamente para salvar o atendimento.")
        val status = attendance.status.firebaseValue
        val canonicalStopPath = canonicalStopPath(attendance)
        val updates = baseUpdates(attendance, customer, mirrorToSharedRoute).apply {
            // Resumo atual da parada: preserva o comportamento de telas e relatorios legados.
            put("$canonicalStopPath/status", status)
            put("$canonicalStopPath/result", status)
            put("$canonicalStopPath/wasVisited", attendance.status == VisitAttendanceStatus.VISITED)
            put("$canonicalStopPath/feedback", attendance.feedback)
            put("$canonicalStopPath/notVisitedReason", attendance.notVisitedReason)
            put("$canonicalStopPath/commercialOutcome", attendance.commercialOutcome)
            put("$canonicalStopPath/nextAction", attendance.nextAction)
            put("$canonicalStopPath/nextActionDueDate", attendance.nextActionDueDate)
            put("$canonicalStopPath/feedbackAt", attendance.updatedAt)
            put("$canonicalStopPath/visitedAt", attendance.updatedAt)
            put("$canonicalStopPath/feedbackLatitude", attendance.checkOutLocation?.latitude)
            put("$canonicalStopPath/feedbackLongitude", attendance.checkOutLocation?.longitude)
            put("$canonicalStopPath/feedbackAccuracyMeters", attendance.checkOutAccuracyMeters)
            put("$canonicalStopPath/feedbackDistanceToCustomerMeters", attendance.checkOutDistanceToCustomerMeters)
            put("$canonicalStopPath/feedbackLocation", attendance.checkOutLocation?.toFirebaseMap(attendance.checkOutAccuracyMeters))
            put("$canonicalStopPath/checkInAt", attendance.checkInAt)
            put("$canonicalStopPath/checkInLocation", attendance.checkInLocation.toFirebaseMap(attendance.checkInAccuracyMeters))
            put("$canonicalStopPath/checkInDistanceToCustomerMeters", attendance.checkInDistanceToCustomerMeters)
            put("$canonicalStopPath/checkOutAt", attendance.checkOutAt)
            put("$canonicalStopPath/checkOutLocation", attendance.checkOutLocation?.toFirebaseMap(attendance.checkOutAccuracyMeters))
            put("$canonicalStopPath/checkOutDistanceToCustomerMeters", attendance.checkOutDistanceToCustomerMeters)
            put("$canonicalStopPath/visitDurationSeconds", attendance.visitDurationSeconds)

            if (mirrorToSharedRoute) {
                val sellerStopPath = "sharedRoutesBySeller/$uid/${attendance.routeId}/stops/${attendance.stopId}"
                put("$sellerStopPath/status", status)
                put("$sellerStopPath/result", status)
                put("$sellerStopPath/wasVisited", attendance.status == VisitAttendanceStatus.VISITED)
                put("$sellerStopPath/feedback", attendance.feedback)
                put("$sellerStopPath/notVisitedReason", attendance.notVisitedReason)
                put("$sellerStopPath/commercialOutcome", attendance.commercialOutcome)
                put("$sellerStopPath/nextAction", attendance.nextAction)
                put("$sellerStopPath/nextActionDueDate", attendance.nextActionDueDate)
                put("$sellerStopPath/feedbackAt", attendance.updatedAt)
                put("$sellerStopPath/visitedAt", attendance.updatedAt)
                put("$sellerStopPath/feedbackLatitude", attendance.checkOutLocation?.latitude)
                put("$sellerStopPath/feedbackLongitude", attendance.checkOutLocation?.longitude)
                put("$sellerStopPath/feedbackAccuracyMeters", attendance.checkOutAccuracyMeters)
                put("$sellerStopPath/feedbackDistanceToCustomerMeters", attendance.checkOutDistanceToCustomerMeters)
                put("$sellerStopPath/feedbackLocation", attendance.checkOutLocation?.toFirebaseMap(attendance.checkOutAccuracyMeters))
                put("$sellerStopPath/checkInAt", attendance.checkInAt)
                put("$sellerStopPath/checkInLocation", attendance.checkInLocation.toFirebaseMap(attendance.checkInAccuracyMeters))
                put("$sellerStopPath/checkInDistanceToCustomerMeters", attendance.checkInDistanceToCustomerMeters)
                put("$sellerStopPath/checkOutAt", attendance.checkOutAt)
                put("$sellerStopPath/checkOutLocation", attendance.checkOutLocation?.toFirebaseMap(attendance.checkOutAccuracyMeters))
                put("$sellerStopPath/checkOutDistanceToCustomerMeters", attendance.checkOutDistanceToCustomerMeters)
                put("$sellerStopPath/visitDurationSeconds", attendance.visitDurationSeconds)
            }

            putAll(
                firebaseVisitEventRepository.eventUpdates(
                    listOf(attendance.toEvent(customer, VisitEventType.FEEDBACK_SUBMITTED))
                )
            )
        }
        firebaseDatabase.reference.updateChildren(updates).await()
    }

    private fun baseUpdates(
        attendance: VisitAttendance,
        customer: Customer,
        mirrorToSharedRoute: Boolean
    ): MutableMap<String, Any?> {
        val uid = firebaseAuth.currentUser?.uid ?: error("Faca login novamente para registrar o atendimento.")
        val canonicalPath = "${canonicalStopPath(attendance)}/attendances/${attendance.id}"
        val updates = mutableMapOf<String, Any?>(
            canonicalPath to attendance.toFirebaseMap(customer, uid),
            "plannedRouteStops/${attendance.routeId}/${attendance.stopId}/updatedAt" to ServerValue.TIMESTAMP,
            "plannedRoutes/${attendance.routeId}/updatedAt" to ServerValue.TIMESTAMP
        )
        if (mirrorToSharedRoute) {
            updates["sharedRoutesBySeller/$uid/${attendance.routeId}/stops/${attendance.stopId}/attendances/${attendance.id}"] =
                attendance.toFirebaseMap(customer, uid)
            updates["sharedRoutesBySeller/$uid/${attendance.routeId}/stops/${attendance.stopId}/updatedAt"] = ServerValue.TIMESTAMP
            updates["sharedRoutesBySeller/$uid/${attendance.routeId}/updatedAt"] = ServerValue.TIMESTAMP
        }
        return updates
    }

    private fun canonicalStopPath(attendance: VisitAttendance): String {
        return "plannedRouteStops/${attendance.routeId}/${attendance.stopId}"
    }
}

private fun VisitAttendance.toFirebaseMap(customer: Customer, sellerUid: String): Map<String, Any?> = buildMap {
    put("id", id)
    put("routeId", routeId)
    put("stopId", stopId)
    put("customerId", customerId)
    put("customerExternalId", customer.externalId)
    put("customerName", customerName)
    put("sellerUid", sellerUid)
    put("status", status.firebaseValue)
    put("checkInAt", checkInAt)
    put("checkInLocation", checkInLocation.toFirebaseMap(checkInAccuracyMeters))
    put("checkInAccuracyMeters", checkInAccuracyMeters)
    put("checkInDistanceToCustomerMeters", checkInDistanceToCustomerMeters)
    put("checkOutAt", checkOutAt)
    put("checkOutLocation", checkOutLocation?.toFirebaseMap(checkOutAccuracyMeters))
    put("checkOutAccuracyMeters", checkOutAccuracyMeters)
    put("checkOutDistanceToCustomerMeters", checkOutDistanceToCustomerMeters)
    put("visitDurationSeconds", visitDurationSeconds)
    put("feedback", feedback)
    put("notVisitedReason", notVisitedReason)
    put("commercialOutcome", commercialOutcome)
    put("nextAction", nextAction)
    put("nextActionDueDate", nextActionDueDate)
    put("updatedAt", updatedAt)
}

private fun Coordinate.toFirebaseMap(accuracyMeters: Float?): Map<String, Any?> = mapOf(
    "latitude" to latitude,
    "longitude" to longitude,
    "accuracyMeters" to accuracyMeters
)

private fun VisitAttendance.toEvent(customer: Customer, type: VisitEventType): VisitEventDraft {
    val location = if (type == VisitEventType.CHECK_IN) checkInLocation else checkOutLocation ?: checkInLocation
    val accuracy = if (type == VisitEventType.CHECK_IN) checkInAccuracyMeters else checkOutAccuracyMeters
    val distance = if (type == VisitEventType.CHECK_IN) {
        checkInDistanceToCustomerMeters
    } else {
        checkOutDistanceToCustomerMeters
    }
    return VisitEventDraft(
        routeId = routeId,
        stopId = stopId,
        customer = customer,
        type = type,
        attendanceId = id,
        visitStatus = status.firebaseValue,
        feedback = feedback,
        notVisitedReason = notVisitedReason,
        commercialOutcome = commercialOutcome,
        nextAction = nextAction,
        nextActionDueDate = nextActionDueDate,
        location = location,
        locationAccuracyMeters = accuracy,
        distanceToCustomerMeters = distance,
        visitDurationSeconds = visitDurationSeconds
    )
}
