package com.xateenergia.vendedoresminum.domain.model

/** Rota preparada por um administrador e atribuida a um vendedor especifico. */
data class SharedRouteAssignment(
    val id: String,
    val name: String,
    val sellerUid: String,
    val sellerName: String? = null,
    val state: String? = null,
    val dueDate: String? = null,
    val targetCompletionPercent: Int = 90,
    val notes: String? = null,
    val estimatedDistanceMeters: Double? = null,
    val estimatedDurationSeconds: Double? = null,
    val status: String = "assigned",
    val stops: List<SharedRouteStop> = emptyList()
)

data class SharedRouteStop(
    val id: String,
    val customer: Customer,
    val order: Int,
    val status: String = "assigned"
)
