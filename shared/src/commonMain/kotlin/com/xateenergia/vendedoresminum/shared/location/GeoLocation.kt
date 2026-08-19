package com.xateenergia.vendedoresminum.shared.location

data class GeoLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val capturedAtMillis: Long
)
