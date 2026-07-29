package com.xateenergia.vendedoresminum.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.xateenergia.vendedoresminum.data.dao.PlannedRouteDao
import com.xateenergia.vendedoresminum.data.entities.PlannedRouteEntity
import com.xateenergia.vendedoresminum.data.entities.PlannedRouteStopEntity
import com.xateenergia.vendedoresminum.domain.model.PlannedRouteSummary
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

@Singleton
class PlannedRouteRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val plannedRouteDao: PlannedRouteDao
) {
    fun observeSummaries(): Flow<List<PlannedRouteSummary>> = plannedRouteDao.observeSummaries()

    suspend fun saveRoute(route: PlannedRouteEntity, stops: List<PlannedRouteStopEntity>): Long {
        val routeId = plannedRouteDao.saveRoute(route, stops)
        val summary = plannedRouteDao.getSummaryById(routeId)
        if (summary != null) {
            syncRouteToFirebase(summary, summary.isCompleted, summary.notCompletedReason)
        }
        return routeId
    }

    suspend fun updateRouteCompletionStatus(routeId: Long, isCompleted: Boolean, reason: String?) {
        plannedRouteDao.updateRouteCompletionStatus(routeId, isCompleted, reason)
        val summary = plannedRouteDao.getSummaryById(routeId)
        if (summary != null) {
            syncRouteToFirebase(summary, isCompleted, reason)
        }
    }

    suspend fun deleteAll() {
        plannedRouteDao.deleteAll()
    }

    private fun isFirebaseConfigured(): Boolean {
        return FirebaseApp.getApps(context).isNotEmpty()
    }

    private suspend fun syncRouteToFirebase(
        summary: PlannedRouteSummary,
        isCompleted: Boolean,
        reason: String?
    ) {
        if (!isFirebaseConfigured()) return

        runCatching {
            val currentUser = FirebaseAuth.getInstance().currentUser ?: return@runCatching
            val database = FirebaseDatabase.getInstance()
            val statusString = when {
                isCompleted -> "COMPLETED"
                !reason.isNullOrBlank() -> "NOT_COMPLETED"
                else -> "PENDING"
            }

            val routeData = mapOf<String, Any?>(
                "routeId" to summary.id,
                "userId" to currentUser.uid,
                "userEmail" to currentUser.email,
                "name" to summary.name,
                "mainCustomerName" to summary.mainCustomerName,
                "mainLatitude" to summary.mainLatitude,
                "mainLongitude" to summary.mainLongitude,
                "radiusKm" to summary.radiusKm,
                "stopCount" to summary.stopCount,
                "isCompleted" to isCompleted,
                "status" to statusString,
                "notCompletedReason" to reason,
                "createdAt" to summary.createdAt,
                "updatedAt" to ServerValue.TIMESTAMP
            )

            // 1. Grava no nó global de rotas
            database.getReference("routes")
                .child(summary.id.toString())
                .updateChildren(routeData)
                .await()

            // 2. Grava no nó específico do usuário/vendedor
            database.getReference("users")
                .child(currentUser.uid)
                .child("routes")
                .child(summary.id.toString())
                .updateChildren(routeData)
                .await()

            // 3. Atualiza o resumo de status da última rota do vendedor
            val userStatusUpdate = mapOf<String, Any?>(
                "lastRouteId" to summary.id,
                "lastRouteName" to summary.name,
                "lastRouteStatus" to statusString,
                "lastRouteReason" to reason,
                "lastRouteUpdatedAt" to ServerValue.TIMESTAMP
            )
            database.getReference("users")
                .child(currentUser.uid)
                .updateChildren(userStatusUpdate)
                .await()
        }.onFailure { e ->
            Log.e("RouteSync", "Erro ao sincronizar rota com Firebase: ${e.message}", e)
        }
    }
}


