package com.brokenpip3.gymbro.data.repositories

import android.content.Context
import androidx.room.Room
import com.brokenpip3.gymbro.data.GymbroDatabase
import com.brokenpip3.gymbro.data.entities.ExerciseEntity
import com.brokenpip3.gymbro.data.entities.ExerciseResultEntity
import com.brokenpip3.gymbro.data.entities.ScheduleEntity
import com.brokenpip3.gymbro.data.entities.ScheduleExerciseEntity
import com.brokenpip3.gymbro.data.entities.SetResultEntity
import com.brokenpip3.gymbro.data.entities.WorkoutRunEntity
import com.brokenpip3.gymbro.domain.TrackingMode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TransactionRollbackTest {
    @Test
    fun failedWorkoutStartRollsBackRunAndEarlierExerciseResults() =
        runTest {
            val database = database()
            val exerciseDao = database.exerciseDao()
            val scheduleDao = database.scheduleDao()
            val firstExerciseId = exerciseDao.insertExercise(exercise("Squat"))
            val secondExerciseId = exerciseDao.insertExercise(exercise("Lunge"))
            val scheduleId = scheduleDao.insertSchedule(schedule())
            scheduleDao.insertScheduleExercises(
                listOf(
                    scheduleExercise(scheduleId, firstExerciseId, sortOrder = 0),
                    scheduleExercise(scheduleId, secondExerciseId, sortOrder = 1),
                ),
            )
            database.openHelper.writableDatabase.execSQL(
                """
                CREATE TRIGGER fail_second_exercise_result
                BEFORE INSERT ON exercise_results
                WHEN NEW.sortOrder = 1
                BEGIN
                    SELECT RAISE(ABORT, 'forced test failure');
                END
                """.trimIndent(),
            )

            val repository = WorkoutRepository(database)
            runCatching { repository.startWorkout(scheduleId, nowMillis = 1_000L) }

            assertEquals(emptyList<Any>(), database.workoutRunDao().getAllWorkoutRuns())
            assertEquals(emptyList<Any>(), database.workoutRunDao().getAllExerciseResults())
            assertEquals(emptyList<Any>(), database.workoutRunDao().getAllSetResults())
            database.close()
        }

    @Test
    fun failedScheduleDeleteRestoresAssignments() =
        runTest {
            val database = database()
            val exerciseId = database.exerciseDao().insertExercise(exercise("Squat"))
            val scheduleId = database.scheduleDao().insertSchedule(schedule())
            val assignmentId =
                database.scheduleDao().insertScheduleExercise(
                    scheduleExercise(scheduleId, exerciseId, sortOrder = 0),
                )
            database.openHelper.writableDatabase.execSQL(
                """
                CREATE TRIGGER fail_schedule_delete
                BEFORE DELETE ON schedules
                BEGIN
                    SELECT RAISE(ABORT, 'forced test failure');
                END
                """.trimIndent(),
            )

            val repository = ScheduleRepository(database.scheduleDao(), database.exerciseDao())
            runCatching { repository.deleteSchedule(scheduleId) }

            assertEquals(scheduleId, database.scheduleDao().getSchedule(scheduleId)?.id)
            assertEquals(assignmentId, database.scheduleDao().getScheduleExercise(assignmentId)?.id)
            database.close()
        }

    @Test
    fun discardingActiveWorkoutRemovesItsResultsFromRoom() =
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
            database.workoutRunDao().insertSetResult(
                SetResultEntity(
                    exerciseResultId = exerciseResultId,
                    setOrder = 0,
                    reps = 8,
                    weight = 80.0,
                    durationSeconds = null,
                    distance = null,
                    notes = null,
                ),
            )

            WorkoutRepository(database).discardWorkout(workoutRunId)

            assertEquals(null, database.workoutRunDao().getWorkoutRun(workoutRunId))
            assertEquals(emptyList<Any>(), database.workoutRunDao().getAllExerciseResults())
            assertEquals(emptyList<Any>(), database.workoutRunDao().getAllSetResults())
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

private fun exercise(name: String): ExerciseEntity =
    ExerciseEntity(
        name = name,
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
    sortOrder: Int,
): ScheduleExerciseEntity =
    ScheduleExerciseEntity(
        scheduleId = scheduleId,
        exerciseId = exerciseId,
        sortOrder = sortOrder,
        targetSets = null,
        targetReps = null,
        targetWeight = null,
        targetDurationSeconds = null,
        targetDistance = null,
    )
