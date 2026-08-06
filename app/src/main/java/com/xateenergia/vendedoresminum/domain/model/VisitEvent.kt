package com.xateenergia.vendedoresminum.domain.model

/**
 * Representa um ponto imutavel da linha do tempo de uma visita. O estado atual
 * continua em plannedRouteStops, enquanto estes eventos servem para auditoria,
 * indicadores do backoffice e futura sincronizacao com o Odoo.
 */
enum class VisitEventType(val firebaseValue: String) {
    ROUTE_STARTED("route_started"),
    ROUTE_PROGRESS("route_progress"),
    STOP_ARRIVED("stop_arrived"),
    CHECK_IN("check_in"),
    FEEDBACK_SUBMITTED("feedback_submitted"),
    CHECK_OUT("check_out"),
    STOP_DEPARTED("stop_departed"),
    ROUTE_FINISHED("route_finished")
}

data class VisitEventDraft(
    val routeId: String,
    val stopId: String,
    val customer: Customer,
    val type: VisitEventType,
    val visitStatus: String? = null,
    val feedback: String? = null,
    val notVisitedReason: String? = null,
    val commercialOutcome: String? = null,
    val nextAction: String? = null,
    val nextActionDueDate: String? = null,
    val location: Coordinate,
    val locationAccuracyMeters: Float? = null,
    val distanceToCustomerMeters: Double? = null,
    // Campos de telemetria permitem comparar o planejado com a execucao real.
    val plannedDistanceMeters: Double? = null,
    val plannedDurationSeconds: Double? = null,
    val actualDistanceMeters: Double? = null,
    val actualDurationSeconds: Long? = null,
    val movingDurationSeconds: Long? = null,
    val stoppedDurationSeconds: Long? = null,
    val visitDurationSeconds: Long? = null,
    val locationSampleCount: Int? = null,
    val locationSource: String? = null
)
