package com.brokenpip3.gymbro.data.repositories

import com.brokenpip3.gymbro.data.dao.ExerciseDao
import com.brokenpip3.gymbro.data.dao.ScheduleDao
import com.brokenpip3.gymbro.data.dao.WorkoutRunDao
import com.brokenpip3.gymbro.data.entities.ExerciseEntity
import com.brokenpip3.gymbro.data.entities.ExerciseResultEntity
import com.brokenpip3.gymbro.data.entities.ExerciseUsage
import com.brokenpip3.gymbro.data.entities.ScheduleEntity
import com.brokenpip3.gymbro.data.entities.ScheduleExerciseEntity
import com.brokenpip3.gymbro.data.entities.SetResultEntity
import com.brokenpip3.gymbro.data.entities.WorkoutRunEntity
import com.brokenpip3.gymbro.domain.TrackingMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

@Suppress("LargeClass")
class WorkoutRepositoryTest {
    @Test
    fun startWorkoutCreatesOneUnfinishedRun() =
        runTest {
            val workoutRunDao = FakeWorkoutRunDao(insertWorkoutRunId = 10)
            val transactionRunner = CountingTransactionRunner()
            val repository =
                WorkoutRepository(
                    workoutRunDao = workoutRunDao,
                    scheduleDao =
                        FakeWorkoutScheduleDao(
                            schedule = schedule(id = 7, name = "Push Day"),
                            scheduleExercises =
                                listOf(
                                    scheduleExercise(scheduleId = 7, exerciseId = 42, sortOrder = 0),
                                ),
                        ),
                    exerciseDao = FakeWorkoutExerciseDao(),
                    transactionRunner = transactionRunner,
                )

            val runId = repository.startWorkout(scheduleId = 7, nowMillis = 1234L)

            assertEquals(10, runId)
            assertEquals(1, transactionRunner.runCount)
            assertEquals(1, workoutRunDao.insertedWorkoutRuns.size)
            assertEquals(
                WorkoutRunEntity(
                    scheduleId = 7,
                    scheduleNameSnapshot = "Push Day",
                    startedAt = 1234L,
                    finishedAt = null,
                    notes = null,
                ),
                workoutRunDao.insertedWorkoutRuns.single(),
            )
        }

    @Test
    fun startWorkoutReturnsExistingRunWhenUnfinishedRunExists() =
        runTest {
            val workoutRunDao =
                FakeWorkoutRunDao(
                    activeWorkoutRun =
                        WorkoutRunEntity(
                            id = 44,
                            scheduleId = 7,
                            scheduleNameSnapshot = "Push Day",
                            startedAt = 1000L,
                            finishedAt = null,
                            notes = null,
                        ),
                )
            val transactionRunner = CountingTransactionRunner()
            val repository =
                WorkoutRepository(
                    workoutRunDao = workoutRunDao,
                    scheduleDao = FakeWorkoutScheduleDao(schedule = schedule(id = 7, name = "Push Day")),
                    exerciseDao = FakeWorkoutExerciseDao(),
                    transactionRunner = transactionRunner,
                )

            val runId = repository.startWorkout(scheduleId = 7, nowMillis = 1234L)

            assertEquals(44, runId)
            assertEquals(1, transactionRunner.runCount)
            assertEquals(emptyList<WorkoutRunEntity>(), workoutRunDao.insertedWorkoutRuns)
        }

    @Test
    fun startWorkoutRejectsScheduleWithNoAssignedExercisesAndDoesNotInsertRun() =
        runTest {
            val workoutRunDao = FakeWorkoutRunDao(insertWorkoutRunId = 10)
            val repository =
                WorkoutRepository(
                    workoutRunDao = workoutRunDao,
                    scheduleDao = FakeWorkoutScheduleDao(schedule = schedule(id = 7, name = "Push Day")),
                    exerciseDao = FakeWorkoutExerciseDao(),
                )

            var error: IllegalArgumentException? = null
            try {
                repository.startWorkout(scheduleId = 7, nowMillis = 1234L)
                fail("Expected IllegalArgumentException")
            } catch (caught: IllegalArgumentException) {
                error = caught
            }

            assertEquals("Cannot start workout: schedule 7 has no assigned exercises", error?.message)
            assertEquals(emptyList<WorkoutRunEntity>(), workoutRunDao.insertedWorkoutRuns)
            assertEquals(emptyList<ExerciseResultEntity>(), workoutRunDao.insertedExerciseResults)
        }

    @Test
    fun startWorkoutSnapshotsAssignedExerciseNamesAndTrackingModesInScheduleOrder() =
        runTest {
            val workoutRunDao = FakeWorkoutRunDao(insertWorkoutRunId = 12)
            val repository =
                WorkoutRepository(
                    workoutRunDao = workoutRunDao,
                    scheduleDao =
                        FakeWorkoutScheduleDao(
                            schedule = schedule(id = 7, name = "Leg Day"),
                            scheduleExercises =
                                listOf(
                                    scheduleExercise(scheduleId = 7, exerciseId = 2, sortOrder = 1),
                                    scheduleExercise(scheduleId = 7, exerciseId = 1, sortOrder = 0),
                                    scheduleExercise(scheduleId = 7, exerciseId = 99, sortOrder = 2),
                                ),
                        ),
                    exerciseDao =
                        FakeWorkoutExerciseDao(
                            exercises =
                                mapOf(
                                    1L to
                                        exercise(
                                            id = 1,
                                            name = "Squat",
                                            trackingMode = TrackingMode.Strength.databaseValue,
                                        ),
                                    2L to
                                        exercise(
                                            id = 2,
                                            name = "Plank",
                                            trackingMode = TrackingMode.Timed.databaseValue,
                                        ),
                                ),
                        ),
                )

            repository.startWorkout(scheduleId = 7, nowMillis = 1234L)

            assertEquals(
                listOf(
                    ExerciseResultEntity(
                        workoutRunId = 12,
                        exerciseId = 1,
                        exerciseNameSnapshot = "Squat",
                        trackingModeSnapshot = TrackingMode.Strength.databaseValue,
                        sortOrder = 0,
                        notes = null,
                    ),
                    ExerciseResultEntity(
                        workoutRunId = 12,
                        exerciseId = 2,
                        exerciseNameSnapshot = "Plank",
                        trackingModeSnapshot = TrackingMode.Timed.databaseValue,
                        sortOrder = 1,
                        notes = null,
                    ),
                    ExerciseResultEntity(
                        workoutRunId = 12,
                        exerciseId = 99,
                        exerciseNameSnapshot = "Deleted exercise",
                        trackingModeSnapshot = TrackingMode.Bodyweight.databaseValue,
                        sortOrder = 2,
                        notes = null,
                    ),
                ),
                workoutRunDao.insertedExerciseResults,
            )
        }

