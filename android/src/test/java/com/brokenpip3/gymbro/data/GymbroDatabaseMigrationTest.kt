package com.brokenpip3.gymbro.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class GymbroDatabaseMigrationTest {
    @Test
    fun currentVersionOneSchemaMigratesWithoutChangingExistingData() {
        val databaseFile = createVersionOneDatabase(legacySchema = false)

        val database = openMigratedDatabase(databaseFile)
        val migratedDatabase = database.openHelper.writableDatabase

        assertEquals(2, migratedDatabase.version)
        migratedDatabase.query("SELECT reps, isCompleted FROM set_results WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(8, cursor.getInt(cursor.getColumnIndexOrThrow("reps")))
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("isCompleted")))
        }
        assertHasSetOrderIndex(migratedDatabase)

        database.close()
        databaseFile.delete()
    }

    @Test
    fun legacyVersionOneSchemaAddsCompletionAndNormalizesDuplicateSetOrders() {
        val databaseFile = createVersionOneDatabase(legacySchema = true, includeDuplicateSet = true)

        val database = openMigratedDatabase(databaseFile)
        val migratedDatabase = database.openHelper.writableDatabase

        assertEquals(2, migratedDatabase.version)
        migratedDatabase
            .query(
                "SELECT setOrder, isCompleted FROM set_results WHERE exerciseResultId = 2 ORDER BY id",
            ).use { cursor ->
                assertEquals(2, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("setOrder")))
                assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("isCompleted")))
                assertTrue(cursor.moveToNext())
                assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("setOrder")))
                assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("isCompleted")))
            }
        assertHasSetOrderIndex(migratedDatabase)

        database.close()
        databaseFile.delete()
    }

    private fun openMigratedDatabase(databaseFile: File): GymbroDatabase =
        Room
            .databaseBuilder(
                RuntimeEnvironment.getApplication() as Context,
                GymbroDatabase::class.java,
                databaseFile.path,
            ).addMigrations(MIGRATION_1_2)
            .build()

    private fun assertHasSetOrderIndex(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database
            .query(
                """
                SELECT name FROM sqlite_master
                WHERE type = 'index'
                  AND name = 'index_set_results_exerciseResultId_setOrder'
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
            }
    }
}

private fun createVersionOneDatabase(
    legacySchema: Boolean,
    includeDuplicateSet: Boolean = false,
): File {
    val context = RuntimeEnvironment.getApplication() as Context
    val databaseFile = File(context.cacheDir, "gymbro-v1-${System.nanoTime()}.db")
    val database = SQLiteDatabase.openOrCreateDatabase(databaseFile, null)
    createCoreTables(database)
    createSetResultsTable(database, legacySchema)

    if (!legacySchema) {
        database.execSQL(
            "CREATE UNIQUE INDEX index_set_results_exerciseResultId_setOrder " +
                "ON set_results(exerciseResultId, setOrder)",
        )
    }
    insertSetResults(database, legacySchema, includeDuplicateSet)
    database.execSQL("PRAGMA user_version = 1")
    database.close()
    return databaseFile
}

private fun createCoreTables(database: SQLiteDatabase) {
    database.execSQL(
        """
        CREATE TABLE exercises (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            name TEXT NOT NULL,
            notes TEXT,
            trackingMode TEXT NOT NULL,
            createdAt INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL
        )
        """.trimIndent(),
    )
    database.execSQL(
        """
        CREATE TABLE schedules (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            name TEXT NOT NULL,
            notes TEXT,
            createdAt INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL
        )
        """.trimIndent(),
    )
    database.execSQL(
        """
        CREATE TABLE schedule_exercises (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            scheduleId INTEGER NOT NULL,
            exerciseId INTEGER NOT NULL,
            sortOrder INTEGER NOT NULL,
            targetSets INTEGER,
            targetReps INTEGER,
            targetWeight REAL,
            targetDurationSeconds INTEGER,
            targetDistance REAL
        )
        """.trimIndent(),
    )
    database.execSQL(
        """
        CREATE TABLE workout_runs (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            scheduleId INTEGER NOT NULL,
            scheduleNameSnapshot TEXT NOT NULL,
            startedAt INTEGER NOT NULL,
            finishedAt INTEGER,
            notes TEXT
        )
        """.trimIndent(),
    )
    database.execSQL(
        """
        CREATE TABLE exercise_results (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            workoutRunId INTEGER NOT NULL,
            exerciseId INTEGER,
            exerciseNameSnapshot TEXT NOT NULL,
            trackingModeSnapshot TEXT NOT NULL,
            sortOrder INTEGER NOT NULL,
            notes TEXT
        )
        """.trimIndent(),
    )
}

private fun createSetResultsTable(
    database: SQLiteDatabase,
    legacySchema: Boolean,
) {
    val completionColumn = if (legacySchema) "" else ", isCompleted INTEGER NOT NULL DEFAULT 1"
    database.execSQL(
        """
        CREATE TABLE set_results (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            exerciseResultId INTEGER NOT NULL,
            setOrder INTEGER NOT NULL,
            reps INTEGER,
            weight REAL,
            durationSeconds INTEGER,
            distance REAL,
            notes TEXT$completionColumn
        )
        """.trimIndent(),
    )
}

private fun insertSetResults(
    database: SQLiteDatabase,
    legacySchema: Boolean,
    includeDuplicateSet: Boolean,
) {
    val firstExerciseResultId = if (includeDuplicateSet) 2 else 1
    database.execSQL(
        """
        INSERT INTO set_results (
            id, exerciseResultId, setOrder, reps, weight, durationSeconds, distance, notes
            ${if (legacySchema) "" else ", isCompleted"}
        ) VALUES (1, $firstExerciseResultId, 0, 8, 80.0, NULL, NULL, 'legacy'${if (legacySchema) "" else ", 1"})
        """.trimIndent(),
    )
    if (includeDuplicateSet) {
        database.execSQL(
            """
            INSERT INTO set_results (
                id, exerciseResultId, setOrder, reps, weight, durationSeconds, distance, notes
            ) VALUES (2, 2, 0, 10, 90.0, NULL, NULL, 'duplicate order')
            """.trimIndent(),
        )
    }
}
