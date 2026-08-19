package com.xateenergia.vendedoresminum.shared.location

interface LocationProvider {
    suspend fun getCurrentLocation(): GeoLocation?
}
