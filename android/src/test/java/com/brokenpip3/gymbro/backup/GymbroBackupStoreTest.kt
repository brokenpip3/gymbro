package com.brokenpip3.gymbro.backup

import com.brokenpip3.gymbro.data.entities.ExerciseEntity
import com.brokenpip3.gymbro.data.entities.ExerciseResultEntity
import com.brokenpip3.gymbro.data.entities.ScheduleEntity
import com.brokenpip3.gymbro.data.entities.ScheduleExerciseEntity
import com.brokenpip3.gymbro.data.entities.SetResultEntity
import com.brokenpip3.gymbro.data.entities.WorkoutRunEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GymbroBackupStoreTest {
    @Test
    fun decodingLegacyBackupWithoutScopeDefaultsToFull() {
        val json =
            """{"schemaVersion":1,"exercises":[],"schedules":[],"scheduleExercises":[],""" +
                """"workoutRuns":[],"exerciseResults":[],"setResults":[]}"""

        val backup = GymbroBackupCodec.decode(json)

        assertEquals("full", backup.scope)
    }

    @Test
    fun importRejectsExercisesScopeContainingSchedulesOrWorkoutData() =
        runTest {
            val store = GymbroBackupStore(FakeGymbroBackupDataSource())
            val json =
                """{"schemaVersion":1,"scope":"exercises","exercises":[{"id":1,"name":"Squat",""" +
                    """"notes":null,"trackingMode":"strength","createdAt":1,"updatedAt":1}],""" +
                    """"schedules":[{"id":2,"name":"Leg day","notes":null,"createdAt":1,"updatedAt":1}],""" +
                    """"scheduleExercises":[],"workoutRuns":[],"exerciseResults":[],"setResults":[]}"""

            store.captureImportException(json)
        }

    @Test
    fun importRejectsExercisesScopeWithNoExercises() =
        runTest {
            val store = GymbroBackupStore(FakeGymbroBackupDataSource())

            store.captureImportException(backupText(scope = "exercises"))
        }

    @Test
    fun importRejectsUnknownScope() =
        runTest {
            val store = GymbroBackupStore(FakeGymbroBackupDataSource())

            store.captureImportException(backupText(scope = "bogus"))
        }

    @Test
    fun exportExercisesScopeEmitsOnlyExercises() =
        runTest {
            val store = GymbroBackupStore(sampleDataSource())

            val backup = GymbroBackupCodec.decode(store.exportText(BackupScope.EXERCISES))

            assertEquals(BackupScope.EXERCISES.wireValue, backup.scope)
            assertTrue(backup.exercises.isNotEmpty())
            assertTrue(backup.schedules.isEmpty())
            assertTrue(backup.scheduleExercises.isEmpty())
            assertTrue(backup.workoutRuns.isEmpty())
            assertTrue(backup.exerciseResults.isEmpty())
            assertTrue(backup.setResults.isEmpty())
        }

    @Test
    fun exportExercisesSchedulesScopeOmitsWorkoutHistory() =
        runTest {
            val store = GymbroBackupStore(sampleDataSource())

            val backup = GymbroBackupCodec.decode(store.exportText(BackupScope.EXERCISES_SCHEDULES))

            assertTrue(backup.exercises.isNotEmpty())
            assertTrue(backup.schedules.isNotEmpty())
            assertTrue(backup.scheduleExercises.isNotEmpty())
            assertTrue(backup.workoutRuns.isEmpty())
            assertTrue(backup.exerciseResults.isEmpty())
            assertTrue(backup.setResults.isEmpty())
        }

    @Test
    fun scopeFromWireValueMapsKnownValuesAndNullsUnknown() {
        assertEquals(BackupScope.FULL, BackupScope.fromWireValueOrNull("full"))
        assertEquals(BackupScope.EXERCISES, BackupScope.fromWireValueOrNull("exercises"))
        assertEquals(
            BackupScope.EXERCISES_SCHEDULES,
            BackupScope.fromWireValueOrNull("exercises_schedules"),
        )
        assertEquals(null, BackupScope.fromWireValueOrNull("bogus"))
        assertEquals(null, BackupScope.fromWireValueOrNull(null))
    }

    @Test
    fun exportTextIncludesAllCurrentTables() =
        runTest {
            val source =
                FakeGymbroBackupDataSource(
                    exercises =
                        listOf(
                            ExerciseEntity(
                                id = 1,
                                name = "Squat",
                                notes = "Low bar",
                                trackingMode = "strength",
                                createdAt = 10,
                                updatedAt = 20,
                            ),
                        ),
                    schedules =
                        listOf(
                            ScheduleEntity(
                                id = 2,
                                name = "Leg day",
                                notes = null,
                                createdAt = 30,
                                updatedAt = 40,
                            ),
                        ),
                    scheduleExercises =
                        listOf(
                            ScheduleExerciseEntity(
                                id = 3,
                                scheduleId = 2,
                                exerciseId = 1,
                                sortOrder = 0,
                                targetSets = 3,
                                targetReps = 10,
                                targetWeight = 60.0,
                                targetDurationSeconds = null,
                                targetDistance = null,
                            ),
                        ),
                    workoutRuns =
                        listOf(
                            WorkoutRunEntity(
                                id = 4,
                                scheduleId = 2,
                                scheduleNameSnapshot = "Leg day",
                                startedAt = 50,
                                finishedAt = 90,
                                notes = "Done",
                            ),
                        ),
                    exerciseResults =
                        listOf(
                            ExerciseResultEntity(
                                id = 5,
                                workoutRunId = 4,
                                exerciseId = 1,
                                exerciseNameSnapshot = "Squat",
                                trackingModeSnapshot = "strength",
                                sortOrder = 0,
                                notes = "Felt good",
                            ),
                        ),
                    setResults =
                        listOf(
                            SetResultEntity(
                                id = 6,
                                exerciseResultId = 5,
                                setOrder = 1,
                                reps = 10,
                                weight = 60.0,
                                durationSeconds = null,
                                distance = null,
                                notes = null,
                                isCompleted = false,
                            ),
                        ),
                )
            val store = GymbroBackupStore(source)

            val backup = GymbroBackupCodec.decode(store.exportText())

            assertEquals(GymbroBackupCodec.SUPPORTED_SCHEMA_VERSION, backup.schemaVersion)
            assertEquals(1L, backup.exercises.single().id)
            assertEquals(2L, backup.schedules.single().id)
            assertEquals(3L, backup.scheduleExercises.single().id)
            assertEquals(4L, backup.workoutRuns.single().id)
            assertEquals(5L, backup.exerciseResults.single().id)
            assertEquals(false, backup.setResults.single().isCompleted)
        }

    @Test
    fun importTextReplacesLocalDataWithDecodedBackup() =
        runTest {
            val source = FakeGymbroBackupDataSource(exercises = listOf(oldExercise()))
            val store = GymbroBackupStore(source)

            store.importText(importBackupText())

            assertEquals(
                listOf(
                    "set_results",
                    "exercise_results",
                    "workout_runs",
                    "schedule_exercises",
                    "schedules",
                    "exercises",
                ),
                source.transactionDeleteOrder,
            )
            assertEquals(7L, source.exercises.single().id)
            assertEquals(8L, source.schedules.single().id)
            assertEquals(9L, source.scheduleExercises.single().id)
            assertEquals(10L, source.workoutRuns.single().id)
            assertEquals(11L, source.exerciseResults.single().id)
            assertEquals(false, source.setResults.single().isCompleted)
        }

    @Test
    fun importTextRejectsDuplicatePrimaryIdsBeforeReplacingData() =
        runTest {
            val source = FakeGymbroBackupDataSource()
            val store = GymbroBackupStore(source)

            val exception =
                store.captureImportException(
                    backupText(
                        exercises =
                            listOf(
                                backupExercise(id = 1),
                                backupExercise(id = 1),
                            ),
                    ),
                )

            assertTrue(exception.message.orEmpty().contains("Duplicate exercises id: 1"))
            assertEquals(emptyList<String>(), source.transactionDeleteOrder)
        }

    @Test
    fun importTextRejectsMissingScheduleExerciseScheduleBeforeReplacingData() =
        runTest {
            val source = FakeGymbroBackupDataSource()
            val store = GymbroBackupStore(source)

            val exception =
                store.captureImportException(
                    backupText(
                        exercises = listOf(backupExercise(id = 1)),
                        scheduleExercises =
                            listOf(
                                backupScheduleExercise(
                                    id = 2,
                                    scheduleId = 404,
                                    exerciseId = 1,
                                ),
                            ),
                    ),
                )

            assertTrue(exception.message.orEmpty().contains("scheduleExercises.scheduleId 404 was not found"))
            assertEquals(emptyList<String>(), source.transactionDeleteOrder)
        }

    @Test
    fun importTextAllowsWorkoutRunMissingScheduleBecauseHistoryUsesSnapshot() =
        runTest {
            val source = FakeGymbroBackupDataSource()
            val store = GymbroBackupStore(source)

            store.importText(
                backupText(
                    workoutRuns = listOf(backupWorkoutRun(id = 3, scheduleId = 404)),
                ),
            )

            assertEquals(3L, source.workoutRuns.single().id)
            assertEquals(404L, source.workoutRuns.single().scheduleId)
        }

    @Test
    fun importTextAllowsExerciseResultMissingExerciseBecauseHistoryUsesSnapshot() =
        runTest {
            val source = FakeGymbroBackupDataSource()
            val store = GymbroBackupStore(source)

            store.importText(
                backupText(
                    workoutRuns = listOf(backupWorkoutRun(id = 3, scheduleId = 404)),
                    exerciseResults =
                        listOf(
                            backupExerciseResult(
                                id = 4,
                                workoutRunId = 3,
                                exerciseId = 505,
                            ),
                        ),
                ),
            )

            assertEquals(505L, source.exerciseResults.single().exerciseId)
        }

    @Test
    fun importTextRejectsDuplicateSetOrderBeforeReplacingData() =
        runTest {
            val source = FakeGymbroBackupDataSource()
            val store = GymbroBackupStore(source)

            val exception =
                store.captureImportException(
                    backupText(
                        workoutRuns = listOf(backupWorkoutRun(id = 2, scheduleId = 1)),
                        exerciseResults = listOf(backupExerciseResult(id = 3, workoutRunId = 2)),
                        setResults =
                            listOf(
                                backupSetResult(id = 4, exerciseResultId = 3, setOrder = 1),
                                backupSetResult(id = 5, exerciseResultId = 3, setOrder = 1),
                            ),
                    ),
                )

            assertTrue(exception.message.orEmpty().contains("Duplicate set_results exerciseResultId/setOrder"))
            assertEquals(emptyList<String>(), source.transactionDeleteOrder)
        }

    @Test
    fun importTextRejectsNonPositiveIdsBeforeReplacingData() =
        runTest {
            val source = FakeGymbroBackupDataSource()
            val store = GymbroBackupStore(source)

            val exception =
                store.captureImportException(
                    backupText(exercises = listOf(backupExercise(id = 0))),
                )

            assertTrue(exception.message.orEmpty().contains("exercises id must be positive: 0"))
            assertEquals(emptyList<String>(), source.transactionDeleteOrder)
        }

    @Test
    fun importTextRejectsUnknownTrackingModesBeforeReplacingData() =
        runTest {
            val source = FakeGymbroBackupDataSource()
            val store = GymbroBackupStore(source)

            val exception =
                store.captureImportException(
                    backupText(
                        exercises = listOf(backupExercise(id = 1).copy(trackingMode = "unknown")),
                    ),
                )

            assertTrue(exception.message.orEmpty().contains("Unknown tracking mode: unknown"))
            assertEquals(emptyList<String>(), source.transactionDeleteOrder)
        }

    @Test
    fun importTextRejectsDuplicateScheduleOrderBeforeReplacingData() =
        runTest {
            val source = FakeGymbroBackupDataSource()
            val store = GymbroBackupStore(source)

            val exception =
                store.captureImportException(
                    backupText(
                        exercises = listOf(backupExercise(id = 1)),
                        schedules = listOf(backupSchedule(id = 2)),
                        scheduleExercises =
                            listOf(
                                backupScheduleExercise(id = 3, scheduleId = 2, exerciseId = 1),
                                backupScheduleExercise(id = 4, scheduleId = 2, exerciseId = 1),
                            ),
                    ),
                )

            assertTrue(exception.message.orEmpty().contains("Duplicate schedule_exercises parent/order: 2/0"))
            assertEquals(emptyList<String>(), source.transactionDeleteOrder)
        }
}

