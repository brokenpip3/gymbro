package com.brokenpip3.gymbro.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.brokenpip3.gymbro.data.GymbroDatabase
import com.brokenpip3.gymbro.data.entities.ExerciseEntity
import com.brokenpip3.gymbro.data.entities.ScheduleEntity
import com.brokenpip3.gymbro.data.entities.ScheduleExerciseEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomGymbroBackupStoreTest {
    private lateinit var database: GymbroDatabase
    private lateinit var store: GymbroBackupStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, GymbroDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        store = GymbroBackupStore(RoomGymbroBackupDataSource(database))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun mergeImportAddsExercisesWithFreshIdsAndRemapsScheduleRows() =
        runTest {
            database.exerciseDao().insertExercise(exercise(id = 0, name = "Bench Press"))
            val json =
                backupText(
                    scope = "exercises_schedules",
                    exercises =
                        listOf(
                            backupExercise(id = 10, name = "Bench Press"),
                            backupExercise(id = 11, name = "Squat"),
                        ),
                    schedules = listOf(backupSchedule(id = 20, name = "Push Day")),
                    scheduleExercises =
                        listOf(
                            backupScheduleExercise(id = 30, scheduleId = 20, exerciseId = 10, sortOrder = 0),
                            backupScheduleExercise(id = 31, scheduleId = 20, exerciseId = 11, sortOrder = 1),
                        ),
                )

            store.importText(json)

            val exercises = database.exerciseDao().getAllExercises()
            assertEquals(2, exercises.size)
            val bench = exercises.first { it.name == "Bench Press" }
            val squat = exercises.first { it.name == "Squat" }
            assertTrue(exercises.none { it.id == 10L || it.id == 11L })
            val schedules = database.scheduleDao().getAllSchedules()
            assertEquals(1, schedules.size)
            assertTrue(schedules.none { it.id == 20L })
            val rows = database.scheduleDao().getAllScheduleExercises()
            assertEquals(2, rows.size)
            assertTrue(rows.all { it.scheduleId == schedules.first().id })
            assertTrue(rows.none { it.exerciseId == 10L || it.exerciseId == 11L })
            assertTrue(rows.any { it.exerciseId == bench.id })
            assertTrue(rows.any { it.exerciseId == squat.id })
        }

    @Test
    fun mergeImportAppendsRowsToExistingScheduleAfterCurrentExercises() =
        runTest {
            val exerciseId = database.exerciseDao().insertExercise(exercise(id = 0, name = "Dips"))
            val scheduleId = database.scheduleDao().insertSchedule(schedule(id = 0, name = "Push Day"))
            database.scheduleDao().insertScheduleExercise(
                ScheduleExerciseEntity(
                    id = 0,
                    scheduleId = scheduleId,
                    exerciseId = exerciseId,
                    sortOrder = 0,
                    targetSets = null,
                    targetReps = null,
                    targetWeight = null,
                    targetDurationSeconds = null,
                    targetDistance = null,
                ),
            )
            val json =
                backupText(
                    scope = "exercises_schedules",
                    exercises = listOf(backupExercise(id = 10, name = "Squat")),
                    schedules = listOf(backupSchedule(id = 20, name = "push day")),
                    scheduleExercises =
                        listOf(backupScheduleExercise(id = 30, scheduleId = 20, exerciseId = 10, sortOrder = 0)),
                )

            store.importText(json)

            val rows = database.scheduleDao().getScheduleExercises(scheduleId)
            assertEquals(2, rows.size)
            assertEquals(1, rows.maxOf { it.sortOrder })
        }

    @Test
    fun mergeImportValidationFailureLeavesDatabaseUnchanged() =
        runTest {
            val exerciseId = database.exerciseDao().insertExercise(exercise(id = 0, name = "Dips"))
            val json =
                backupText(
                    scope = "exercises_schedules",
                    exercises = listOf(backupExercise(id = 10, name = "Squat")),
                    schedules = listOf(backupSchedule(id = 20, name = "Push Day")),
                    scheduleExercises =
                        listOf(backupScheduleExercise(id = 30, scheduleId = 20, exerciseId = 99, sortOrder = 0)),
                )

            val exception = runCatching { store.importText(json) }.exceptionOrNull()

            assertTrue(exception is InvalidBackupException)
            assertEquals(1, database.exerciseDao().getAllExercises().size)
            assertTrue(database.exerciseDao().getAllExercises().any { it.id == exerciseId })
            assertTrue(database.scheduleDao().getAllSchedules().isEmpty())
        }

    @Test
    fun importsAndExportsFullBackupWithExplicitIdsAndSetCompletion() =
        runTest {
            store.importText(fullBackupText())

            val exported = GymbroBackupCodec.decode(store.exportText())

            assertEquals(7L, exported.exercises.single().id)
            assertEquals(8L, exported.schedules.single().id)
            assertEquals(9L, exported.scheduleExercises.single().id)
            assertEquals(10L, exported.workoutRuns.single().id)
            assertEquals(11L, exported.exerciseResults.single().id)
            assertEquals(12L, exported.setResults.single().id)
            assertEquals(false, exported.setResults.single().isCompleted)
        }

    @Test
    fun invalidDuplicateSetOrderImportDoesNotDeleteExistingData() =
        runTest {
            store.importText(fullBackupText())

            val exception =
                runCatching {
                    store.importText(duplicateSetOrderBackupText())
                }.exceptionOrNull()

            assertTrue(exception is InvalidBackupException)
            val exported = GymbroBackupCodec.decode(store.exportText())
            assertEquals(7L, exported.exercises.single().id)
            assertEquals(12L, exported.setResults.single().id)
        }

    @Test
    fun importsAndExportsHistoryForDeletedScheduleAndExerciseSnapshots() =
        runTest {
            store.importText(deletedHistoryBackupText())

            val exported = GymbroBackupCodec.decode(store.exportText())

            assertEquals(emptyList<BackupSchedule>(), exported.schedules)
            assertEquals(emptyList<BackupExercise>(), exported.exercises)
            assertEquals(404L, exported.workoutRuns.single().scheduleId)
            assertEquals("Deleted schedule", exported.workoutRuns.single().scheduleNameSnapshot)
            assertEquals(505L, exported.exerciseResults.single().exerciseId)
            assertEquals("Deleted exercise", exported.exerciseResults.single().exerciseNameSnapshot)
        }
}

