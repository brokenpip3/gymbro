package com.brokenpip3.gymbro.data.repositories

import com.brokenpip3.gymbro.data.dao.ExerciseDao
import com.brokenpip3.gymbro.data.dao.WorkoutRunDao
import com.brokenpip3.gymbro.data.entities.ExerciseEntity
import com.brokenpip3.gymbro.data.entities.ExerciseUsage
import com.brokenpip3.gymbro.ui.screens.exercises.ExerciseCreator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class ExerciseRepository(
    private val exerciseDao: ExerciseDao,
    private val workoutRunDao: WorkoutRunDao? = null,
) : ExerciseCreator {
    override fun observeExercises(): Flow<List<ExerciseEntity>> = exerciseDao.observeExercises()

    @Suppress("ktlint:standard:function-expression-body")
    override fun observeExerciseUsage(): Flow<List<ExerciseUsage>> {
        return workoutRunDao?.observeExerciseUsage() ?: flowOf(emptyList())
    }

    override suspend fun createExercise(
        name: String,
        notes: String?,
        trackingMode: String,
        nowMillis: Long,
    ): Long =
        exerciseDao.insertExercise(
            ExerciseEntity(
                name = name,
                notes = notes,
                trackingMode = trackingMode,
                createdAt = nowMillis,
                updatedAt = nowMillis,
            ),
        )

    override suspend fun getExercise(id: Long): ExerciseEntity? = exerciseDao.getExercise(id)

    override suspend fun updateExercise(
        id: Long,
        name: String,
        notes: String?,
        trackingMode: String,
        nowMillis: Long,
    ) {
        val exercise =
            exerciseDao.getExercise(id)
                ?: throw IllegalArgumentException("Exercise $id was not found")
        exerciseDao.updateExercise(
            exercise.copy(
                name = name,
                notes = notes,
                trackingMode = trackingMode,
                updatedAt = nowMillis,
            ),
        )
    }

    override suspend fun deleteExercise(id: Long) {
        require(exerciseDao.deleteExerciseAndScheduleAssignments(id)) {
            "Exercise $id was not found"
        }
    }
}
