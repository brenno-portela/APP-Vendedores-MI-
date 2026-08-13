package com.xateenergia.vendedoresminum.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xateenergia.vendedoresminum.data.entities.AttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AttendanceEntity)

    @Query(
        """
        SELECT * FROM visit_attendances
        WHERE routeId = :routeId AND clientId = :clientId
        ORDER BY attendanceNumber ASC
        """
    )
    fun observeByClient(routeId: String, clientId: Long): Flow<List<AttendanceEntity>>

    @Query(
        """
        SELECT * FROM visit_attendances
        WHERE routeId = :routeId
        ORDER BY checkInAt ASC
        """
    )
    fun observeByRoute(routeId: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM visit_attendances WHERE status = 'in_progress' LIMIT 1")
    suspend fun getActiveAttendance(): AttendanceEntity?

    @Query(
        """
        SELECT COUNT(*) FROM visit_attendances
        WHERE routeId = :routeId AND clientId = :clientId
        """
    )
    suspend fun countByClient(routeId: String, clientId: Long): Int

    @Query(
        """
        SELECT * FROM visit_attendances
        WHERE routeId = :routeId AND clientId = :clientId AND status = 'in_progress'
        LIMIT 1
        """
    )
    suspend fun getActiveByClient(routeId: String, clientId: Long): AttendanceEntity?

    @Query("DELETE FROM visit_attendances WHERE routeId = :routeId")
    suspend fun deleteByRoute(routeId: String)
}
