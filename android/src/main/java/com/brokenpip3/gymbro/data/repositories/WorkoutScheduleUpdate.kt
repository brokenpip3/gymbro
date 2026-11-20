package com.brokenpip3.gymbro.data.repositories

import com.brokenpip3.gymbro.data.entities.ScheduleEntity
import com.brokenpip3.gymbro.data.entities.ScheduleExerciseEntity

/** The exact schedule and defaults shown before the user opts in to saving. */
data class WorkoutScheduleUpdate(
    val workoutRunId: Long,
    val schedule: ScheduleEntity?,
    val originalExercises: List<ScheduleExerciseEntity>,
    val exercises: List<WorkoutScheduleExercise>,
    val unavailableReason: String? = null,
)

data class WorkoutScheduleExercise(
    val name: String,
    val trackingMode: String,
    val defaults: ScheduleExerciseEntity,
)
