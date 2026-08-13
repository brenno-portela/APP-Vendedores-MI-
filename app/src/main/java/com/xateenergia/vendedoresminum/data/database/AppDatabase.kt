package com.xateenergia.vendedoresminum.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.xateenergia.vendedoresminum.data.dao.AttendanceDao
import com.xateenergia.vendedoresminum.data.dao.CustomerDao
import com.xateenergia.vendedoresminum.data.dao.PlannedRouteDao
import com.xateenergia.vendedoresminum.data.entities.AttendanceEntity
import com.xateenergia.vendedoresminum.data.entities.CustomerEntity
import com.xateenergia.vendedoresminum.data.entities.PlannedRouteEntity
import com.xateenergia.vendedoresminum.data.entities.PlannedRouteStopEntity

@Database(
    entities = [
        CustomerEntity::class,
        PlannedRouteEntity::class,
        PlannedRouteStopEntity::class,
        AttendanceEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun plannedRouteDao(): PlannedRouteDao
    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vendedores_minum.db"
                )
                    .addMigrations(
                        MIGRATION_1_TO_2,
                        MIGRATION_2_TO_3,
                        MIGRATION_3_TO_4,
                        MIGRATION_4_TO_5,
                        MIGRATION_5_TO_6
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_TO_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE customers ADD COLUMN opportunity TEXT")
                database.execSQL("ALTER TABLE customers ADD COLUMN cnpjCpf TEXT")
                database.execSQL("ALTER TABLE customers ADD COLUMN externalId TEXT")
                database.execSQL("ALTER TABLE customers ADD COLUMN email TEXT")
                database.execSQL("ALTER TABLE customers ADD COLUMN responsavel TEXT")
                database.execSQL("ALTER TABLE customers ADD COLUMN ultimaAtualizacao TEXT")
                database.execSQL("ALTER TABLE customers ADD COLUMN distributor TEXT")
                database.execSQL("ALTER TABLE customers ADD COLUMN responsableSalesperson TEXT")
                database.execSQL("ALTER TABLE customers ADD COLUMN tags TEXT")
                database.execSQL("ALTER TABLE customers ADD COLUMN expectedRevenue TEXT")
                database.execSQL("ALTER TABLE customers ADD COLUMN origem TEXT")
                database.execSQL("ALTER TABLE customers ADD COLUMN pipelineStage TEXT")
                database.execSQL("ALTER TABLE customers ADD COLUMN clientName TEXT")
                database.execSQL("ALTER TABLE customers ADD COLUMN country TEXT")
            }
        }

        private val MIGRATION_2_TO_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE planned_routes ADD COLUMN isCompleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_3_TO_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE planned_routes ADD COLUMN notCompletedReason TEXT")
            }
        }

        private val MIGRATION_4_TO_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Cada parada passa a guardar seu proprio resultado de visita e a localizacao
                // onde o vendedor registrou o feedback.
                database.execSQL("ALTER TABLE planned_route_stops ADD COLUMN visitStatus TEXT NOT NULL DEFAULT 'pending'")
                database.execSQL("ALTER TABLE planned_route_stops ADD COLUMN feedback TEXT")
                database.execSQL("ALTER TABLE planned_route_stops ADD COLUMN feedbackAt INTEGER")
                database.execSQL("ALTER TABLE planned_route_stops ADD COLUMN feedbackLatitude REAL")
                database.execSQL("ALTER TABLE planned_route_stops ADD COLUMN feedbackLongitude REAL")
            }
        }

        private val MIGRATION_5_TO_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS visit_attendances (
                        id TEXT NOT NULL PRIMARY KEY,
                        attendanceNumber INTEGER NOT NULL,
                        routeId TEXT NOT NULL,
                        clientId INTEGER NOT NULL,
                        sellerUid TEXT NOT NULL,
                        checkInAt INTEGER NOT NULL,
                        checkOutAt INTEGER,
                        durationSeconds INTEGER,
                        checkInLatitude REAL,
                        checkInLongitude REAL,
                        checkInAccuracyMeters REAL,
                        checkInDistanceToClientMeters REAL,
                        checkOutLatitude REAL,
                        checkOutLongitude REAL,
                        checkOutAccuracyMeters REAL,
                        checkOutDistanceToClientMeters REAL,
                        checkInGpsValidated INTEGER NOT NULL DEFAULT 0,
                        checkOutGpsValidated INTEGER,
                        resultStatus TEXT,
                        resultReason TEXT,
                        feedback TEXT,
                        status TEXT NOT NULL DEFAULT 'in_progress'
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
