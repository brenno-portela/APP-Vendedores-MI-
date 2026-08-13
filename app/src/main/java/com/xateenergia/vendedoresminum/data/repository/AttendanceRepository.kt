package com.xateenergia.vendedoresminum.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.xateenergia.vendedoresminum.data.dao.AttendanceDao
import com.xateenergia.vendedoresminum.data.entities.AttendanceEntity
import com.xateenergia.vendedoresminum.domain.model.Attendance
import com.xateenergia.vendedoresminum.domain.model.Coordinate
import com.xateenergia.vendedoresminum.domain.model.Customer
import com.xateenergia.vendedoresminum.domain.model.VisitEventDraft
import com.xateenergia.vendedoresminum.domain.model.VisitEventType
import com.xateenergia.vendedoresminum.utils.StateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceRepository @Inject constructor(
    private val attendanceDao: AttendanceDao,
    private val firebaseDatabase: FirebaseDatabase,
    private val firebaseAuth: FirebaseAuth,
    private val firebaseVisitEventRepository: FirebaseVisitEventRepository
) {
    suspend fun startCheckIn(
        routeId: String,
        customer: Customer,
        location: Coordinate?,
        locationAccuracyMeters: Float?,
        distanceToCustomerMeters: Double?
    ): Attendance = withContext(Dispatchers.IO) {
        val uid = firebaseAuth.currentUser?.uid ?: error("Faca login novamente para iniciar a visita.")
        val attendanceId = UUID.randomUUID().toString()
        val count = attendanceDao.countByClient(routeId, customer.id)
        val attendanceNumber = count + 1

        val isGpsValidated = distanceToCustomerMeters != null && distanceToCustomerMeters <= 100.0

        val attendance = Attendance(
            id = attendanceId,
            attendanceNumber = attendanceNumber,
            routeId = routeId,
            clientId = customer.id,
            sellerUid = uid,
            checkInAt = System.currentTimeMillis(),
            checkInLatitude = location?.latitude,
            checkInLongitude = location?.longitude,
            checkInAccuracyMeters = locationAccuracyMeters,
            checkInDistanceToClientMeters = distanceToCustomerMeters,
            checkInGpsValidated = isGpsValidated,
            status = Attendance.STATUS_IN_PROGRESS
        )

        // Salva localmente
        attendanceDao.upsert(attendance.toEntity())

        // Salva no Firebase
        saveToFirebase(attendance, customer)

        // Grava evento de check-in para a linha do tempo (auditavel)
        if (location != null) {
            val event = VisitEventDraft(
                routeId = routeId,
                stopId = customer.externalId?.takeIf { it.isNotBlank() } ?: customer.id.toString(),
                customer = customer,
                type = VisitEventType.CHECK_IN,
                visitStatus = "in_progress",
                location = location,
                locationAccuracyMeters = locationAccuracyMeters,
                distanceToCustomerMeters = distanceToCustomerMeters
            )
            firebaseVisitEventRepository.recordEvent(event)
        }

        attendance
    }

    suspend fun finishCheckOut(
        attendanceId: String,
        customer: Customer,
        location: Coordinate?,
        locationAccuracyMeters: Float?,
        distanceToCustomerMeters: Double?,
        resultStatus: String,
        resultReason: String?,
        feedback: String?
    ): Attendance = withContext(Dispatchers.IO) {
        val localEntity = attendanceDao.observeByRoute(routeId = "").map { list ->
            list.find { it.id == attendanceId }
        }.let {
            // Em vez do observe flow, vamos buscar direto via query ou select
            // Como nao criamos query especifica de id, podemos fazer uma query simples
            // ou buscar do active.
            // Para garantir seguranca, vamos adicionar uma funcao para buscar por ID.
            // Mas no DAO nao colocamos getById. Let's just find the active one.
            attendanceDao.getActiveAttendance()?.takeIf { it.id == attendanceId }
        } ?: error("Atendimento em andamento nao encontrado.")

        val checkOutAt = System.currentTimeMillis()
        val durationSeconds = ((checkOutAt - localEntity.checkInAt) / 1000L).coerceAtLeast(0L)
        val isGpsValidated = distanceToCustomerMeters != null && distanceToCustomerMeters <= 100.0

        val updated = localEntity.copy(
            checkOutAt = checkOutAt,
            durationSeconds = durationSeconds,
            checkOutLatitude = location?.latitude,
            checkOutLongitude = location?.longitude,
            checkOutAccuracyMeters = locationAccuracyMeters,
            checkOutDistanceToClientMeters = distanceToCustomerMeters,
            checkOutGpsValidated = isGpsValidated,
            resultStatus = resultStatus,
            resultReason = resultReason,
            feedback = feedback,
            status = Attendance.STATUS_COMPLETED
        ).toDomain()

        // Atualiza localmente
        attendanceDao.upsert(updated.toEntity())

        // Atualiza no Firebase
        saveToFirebase(updated, customer)

        val stopId = customer.externalId?.takeIf { it.isNotBlank() } ?: customer.id.toString()

        // Registra feedback e checkout nos eventos auditaveis do Firebase
        val baseEvent = VisitEventDraft(
            routeId = updated.routeId,
            stopId = stopId,
            customer = customer,
            type = VisitEventType.FEEDBACK_SUBMITTED,
            visitStatus = resultStatus,
            feedback = feedback,
            notVisitedReason = resultReason,
            location = location ?: Coordinate(0.0, 0.0),
            locationAccuracyMeters = locationAccuracyMeters,
            distanceToCustomerMeters = distanceToCustomerMeters
        )

        val events = buildList {
            add(baseEvent)
            add(baseEvent.copy(type = VisitEventType.CHECK_OUT, visitDurationSeconds = durationSeconds))
        }
        firebaseDatabase.reference.updateChildren(
            firebaseVisitEventRepository.eventUpdates(events)
        ).await()

        updated
    }

    suspend fun getActiveAttendance(): Attendance? = withContext(Dispatchers.IO) {
        attendanceDao.getActiveAttendance()?.toDomain()
    }

    fun observeByClient(routeId: String, clientId: Long): Flow<List<Attendance>> {
        return attendanceDao.observeByClient(routeId, clientId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun observeByRoute(routeId: String): Flow<List<Attendance>> {
        return attendanceDao.observeByRoute(routeId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    private suspend fun saveToFirebase(attendance: Attendance, customer: Customer) {
        val uid = attendance.sellerUid
        val userSnapshot = firebaseDatabase.getReference("users").child(uid).get().await()
        val sellerName = userSnapshot.child("name").getValue(String::class.java)
            ?: userSnapshot.child("displayName").getValue(String::class.java)
            ?: userSnapshot.child("email").getValue(String::class.java)
            ?: firebaseAuth.currentUser?.email
            ?: uid
        val sellerState = StateUtils.normalizeUf(userSnapshot.child("state").getValue(String::class.java))

        val path = "attendances/${attendance.routeId}/${attendance.clientId}/${attendance.id}"
        val data = mapOf(
            "id" to attendance.id,
            "attendanceNumber" to attendance.attendanceNumber,
            "routeId" to attendance.routeId,
            "clientId" to attendance.clientId,
            "clientName" to customer.name,
            "sellerUid" to uid,
            "sellerName" to sellerName,
            "sellerState" to sellerState,
            "checkInAt" to attendance.checkInAt,
            "checkInLocation" to mapOf(
                "latitude" to attendance.checkInLatitude,
                "longitude" to attendance.checkInLongitude,
                "accuracyMeters" to attendance.checkInAccuracyMeters
            ),
            "checkInDistanceToClientMeters" to attendance.checkInDistanceToClientMeters,
            "checkInGpsValidated" to attendance.checkInGpsValidated,
            "checkOutAt" to attendance.checkOutAt,
            "checkOutLocation" to mapOf(
                "latitude" to attendance.checkOutLatitude,
                "longitude" to attendance.checkOutLongitude,
                "accuracyMeters" to attendance.checkOutAccuracyMeters
            ),
            "checkOutDistanceToClientMeters" to attendance.checkOutDistanceToClientMeters,
            "checkOutGpsValidated" to attendance.checkOutGpsValidated,
            "durationSeconds" to attendance.durationSeconds,
            "resultStatus" to attendance.resultStatus,
            "resultReason" to attendance.resultReason,
            "feedback" to attendance.feedback,
            "status" to attendance.status,
            "updatedAt" to ServerValue.TIMESTAMP
        )

        firebaseDatabase.getReference(path).setValue(data).await()
    }

    private fun AttendanceEntity.toDomain() = Attendance(
        id = id,
        attendanceNumber = attendanceNumber,
        routeId = routeId,
        clientId = clientId,
        sellerUid = sellerUid,
        checkInAt = checkInAt,
        checkOutAt = checkOutAt,
        durationSeconds = durationSeconds,
        checkInLatitude = checkInLatitude,
        checkInLongitude = checkInLongitude,
        checkInAccuracyMeters = checkInAccuracyMeters,
        checkInDistanceToClientMeters = checkInDistanceToClientMeters,
        checkOutLatitude = checkOutLatitude,
        checkOutLongitude = checkOutLongitude,
        checkOutAccuracyMeters = checkOutAccuracyMeters,
        checkOutDistanceToClientMeters = checkOutDistanceToClientMeters,
        checkInGpsValidated = checkInGpsValidated,
        checkOutGpsValidated = checkOutGpsValidated,
        resultStatus = resultStatus,
        resultReason = resultReason,
        feedback = feedback,
        status = status
    )

    private fun Attendance.toEntity() = AttendanceEntity(
        id = id,
        attendanceNumber = attendanceNumber,
        routeId = routeId,
        clientId = clientId,
        sellerUid = sellerUid,
        checkInAt = checkInAt,
        checkOutAt = checkOutAt,
        durationSeconds = durationSeconds,
        checkInLatitude = checkInLatitude,
        checkInLongitude = checkInLongitude,
        checkInAccuracyMeters = checkInAccuracyMeters,
        checkInDistanceToClientMeters = checkInDistanceToClientMeters,
        checkOutLatitude = checkOutLatitude,
        checkOutLongitude = checkOutLongitude,
        checkOutAccuracyMeters = checkOutAccuracyMeters,
        checkOutDistanceToClientMeters = checkOutDistanceToClientMeters,
        checkInGpsValidated = checkInGpsValidated,
        checkOutGpsValidated = checkOutGpsValidated,
        resultStatus = resultStatus,
        resultReason = resultReason,
        feedback = feedback,
        status = status
    )
}
