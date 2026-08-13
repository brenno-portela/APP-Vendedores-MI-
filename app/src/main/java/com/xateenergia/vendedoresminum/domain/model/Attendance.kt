package com.xateenergia.vendedoresminum.domain.model

/**
 * Representa um registro independente de atendimento (check-in/check-out explicito).
 * Cada tentativa de atendimento a um cliente gera um novo Attendance, sem sobrescrever
 * os anteriores. A relacao com a rota e 1:N (um cliente pode ter varios atendimentos).
 */
data class Attendance(
    val id: String,
    val attendanceNumber: Int,
    val routeId: String,
    val clientId: Long,
    val sellerUid: String,

    // Timestamps
    val checkInAt: Long,
    val checkOutAt: Long? = null,
    val durationSeconds: Long? = null,

    // Geo check-in
    val checkInLatitude: Double? = null,
    val checkInLongitude: Double? = null,
    val checkInAccuracyMeters: Float? = null,
    val checkInDistanceToClientMeters: Double? = null,

    // Geo check-out
    val checkOutLatitude: Double? = null,
    val checkOutLongitude: Double? = null,
    val checkOutAccuracyMeters: Float? = null,
    val checkOutDistanceToClientMeters: Double? = null,

    // Validacao GPS
    val checkInGpsValidated: Boolean = false,
    val checkOutGpsValidated: Boolean? = null,

    // Negocio
    val resultStatus: String? = null,
    val resultReason: String? = null,
    val feedback: String? = null,

    // Estado
    val status: String = STATUS_IN_PROGRESS
) {
    val isInProgress: Boolean get() = status == STATUS_IN_PROGRESS
    val isCompleted: Boolean get() = status == STATUS_COMPLETED

    companion object {
        const val STATUS_IN_PROGRESS = "in_progress"
        const val STATUS_COMPLETED = "completed"
        const val RESULT_ATTENDED = "attended"
        const val RESULT_NOT_ATTENDED = "not_attended"
    }
}