    @Test
    fun startWorkoutPreservesDeletedScheduledExerciseIdentityAndLastSnapshot() =
        runTest {
            val previousRun =
                WorkoutRunEntity(
                    id = 20,
                    scheduleId = 7,
                    scheduleNameSnapshot = "Leg Day",
                    startedAt = 1_000L,
                    finishedAt = 2_000L,
                    notes = null,
                )
            val workoutRunDao =
                FakeWorkoutRunDao(
                    workoutRuns = mutableMapOf(previousRun.id to previousRun),
                    exerciseResults =
                        listOf(
                            exerciseResult(
                                id = 100,
                                workoutRunId = previousRun.id,
                                exerciseId = 99L,
                                exerciseNameSnapshot = "Cable Row",
                                trackingModeSnapshot = TrackingMode.Strength.databaseValue,
                                sortOrder = 0,
                            ),
                        ),
                    insertWorkoutRunId = 30,
                )
            val repository =
                WorkoutRepository(
                    workoutRunDao = workoutRunDao,
                    scheduleDao =
                        FakeWorkoutScheduleDao(
                            schedule = schedule(id = 7, name = "Leg Day"),
                            scheduleExercises =
                                listOf(
                                    scheduleExercise(scheduleId = 7, exerciseId = 99, sortOrder = 0),
                                ),
                        ),
                    exerciseDao = FakeWorkoutExerciseDao(),
                )

            repository.startWorkout(scheduleId = 7, nowMillis = 3_000L)

            assertEquals(
                ExerciseResultEntity(
                    workoutRunId = 30,
                    exerciseId = 99L,
                    exerciseNameSnapshot = "Cable Row",
                    trackingModeSnapshot = TrackingMode.Strength.databaseValue,
                    sortOrder = 0,
                    notes = null,
                ),
                workoutRunDao.insertedExerciseResults.single(),
            )
        }

    @Suppress("LongMethod")
    @Test
    fun startWorkoutCopiesLatestCompletedSetMetricsAsIncompleteRows() =
        runTest {
            val latestCompletedRun =
                WorkoutRunEntity(
                    id = 20,
                    scheduleId = 7,
                    scheduleNameSnapshot = "Push Day",
                    startedAt = 1000L,
                    finishedAt = 2000L,
                    notes = null,
                )
            val workoutRunDao =
                FakeWorkoutRunDao(
                    workoutRuns = mutableMapOf(latestCompletedRun.id to latestCompletedRun),
                    exerciseResults =
                        listOf(
                            exerciseResult(
                                id = 100,
                                workoutRunId = latestCompletedRun.id,
                                exerciseId = 42,
                                exerciseNameSnapshot = "Bench Press",
                                trackingModeSnapshot = TrackingMode.Strength.databaseValue,
                                sortOrder = 0,
                            ),
                            exerciseResult(
                                id = 101,
                                workoutRunId = latestCompletedRun.id,
                                exerciseId = null,
                                exerciseNameSnapshot = "Plank",
                                trackingModeSnapshot = TrackingMode.Timed.databaseValue,
                                sortOrder = 1,
                            ),
                        ),
                    setResults =
                        listOf(
                            setResult(
                                exerciseResultId = 100,
                                setOrder = 1,
                                reps = 9,
                                weight = 82.5,
                                durationSeconds = null,
                                distance = null,
                            ),
                            setResult(
                                exerciseResultId = 100,
                                setOrder = 0,
                                reps = 8,
                                weight = 80.0,
                                durationSeconds = null,
                                distance = null,
                            ),
                            setResult(
                                exerciseResultId = 101,
                                setOrder = 0,
                                reps = null,
                                weight = null,
                                durationSeconds = 60,
                                distance = null,
                            ),
                        ),
                    insertWorkoutRunId = 30,
                )
            val repository =
                WorkoutRepository(
                    workoutRunDao = workoutRunDao,
                    scheduleDao =
                        FakeWorkoutScheduleDao(
                            schedule = schedule(id = 7, name = "Push Day"),
                            scheduleExercises =
                                listOf(
                                    scheduleExercise(scheduleId = 7, exerciseId = 42, sortOrder = 0),
                                    scheduleExercise(scheduleId = 7, exerciseId = 99, sortOrder = 1),
                                ),
                        ),
                    exerciseDao =
                        FakeWorkoutExerciseDao(
                            exercises =
                                mapOf(
                                    42L to
                                        exercise(
                                            id = 42,
                                            name = "Bench Press",
                                            trackingMode = TrackingMode.Strength.databaseValue,
                                        ),
                                    99L to
                                        exercise(
                                            id = 99,
                                            name = "Plank",
                                            trackingMode = TrackingMode.Timed.databaseValue,
                                        ),
                                ),
                        ),
                )

            repository.startWorkout(scheduleId = 7, nowMillis = 3000L)

            assertEquals(
                listOf(
                    SetResultEntity(
                        exerciseResultId = 1,
                        setOrder = 0,
                        reps = 8,
                        weight = 80.0,
                        durationSeconds = null,
                        distance = null,
                        notes = null,
                        isCompleted = false,
                    ),
                    SetResultEntity(
                        exerciseResultId = 1,
                        setOrder = 1,
                        reps = 9,
                        weight = 82.5,
                        durationSeconds = null,
                        distance = null,
                        notes = null,
                        isCompleted = false,
                    ),
                    SetResultEntity(
                        exerciseResultId = 2,
                        setOrder = 0,
                        reps = null,
                        weight = null,
                        durationSeconds = 60,
                        distance = null,
                        notes = null,
                        isCompleted = false,
                    ),
                ),
                workoutRunDao.insertedSetResults,
            )
        }

    @Test
    fun startWorkoutUsesCurrentModeTargetsAfterTrackingModeChanges() =
        runTest {
            val latestCompletedRun =
                WorkoutRunEntity(
                    id = 20,
                    scheduleId = 7,
                    scheduleNameSnapshot = "Conditioning",
                    startedAt = 1000L,
                    finishedAt = 2000L,
                    notes = null,
                )
            val workoutRunDao =
                FakeWorkoutRunDao(
                    workoutRuns = mutableMapOf(latestCompletedRun.id to latestCompletedRun),
                    exerciseResults =
                        listOf(
                            exerciseResult(
                                id = 100,
                                workoutRunId = latestCompletedRun.id,
                                exerciseId = 42,
                                exerciseNameSnapshot = "Run",
                                trackingModeSnapshot = TrackingMode.Strength.databaseValue,
                                sortOrder = 0,
                            ),
                        ),
                    setResults =
                        listOf(
                            setResult(
                                exerciseResultId = 100,
                                setOrder = 0,
                                reps = 8,
                                weight = 80.0,
                            ),
                        ),
                    insertWorkoutRunId = 30,
                )
            val repository =
                WorkoutRepository(
                    workoutRunDao = workoutRunDao,
                    scheduleDao =
                        FakeWorkoutScheduleDao(
                            schedule = schedule(id = 7, name = "Conditioning"),
                            scheduleExercises =
                                listOf(
                                    scheduleExercise(
                                        scheduleId = 7,
                                        exerciseId = 42,
                                        sortOrder = 0,
                                        targetSets = 2,
                                        targetDurationSeconds = 600L,
                                        targetDistance = 2.5,
                                    ),
                                ),
                        ),
                    exerciseDao =
                        FakeWorkoutExerciseDao(
                            exercises =
                                mapOf(
                                    42L to
                                        exercise(
                                            id = 42,
                                            name = "Run",
                                            trackingMode = TrackingMode.Timed.databaseValue,
                                        ),
                                ),
                        ),
                )

            repository.startWorkout(scheduleId = 7, nowMillis = 3000L)

            assertEquals(
                listOf(
                    setResult(
                        exerciseResultId = 1,
                        setOrder = 0,
                        durationSeconds = 600L,
                        distance = 2.5,
                    ).copy(isCompleted = false),
                    setResult(
                        exerciseResultId = 1,
                        setOrder = 1,
                        durationSeconds = 600L,
                        distance = 2.5,
                    ).copy(isCompleted = false),
                ),
                workoutRunDao.insertedSetResults,
            )
        }

