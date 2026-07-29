package com.xateenergia.vendedoresminum.domain.model

data class RouteInstruction(
    val text: String,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val maneuverLocation: Coordinate? = null
)
