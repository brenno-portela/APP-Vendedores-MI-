package com.xateenergia.vendedoresminum.domain.model

/**
 * Um atendimento e uma tentativa independente de visitar uma parada da rota.
 * O cliente pode ter quantos atendimentos forem necessarios no mesmo dia sem
 * que um feedback substitua o anterior.
 */
data class VisitAttendance(
    val id: String,
    val routeId: String,
    val stopId: String,
    val customerId: Long,
    val customerName: String,
    val status: VisitAttendanceStatus,
    val checkInAt: Long,
    val checkInLocation: Coordinate,
    val checkInAccuracyMeters: Float?,
    val checkInDistanceToCustomerMeters: Double,
    val checkOutAt: Long? = null,
    val checkOutLocation: Coordinate? = null,
    val checkOutAccuracyMeters: Float? = null,
    val checkOutDistanceToCustomerMeters: Double? = null,
    val visitDurationSeconds: Long? = null,
    val feedback: String? = null,
    val notVisitedReason: String? = null,
    val commercialOutcome: String? = null,
    val nextAction: String? = null,
    val nextActionDueDate: String? = null,
    val updatedAt: Long = checkInAt
) {
    val isOpen: Boolean
        get() = status == VisitAttendanceStatus.IN_PROGRESS ||
            status == VisitAttendanceStatus.AWAITING_FEEDBACK
}

enum class VisitAttendanceStatus(val firebaseValue: String) {
    IN_PROGRESS("in_progress"),
    AWAITING_FEEDBACK("awaiting_feedback"),
    VISITED("visited"),
    NOT_VISITED("not_visited");

    companion object {
        fun fromFirebase(value: String?): VisitAttendanceStatus {
            return values().firstOrNull { it.firebaseValue == value }
                ?: IN_PROGRESS
        }
    }
}

/** Etapas exibidas pela gaveta de atendimento sobre o mapa. */
enum class AttendancePanelMode {
    HIDDEN,
    PRE_CHECK_IN,
    IN_PROGRESS,
    POST_CHECK_OUT,
    RETURN_LIST
}