    @Test
    fun startWorkoutCopiesDuplicateExercisesByScheduleSlotOrder() =
        runTest {
            val latestCompletedRun =
                WorkoutRunEntity(
                    id = 20,
                    scheduleId = 7,
                    scheduleNameSnapshot = "Push Day",
                    startedAt = 1000L,
                    finishedAt = 2000L,
                    notes = null,
                )
            val workoutRunDao =
                FakeWorkoutRunDao(
                    workoutRuns = mutableMapOf(latestCompletedRun.id to latestCompletedRun),
                    exerciseResults =
                        listOf(
                            exerciseResult(
                                id = 100,
                                workoutRunId = latestCompletedRun.id,
                                exerciseId = 42,
                                exerciseNameSnapshot = "Bench Press",
                                trackingModeSnapshot = TrackingMode.Strength.databaseValue,
                                sortOrder = 0,
                            ),
                            exerciseResult(
                                id = 101,
                                workoutRunId = latestCompletedRun.id,
                                exerciseId = 42,
                                exerciseNameSnapshot = "Bench Press",
                                trackingModeSnapshot = TrackingMode.Strength.databaseValue,
                                sortOrder = 1,
                            ),
                        ),
                    setResults =
                        listOf(
                            setResult(exerciseResultId = 100, setOrder = 0, reps = 8, weight = 80.0),
                            setResult(exerciseResultId = 101, setOrder = 0, reps = 12, weight = 40.0),
                        ),
                    insertWorkoutRunId = 30,
                )
            val repository =
                WorkoutRepository(
                    workoutRunDao = workoutRunDao,
                    scheduleDao =
                        FakeWorkoutScheduleDao(
                            schedule = schedule(id = 7, name = "Push Day"),
                            scheduleExercises =
                                listOf(
                                    scheduleExercise(scheduleId = 7, exerciseId = 42, sortOrder = 0),
                                    scheduleExercise(scheduleId = 7, exerciseId = 42, sortOrder = 1),
                                ),
                        ),
                    exerciseDao =
                        FakeWorkoutExerciseDao(
                            exercises =
                                mapOf(
                                    42L to
                                        exercise(
                                            id = 42,
                                            name = "Bench Press",
                                            trackingMode = TrackingMode.Strength.databaseValue,
                                        ),
                                ),
                        ),
                )

            repository.startWorkout(scheduleId = 7, nowMillis = 3000L)

            assertEquals(
                listOf(
                    setResult(exerciseResultId = 1, setOrder = 0, reps = 8, weight = 80.0)
                        .copy(isCompleted = false),
                    setResult(exerciseResultId = 2, setOrder = 0, reps = 12, weight = 40.0)
                        .copy(isCompleted = false),
                ),
                workoutRunDao.insertedSetResults,
            )
        }

    @Test
    fun startWorkoutUsesScheduleTargetsWhenExerciseWasNotInPreviousRun() =
        runTest {
            val latestCompletedRun =
                WorkoutRunEntity(
                    id = 20,
                    scheduleId = 7,
                    scheduleNameSnapshot = "Push Day",
                    startedAt = 1000L,
                    finishedAt = 2000L,
                    notes = null,
                )
            val workoutRunDao =
                FakeWorkoutRunDao(
                    workoutRuns = mutableMapOf(latestCompletedRun.id to latestCompletedRun),
                    exerciseResults =
                        listOf(
                            exerciseResult(
                                id = 100,
                                workoutRunId = latestCompletedRun.id,
                                exerciseId = 99,
                                exerciseNameSnapshot = "Squat",
                                trackingModeSnapshot = TrackingMode.Strength.databaseValue,
                                sortOrder = 0,
                            ),
                        ),
                    setResults =
                        listOf(
                            setResult(exerciseResultId = 100, setOrder = 0, reps = 5, weight = 120.0),
                        ),
                    insertWorkoutRunId = 30,
                )
            val repository =
                WorkoutRepository(
                    workoutRunDao = workoutRunDao,
                    scheduleDao =
                        FakeWorkoutScheduleDao(
                            schedule = schedule(id = 7, name = "Push Day"),
                            scheduleExercises =
                                listOf(
                                    scheduleExercise(
                                        scheduleId = 7,
                                        exerciseId = 42,
                                        sortOrder = 0,
                                        targetSets = 3,
                                        targetReps = 8,
                                        targetWeight = 80.0,
                                    ),
                                ),
                        ),
                    exerciseDao =
                        FakeWorkoutExerciseDao(
                            exercises =
                                mapOf(
                                    42L to
                                        exercise(
                                            id = 42,
                                            name = "Bench Press",
                                            trackingMode = TrackingMode.Strength.databaseValue,
                                        ),
                                ),
                        ),
                )

            repository.startWorkout(scheduleId = 7, nowMillis = 3000L)

            assertEquals(
                listOf(
                    setResult(exerciseResultId = 1, setOrder = 0, reps = 8, weight = 80.0).copy(isCompleted = false),
                    setResult(exerciseResultId = 1, setOrder = 1, reps = 8, weight = 80.0).copy(isCompleted = false),
                    setResult(exerciseResultId = 1, setOrder = 2, reps = 8, weight = 80.0).copy(isCompleted = false),
                ),
                workoutRunDao.insertedSetResults,
            )
        }

    @Test
    fun startWorkoutUsesTimedScheduleTargetsWhenThereIsNoPreviousRun() =
        runTest {
            val workoutRunDao = FakeWorkoutRunDao(insertWorkoutRunId = 30)
            val repository =
                WorkoutRepository(
                    workoutRunDao = workoutRunDao,
                    scheduleDao =
                        FakeWorkoutScheduleDao(
                            schedule = schedule(id = 7, name = "Run Day"),
                            scheduleExercises =
                                listOf(
                                    scheduleExercise(
                                        scheduleId = 7,
                                        exerciseId = 42,
                                        sortOrder = 0,
                                        targetSets = 2,
                                        targetDurationSeconds = 600L,
                                        targetDistance = 2.5,
                                    ),
                                ),
                        ),
                    exerciseDao =
                        FakeWorkoutExerciseDao(
                            exercises =
                                mapOf(
                                    42L to
                                        exercise(
                                            id = 42,
                                            name = "Run",
                                            trackingMode = TrackingMode.Timed.databaseValue,
                                        ),
                                ),
                        ),
                )

            repository.startWorkout(scheduleId = 7, nowMillis = 3000L)

            assertEquals(
                listOf(
                    setResult(
                        exerciseResultId = 1,
                        setOrder = 0,
                        durationSeconds = 600L,
                        distance = 2.5,
                    ).copy(isCompleted = false),
                    setResult(
                        exerciseResultId = 1,
                        setOrder = 1,
                        durationSeconds = 600L,
                        distance = 2.5,
                    ).copy(isCompleted = false),
                ),
                workoutRunDao.insertedSetResults,
            )
        }

