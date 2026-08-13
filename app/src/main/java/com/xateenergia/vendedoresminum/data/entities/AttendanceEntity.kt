package com.xateenergia.vendedoresminum.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidade Room para persistir atendimentos localmente. Garante que o cronometro
 * sobrevive ao app ser fechado e que os dados estao disponiveis offline ate a
 * sincronizacao com o Firebase.
 */
@Entity(tableName = "visit_attendances")
data class AttendanceEntity(
    @PrimaryKey val id: String,
    val attendanceNumber: Int,
    val routeId: String,
    val clientId: Long,
    val sellerUid: String,
    val checkInAt: Long,
    val checkOutAt: Long? = null,
    val durationSeconds: Long? = null,
    val checkInLatitude: Double? = null,
    val checkInLongitude: Double? = null,
    val checkInAccuracyMeters: Float? = null,
    val checkInDistanceToClientMeters: Double? = null,
    val checkOutLatitude: Double? = null,
    val checkOutLongitude: Double? = null,
    val checkOutAccuracyMeters: Float? = null,
    val checkOutDistanceToClientMeters: Double? = null,
    val checkInGpsValidated: Boolean = false,
    val checkOutGpsValidated: Boolean? = null,
    val resultStatus: String? = null,
    val resultReason: String? = null,
    val feedback: String? = null,
    val status: String = "in_progress"
)
