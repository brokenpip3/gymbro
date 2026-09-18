package com.brokenpip3.gymbro.ui.screens.workout

import com.brokenpip3.gymbro.domain.TrackingMode

data class ActiveWorkoutUiState(
    val isLoading: Boolean = true,
    val activeWorkout: ActiveWorkoutUiModel? = null,
    val errorMessage: String? = null,
    val exerciseInfo: ExerciseInfoUiModel? = null,
    val finishedWorkoutRunId: Long? = null,
    val discardedWorkoutRunId: Long? = null,
    val availableExercises: List<AvailableWorkoutExerciseUiModel> = emptyList(),
    val isAddExerciseDialogVisible: Boolean = false,
    val addExerciseErrorMessage: String? = null,
)

data class ActiveWorkoutUiModel(
    val runId: Long,
    val scheduleName: String,
    val startedAt: Long,
    val notes: String? = null,
    val exercises: List<WorkoutExerciseUiModel>,
)

internal val ActiveWorkoutUiModel.progress: WorkoutProgressUiModel
    get() {
        val allSets = exercises.flatMap { exercise -> exercise.sets }
        return WorkoutProgressUiModel(
            completedSets = allSets.count { set -> set.isCompleted },
            totalSets = allSets.size,
            completedExercises =
                exercises.count { exercise ->
                    exercise.sets.isNotEmpty() && exercise.sets.all { set -> set.isCompleted }
                },
            totalExercises = exercises.size,
        )
    }

internal data class WorkoutProgressUiModel(
    val completedSets: Int,
    val totalSets: Int,
    val completedExercises: Int,
    val totalExercises: Int,
) {
    val fraction: Float
        get() =
            if (totalSets == 0) {
                0f
            } else {
                completedSets.toFloat() / totalSets.toFloat()
            }

    val label: String
        get() = "$completedSets of $totalSets sets · $completedExercises of $totalExercises exercises"
}

data class WorkoutExerciseUiModel(
    val exerciseResultId: Long,
    val exerciseName: String,
    val trackingMode: TrackingMode,
    val notes: String?,
    val sets: List<WorkoutSetUiModel>,
)

data class WorkoutSetUiModel(
    val id: Long,
    val setOrder: Int,
    val reps: Int?,
    val weight: Double?,
    val durationSeconds: Long?,
    val distance: Double?,
    val notes: String?,
    val isCompleted: Boolean = true,
)

data class AvailableWorkoutExerciseUiModel(
    val id: Long,
    val name: String,
    val trackingMode: TrackingMode,
)

data class SetEditRequest(
    val setId: Long,
    val exerciseResultId: Long,
    val trackingMode: TrackingMode,
    val reps: Int?,
    val weight: Double?,
    val durationSeconds: Long?,
    val distance: Double?,
)

data class SetNotesEditRequest(
    val setId: Long,
    val setNumber: Int,
    val notes: String?,
)

data class ExerciseInfoRequest(
    val exerciseResultId: Long,
    val exerciseName: String,
)

data class ExerciseNotesEditRequest(
    val exerciseResultId: Long,
    val exerciseName: String,
    val notes: String?,
)

data class ExerciseInfoUiModel(
    val exerciseName: String,
    val notes: String?,
    val headline: String?,
    val history: List<String>,
    val exerciseId: Long? = null,
)