private fun fullBackupText(): String =
    GymbroBackupCodec.encode(
        GymbroBackup(
            schemaVersion = GymbroBackupCodec.SUPPORTED_SCHEMA_VERSION,
            exercises =
                listOf(
                    BackupExercise(
                        id = 7,
                        name = "Bench",
                        notes = "Paused",
                        trackingMode = "strength",
                        createdAt = 10,
                        updatedAt = 20,
                    ),
                ),
            schedules =
                listOf(
                    BackupSchedule(
                        id = 8,
                        name = "Push",
                        notes = null,
                        createdAt = 30,
                        updatedAt = 40,
                    ),
                ),
            scheduleExercises =
                listOf(
                    BackupScheduleExercise(
                        id = 9,
                        scheduleId = 8,
                        exerciseId = 7,
                        sortOrder = 0,
                        targetSets = 5,
                        targetReps = 5,
                        targetWeight = 80.0,
                        targetDurationSeconds = null,
                        targetDistance = null,
                    ),
                ),
            workoutRuns =
                listOf(
                    BackupWorkoutRun(
                        id = 10,
                        scheduleId = 8,
                        scheduleNameSnapshot = "Push",
                        startedAt = 50,
                        finishedAt = 70,
                        notes = null,
                    ),
                ),
            exerciseResults =
                listOf(
                    BackupExerciseResult(
                        id = 11,
                        workoutRunId = 10,
                        exerciseId = 7,
                        exerciseNameSnapshot = "Bench",
                        trackingModeSnapshot = "strength",
                        sortOrder = 0,
                        notes = null,
                    ),
                ),
            setResults =
                listOf(
                    BackupSetResult(
                        id = 12,
                        exerciseResultId = 11,
                        setOrder = 1,
                        reps = 5,
                        weight = 80.0,
                        durationSeconds = null,
                        distance = null,
                        notes = "Clean",
                        isCompleted = false,
                    ),
                ),
        ),
    )