private suspend fun GymbroBackupStore.captureImportException(text: String): InvalidBackupException {
    val exception = runCatching { importText(text) }.exceptionOrNull()
    assertTrue(exception is InvalidBackupException)
    return exception as InvalidBackupException
}

private fun sampleDataSource(): FakeGymbroBackupDataSource =
    FakeGymbroBackupDataSource(
        exercises = listOf(oldExercise()),
        schedules =
            listOf(
                ScheduleEntity(
                    id = 2,
                    name = "Leg day",
                    notes = null,
                    createdAt = 30,
                    updatedAt = 40,
                ),
            ),
        scheduleExercises =
            listOf(
                ScheduleExerciseEntity(
                    id = 3,
                    scheduleId = 2,
                    exerciseId = 99,
                    sortOrder = 0,
                    targetSets = 3,
                    targetReps = 10,
                    targetWeight = 60.0,
                    targetDurationSeconds = null,
                    targetDistance = null,
                ),
            ),
        workoutRuns =
            listOf(
                WorkoutRunEntity(
                    id = 4,
                    scheduleId = 2,
                    scheduleNameSnapshot = "Leg day",
                    startedAt = 50,
                    finishedAt = 60,
                    notes = null,
                ),
            ),
        exerciseResults =
            listOf(
                ExerciseResultEntity(
                    id = 5,
                    workoutRunId = 4,
                    exerciseId = 99,
                    exerciseNameSnapshot = "Old",
                    trackingModeSnapshot = "strength",
                    sortOrder = 0,
                    notes = null,
                ),
            ),
        setResults =
            listOf(
                SetResultEntity(
                    id = 6,
                    exerciseResultId = 5,
                    setOrder = 0,
                    reps = 8,
                    weight = 70.0,
                    durationSeconds = null,
                    distance = null,
                    notes = null,
                    isCompleted = true,
                ),
            ),
    )

