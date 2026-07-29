package com.xateenergia.vendedoresminum.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.xateenergia.vendedoresminum.data.entities.PlannedRouteEntity
import com.xateenergia.vendedoresminum.data.entities.PlannedRouteStopEntity
import com.xateenergia.vendedoresminum.domain.model.PlannedRouteStopSummary
import com.xateenergia.vendedoresminum.domain.model.PlannedRouteSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannedRouteDao {
    @Query(
        """
        SELECT planned_routes.id AS id,
               planned_routes.name AS name,
               planned_routes.mainCustomerName AS mainCustomerName,
               planned_routes.mainLatitude AS mainLatitude,
               planned_routes.mainLongitude AS mainLongitude,
               planned_routes.radiusKm AS radiusKm,
               planned_routes.createdAt AS createdAt,
               planned_routes.isCompleted AS isCompleted,
               planned_routes.notCompletedReason AS notCompletedReason,
               COUNT(planned_route_stops.customerId) AS stopCount
        FROM planned_routes
        LEFT JOIN planned_route_stops ON planned_routes.id = planned_route_stops.routeId
        GROUP BY planned_routes.id
        ORDER BY planned_routes.createdAt DESC
        """
    )
    fun observeSummaries(): Flow<List<PlannedRouteSummary>>

    @Query("UPDATE planned_routes SET isCompleted = :isCompleted, notCompletedReason = :reason WHERE id = :routeId")
    suspend fun updateRouteCompletionStatus(routeId: Long, isCompleted: Boolean, reason: String?)

    @Query(
        """
        SELECT planned_route_stops.routeId AS routeId,
               planned_route_stops.customerId AS customerId,
               planned_route_stops.orderIndex AS orderIndex,
               planned_route_stops.distanceMeters AS distanceMeters,
               planned_route_stops.visitStatus AS visitStatus,
               planned_route_stops.feedback AS feedback,
               planned_route_stops.feedbackAt AS feedbackAt,
               planned_route_stops.feedbackLatitude AS feedbackLatitude,
               planned_route_stops.feedbackLongitude AS feedbackLongitude,
               customers.name AS customerName,
               customers.clientName AS companyName,
               customers.phone AS phone
        FROM planned_route_stops
        LEFT JOIN customers ON customers.id = planned_route_stops.customerId
        WHERE planned_route_stops.routeId = :routeId
        ORDER BY planned_route_stops.orderIndex ASC
        """
    )
    fun observeStopSummaries(routeId: Long): Flow<List<PlannedRouteStopSummary>>

    @Query(
        """
        UPDATE planned_route_stops
        SET visitStatus = :visitStatus,
            feedback = :feedback,
            feedbackAt = :feedbackAt,
            feedbackLatitude = :feedbackLatitude,
            feedbackLongitude = :feedbackLongitude
        WHERE routeId = :routeId AND customerId = :customerId
        """
    )
    suspend fun updateStopFeedback(
        routeId: Long,
        customerId: Long,
        visitStatus: String,
        feedback: String,
        feedbackAt: Long,
        feedbackLatitude: Double,
        feedbackLongitude: Double
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: PlannedRouteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStops(stops: List<PlannedRouteStopEntity>)

    @Transaction
    suspend fun saveRoute(route: PlannedRouteEntity, stops: List<PlannedRouteStopEntity>): Long {
        val routeId = insertRoute(route)
        insertStops(stops.map { it.copy(routeId = routeId) })
        return routeId
    }

    @Query("DELETE FROM planned_routes")
    suspend fun deleteAll()
}