private fun duplicateSetOrderBackupText(): String =
    GymbroBackupCodec.encode(
        GymbroBackup(
            schemaVersion = GymbroBackupCodec.SUPPORTED_SCHEMA_VERSION,
            workoutRuns =
                listOf(
                    BackupWorkoutRun(
                        id = 20,
                        scheduleId = 404,
                        scheduleNameSnapshot = "Deleted schedule",
                        startedAt = 1,
                        finishedAt = 2,
                        notes = null,
                    ),
                ),
            exerciseResults =
                listOf(
                    BackupExerciseResult(
                        id = 21,
                        workoutRunId = 20,
                        exerciseId = null,
                        exerciseNameSnapshot = "Run",
                        trackingModeSnapshot = "timed",
                        sortOrder = 0,
                        notes = null,
                    ),
                ),
            setResults =
                listOf(
                    BackupSetResult(
                        id = 22,
                        exerciseResultId = 21,
                        setOrder = 1,
                        reps = null,
                        weight = null,
                        durationSeconds = 60,
                        distance = null,
                        notes = null,
                    ),
                    BackupSetResult(
                        id = 23,
                        exerciseResultId = 21,
                        setOrder = 1,
                        reps = null,
                        weight = null,
                        durationSeconds = 70,
                        distance = null,
                        notes = null,
                    ),
                ),
        ),
    )

private fun deletedHistoryBackupText(): String =
    GymbroBackupCodec.encode(
        GymbroBackup(
            schemaVersion = GymbroBackupCodec.SUPPORTED_SCHEMA_VERSION,
            workoutRuns =
                listOf(
                    BackupWorkoutRun(
                        id = 30,
                        scheduleId = 404,
                        scheduleNameSnapshot = "Deleted schedule",
                        startedAt = 10,
                        finishedAt = 20,
                        notes = null,
                    ),
                ),
            exerciseResults =
                listOf(
                    BackupExerciseResult(
                        id = 31,
                        workoutRunId = 30,
                        exerciseId = 505,
                        exerciseNameSnapshot = "Deleted exercise",
                        trackingModeSnapshot = "strength",
                        sortOrder = 0,
                        notes = null,
                    ),
                ),
            setResults =
                listOf(
                    BackupSetResult(
                        id = 32,
                        exerciseResultId = 31,
                        setOrder = 1,
                        reps = 8,
                        weight = 50.0,
                        durationSeconds = null,
                        distance = null,
                        notes = null,
                    ),
                ),
        ),
    )

private fun exercise(
    id: Long,
    name: String,
): ExerciseEntity =
    ExerciseEntity(
        id = id,
        name = name,
        notes = null,
        trackingMode = "strength",
        createdAt = 1,
        updatedAt = 1,
    )

private fun schedule(
    id: Long,
    name: String,
): ScheduleEntity =
    ScheduleEntity(
        id = id,
        name = name,
        notes = null,
        createdAt = 1,
        updatedAt = 1,
    )

private fun backupExercise(
    id: Long,
    name: String,
): BackupExercise =
    BackupExercise(
        id = id,
        name = name,
        notes = null,
        trackingMode = "strength",
        createdAt = 1,
        updatedAt = 1,
    )

private fun backupSchedule(
    id: Long,
    name: String,
): BackupSchedule =
    BackupSchedule(
        id = id,
        name = name,
        notes = null,
        createdAt = 1,
        updatedAt = 1,
    )

private fun backupScheduleExercise(
    id: Long,
    scheduleId: Long,
    exerciseId: Long,
    sortOrder: Int,
): BackupScheduleExercise =
    BackupScheduleExercise(
        id = id,
        scheduleId = scheduleId,
        exerciseId = exerciseId,
        sortOrder = sortOrder,
        targetSets = null,
        targetReps = null,
        targetWeight = null,
        targetDurationSeconds = null,
        targetDistance = null,
    )

private fun backupText(
    scope: String = "full",
    exercises: List<BackupExercise> = emptyList(),
    schedules: List<BackupSchedule> = emptyList(),
    scheduleExercises: List<BackupScheduleExercise> = emptyList(),
    workoutRuns: List<BackupWorkoutRun> = emptyList(),
    exerciseResults: List<BackupExerciseResult> = emptyList(),
    setResults: List<BackupSetResult> = emptyList(),
): String =
    GymbroBackupCodec.encode(
        GymbroBackup(
            schemaVersion = GymbroBackupCodec.SUPPORTED_SCHEMA_VERSION,
            scope = scope,
            exercises = exercises,
            schedules = schedules,
            scheduleExercises = scheduleExercises,
            workoutRuns = workoutRuns,
            exerciseResults = exerciseResults,
            setResults = setResults,
        ),
    )
