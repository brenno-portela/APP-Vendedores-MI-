package com.xateenergia.vendedoresminum.domain.model

/** Dados de auditoria de uma parada exibidos no historico da rota. */
data class PlannedRouteStopSummary(
    val routeId: Long,
    val customerId: Long,
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