    @Test
    fun startWorkoutDoesNotCopyFromDifferentSchedule() =
        runTest {
            val completedRunForDifferentSchedule =
                WorkoutRunEntity(
                    id = 20,
                    scheduleId = 8,
                    scheduleNameSnapshot = "Other Push Day",
                    startedAt = 1000L,
                    finishedAt = 2000L,
                    notes = null,
                )
            val workoutRunDao =
                FakeWorkoutRunDao(
                    workoutRuns =
                        mutableMapOf(
                            completedRunForDifferentSchedule.id to completedRunForDifferentSchedule,
                        ),
                    exerciseResults =
                        listOf(
                            exerciseResult(
                                id = 100,
                                workoutRunId = completedRunForDifferentSchedule.id,
                                exerciseId = 42,
                                exerciseNameSnapshot = "Bench Press",
                                trackingModeSnapshot = TrackingMode.Strength.databaseValue,
                                sortOrder = 0,
                            ),
                        ),
                    setResults =
                        listOf(
                            setResult(exerciseResultId = 100, setOrder = 0, reps = 8, weight = 80.0),
                        ),
                    insertWorkoutRunId = 30,
                )
            val repository =
                WorkoutRepository(
                    workoutRunDao = workoutRunDao,
                    scheduleDao =
                        FakeWorkoutScheduleDao(
                            schedule = schedule(id = 7, name = "Push Day"),
                            scheduleExercises =
                                listOf(
                                    scheduleExercise(scheduleId = 7, exerciseId = 42, sortOrder = 0),
                                ),
                        ),
                    exerciseDao =
                        FakeWorkoutExerciseDao(
                            exercises =
                                mapOf(
                                    42L to
                                        exercise(
                                            id = 42,
                                            name = "Bench Press",
                                            trackingMode = TrackingMode.Strength.databaseValue,
                                        ),
                                ),
                        ),
                )

            repository.startWorkout(scheduleId = 7, nowMillis = 3000L)

            assertEquals(emptyList<SetResultEntity>(), workoutRunDao.insertedSetResults)
        }

    @Test
    fun startWorkoutStartsEmptyWhenExerciseWasNotInPreviousRun() =
        runTest {
            val latestCompletedRun =
                WorkoutRunEntity(
                    id = 20,
                    scheduleId = 7,
                    scheduleNameSnapshot = "Push Day",
                    startedAt = 1000L,
                    finishedAt = 2000L,
                    notes = null,
                )
            val workoutRunDao =
                FakeWorkoutRunDao(
                    workoutRuns = mutableMapOf(latestCompletedRun.id to latestCompletedRun),
                    exerciseResults =
                        listOf(
                            exerciseResult(
                                id = 100,
                                workoutRunId = latestCompletedRun.id,
                                exerciseId = 99,
                                exerciseNameSnapshot = "Squat",
                                trackingModeSnapshot = TrackingMode.Strength.databaseValue,
                                sortOrder = 0,
                            ),
                        ),
                    setResults =
                        listOf(
                            setResult(exerciseResultId = 100, setOrder = 0, reps = 5, weight = 120.0),
                        ),
                    insertWorkoutRunId = 30,
                )
            val repository =
                WorkoutRepository(
                    workoutRunDao = workoutRunDao,
                    scheduleDao =
                        FakeWorkoutScheduleDao(
                            schedule = schedule(id = 7, name = "Push Day"),
                            scheduleExercises =
                                listOf(
                                    scheduleExercise(scheduleId = 7, exerciseId = 42, sortOrder = 0),
                                ),
                        ),
                    exerciseDao =
                        FakeWorkoutExerciseDao(
                            exercises =
                                mapOf(
                                    42L to
                                        exercise(
                                            id = 42,
                                            name = "Bench Press",
                                            trackingMode = TrackingMode.Strength.databaseValue,
                                        ),
                                ),
                        ),
                )

            repository.startWorkout(scheduleId = 7, nowMillis = 3000L)

            assertEquals(emptyList<SetResultEntity>(), workoutRunDao.insertedSetResults)
        }

    @Test
    fun finishWorkoutSetsFinishedAt() =
        runTest {
            val workoutRunDao = FakeWorkoutRunDao(workoutRuns = mutableMapOf(3L to unfinishedWorkoutRun()))
            val transactionRunner = CountingTransactionRunner()
            val repository =
                WorkoutRepository(
                    workoutRunDao = workoutRunDao,
                    scheduleDao = FakeWorkoutScheduleDao(),
                    exerciseDao = FakeWorkoutExerciseDao(),
                    transactionRunner = transactionRunner,
                )

            repository.finishWorkout(workoutRunId = 3, nowMillis = 2000L)

            assertEquals(1, transactionRunner.runCount)
            assertEquals(2000L, workoutRunDao.updatedWorkoutRun?.finishedAt)
        }

    @Test
    fun finishWorkoutDoesNotOverwriteCompletedRun() =
        runTest {
            val workoutRunDao =
                FakeWorkoutRunDao(workoutRuns = mutableMapOf(3L to unfinishedWorkoutRun(finishedAt = 1500L)))
            val transactionRunner = CountingTransactionRunner()
            val repository =
                WorkoutRepository(
                    workoutRunDao = workoutRunDao,
                    scheduleDao = FakeWorkoutScheduleDao(),
                    exerciseDao = FakeWorkoutExerciseDao(),
                    transactionRunner = transactionRunner,
                )

            repository.finishWorkout(workoutRunId = 3, nowMillis = 2000L)

            assertEquals(1, transactionRunner.runCount)
            assertEquals(null, workoutRunDao.updatedWorkoutRun)
        }

    @Test
    fun updateWorkoutNotesDelegatesToDao() =
        runTest {
            val workoutRunDao = FakeWorkoutRunDao()
            val repository =
                WorkoutRepository(
                    workoutRunDao = workoutRunDao,
                    scheduleDao = FakeWorkoutScheduleDao(),
                    exerciseDao = FakeWorkoutExerciseDao(),
                )

            repository.updateWorkoutNotes(workoutRunId = 3L, notes = "Strong session")

            assertEquals(3L to "Strong session", workoutRunDao.updatedWorkoutNotes)
        }

