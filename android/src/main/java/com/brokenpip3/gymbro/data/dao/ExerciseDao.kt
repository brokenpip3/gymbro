package com.brokenpip3.gymbro.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.brokenpip3.gymbro.data.entities.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name COLLATE NOCASE")
    fun observeExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExercise(id: Long): ExerciseEntity?

    @Query("SELECT * FROM exercises ORDER BY id")
    suspend fun getAllExercises(): List<ExerciseEntity>

    @Insert
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Insert
    suspend fun insertExercises(exercises: List<ExerciseEntity>)

    @Update
    suspend fun updateExercise(exercise: ExerciseEntity)

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun deleteExercise(id: Long): Int

    @Query("DELETE FROM schedule_exercises WHERE exerciseId = :exerciseId")
    suspend fun deleteScheduleExercisesForExercise(exerciseId: Long)

    @Transaction
    suspend fun deleteExerciseAndScheduleAssignments(exerciseId: Long): Boolean {
        deleteScheduleExercisesForExercise(exerciseId)
        return deleteExercise(exerciseId) > 0
    }

    @Query("DELETE FROM exercises")
    suspend fun deleteAllExercises()
}
