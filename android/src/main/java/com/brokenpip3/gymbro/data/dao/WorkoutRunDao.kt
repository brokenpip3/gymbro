package com.brokenpip3.gymbro.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.brokenpip3.gymbro.data.entities.ExerciseResultEntity
import com.brokenpip3.gymbro.data.entities.ExerciseUsage
import com.brokenpip3.gymbro.data.entities.SetResultEntity
import com.brokenpip3.gymbro.data.entities.WorkoutRunEntity
import kotlinx.coroutines.flow.Flow

@Dao
@Suppress("TooManyFunctions")
interface WorkoutRunDao {
    @Query("SELECT * FROM workout_runs ORDER BY startedAt DESC")
    fun observeWorkoutRuns(): Flow<List<WorkoutRunEntity>>

    @Query("SELECT * FROM workout_runs WHERE finishedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeActiveWorkoutRun(): Flow<WorkoutRunEntity?>

    @Query("SELECT * FROM workout_runs WHERE id = :id")
    suspend fun getWorkoutRun(id: Long): WorkoutRunEntity?

    @Query("SELECT * FROM workout_runs ORDER BY id")
    suspend fun getAllWorkoutRuns(): List<WorkoutRunEntity>

    @Query("SELECT * FROM workout_runs WHERE finishedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveWorkoutRun(): WorkoutRunEntity?

    @Query(
        """
        SELECT * FROM workout_runs
        WHERE scheduleId = :scheduleId AND finishedAt IS NOT NULL
        ORDER BY finishedAt DESC
        LIMIT 1
        """,
    )
    suspend fun getLatestCompletedWorkoutRunForSchedule(scheduleId: Long): WorkoutRunEntity?

    @Insert
    suspend fun insertWorkoutRun(workoutRun: WorkoutRunEntity): Long

    @Insert
    suspend fun insertWorkoutRuns(workoutRuns: List<WorkoutRunEntity>)

    @Update
    suspend fun updateWorkoutRun(workoutRun: WorkoutRunEntity)

    @Query("UPDATE workout_runs SET notes = :notes WHERE id = :workoutRunId")
    suspend fun updateWorkoutRunNotes(
        workoutRunId: Long,
        notes: String?,
    )

    @Insert
    suspend fun insertExerciseResult(exerciseResult: ExerciseResultEntity): Long

    @Insert
    suspend fun insertExerciseResults(exerciseResults: List<ExerciseResultEntity>)

    @Insert
    suspend fun insertSetResult(setResult: SetResultEntity): Long

    @Insert
    suspend fun insertSetResults(setResults: List<SetResultEntity>)

    @Query("SELECT * FROM exercise_results WHERE workoutRunId = :workoutRunId ORDER BY sortOrder")
    fun observeExerciseResults(workoutRunId: Long): Flow<List<ExerciseResultEntity>>

    @Query("SELECT COUNT(*) FROM exercise_results")
    fun observeExerciseResultChanges(): Flow<Int>

    @Query("SELECT * FROM exercise_results WHERE workoutRunId = :workoutRunId ORDER BY sortOrder")
    suspend fun getExerciseResults(workoutRunId: Long): List<ExerciseResultEntity>

    @Query("SELECT * FROM exercise_results WHERE id = :id")
    suspend fun getExerciseResult(id: Long): ExerciseResultEntity?

    @Query("SELECT * FROM exercise_results ORDER BY id")
    suspend fun getAllExerciseResults(): List<ExerciseResultEntity>

    @Query("SELECT * FROM set_results WHERE exerciseResultId = :exerciseResultId ORDER BY setOrder")
    fun observeSetResults(exerciseResultId: Long): Flow<List<SetResultEntity>>

    @Query("SELECT COUNT(*) FROM set_results")
    fun observeSetResultChanges(): Flow<Int>

    @Query("SELECT * FROM set_results WHERE exerciseResultId = :exerciseResultId ORDER BY setOrder")
    suspend fun getSetResults(exerciseResultId: Long): List<SetResultEntity>

    @Query("SELECT * FROM set_results ORDER BY id")
    suspend fun getAllSetResults(): List<SetResultEntity>

    @Update
    suspend fun updateSetResult(setResult: SetResultEntity)

    @Query(
        """
        UPDATE set_results
        SET reps = :reps,
            weight = :weight,
            durationSeconds = :durationSeconds,
            distance = :distance
        WHERE id = :setId
        """,
    )
    suspend fun updateSetMetrics(
        setId: Long,
        reps: Int?,
        weight: Double?,
        durationSeconds: Long?,
        distance: Double?,
    )

    @Query("UPDATE set_results SET isCompleted = :isCompleted WHERE id = :setId")
    suspend fun updateSetCompletion(
        setId: Long,
        isCompleted: Boolean,
    )

    @Query("UPDATE set_results SET notes = :notes WHERE id = :setId")
    suspend fun updateSetNotes(
        setId: Long,
        notes: String?,
    )

    @Query("UPDATE exercise_results SET notes = :notes WHERE id = :exerciseResultId")
    suspend fun updateExerciseResultNotes(
        exerciseResultId: Long,
        notes: String?,
    )

    @Query("DELETE FROM set_results WHERE id = :id")
    suspend fun deleteSetResult(id: Long)

    @Query("DELETE FROM set_results WHERE exerciseResultId = :exerciseResultId")
    suspend fun deleteSetResultsForExercise(exerciseResultId: Long)

    @Query("DELETE FROM exercise_results WHERE id = :id")
    suspend fun deleteExerciseResult(id: Long)

    @Query("DELETE FROM workout_runs WHERE id = :id")
    suspend fun deleteWorkoutRun(id: Long): Int

    @Query("DELETE FROM set_results")
    suspend fun deleteAllSetResults()

    @Query("DELETE FROM exercise_results")
    suspend fun deleteAllExerciseResults()

    @Query("DELETE FROM workout_runs")
    suspend fun deleteAllWorkoutRuns()

    @Query("SELECT * FROM workout_runs WHERE finishedAt IS NOT NULL ORDER BY finishedAt DESC")
    fun observeCompletedWorkoutRuns(): Flow<List<WorkoutRunEntity>>

    @Query(
        """
        SELECT exercise_results.exerciseId AS exerciseId,
               COUNT(DISTINCT workout_runs.id) AS sessionCount,
               MAX(workout_runs.finishedAt) AS lastCompletedAt
        FROM exercise_results
        INNER JOIN workout_runs ON workout_runs.id = exercise_results.workoutRunId
        WHERE exercise_results.exerciseId IS NOT NULL
          AND workout_runs.finishedAt IS NOT NULL
        GROUP BY exercise_results.exerciseId
        ORDER BY lastCompletedAt DESC
        """,
    )
    fun observeExerciseUsage(): Flow<List<ExerciseUsage>>
}
