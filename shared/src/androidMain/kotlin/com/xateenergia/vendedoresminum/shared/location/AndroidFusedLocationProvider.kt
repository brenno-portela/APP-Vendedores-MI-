package com.xateenergia.vendedoresminum.shared.location

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

actual interface PlatformLocationProvider : LocationProvider

class AndroidFusedLocationProvider(
    private val fusedLocationProviderClient: FusedLocationProviderClient
) : PlatformLocationProvider {
    override suspend fun getCurrentLocation(): GeoLocation? {
        val location = fusedLocationProviderClient
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .await()

        return location?.let {
            GeoLocation(
                latitude = it.latitude,
                longitude = it.longitude,
                accuracyMeters = if (it.hasAccuracy()) it.accuracy else null,
                capturedAtMillis = it.time
            )
        }
    }
}
