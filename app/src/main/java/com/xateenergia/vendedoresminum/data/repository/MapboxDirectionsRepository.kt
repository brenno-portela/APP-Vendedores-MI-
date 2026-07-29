package com.xateenergia.vendedoresminum.data.repository

import android.content.Context
import com.xateenergia.vendedoresminum.R
import com.xateenergia.vendedoresminum.domain.model.Coordinate
import com.xateenergia.vendedoresminum.domain.model.RoadRoute
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Singleton
class MapboxDirectionsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun getDrivingRoute(points: List<Coordinate>): RoadRoute = withContext(Dispatchers.IO) {
        require(points.size >= 2) { "Selecione pelo menos dois pontos para calcular a rota." }
        require(points.size <= MAX_COORDINATES) {
            "O Mapbox Directions aceita ate $MAX_COORDINATES pontos por rota. Reduza a selecao."
        }

        val token = context.getString(R.string.mapbox_access_token).trim()
        require(token.isNotBlank()) { "Token do Mapbox nao configurado no app." }

        val coordinates = points.joinToString(";") { point ->
            "${point.longitude},${point.latitude}"
        }
        val url = URL(
            "https://api.mapbox.com/directions/v5/mapbox/driving-traffic/$coordinates" +
                "?geometries=geojson&overview=full&steps=false&access_token=${token.urlEncode()}"
        )

        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
        }

        try {
            val responseCode = connection.responseCode
            val body = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }

            if (responseCode !in 200..299) {
                val message = body.takeIf { it.isNotBlank() } ?: "HTTP $responseCode"
                error("Falha ao calcular rota no Mapbox: $message")
            }

            body.toRoadRoute()
        } finally {
            connection.disconnect()
        }
    }

    private fun String.toRoadRoute(): RoadRoute {
        val json = JSONObject(this)
        val code = json.optString("code")
        if (code.isNotBlank() && code != "Ok") {
            error(json.optString("message").ifBlank { "Mapbox retornou $code para a rota." })
        }

        val routes = json.getJSONArray("routes")
        require(routes.length() > 0) { "Mapbox nao encontrou uma rota para esses pontos." }

        val route = routes.getJSONObject(0)
        val geometry = route.getJSONObject("geometry")
        val coordinates = geometry.getJSONArray("coordinates")
        val routePoints = buildList {
            for (index in 0 until coordinates.length()) {
                val pair = coordinates.getJSONArray(index)
                add(Coordinate(latitude = pair.getDouble(1), longitude = pair.getDouble(0)))
            }
        }

        return RoadRoute(
            points = routePoints,
            distanceMeters = route.optDouble("distance", 0.0),
            durationSeconds = route.optDouble("duration", 0.0)
        )
    }

    private fun String.urlEncode(): String {
        return URLEncoder.encode(this, Charsets.UTF_8.name())
    }

    private companion object {
        const val MAX_COORDINATES = 25
    }
}