    @Test
    fun discardWorkoutRemovesOnlyTheSelectedRunAndItsResults() =
        runTest {
            val activeRun = unfinishedWorkoutRun()
            val workoutRunDao =
                FakeWorkoutRunDao(
                    workoutRuns = mutableMapOf(activeRun.id to activeRun),
                    exerciseResults =
                        listOf(
                            exerciseResult(
                                id = 10L,
                                workoutRunId = activeRun.id,
                                exerciseId = 1L,
                                exerciseNameSnapshot = "Squat",
                                trackingModeSnapshot = TrackingMode.Strength.databaseValue,
                                sortOrder = 0,
                            ),
                            exerciseResult(
                                id = 11L,
                                workoutRunId = 99L,
                                exerciseId = 2L,
                                exerciseNameSnapshot = "Press",
                                trackingModeSnapshot = TrackingMode.Strength.databaseValue,
                                sortOrder = 0,
                            ),
                        ),
                    setResults =
                        listOf(
                            setResult(exerciseResultId = 10L, setOrder = 0, reps = 8, weight = 80.0),
                            setResult(exerciseResultId = 11L, setOrder = 0, reps = 5, weight = 50.0),
                        ),
                )
            val repository =
                WorkoutRepository(
                    workoutRunDao = workoutRunDao,
                    scheduleDao = FakeWorkoutScheduleDao(),
                    exerciseDao = FakeWorkoutExerciseDao(),
                )

            repository.discardWorkout(workoutRunId = activeRun.id)

            assertEquals(null, workoutRunDao.getWorkoutRun(activeRun.id))
            assertEquals(emptyList<ExerciseResultEntity>(), workoutRunDao.getExerciseResults(activeRun.id))
            assertEquals(emptyList<SetResultEntity>(), workoutRunDao.getSetResults(exerciseResultId = 10L))
            assertEquals(1, workoutRunDao.getExerciseResults(99L).size)
            assertEquals(1, workoutRunDao.getSetResults(exerciseResultId = 11L).size)
        }

    @Test
    fun addExerciseToActiveWorkoutSnapshotsExerciseAtEnd() =
        runTest {
            val workoutRunDao =
                FakeWorkoutRunDao(
                    workoutRuns = mutableMapOf(3L to unfinishedWorkoutRun()),
                    exerciseResults =
                        listOf(
                            exerciseResult(
                                id = 10L,
                                workoutRunId = 3L,
                                exerciseId = 1L,
                                exerciseNameSnapshot = "Squat",
                                trackingModeSnapshot = TrackingMode.Strength.databaseValue,
                                sortOrder = 0,
                            ),
                            exerciseResult(
                                id = 11L,
                                workoutRunId = 3L,
                                exerciseId = 2L,
                                exerciseNameSnapshot = "Run",
                                trackingModeSnapshot = TrackingMode.Timed.databaseValue,
                                sortOrder = 2,
                            ),
                        ),
                    insertExerciseResultId = 25L,
                )
            val repository =
                WorkoutRepository(
                    workoutRunDao = workoutRunDao,
                    scheduleDao = FakeWorkoutScheduleDao(),
                    exerciseDao =
                        FakeWorkoutExerciseDao(
                            exercises =
                                mapOf(
                                    42L to
                                        exercise(
                                            id = 42L,
                                            name = "Pull Up",
                                            trackingMode = TrackingMode.Bodyweight.databaseValue,
                                        ),
                                ),
                        ),
                )

            val exerciseResultId = repository.addExerciseToActiveWorkout(workoutRunId = 3L, exerciseId = 42L)

            assertEquals(25L, exerciseResultId)
            assertEquals(
                ExerciseResultEntity(
                    workoutRunId = 3L,
                    exerciseId = 42L,
                    exerciseNameSnapshot = "Pull Up",
                    trackingModeSnapshot = TrackingMode.Bodyweight.databaseValue,
                    sortOrder = 3,
                    notes = null,
                ),
                workoutRunDao.insertedExerciseResults.single(),
            )
        }

    @Test
    fun addExerciseToActiveWorkoutRejectsMissingWorkout() =
        runTest {
            val repository =
                WorkoutRepository(
                    workoutRunDao = FakeWorkoutRunDao(),
                    scheduleDao = FakeWorkoutScheduleDao(),
                    exerciseDao = FakeWorkoutExerciseDao(),
                )

            val error =
                runCatching {
                    repository.addExerciseToActiveWorkout(workoutRunId = 99L, exerciseId = 42L)
                }.exceptionOrNull()

            assertEquals("Cannot add exercise: active workout 99 was not found", error?.message)
        }

    @Test
    fun addExerciseToActiveWorkoutRejectsFinishedWorkout() =
        runTest {
            val repository =
                WorkoutRepository(
                    workoutRunDao =
                        FakeWorkoutRunDao(
                            workoutRuns = mutableMapOf(3L to unfinishedWorkoutRun(finishedAt = 9L)),
                        ),
                    scheduleDao = FakeWorkoutScheduleDao(),
                    exerciseDao = FakeWorkoutExerciseDao(),
                )

            val error =
                runCatching {
                    repository.addExerciseToActiveWorkout(workoutRunId = 3L, exerciseId = 42L)
                }.exceptionOrNull()

            assertEquals("Cannot add exercise: workout 3 is already finished", error?.message)
        }

    @Test
    fun addExerciseToActiveWorkoutRejectsMissingExercise() =
        runTest {
            val repository =
                WorkoutRepository(
                    workoutRunDao = FakeWorkoutRunDao(workoutRuns = mutableMapOf(3L to unfinishedWorkoutRun())),
                    scheduleDao = FakeWorkoutScheduleDao(),
                    exerciseDao = FakeWorkoutExerciseDao(),
                )

            val error =
                runCatching {
                    repository.addExerciseToActiveWorkout(workoutRunId = 3L, exerciseId = 42L)
                }.exceptionOrNull()

            assertEquals("Cannot add exercise: exercise 42 was not found", error?.message)
        }

    @Test
    fun getExerciseResultReturnsExerciseResultById() =
        runTest {
            val exerciseResult =
                ExerciseResultEntity(
                    id = 42L,
                    workoutRunId = 3L,
                    exerciseId = 9L,
                    exerciseNameSnapshot = "Bench Press",
                    trackingModeSnapshot = TrackingMode.Strength.databaseValue,
                    sortOrder = 0,
                    notes = "Keep shoulder blades tight.",
                )
            val repository =
                WorkoutRepository(
                    workoutRunDao = FakeWorkoutRunDao(exerciseResults = listOf(exerciseResult)),
                    scheduleDao = FakeWorkoutScheduleDao(),
                    exerciseDao = FakeWorkoutExerciseDao(),
                )

            assertEquals(exerciseResult, repository.getExerciseResult(42L))
        }