private fun oldExercise(): ExerciseEntity =
    ExerciseEntity(
        id = 99,
        name = "Old",
        notes = null,
        trackingMode = "strength",
        createdAt = 1,
        updatedAt = 1,
    )

private fun importBackupText(): String =
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

private fun backupExercise(id: Long): BackupExercise =
    BackupExercise(
        id = id,
        name = "Exercise $id",
        notes = null,
        trackingMode = "strength",
        createdAt = id,
        updatedAt = id,
    )

private fun backupSchedule(id: Long): BackupSchedule =
    BackupSchedule(
        id = id,
        name = "Schedule $id",
        notes = null,
        createdAt = id,
        updatedAt = id,
    )

private fun backupScheduleExercise(
    id: Long,
    scheduleId: Long,
    exerciseId: Long,
): BackupScheduleExercise =
    BackupScheduleExercise(
        id = id,
        scheduleId = scheduleId,
        exerciseId = exerciseId,
        sortOrder = 0,
        targetSets = null,
        targetReps = null,
        targetWeight = null,
        targetDurationSeconds = null,
        targetDistance = null,
    )

private fun backupWorkoutRun(
    id: Long,
    scheduleId: Long,
): BackupWorkoutRun =
    BackupWorkoutRun(
        id = id,
        scheduleId = scheduleId,
        scheduleNameSnapshot = "Schedule $scheduleId",
        startedAt = id,
        finishedAt = id + 1,
        notes = null,
    )

