package com.brokenpip3.gymbro.data.repositories

import com.brokenpip3.gymbro.domain.TrackingMode

data class CompletedWorkoutEdit(
    val workoutRunId: Long,
    val startedAt: Long,
    val finishedAt: Long,
    val sets: List<CompletedSetEdit>,
) {
    fun validateTimes(nowMillis: Long) {
        require(startedAt >= 0 && finishedAt >= startedAt) { "End must be at or after the start" }
        require(finishedAt <= nowMillis) { "Workout times cannot be in the future" }
    }
}

data class CompletedSetEdit(
    val setId: Long,
    val reps: Int?,
    val weight: Double?,
    val durationSeconds: Long?,
    val distance: Double?,
    val isCompleted: Boolean,
) {
    fun validate(mode: TrackingMode) {
        require(reps == null || reps > 0) { "Reps must be a positive whole number" }
        require(weight == null || (weight.isFinite() && weight > 0)) { "Weight must be a positive finite number" }
        require(durationSeconds == null || durationSeconds > 0) { "Duration must be positive whole seconds" }
        require(distance == null || (distance.isFinite() && distance > 0)) {
            "Distance must be a positive finite number"
        }
        if (isCompleted) {
            when (mode) {
                TrackingMode.Strength ->
                    require(reps != null && weight != null) { "Strength sets need reps and weight" }
                TrackingMode.Bodyweight -> require(reps != null) { "Bodyweight sets need reps" }
                TrackingMode.Timed -> require(durationSeconds != null) { "Timed sets need a duration" }
            }
        }
    }
}
