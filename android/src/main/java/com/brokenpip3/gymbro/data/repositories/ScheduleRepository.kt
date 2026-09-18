package com.brokenpip3.gymbro.data.repositories

import com.brokenpip3.gymbro.data.dao.ExerciseDao
import com.brokenpip3.gymbro.data.dao.ScheduleDao
import com.brokenpip3.gymbro.data.dao.WorkoutRunDao
import com.brokenpip3.gymbro.data.entities.ExerciseEntity
import com.brokenpip3.gymbro.data.entities.ScheduleEntity
import com.brokenpip3.gymbro.data.entities.ScheduleExerciseEntity
import com.brokenpip3.gymbro.data.entities.WorkoutRunEntity
import com.brokenpip3.gymbro.ui.screens.schedules.ScheduleCreator
import com.brokenpip3.gymbro.ui.screens.schedules.ScheduleDetailSource
import com.brokenpip3.gymbro.ui.screens.schedules.ScheduleExerciseTargets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Suppress("TooManyFunctions")
class ScheduleRepository(
    private val scheduleDao: ScheduleDao,
    private val exerciseDao: ExerciseDao,
    private val workoutRunDao: WorkoutRunDao? = null,
) : ScheduleCreator,
    ScheduleDetailSource {
    override fun observeSchedules(): Flow<List<ScheduleEntity>> = scheduleDao.observeSchedules()

    override fun observeSchedule(id: Long): Flow<ScheduleEntity?> = scheduleDao.observeSchedule(id)

    override fun observeScheduleExercises(scheduleId: Long): Flow<List<ScheduleExerciseEntity>> =
        scheduleDao.observeScheduleExercises(scheduleId)

    override fun observeAvailableExercises(): Flow<List<ExerciseEntity>> = exerciseDao.observeExercises()

    override fun observeActiveWorkoutRun(): Flow<WorkoutRunEntity?> {
        val dao = workoutRunDao ?: return flowOf(null)
        return dao.observeActiveWorkoutRun()
    }

    override fun observeCompletedWorkoutRuns(): Flow<List<WorkoutRunEntity>> =
        workoutRunDao?.observeCompletedWorkoutRuns() ?: flowOf(emptyList())

    override suspend fun createSchedule(
        name: String,
        notes: String?,
        nowMillis: Long,
    ): Long =
        scheduleDao.insertSchedule(
            ScheduleEntity(
                name = name,
                notes = notes,
                createdAt = nowMillis,
                updatedAt = nowMillis,
            ),
        )

    override suspend fun updateSchedule(
        id: Long,
        name: String,
        notes: String?,
        nowMillis: Long,
    ) {
        val schedule =
            requireNotNull(scheduleDao.getSchedule(id)) {
                "Schedule $id was not found"
            }
        scheduleDao.updateSchedule(
            schedule.copy(
                name = name,
                notes = notes,
                updatedAt = nowMillis,
            ),
        )
    }

    override suspend fun deleteSchedule(id: Long) {
        require(scheduleDao.deleteScheduleAndAssignments(id)) {
            "Schedule $id was not found"
        }
    }

    override suspend fun assignExercise(
        scheduleId: Long,
        exerciseId: Long,
    ): Long = scheduleDao.insertScheduleExerciseAtEnd(scheduleId, exerciseId)

    override suspend fun removeExerciseFromSchedule(scheduleExerciseId: Long) {
        requireNotNull(scheduleDao.getScheduleExercise(scheduleExerciseId)) {
            "Schedule exercise $scheduleExerciseId was not found"
        }
        scheduleDao.deleteScheduleExercise(scheduleExerciseId)
    }

    override suspend fun moveScheduleExercise(
        scheduleId: Long,
        scheduleExerciseId: Long,
        direction: MoveDirection,
    ) {
        val orderedExercises = scheduleDao.getScheduleExercises(scheduleId).sortedBy { it.sortOrder }
        val currentIndex = orderedExercises.indexOfFirst { it.id == scheduleExerciseId }
        require(currentIndex != -1) {
            "Schedule exercise $scheduleExerciseId was not found in schedule $scheduleId"
        }

        val targetIndex =
            when (direction) {
                MoveDirection.Up -> currentIndex - 1
                MoveDirection.Down -> currentIndex + 1
            }
        if (targetIndex !in orderedExercises.indices) return

        val reordered = orderedExercises.toMutableList()
        val selected = reordered.removeAt(currentIndex)
        reordered.add(targetIndex, selected)

        reordered.forEachIndexed { index, scheduleExercise ->
            scheduleDao.updateScheduleExercise(scheduleExercise.copy(sortOrder = index))
        }
    }

    override suspend fun updateScheduleExerciseTargets(
        scheduleExerciseId: Long,
        targets: ScheduleExerciseTargets,
    ) {
        val scheduleExercise =
            requireNotNull(scheduleDao.getScheduleExercise(scheduleExerciseId)) {
                "Schedule exercise $scheduleExerciseId was not found"
            }
        scheduleDao.updateScheduleExercise(
            scheduleExercise.copy(
                targetSets = targets.targetSets,
                targetReps = targets.targetReps,
                targetWeight = targets.targetWeight,
                targetDurationSeconds = targets.targetDurationSeconds,
                targetDistance = targets.targetDistance,
            ),
        )
    }
}
