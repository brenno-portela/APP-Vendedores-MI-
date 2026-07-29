package com.xateenergia.vendedoresminum.domain.model

data class RoadRoute(
    val points: List<Coordinate>,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val instructions: List<RouteInstruction> = emptyList()
)
