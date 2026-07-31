package com.xateenergia.vendedoresminum.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

const val DAILY_ROUTE_CREATION_LIMIT = 5

data class DailyRouteQuota(
    val used: Int,
    val dayKey: String,
    val limit: Int = DAILY_ROUTE_CREATION_LIMIT
) {
    val remaining: Int
        get() = (limit - used).coerceAtLeast(0)
}

class DailyRouteLimitReachedException : IllegalStateException(
    "Limite diario de $DAILY_ROUTE_CREATION_LIMIT rotas atingido. Tente novamente amanha."
)

/**
 * Mantem a cota no Realtime Database para que o limite seja compartilhado por
 * todos os aparelhos usados pelo mesmo vendedor.
 */
@Singleton
class FirebaseRouteQuotaRepository @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
    private val firebaseAuth: FirebaseAuth
) {
    suspend fun getTodayQuota(): DailyRouteQuota = withContext(Dispatchers.IO) {
        val uid = requireCurrentUserId()
        val dayKey = todayKey()
        val snapshot = quotaReference(uid, dayKey).get().await()
        quotaFromSnapshot(snapshot, dayKey)
    }

    /**
     * Reserva uma rota usando transacao. Duas tentativas simultaneas nunca
     * conseguem ultrapassar o limite diario.
     */
    suspend fun reserveRouteCreation(): DailyRouteQuota = withContext(Dispatchers.IO) {
        val uid = requireCurrentUserId()
        val dayKey = todayKey()
        val reference = quotaReference(uid, dayKey)

        suspendCancellableCoroutine { continuation ->
            reference.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val used = currentData.routeCount()
                    if (used >= DAILY_ROUTE_CREATION_LIMIT) {
                        return Transaction.abort()
                    }

                    currentData.child("count").value = used + 1
                    currentData.child("limit").value = DAILY_ROUTE_CREATION_LIMIT
                    currentData.child("day").value = dayKey
                    currentData.child("updatedAt").value = System.currentTimeMillis()
                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    if (!continuation.isActive) return
                    when {
                        error != null -> continuation.resumeWithException(error.toException())
                        !committed -> continuation.resumeWithException(DailyRouteLimitReachedException())
                        else -> continuation.resume(quotaFromSnapshot(currentData, dayKey))
                    }
                }
            })
        }
    }

    /** Devolve a reserva quando a rota nao consegue ser salva. */
    suspend fun releaseRouteCreation(): DailyRouteQuota = withContext(Dispatchers.IO) {
        val uid = requireCurrentUserId()
        val dayKey = todayKey()
        val reference = quotaReference(uid, dayKey)

        suspendCancellableCoroutine { continuation ->
            reference.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val used = currentData.routeCount()
                    if (used <= 0) return Transaction.abort()

                    currentData.child("count").value = used - 1
                    currentData.child("limit").value = DAILY_ROUTE_CREATION_LIMIT
                    currentData.child("day").value = dayKey
                    currentData.child("updatedAt").value = System.currentTimeMillis()
                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    if (!continuation.isActive) return
                    when {
                        error != null -> continuation.resumeWithException(error.toException())
                        else -> continuation.resume(quotaFromSnapshot(currentData, dayKey))
                    }
                }
            })
        }
    }

    private fun quotaReference(uid: String, dayKey: String): DatabaseReference =
        firebaseDatabase.getReference("routeCreationLimits").child(uid).child(dayKey)

    private fun requireCurrentUserId(): String {
        return firebaseAuth.currentUser?.uid
            ?: throw IllegalStateException("Faca login novamente para criar uma rota.")
    }

    private fun todayKey(): String {
        return LocalDate.now(ZoneId.of("America/Sao_Paulo")).toString()
    }

    private fun quotaFromSnapshot(snapshot: DataSnapshot?, dayKey: String): DailyRouteQuota {
        val used = (snapshot?.child("count")?.value as? Number)?.toInt()?.coerceAtLeast(0) ?: 0
        return DailyRouteQuota(used = used, dayKey = dayKey)
    }

    private fun MutableData.routeCount(): Int {
        return (child("count").value as? Number)?.toInt()?.coerceAtLeast(0) ?: 0
    }
}
