package com.brokenpip3.gymbro.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.brokenpip3.gymbro.ui.screens.workout.ActiveWorkoutScreen
import com.brokenpip3.gymbro.ui.screens.workout.ActiveWorkoutUiState
import com.brokenpip3.gymbro.ui.screens.workout.ExerciseInfoRequest

@Composable
@Suppress("LongParameterList")
fun WorkoutScreen(
    uiState: ActiveWorkoutUiState = ActiveWorkoutUiState(isLoading = false),
    onAddSetRow: (Long) -> Unit = {},
    onUpdateSetMetrics: (Long, String, String, String, String, String) -> Unit = { _, _, _, _, _, _ -> },
    onUpdateSetCompletion: (Long, Boolean) -> Unit = { _, _ -> },
    onDeleteSet: (Long) -> Unit = {},
    onFinishWorkout: () -> Unit = {},
    onShowExerciseInfo: (ExerciseInfoRequest) -> Unit = {},
    onDismissExerciseInfo: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    ActiveWorkoutScreen(
        uiState = uiState,
        onAddSetRow = onAddSetRow,
        onUpdateSetMetrics = onUpdateSetMetrics,
        onUpdateSetCompletion = onUpdateSetCompletion,
        onDeleteSet = onDeleteSet,
        onFinishWorkout = onFinishWorkout,
        onShowExerciseInfo = onShowExerciseInfo,
        onDismissExerciseInfo = onDismissExerciseInfo,
        modifier = modifier,
    )
}