    @Test
    fun addSetStartsAtSetOrderZero() =
        runTest {
            val workoutRunDao = FakeWorkoutRunDao(insertSetResultId = 17)
            val transactionRunner = CountingTransactionRunner()
            val repository =
                WorkoutRepository(
                    workoutRunDao = workoutRunDao,
                    scheduleDao = FakeWorkoutScheduleDao(),
                    exerciseDao = FakeWorkoutExerciseDao(),
                    transactionRunner = transactionRunner,
                )

            val setId =
                repository.addSet(
                    exerciseResultId = 5,
                    reps = 10,
                    weight = 100.0,
                    durationSeconds = null,
                    distance = null,
                    notes = "solid",
                )

            assertEquals(17, setId)
            assertEquals(1, transactionRunner.runCount)
            assertEquals(
                SetResultEntity(
                    exerciseResultId = 5,
                    setOrder = 0,
                    reps = 10,
                    weight = 100.0,
                    durationSeconds = null,
                    distance = null,
                    notes = "solid",
                ),
                workoutRunDao.insertedSetResults.single(),
            )
        }

    @Test
    fun addSetAppendsAfterExistingSetResults() =
        runTest {
            val workoutRunDao =
                FakeWorkoutRunDao(
                    setResults =
                        listOf(
                            setResult(exerciseResultId = 5, setOrder = 0),
                            setResult(exerciseResultId = 5, setOrder = 1),
                        ),
                )
            val transactionRunner = CountingTransactionRunner()
            val repository =
                WorkoutRepository(
                    workoutRunDao = workoutRunDao,
                    scheduleDao = FakeWorkoutScheduleDao(),
                    exerciseDao = FakeWorkoutExerciseDao(),
                    transactionRunner = transactionRunner,
                )

            repository.addSet(
                exerciseResultId = 5,
                reps = null,
                weight = null,
                durationSeconds = 30,
                distance = null,
                notes = null,
            )

            assertEquals(1, transactionRunner.runCount)
            assertEquals(2, workoutRunDao.insertedSetResults.single().setOrder)
        }

    @Test
    fun addEmptySetCreatesIncompleteRowForExercise() =
        runTest {
            val workoutRunDao = FakeWorkoutRunDao(insertSetResultId = 21)
            val repository =
                WorkoutRepository(
                    workoutRunDao = workoutRunDao,
                    scheduleDao = FakeWorkoutScheduleDao(),
                    exerciseDao = FakeWorkoutExerciseDao(),
                )

            val setId = repository.addEmptySet(exerciseResultId = 5)

            assertEquals(21, setId)
            assertEquals(
                SetResultEntity(
                    exerciseResultId = 5,
                    setOrder = 0,
                    reps = null,
                    weight = null,
                    durationSeconds = null,
                    distance = null,
                    notes = null,
                    isCompleted = false,
                ),
                workoutRunDao.insertedSetResults.single(),
            )
        }

    @Test
    fun addSetFromPreviousCopiesLatestMetricsAsIncomplete() =
        runTest {
            val workoutRunDao =
                FakeWorkoutRunDao(
                    insertSetResultId = 22,
                    setResults =
                        listOf(
                            setResult(exerciseResultId = 5, setOrder = 0, reps = 8, weight = 27.5),
                            setResult(
                                exerciseResultId = 5,
                                setOrder = 2,
                                reps = 10,
                                weight = 30.0,
                                durationSeconds = 45,
                                distance = 120.5,
                            ),
                            setResult(exerciseResultId = 5, setOrder = 1, reps = 9, weight = 28.0),
                        ),
                )
            val repository =
                WorkoutRepository(
                    workoutRunDao = workoutRunDao,
                    scheduleDao = FakeWorkoutScheduleDao(),
                    exerciseDao = FakeWorkoutExerciseDao(),
                )

            val setId = repository.addSetFromPrevious(exerciseResultId = 5)

            assertEquals(22, setId)
            assertEquals(
                SetResultEntity(
                    exerciseResultId = 5,
                    setOrder = 3,
                    reps = 10,
                    weight = 30.0,
                    durationSeconds = 45,
                    distance = 120.5,
                    notes = null,
                    isCompleted = false,
                ),
                workoutRunDao.insertedSetResults.single(),
            )
        }

    @Test
    fun updateSetMetricsPersistsEditedValues() =
        runTest {
            val workoutRunDao = FakeWorkoutRunDao(insertSetResultId = 23)
            val repository =
                WorkoutRepository(
                    workoutRunDao = workoutRunDao,
                    scheduleDao = FakeWorkoutScheduleDao(),
                    exerciseDao = FakeWorkoutExerciseDao(),
                )
            val setId = repository.addEmptySet(exerciseResultId = 5)

            repository.updateSetMetrics(
                setId = setId,
                reps = 12,
                weight = 32.5,
                durationSeconds = 60,
                distance = 400.0,
            )

            val persistedSet = workoutRunDao.getSetResults(exerciseResultId = 5).single()
            assertEquals(12, persistedSet.reps)
            assertEquals(32.5, persistedSet.weight)
            assertEquals(60L, persistedSet.durationSeconds)
            assertEquals(400.0, persistedSet.distance)
        }

    @Test
    fun updateSetNotesPersistsNotes() =
        runTest {
            val workoutRunDao =
                FakeWorkoutRunDao(
                    setResults =
                        listOf(
                            setResult(
                                id = 24,
                                exerciseResultId = 5,
                                setOrder = 0,
                                reps = 8,
                                weight = 80.0,
                            ),
                        ),
                )
            val repository =
                WorkoutRepository(
                    workoutRunDao = workoutRunDao,
                    scheduleDao = FakeWorkoutScheduleDao(),
                    exerciseDao = FakeWorkoutExerciseDao(),
                )

            repository.updateSetNotes(
                setId = 24,
                notes = "RPE 8, clean reps.",
            )

            assertEquals(
                "RPE 8, clean reps.",
                workoutRunDao.getSetResults(exerciseResultId = 5).single().notes,
            )
        }

    @Test
    fun setCompletionCanBeToggled() =
        runTest {
            val workoutRunDao = FakeWorkoutRunDao(insertSetResultId = 24)
            val repository =
                WorkoutRepository(
                    workoutRunDao = workoutRunDao,
                    scheduleDao = FakeWorkoutScheduleDao(),
                    exerciseDao = FakeWorkoutExerciseDao(),
                )
            val setId = repository.addEmptySet(exerciseResultId = 5)

            repository.updateSetCompletion(setId = setId, isCompleted = true)

            assertEquals(true, workoutRunDao.getSetResults(exerciseResultId = 5).single().isCompleted)
        }

    @Test
    fun updateExerciseResultNotesPersistsNotes() =
        runTest {
            val exerciseResult =
                exerciseResult(
                    id = 31,
                    workoutRunId = 3,
                    exerciseId = 7,
                    exerciseNameSnapshot = "Bench press",
                    trackingModeSnapshot = TrackingMode.Strength.databaseValue,
                    sortOrder = 0,
                )
            val workoutRunDao = FakeWorkoutRunDao(exerciseResults = listOf(exerciseResult))
            val repository =
                WorkoutRepository(
                    workoutRunDao = workoutRunDao,
                    scheduleDao = FakeWorkoutScheduleDao(),
                    exerciseDao = FakeWorkoutExerciseDao(),
                )

            repository.updateExerciseResultNotes(
                exerciseResultId = 31,
                notes = "Keep shoulder blades tight.",
            )

            assertEquals(
                "Keep shoulder blades tight.",
                workoutRunDao.getExerciseResult(31)?.notes,
            )
        }
}

