package com.xateenergia.vendedoresminum.domain.usecase

import com.google.firebase.functions.FirebaseFunctions
import com.xateenergia.vendedoresminum.domain.model.Coordinate
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Busca manual de endereco centralizada no backend. O app nunca usa o
 * Android Geocoder como fallback, pois ele poderia produzir um destino
 * diferente daquele validado e auditado pelo Mapbox no Firebase.
 */
class GeocodeAddressUseCase @Inject constructor(
    private val functions: FirebaseFunctions
) {
    suspend operator fun invoke(address: String): Coordinate? = withContext(Dispatchers.IO) {
        val query = address.trim()
        if (query.isBlank()) return@withContext null

        val result = functions
            .getHttpsCallable("geocodeAddress")
            .call(hashMapOf("address" to query))
            .await()
        val data = result.data as? Map<*, *> ?: error("Resposta inválida do serviço de endereços.")
        if (data["found"] != true) {
            error(data["reason"] as? String ?: "Não foi possível confirmar este endereço para navegação.")
        }

        val latitude = (data["navigationLatitude"] as? Number)?.toDouble()
            ?: (data["latitude"] as? Number)?.toDouble()
        val longitude = (data["navigationLongitude"] as? Number)?.toDouble()
            ?: (data["longitude"] as? Number)?.toDouble()
        if (latitude == null || longitude == null || (latitude == 0.0 && longitude == 0.0)) {
            error("O endereço não retornou um ponto de navegação válido.")
        }
        Coordinate(latitude, longitude)
    }
}
