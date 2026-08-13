package com.xateenergia.vendedoresminum.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xateenergia.vendedoresminum.data.entities.VisitAttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitAttendanceDao {
    @Query("SELECT * FROM visit_attendances WHERE routeId = :routeId ORDER BY checkInAt DESC")
    fun observeForRoute(routeId: String): Flow<List<VisitAttendanceEntity>>

    @Query(
        """
        SELECT * FROM visit_attendances
        WHERE routeId = :routeId AND status IN ('in_progress', 'awaiting_feedback')
        ORDER BY checkInAt DESC
        LIMIT 1
        """
    )
    suspend fun findOpenForRoute(routeId: String): VisitAttendanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(attendance: VisitAttendanceEntity)
}