private class CountingTransactionRunner : TransactionRunner {
    var runCount = 0

    override suspend fun <T> run(block: suspend () -> T): T {
        runCount += 1
        return block()
    }
}

private fun schedule(
    id: Long,
    name: String,
): ScheduleEntity =
    ScheduleEntity(
        id = id,
        name = name,
        notes = null,
        createdAt = 100L,
        updatedAt = 100L,
    )

private fun scheduleExercise(
    scheduleId: Long,
    exerciseId: Long,
    sortOrder: Int,
    targetSets: Int? = null,
    targetReps: Int? = null,
    targetWeight: Double? = null,
    targetDurationSeconds: Long? = null,
    targetDistance: Double? = null,
): ScheduleExerciseEntity =
    ScheduleExerciseEntity(
        scheduleId = scheduleId,
        exerciseId = exerciseId,
        sortOrder = sortOrder,
        targetSets = targetSets,
        targetReps = targetReps,
        targetWeight = targetWeight,
        targetDurationSeconds = targetDurationSeconds,
        targetDistance = targetDistance,
    )

private fun exercise(
    id: Long,
    name: String,
    trackingMode: String,
): ExerciseEntity =
    ExerciseEntity(
        id = id,
        name = name,
        notes = null,
        trackingMode = trackingMode,
        createdAt = 100L,
        updatedAt = 100L,
    )

private fun exerciseResult(
    id: Long,
    workoutRunId: Long,
    exerciseId: Long?,
    exerciseNameSnapshot: String,
    trackingModeSnapshot: String,
    sortOrder: Int,
): ExerciseResultEntity =
    ExerciseResultEntity(
        id = id,
        workoutRunId = workoutRunId,
        exerciseId = exerciseId,
        exerciseNameSnapshot = exerciseNameSnapshot,
        trackingModeSnapshot = trackingModeSnapshot,
        sortOrder = sortOrder,
        notes = null,
    )

private fun unfinishedWorkoutRun(finishedAt: Long? = null): WorkoutRunEntity =
    WorkoutRunEntity(
        id = 3,
        scheduleId = 7,
        scheduleNameSnapshot = "Push Day",
        startedAt = 1000L,
        finishedAt = finishedAt,
        notes = null,
    )

private fun setResult(
    id: Long = 0,
    exerciseResultId: Long,
    setOrder: Int,
    reps: Int? = null,
    weight: Double? = null,
    durationSeconds: Long? = null,
    distance: Double? = null,
): SetResultEntity =
    SetResultEntity(
        id = id,
        exerciseResultId = exerciseResultId,
        setOrder = setOrder,
        reps = reps,
        weight = weight,
        durationSeconds = durationSeconds,
        distance = distance,
        notes = null,
    )

@Suppress("LongParameterList")
private class FakeWorkoutRunDao(
    private val workoutRunsFlow: Flow<List<WorkoutRunEntity>> = MutableStateFlow(emptyList()),
    private val activeWorkoutRun: WorkoutRunEntity? = null,
    private val workoutRuns: MutableMap<Long, WorkoutRunEntity> = mutableMapOf(),
    exerciseResults: List<ExerciseResultEntity> = emptyList(),
    setResults: List<SetResultEntity> = emptyList(),
    private val insertWorkoutRunId: Long = 1,
    private val insertExerciseResultId: Long? = null,
    private val insertSetResultId: Long = 1,
) : WorkoutRunDao {
    private val storedExerciseResults = exerciseResults.toMutableList()
    private val storedSetResults = setResults.toMutableList()
    val insertedWorkoutRuns = mutableListOf<WorkoutRunEntity>()
    val insertedExerciseResults = mutableListOf<ExerciseResultEntity>()
    val insertedSetResults = mutableListOf<SetResultEntity>()
    var updatedWorkoutRun: WorkoutRunEntity? = null
    var updatedWorkoutNotes: Pair<Long, String?>? = null
    var updatedSetResult: SetResultEntity? = null
    var deletedSetResultId: Long? = null
    private var nextExerciseResultId = 1L
    private var nextSetResultId = insertSetResultId

    override fun observeWorkoutRuns(): Flow<List<WorkoutRunEntity>> = workoutRunsFlow

    override fun observeActiveWorkoutRun(): Flow<WorkoutRunEntity?> = MutableStateFlow(activeWorkoutRun)

    override suspend fun getWorkoutRun(id: Long): WorkoutRunEntity? = workoutRuns[id]

    override suspend fun getAllWorkoutRuns(): List<WorkoutRunEntity> = workoutRuns.values.sortedBy { it.id }

    override suspend fun getActiveWorkoutRun(): WorkoutRunEntity? = activeWorkoutRun

    @Suppress("ktlint:standard:function-expression-body")
    override suspend fun getLatestCompletedWorkoutRunForSchedule(scheduleId: Long): WorkoutRunEntity? {
        return workoutRuns.values
            .filter { it.scheduleId == scheduleId && it.finishedAt != null }
            .maxByOrNull { it.finishedAt ?: Long.MIN_VALUE }
    }

    override suspend fun insertWorkoutRun(workoutRun: WorkoutRunEntity): Long {
        insertedWorkoutRuns += workoutRun
        workoutRuns[insertWorkoutRunId] = workoutRun.copy(id = insertWorkoutRunId)
        return insertWorkoutRunId
    }

    override suspend fun insertWorkoutRuns(workoutRuns: List<WorkoutRunEntity>) = Unit

    override suspend fun updateWorkoutRun(workoutRun: WorkoutRunEntity) {
        updatedWorkoutRun = workoutRun
    }

    override suspend fun updateWorkoutRunNotes(
        workoutRunId: Long,
        notes: String?,
    ) {
        updatedWorkoutNotes = workoutRunId to notes
    }

    override suspend fun insertExerciseResult(exerciseResult: ExerciseResultEntity): Long {
        insertedExerciseResults += exerciseResult
        if (insertExerciseResultId != null) {
            storedExerciseResults += exerciseResult.copy(id = insertExerciseResultId)
            return insertExerciseResultId
        }
        while (storedExerciseResults.any { it.id == nextExerciseResultId }) {
            nextExerciseResultId += 1
        }
        val insertedId = nextExerciseResultId
        nextExerciseResultId += 1
        storedExerciseResults += exerciseResult.copy(id = insertedId)
        return insertedId
    }

    override suspend fun insertExerciseResults(exerciseResults: List<ExerciseResultEntity>) = Unit

    override suspend fun insertSetResult(setResult: SetResultEntity): Long {
        insertedSetResults += setResult
        while (storedSetResults.any { it.id == nextSetResultId }) {
            nextSetResultId += 1
        }
        val insertedId = nextSetResultId
        nextSetResultId += 1
        storedSetResults += setResult.copy(id = insertedId)
        return insertedId
    }

    override suspend fun insertSetResults(setResults: List<SetResultEntity>) = Unit

    override fun observeExerciseResults(workoutRunId: Long) = emptyExerciseResultsFlow()

    override fun observeExerciseResultChanges(): Flow<Int> = MutableStateFlow(0)

    override suspend fun getExerciseResults(workoutRunId: Long): List<ExerciseResultEntity> =
        storedExerciseResults.filter { it.workoutRunId == workoutRunId }.sortedBy { it.sortOrder }

    @Suppress("ktlint:standard:function-expression-body")
    override suspend fun getExerciseResult(id: Long): ExerciseResultEntity? {
        return storedExerciseResults.firstOrNull { it.id == id }
    }

    override suspend fun getAllExerciseResults(): List<ExerciseResultEntity> = storedExerciseResults.sortedBy { it.id }

    override fun observeSetResults(exerciseResultId: Long): Flow<List<SetResultEntity>> = MutableStateFlow(emptyList())

    override fun observeSetResultChanges(): Flow<Int> = MutableStateFlow(0)

    override suspend fun getSetResults(exerciseResultId: Long): List<SetResultEntity> =
        storedSetResults.filter { it.exerciseResultId == exerciseResultId }.sortedBy { it.setOrder }

    override suspend fun getAllSetResults(): List<SetResultEntity> = storedSetResults.sortedBy { it.id }

    override suspend fun updateSetResult(setResult: SetResultEntity) {
        updatedSetResult = setResult
    }

    override suspend fun updateSetMetrics(
        setId: Long,
        reps: Int?,
        weight: Double?,
        durationSeconds: Long?,
        distance: Double?,
    ) {
        storedSetResults.replaceSet(setId) {
            it.copy(
                reps = reps,
                weight = weight,
                durationSeconds = durationSeconds,
                distance = distance,
            )
        }
    }

    override suspend fun updateSetCompletion(
        setId: Long,
        isCompleted: Boolean,
    ) {
        storedSetResults.replaceSet(setId) {
            it.copy(isCompleted = isCompleted)
        }
    }

    override suspend fun updateSetNotes(
        setId: Long,
        notes: String?,
    ) {
        storedSetResults.replaceSet(setId) {
            it.copy(notes = notes)
        }
    }

    override suspend fun updateExerciseResultNotes(
        exerciseResultId: Long,
        notes: String?,
    ) {
        storedExerciseResults.replaceExerciseResult(exerciseResultId) {
            it.copy(notes = notes)
        }
    }

    override suspend fun deleteSetResult(id: Long) {
        deletedSetResultId = id
    }

    override suspend fun deleteSetResultsForExercise(exerciseResultId: Long) {
        storedSetResults.removeAll { setResult -> setResult.exerciseResultId == exerciseResultId }
    }

    override suspend fun deleteExerciseResult(id: Long) {
        storedExerciseResults.removeAll { exerciseResult -> exerciseResult.id == id }
    }

    override suspend fun deleteWorkoutRun(id: Long): Int =
        if (workoutRuns.remove(id) != null) {
            1
        } else {
            0
        }

    override suspend fun deleteAllSetResults() = Unit

    override suspend fun deleteAllExerciseResults() = Unit

    override suspend fun deleteAllWorkoutRuns() = Unit

    override fun observeCompletedWorkoutRuns(): Flow<List<WorkoutRunEntity>> = workoutRunsFlow

    override fun observeExerciseUsage(): Flow<List<ExerciseUsage>> = MutableStateFlow(emptyList())
}

