package com.brokenpip3.gymbro.data.repositories

import com.brokenpip3.gymbro.data.dao.ExerciseDao
import com.brokenpip3.gymbro.data.dao.ScheduleDao
import com.brokenpip3.gymbro.data.dao.WorkoutRunDao
import com.brokenpip3.gymbro.data.entities.ExerciseResultEntity
import com.brokenpip3.gymbro.data.entities.ScheduleExerciseEntity
import com.brokenpip3.gymbro.data.entities.WorkoutRunEntity
import com.brokenpip3.gymbro.domain.TrackingMode

/** Called within the repository transaction for both previewing and saving. */
internal class WorkoutScheduleUpdater(
    private val workoutDao: WorkoutRunDao,
    private val scheduleDao: ScheduleDao,
    private val exerciseDao: ExerciseDao,
) {
    suspend fun preview(run: WorkoutRunEntity): WorkoutScheduleUpdate? {
        val schedule = scheduleDao.getSchedule(run.scheduleId)
        val original = scheduleDao.getScheduleExercises(run.scheduleId).sortedBy { it.sortOrder }
        val results = workoutDao.getExerciseResults(run.id).sortedBy { it.sortOrder }
        if (schedule != null && original.map { it.exerciseId } == results.map { it.exerciseId }) return null

        var unavailableReason = if (schedule == null) "The original schedule was deleted." else null
        val proposed =
            results.mapIndexed { index, result ->
                val exercise = result.exerciseId?.let { exerciseDao.getExercise(it) }
                if (exercise == null) {
                    unavailableReason = unavailableReason ?: "An exercise was deleted. The schedule cannot be updated."
                } else if (exercise.trackingMode != result.trackingModeSnapshot) {
                    unavailableReason =
                        unavailableReason ?: "An exercise's tracking mode changed. The schedule cannot be updated."
                }
                WorkoutScheduleExercise(
                    name = exercise?.name ?: result.exerciseNameSnapshot,
                    trackingMode = result.trackingModeSnapshot,
                    defaults = result.defaults(run.scheduleId, index),
                )
            }
        return WorkoutScheduleUpdate(run.id, schedule, original, proposed, unavailableReason)
    }

    suspend fun save(
        run: WorkoutRunEntity,
        expected: WorkoutScheduleUpdate,
        nowMillis: Long,
    ) {
        require(expected.workoutRunId == run.id && expected.unavailableReason == null) {
            "The schedule cannot be updated. Finish without updating it."
        }
        require(preview(run) == expected) {
            "The workout or schedule changed. Review it again before saving."
        }
        val schedule = requireNotNull(expected.schedule) { "The original schedule was deleted." }
        scheduleDao.deleteScheduleExercisesForSchedule(schedule.id)
        scheduleDao.insertScheduleExercises(expected.exercises.map { it.defaults })
        scheduleDao.updateSchedule(schedule.copy(updatedAt = nowMillis))
    }

    private suspend fun ExerciseResultEntity.defaults(
        scheduleId: Long,
        order: Int,
    ): ScheduleExerciseEntity {
        val completed = workoutDao.getSetResults(id).filter { it.isCompleted }
        val last = completed.maxByOrNull { it.setOrder }
        return ScheduleExerciseEntity(
            scheduleId = scheduleId,
            exerciseId = exerciseId ?: 0,
            sortOrder = order,
            targetSets = completed.size.takeIf { it > 0 },
            targetReps = last?.reps.takeIf { trackingModeSnapshot != TrackingMode.Timed.databaseValue },
            targetWeight = last?.weight.takeIf { trackingModeSnapshot == TrackingMode.Strength.databaseValue },
            targetDurationSeconds =
                last?.durationSeconds.takeIf { trackingModeSnapshot == TrackingMode.Timed.databaseValue },
            targetDistance = last?.distance.takeIf { trackingModeSnapshot == TrackingMode.Timed.databaseValue },
        )
    }
}
