package com.xateenergia.vendedoresminum.data.repository

import com.xateenergia.vendedoresminum.data.dao.VisitAttendanceDao
import com.xateenergia.vendedoresminum.data.entities.VisitAttendanceEntity
import com.xateenergia.vendedoresminum.domain.model.Coordinate
import com.xateenergia.vendedoresminum.domain.model.VisitAttendance
import com.xateenergia.vendedoresminum.domain.model.VisitAttendanceStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Acesso ao historico local que restaura atendimentos interrompidos pelo sistema. */
@Singleton
class VisitAttendanceRepository @Inject constructor(
    private val visitAttendanceDao: VisitAttendanceDao
) {
    fun observeForRoute(routeId: String): Flow<List<VisitAttendance>> {
        return visitAttendanceDao.observeForRoute(routeId).map { attendances ->
            attendances.map(VisitAttendanceEntity::toDomain)
        }
    }

    suspend fun findOpenForRoute(routeId: String): VisitAttendance? {
        return visitAttendanceDao.findOpenForRoute(routeId)?.toDomain()
    }

    suspend fun save(attendance: VisitAttendance) {
        visitAttendanceDao.upsert(attendance.toEntity())
    }
}

private fun VisitAttendanceEntity.toDomain() = VisitAttendance(
    id = id,
    routeId = routeId,
    stopId = stopId,
    customerId = customerId,
    customerName = customerName,
    status = VisitAttendanceStatus.fromFirebase(status),
    checkInAt = checkInAt,
    checkInLocation = Coordinate(checkInLatitude, checkInLongitude),
    checkInAccuracyMeters = checkInAccuracyMeters,
    checkInDistanceToCustomerMeters = checkInDistanceToCustomerMeters,
    checkOutAt = checkOutAt,
    checkOutLocation = checkOutLatitude?.let { latitude ->
        checkOutLongitude?.let { longitude -> Coordinate(latitude, longitude) }
    },
    checkOutAccuracyMeters = checkOutAccuracyMeters,
    checkOutDistanceToCustomerMeters = checkOutDistanceToCustomerMeters,
    visitDurationSeconds = visitDurationSeconds,
    feedback = feedback,
    notVisitedReason = notVisitedReason,
    commercialOutcome = commercialOutcome,
    nextAction = nextAction,
    nextActionDueDate = nextActionDueDate,
    updatedAt = updatedAt
)

private fun VisitAttendance.toEntity() = VisitAttendanceEntity(
    id = id,
    routeId = routeId,
    stopId = stopId,
    customerId = customerId,
    customerName = customerName,
    status = status.firebaseValue,
    checkInAt = checkInAt,
    checkInLatitude = checkInLocation.latitude,
    checkInLongitude = checkInLocation.longitude,
    checkInAccuracyMeters = checkInAccuracyMeters,
    checkInDistanceToCustomerMeters = checkInDistanceToCustomerMeters,
    checkOutAt = checkOutAt,
    checkOutLatitude = checkOutLocation?.latitude,
    checkOutLongitude = checkOutLocation?.longitude,
    checkOutAccuracyMeters = checkOutAccuracyMeters,
    checkOutDistanceToCustomerMeters = checkOutDistanceToCustomerMeters,
    visitDurationSeconds = visitDurationSeconds,
    feedback = feedback,
    notVisitedReason = notVisitedReason,
    commercialOutcome = commercialOutcome,
    nextAction = nextAction,
    nextActionDueDate = nextActionDueDate,
    updatedAt = updatedAt
)
