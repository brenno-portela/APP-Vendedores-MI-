package com.xateenergia.vendedoresminum.domain.usecase

import android.content.Context
import android.location.Geocoder
import android.os.Build
import com.google.firebase.functions.FirebaseFunctions
import com.xateenergia.vendedoresminum.domain.model.Coordinate
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class GeocodeAddressUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val functions: FirebaseFunctions? = null
) {
    suspend operator fun invoke(address: String): Coordinate? {
        val query = address.trim()
        if (query.isBlank()) return null

        return withContext(Dispatchers.IO) {
            // Tentar geocodificação unificada via backend Firebase / Mapbox v6 em primeiro lugar
            try {
                functions?.let { firebaseFunctions ->
                    val data = hashMapOf("address" to query)
                    val result = firebaseFunctions
                        .getHttpsCallable("geocodeAddress")
                        .call(data)
                        .await()
                    
                    val map = result.data as? Map<*, *>
                    if (map != null && map["found"] == true) {
                        val lat = (map["navigationLatitude"] as? Number)?.toDouble()
                            ?: (map["latitude"] as? Number)?.toDouble()
                        val lon = (map["navigationLongitude"] as? Number)?.toDouble()
                            ?: (map["longitude"] as? Number)?.toDouble()

                        if (lat != null && lon != null) {
                            return@withContext Coordinate(lat, lon)
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback offline se o backend estiver inacessível
            }

            // Fallback para Android Geocoder nativo se offline
            val geocoder = Geocoder(context, Locale("pt", "BR"))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCoroutine { continuation ->
                    geocoder.getFromLocationName(query, 1) { results ->
                        val first = results.firstOrNull()
                        continuation.resume(first?.let { Coordinate(it.latitude, it.longitude) })
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocationName(query, 1)
                    ?.firstOrNull()
                    ?.let { Coordinate(it.latitude, it.longitude) }
            }
        }
    }
}
