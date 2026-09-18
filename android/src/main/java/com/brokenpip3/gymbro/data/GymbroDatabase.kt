package com.brokenpip3.gymbro.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.brokenpip3.gymbro.data.dao.ExerciseDao
import com.brokenpip3.gymbro.data.dao.ScheduleDao
import com.brokenpip3.gymbro.data.dao.WorkoutRunDao
import com.brokenpip3.gymbro.data.entities.ExerciseEntity
import com.brokenpip3.gymbro.data.entities.ExerciseResultEntity
import com.brokenpip3.gymbro.data.entities.ScheduleEntity
import com.brokenpip3.gymbro.data.entities.ScheduleExerciseEntity
import com.brokenpip3.gymbro.data.entities.SetResultEntity
import com.brokenpip3.gymbro.data.entities.WorkoutRunEntity

internal val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            if (!db.hasColumn("set_results", "isCompleted")) {
                db.execSQL("ALTER TABLE set_results ADD COLUMN isCompleted INTEGER NOT NULL DEFAULT 1")
            }
            if (db.hasDuplicateSetOrders()) {
                db.execSQL(
                    """
                    UPDATE set_results AS current
                    SET setOrder = (
                        SELECT COUNT(*)
                        FROM set_results AS prior
                        WHERE prior.exerciseResultId = current.exerciseResultId
                          AND (
                              prior.setOrder < current.setOrder
                              OR (prior.setOrder = current.setOrder AND prior.id < current.id)
                          )
                    )
                    """.trimIndent(),
                )
            }
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_set_results_exerciseResultId_setOrder " +
                    "ON set_results(exerciseResultId, setOrder)",
            )
        }
    }

@Database(
    entities = [
        ExerciseEntity::class,
        ScheduleEntity::class,
        ScheduleExerciseEntity::class,
        WorkoutRunEntity::class,
        ExerciseResultEntity::class,
        SetResultEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class GymbroDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao

    abstract fun scheduleDao(): ScheduleDao

    abstract fun workoutRunDao(): WorkoutRunDao

    companion object {
        fun create(context: Context): GymbroDatabase =
            Room
                .databaseBuilder(
                    context.applicationContext,
                    GymbroDatabase::class.java,
                    "gymbro.db",
                ).addMigrations(MIGRATION_1_2)
                .build()
    }
}

private fun SupportSQLiteDatabase.hasColumn(
    tableName: String,
    columnName: String,
): Boolean =
    query("PRAGMA table_info($tableName)").use { cursor ->
        val nameColumn = cursor.getColumnIndexOrThrow("name")
        generateSequence {
            if (cursor.moveToNext()) cursor.getString(nameColumn) else null
        }.any { it == columnName }
    }

private fun SupportSQLiteDatabase.hasDuplicateSetOrders(): Boolean =
    query(
        """
        SELECT 1
        FROM set_results
        GROUP BY exerciseResultId, setOrder
        HAVING COUNT(*) > 1
        LIMIT 1
        """.trimIndent(),
    ).use { cursor -> cursor.moveToFirst() }
