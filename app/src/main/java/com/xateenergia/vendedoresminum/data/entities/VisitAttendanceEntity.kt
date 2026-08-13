package com.xateenergia.vendedoresminum.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Espelho local de cada atendimento. Ele preserva o cronometro e o checkout
 * mesmo se o Android encerrar o processo enquanto o vendedor esta no cliente.
 */
@Entity(
    tableName = "visit_attendances",
    indices = [
        Index(value = ["routeId"]),
        Index(value = ["customerId"]),
        Index(value = ["status"])
    ]
)
data class VisitAttendanceEntity(
    @PrimaryKey
    val id: String,
    val routeId: String,
    val stopId: String,
    val customerId: Long,
    val customerName: String,
    val status: String,
    val checkInAt: Long,
    val checkInLatitude: Double,
    val checkInLongitude: Double,
    val checkInAccuracyMeters: Float?,
    val checkInDistanceToCustomerMeters: Double,
    val checkOutAt: Long?,
    val checkOutLatitude: Double?,
    val checkOutLongitude: Double?,
    val checkOutAccuracyMeters: Float?,
    val checkOutDistanceToCustomerMeters: Double?,
    val visitDurationSeconds: Long?,
    val feedback: String?,
    val notVisitedReason: String?,
    val commercialOutcome: String?,
    val nextAction: String?,
    val nextActionDueDate: String?,
    val updatedAt: Long
)