private fun MutableList<SetResultEntity>.replaceSet(
    setId: Long,
    update: (SetResultEntity) -> SetResultEntity,
) {
    val index = indexOfFirst { it.id == setId }
    if (index >= 0) {
        this[index] = update(this[index])
    }
}

private fun MutableList<ExerciseResultEntity>.replaceExerciseResult(
    exerciseResultId: Long,
    update: (ExerciseResultEntity) -> ExerciseResultEntity,
) {
    val index = indexOfFirst { it.id == exerciseResultId }
    if (index >= 0) {
        this[index] = update(this[index])
    }
}

private class FakeWorkoutScheduleDao(
    private val schedule: ScheduleEntity? = null,
    private val scheduleExercises: List<ScheduleExerciseEntity> = emptyList(),
) : ScheduleDao {
    override fun observeSchedules(): Flow<List<ScheduleEntity>> = MutableStateFlow(emptyList())

    override fun observeSchedule(id: Long): Flow<ScheduleEntity?> = MutableStateFlow(schedule)

    override suspend fun getSchedule(id: Long): ScheduleEntity? = schedule

    override suspend fun getAllSchedules(): List<ScheduleEntity> = listOfNotNull(schedule)

    override fun observeScheduleExercises(scheduleId: Long) = MutableStateFlow(scheduleExercises)

    override suspend fun getScheduleExercises(scheduleId: Long) = scheduleExercises.sortedBy { it.sortOrder }

    override suspend fun getAllScheduleExercises(): List<ScheduleExerciseEntity> = scheduleExercises

    override suspend fun getScheduleExercise(id: Long) = scheduleExercises.firstOrNull { it.id == id }

    override suspend fun insertSchedule(schedule: ScheduleEntity): Long = 1

    override suspend fun insertSchedules(schedules: List<ScheduleEntity>) = Unit

    override suspend fun updateSchedule(schedule: ScheduleEntity) = Unit

    override suspend fun deleteSchedule(id: Long): Int = 1

    override suspend fun deleteAllSchedules() = Unit

    override suspend fun deleteScheduleExercisesForSchedule(scheduleId: Long) = Unit

    override suspend fun insertScheduleExercise(scheduleExercise: ScheduleExerciseEntity): Long = 1

    override suspend fun insertScheduleExercises(scheduleExercises: List<ScheduleExerciseEntity>) = Unit

    override suspend fun updateScheduleExercise(scheduleExercise: ScheduleExerciseEntity) = Unit

    override suspend fun deleteScheduleExercise(id: Long) = Unit

    override suspend fun deleteAllScheduleExercises() = Unit
}

private class FakeWorkoutExerciseDao(
    private val exercises: Map<Long, ExerciseEntity> = emptyMap(),
) : ExerciseDao {
    override fun observeExercises(): Flow<List<ExerciseEntity>> = MutableStateFlow(exercises.values.toList())

    override suspend fun getExercise(id: Long): ExerciseEntity? = exercises[id]

    override suspend fun getAllExercises(): List<ExerciseEntity> = exercises.values.sortedBy { it.id }

    override suspend fun insertExercise(exercise: ExerciseEntity): Long = 1

    override suspend fun insertExercises(exercises: List<ExerciseEntity>) = Unit

    override suspend fun updateExercise(exercise: ExerciseEntity) = Unit

    override suspend fun deleteExercise(id: Long): Int = 1

    override suspend fun deleteScheduleExercisesForExercise(exerciseId: Long) = Unit

    override suspend fun deleteAllExercises() = Unit
}

private fun emptyExerciseResultsFlow(): Flow<List<ExerciseResultEntity>> = MutableStateFlow(emptyList())
