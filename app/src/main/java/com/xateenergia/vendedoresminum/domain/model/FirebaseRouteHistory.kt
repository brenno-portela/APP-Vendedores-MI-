package com.xateenergia.vendedoresminum.domain.model

/**
 * Representa o historico remoto exibido ao vendedor. O Firebase e a fonte de
 * verdade: se uma rota for removida pelo backoffice, ela desaparece desta lista
 * em tempo real, sem depender do cache Room do aparelho.
 */
data class FirebaseRouteSummary(
    val id: String,
    val name: String,
    val mainCustomerName: String?,
    val radiusKm: Double,
    val createdAt: Long,
    val isCompleted: Boolean,
    val notCompletedReason: String?,
    val stopCount: Int,
    val status: String,
    val distanceMeters: Double?,
    val durationSeconds: Double?
)

/** Dados de uma parada recebidos do Firebase para o historico da rota. */
data class FirebaseRouteStopSummary(
    val routeId: String,
    val customerId: String?,
    val orderIndex: Int,
    val distanceMeters: Double,
    val visitStatus: String,
    val feedback: String?,
    val feedbackAt: Long?,
    val feedbackLatitude: Double?,
    val feedbackLongitude: Double?,
    val customerName: String?,
    val companyName: String?,
    val phone: String?
)
