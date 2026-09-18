package com.brokenpip3.gymbro.data.repositories

import android.content.Context
import androidx.room.Room
import com.brokenpip3.gymbro.data.GymbroDatabase
import com.brokenpip3.gymbro.data.entities.ExerciseEntity
import com.brokenpip3.gymbro.data.entities.ExerciseResultEntity
import com.brokenpip3.gymbro.data.entities.ScheduleEntity
import com.brokenpip3.gymbro.data.entities.ScheduleExerciseEntity
import com.brokenpip3.gymbro.data.entities.WorkoutRunEntity
import com.brokenpip3.gymbro.domain.TrackingMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class WorkoutRepositoryConcurrencyTest {
    @Test
    fun concurrentWorkoutStartsReuseOneActiveRun() =
        runTest {
            val database = database()
            val exerciseId = database.exerciseDao().insertExercise(exercise())
            val scheduleId = database.scheduleDao().insertSchedule(schedule())
            database.scheduleDao().insertScheduleExercise(scheduleExercise(scheduleId, exerciseId))
            val repository = WorkoutRepository(database)

            val runIds =
                List(2) {
                    async(Dispatchers.Default) {
                        repository.startWorkout(scheduleId = scheduleId, nowMillis = 1_000L)
                    }
                }.awaitAll()

            assertEquals(1, database.workoutRunDao().getAllWorkoutRuns().size)
            assertEquals(listOf(runIds.first(), runIds.first()), runIds)
            database.close()
        }

    @Test
    fun unfinishedWorkoutIsVisibleToARecreatedRepository() =
        runTest {
            val database = database()
            val exerciseId = database.exerciseDao().insertExercise(exercise())
            val scheduleId = database.scheduleDao().insertSchedule(schedule())
            database.scheduleDao().insertScheduleExercise(scheduleExercise(scheduleId, exerciseId))

            val startedRunId = WorkoutRepository(database).startWorkout(scheduleId, nowMillis = 1_000L)
            val recreatedRepository = WorkoutRepository(database)

            assertEquals(startedRunId, recreatedRepository.observeActiveWorkoutRun().first()?.id)
            database.close()
        }

    @Test
    fun concurrentSetAddsReceiveUniqueSequentialOrders() =
        runTest {
            val database = database()
            val workoutRunId =
                database.workoutRunDao().insertWorkoutRun(
                    WorkoutRunEntity(
                        scheduleId = 1L,
                        scheduleNameSnapshot = "Leg Day",
                        startedAt = 0L,
                        finishedAt = null,
                        notes = null,
                    ),
                )
            val exerciseResultId =
                database.workoutRunDao().insertExerciseResult(
                    ExerciseResultEntity(
                        workoutRunId = workoutRunId,
                        exerciseId = null,
                        exerciseNameSnapshot = "Squat",
                        trackingModeSnapshot = TrackingMode.Strength.databaseValue,
                        sortOrder = 0,
                        notes = null,
                    ),
                )
            val repository = WorkoutRepository(database)

            List(2) {
                async(Dispatchers.Default) {
                    repository.addSet(
                        exerciseResultId = exerciseResultId,
                        reps = 8,
                        weight = 80.0,
                        durationSeconds = null,
                        distance = null,
                        notes = null,
                    )
                }
            }.awaitAll()

            assertEquals(
                listOf(0, 1),
                database
                    .workoutRunDao()
                    .getSetResults(exerciseResultId)
                    .map { it.setOrder }
                    .sorted(),
            )
            database.close()
        }
}

private fun database(): GymbroDatabase =
    Room
        .inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication() as Context,
            GymbroDatabase::class.java,
        ).allowMainThreadQueries()
        .build()

private fun exercise(): ExerciseEntity =
    ExerciseEntity(
        name = "Squat",
        notes = null,
        trackingMode = TrackingMode.Strength.databaseValue,
        createdAt = 0L,
        updatedAt = 0L,
    )

private fun schedule(): ScheduleEntity =
    ScheduleEntity(
        name = "Leg Day",
        notes = null,
        createdAt = 0L,
        updatedAt = 0L,
    )

private fun scheduleExercise(
    scheduleId: Long,
    exerciseId: Long,
): ScheduleExerciseEntity =
    ScheduleExerciseEntity(
        scheduleId = scheduleId,
        exerciseId = exerciseId,
        sortOrder = 0,
        targetSets = null,
        targetReps = null,
        targetWeight = null,
        targetDurationSeconds = null,
        targetDistance = null,
    )