private fun backupExerciseResult(
    id: Long,
    workoutRunId: Long,
    exerciseId: Long? = null,
): BackupExerciseResult =
    BackupExerciseResult(
        id = id,
        workoutRunId = workoutRunId,
        exerciseId = exerciseId,
        exerciseNameSnapshot = "Exercise",
        trackingModeSnapshot = "strength",
        sortOrder = 0,
        notes = null,
    )

private fun backupSetResult(
    id: Long,
    exerciseResultId: Long,
    setOrder: Int,
): BackupSetResult =
    BackupSetResult(
        id = id,
        exerciseResultId = exerciseResultId,
        setOrder = setOrder,
        reps = 1,
        weight = null,
        durationSeconds = null,
        distance = null,
        notes = null,
    )

private class FakeGymbroBackupDataSource(
    var exercises: List<ExerciseEntity> = emptyList(),
    var schedules: List<ScheduleEntity> = emptyList(),
    var scheduleExercises: List<ScheduleExerciseEntity> = emptyList(),
    var workoutRuns: List<WorkoutRunEntity> = emptyList(),
    var exerciseResults: List<ExerciseResultEntity> = emptyList(),
    var setResults: List<SetResultEntity> = emptyList(),
) : GymbroBackupDataSource {
    val transactionDeleteOrder = mutableListOf<String>()

    override suspend fun readBackupData(): GymbroBackupData =
        GymbroBackupData(
            exercises = exercises,
            schedules = schedules,
            scheduleExercises = scheduleExercises,
            workoutRuns = workoutRuns,
            exerciseResults = exerciseResults,
            setResults = setResults,
        )

    override suspend fun merge(data: GymbroBackupData) {
        val existing = readBackupData()
        exercises = existing.exercises + data.exercises
        schedules = existing.schedules + data.schedules
        scheduleExercises = existing.scheduleExercises + data.scheduleExercises
        workoutRuns = existing.workoutRuns + data.workoutRuns
        exerciseResults = existing.exerciseResults + data.exerciseResults
        setResults = existing.setResults + data.setResults
    }

    override suspend fun replaceAll(data: GymbroBackupData) {
        transactionDeleteOrder += "set_results"
        transactionDeleteOrder += "exercise_results"
        transactionDeleteOrder += "workout_runs"
        transactionDeleteOrder += "schedule_exercises"
        transactionDeleteOrder += "schedules"
        transactionDeleteOrder += "exercises"
        exercises = data.exercises
        schedules = data.schedules
        scheduleExercises = data.scheduleExercises
        workoutRuns = data.workoutRuns
        exerciseResults = data.exerciseResults
        setResults = data.setResults
    }
}
