package com.brokenpip3.gymbro.data.repositories

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.brokenpip3.gymbro.backup.RoomGymbroBackupDataSource
import com.brokenpip3.gymbro.data.GymbroDatabase
import com.brokenpip3.gymbro.data.entities.ExerciseEntity
import com.brokenpip3.gymbro.data.entities.ScheduleEntity
import com.brokenpip3.gymbro.data.entities.ScheduleExerciseEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutScheduleUpdateTest {
    @Test
    fun confirmedScheduleAndDefaultsPersistWithCompletedWorkout() =
        runBlocking {
            withDatabase { database, repository, runId ->
                val preview = requireNotNull(repository.getScheduleUpdatePreview(runId))
                repository.finishWorkout(runId, 2000, preview)
                val rows = database.scheduleDao().getScheduleExercises(requireNotNull(preview.schedule).id)
                assertEquals(preview.exercises.map { it.defaults }, rows.map { it.copy(id = 0) })
                assertEquals(2000L, database.workoutRunDao().getWorkoutRun(runId)?.finishedAt)
            }
        }

    @Test
    fun failedFinishRollsBackScheduleReplacementAndDefaults() =
        runBlocking {
            withDatabase { database, repository, runId ->
                val backup = RoomGymbroBackupDataSource(database)
                val before = backup.readBackupData()
                val preview = requireNotNull(repository.getScheduleUpdatePreview(runId))
                database.openHelper.writableDatabase.execSQL(
                    "CREATE TRIGGER reject_finish BEFORE UPDATE OF finishedAt ON workout_runs " +
                        "BEGIN SELECT RAISE(ABORT, 'Simulated finish failure'); END",
                )
                try {
                    repository.finishWorkout(runId, 2000, preview)
                    fail("Expected finish failure")
                } catch (_: android.database.sqlite.SQLiteException) {
                    assertEquals(before, backup.readBackupData())
                    assertNotNull(database.workoutRunDao().getActiveWorkoutRun())
                }
            }
        }

    private suspend fun withDatabase(block: suspend (GymbroDatabase, WorkoutRepository, Long) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, GymbroDatabase::class.java).build()
        try {
            val exerciseId =
                database.exerciseDao().insertExercise(
                    ExerciseEntity(name = "Press", notes = null, trackingMode = "strength", createdAt = 100, updatedAt = 100),
                )
            val scheduleId =
                database.scheduleDao().insertSchedule(
                    ScheduleEntity(name = "Push", notes = null, createdAt = 100, updatedAt = 100),
                )
            database.scheduleDao().insertScheduleExercise(
                ScheduleExerciseEntity(
                    scheduleId = scheduleId,
                    exerciseId = exerciseId,
                    sortOrder = 0,
                    targetSets = 1,
                    targetReps = 5,
                    targetWeight = 10.0,
                    targetDurationSeconds = null,
                    targetDistance = null,
                ),
            )
            val repository = WorkoutRepository(database)
            val runId = repository.startWorkout(scheduleId, 1000)
            val addedId = repository.addExerciseToActiveWorkout(runId, exerciseId)
            repository.addSet(addedId, reps = 8, weight = 20.0, durationSeconds = null, distance = null, notes = null)
            block(database, repository, runId)
        } finally {
            database.close()
        }
    }
}
