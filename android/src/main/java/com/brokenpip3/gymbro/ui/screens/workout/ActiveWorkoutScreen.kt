@file:Suppress("TooManyFunctions")

package com.brokenpip3.gymbro.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.brokenpip3.gymbro.domain.TrackingMode
import com.brokenpip3.gymbro.ui.components.EmptyState
import com.brokenpip3.gymbro.ui.components.GymbroIcons
import com.brokenpip3.gymbro.ui.rememberKeyboardDismissal
import kotlinx.coroutines.delay

@Composable
fun WorkoutRoute(
    repository: ActiveWorkoutSource,
    onWorkoutFinished: () -> Unit = {},
    onWorkoutDiscarded: () -> Unit = {},
    onOpenExerciseStats: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val viewModel: ActiveWorkoutViewModel =
        viewModel(
            factory =
                remember(repository) {
                    ActiveWorkoutViewModelFactory(repository)
                },
        )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.finishedWorkoutRunId) {
        if (uiState.finishedWorkoutRunId != null) {
            onWorkoutFinished()
            viewModel.acknowledgeWorkoutFinished()
        }
    }
    LaunchedEffect(uiState.discardedWorkoutRunId) {
        if (uiState.discardedWorkoutRunId != null) {
            onWorkoutDiscarded()
            viewModel.acknowledgeWorkoutDiscarded()
        }
    }

    ActiveWorkoutScreen(
        uiState = uiState,
        onAddSetRow = viewModel::addSetRow,
        onUpdateSetMetrics = viewModel::updateSetMetrics,
        onUpdateSetCompletion = viewModel::updateSetCompletion,
        onUpdateSetNotes = viewModel::updateSetNotes,
        onAddSet = viewModel::addSet,
        onAddEmptySet = viewModel::addEmptySet,
        onDeleteSet = viewModel::deleteSet,
        onDeleteExercise = viewModel::deleteExerciseResult,
        onUpdateExerciseNotes = viewModel::updateExerciseNotes,
        onUpdateWorkoutNotes = viewModel::updateWorkoutNotes,
        onFinishWorkout = viewModel::finishWorkout,
        onDiscardWorkout = viewModel::discardWorkout,
        onShowAddExerciseDialog = viewModel::showAddExerciseDialog,
        onDismissAddExerciseDialog = viewModel::dismissAddExerciseDialog,
        onAddExerciseToWorkout = viewModel::addExerciseToWorkout,
        onShowExerciseInfo = { request -> viewModel.showExerciseInfo(request.exerciseResultId) },
        onDismissExerciseInfo = viewModel::dismissExerciseInfo,
        onOpenExerciseStats = { exerciseId ->
            viewModel.dismissExerciseInfo()
            onOpenExerciseStats(exerciseId)
        },
        modifier = modifier,
    )
}

internal class ActiveWorkoutViewModelFactory(
    private val source: ActiveWorkoutSource,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ActiveWorkoutViewModel::class.java)) {
            return ActiveWorkoutViewModel(source = source) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

@Composable
@Suppress("LongParameterList")
fun ActiveWorkoutScreen(
    uiState: ActiveWorkoutUiState,
    onAddSetRow: (Long) -> Unit = {},
    onUpdateSetMetrics: (Long, String, String, String, String, String) -> Unit = { _, _, _, _, _, _ -> },
    onUpdateSetCompletion: (Long, Boolean) -> Unit = { _, _ -> },
    onUpdateSetNotes: (Long, String) -> Unit = { _, _ -> },
    onAddSet: (Long, Int?, Double?, Long?, Double?) -> Unit = { _, _, _, _, _ -> },
    onAddEmptySet: (Long) -> Unit = {},
    onDeleteSet: (Long) -> Unit = {},
    onDeleteExercise: (Long) -> Unit = {},
    onUpdateExerciseNotes: (Long, String) -> Unit = { _, _ -> },
    onUpdateWorkoutNotes: (String) -> Unit = {},
    onFinishWorkout: () -> Unit = {},
    onDiscardWorkout: () -> Unit = {},
    onShowAddExerciseDialog: () -> Unit = {},
    onDismissAddExerciseDialog: () -> Unit = {},
    onAddExerciseToWorkout: (Long) -> Unit = {},
    onShowExerciseInfo: (ExerciseInfoRequest) -> Unit = {},
    onDismissExerciseInfo: () -> Unit = {},
    onOpenExerciseStats: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val actions =
        ActiveWorkoutActions(
            onAddSetRow = onAddSetRow,
            onUpdateSetMetrics = onUpdateSetMetrics,
            onUpdateSetCompletion = onUpdateSetCompletion,
            onUpdateSetNotes = onUpdateSetNotes,
            onAddSet = onAddSet,
            onAddEmptySet = onAddEmptySet,
            onDeleteSet = onDeleteSet,
            onDeleteExercise = onDeleteExercise,
            onUpdateExerciseNotes = onUpdateExerciseNotes,
            onUpdateWorkoutNotes = onUpdateWorkoutNotes,
            onFinishWorkout = onFinishWorkout,
            onDiscardWorkout = onDiscardWorkout,
            onShowAddExerciseDialog = onShowAddExerciseDialog,
            onShowExerciseInfo = onShowExerciseInfo,
        )
    val activeWorkout = uiState.activeWorkout
    when {
        uiState.isLoading -> {
            Text(
                text = "Loading workout...",
                modifier = modifier.padding(16.dp),
            )
        }

        activeWorkout == null -> {
            EmptyState(
                icon = GymbroIcons.Workout,
                title = "No active workout",
                body = "Start a schedule when you are ready to train.",
                actionLabel = null,
                onAction = null,
                modifier = modifier.fillMaxSize(),
            )
        }

        else -> {
            ActiveWorkoutContent(
                workout = activeWorkout,
                errorMessage = uiState.errorMessage,
                actions = actions,
                modifier = modifier,
            )
        }
    }

    uiState.exerciseInfo?.let { exerciseInfo ->
        ExerciseInfoDialog(
            exerciseInfo = exerciseInfo,
            onDismiss = onDismissExerciseInfo,
            onOpenExerciseStats = onOpenExerciseStats,
        )
    }

    if (uiState.isAddExerciseDialogVisible) {
        AddExerciseDialog(
            exercises = uiState.availableExercises,
            errorMessage = uiState.addExerciseErrorMessage,
            onSelectExercise = onAddExerciseToWorkout,
            onDismiss = onDismissAddExerciseDialog,
        )
    }
}

private data class ActiveWorkoutActions(
    val onAddSetRow: (Long) -> Unit,
    val onUpdateSetMetrics: (Long, String, String, String, String, String) -> Unit,
    val onUpdateSetCompletion: (Long, Boolean) -> Unit,
    val onUpdateSetNotes: (Long, String) -> Unit,
    val onAddSet: (Long, Int?, Double?, Long?, Double?) -> Unit,
    val onAddEmptySet: (Long) -> Unit,
    val onDeleteSet: (Long) -> Unit,
    val onDeleteExercise: (Long) -> Unit,
    val onUpdateExerciseNotes: (Long, String) -> Unit,
    val onUpdateWorkoutNotes: (String) -> Unit,
    val onFinishWorkout: () -> Unit,
    val onDiscardWorkout: () -> Unit,
    val onShowAddExerciseDialog: () -> Unit,
    val onShowExerciseInfo: (ExerciseInfoRequest) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongMethod")
private fun ActiveWorkoutContent(
    workout: ActiveWorkoutUiModel,
    errorMessage: String?,
    actions: ActiveWorkoutActions,
    modifier: Modifier = Modifier,
) {
    var nowMillis by remember(workout.startedAt) { mutableStateOf(System.currentTimeMillis()) }
    var editRequest by remember { mutableStateOf<SetEditRequest?>(null) }
    var setNotesEditRequest by remember { mutableStateOf<SetNotesEditRequest?>(null) }
    var notesEditRequest by remember { mutableStateOf<ExerciseNotesEditRequest?>(null) }
    var isWorkoutNotesDialogVisible by remember { mutableStateOf(false) }
    var isFinishConfirmationVisible by remember { mutableStateOf(false) }
    var isDiscardConfirmationVisible by remember { mutableStateOf(false) }
    LaunchedEffect(workout.startedAt) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000L)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("active-workout-list"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ActiveWorkoutHeader(
                workout = workout,
                nowMillis = nowMillis,
                errorMessage = errorMessage,
                onEditWorkoutNotes = { isWorkoutNotesDialogVisible = true },
                onAddExercise = actions.onShowAddExerciseDialog,
            )
        }

        items(
            items = workout.exercises,
            key = { exercise -> exercise.exerciseResultId },
        ) { exercise ->
            DismissibleExerciseItem(
                exercise = exercise,
                actions = actions,
                onEditSet = { request -> editRequest = request },
                onEditSetNotes = { request -> setNotesEditRequest = request },
                onEditExerciseNotes = { request -> notesEditRequest = request },
            )
        }

        item {
            WorkoutActionRow(
                onFinishWorkout = { isFinishConfirmationVisible = true },
                onDiscardWorkout = { isDiscardConfirmationVisible = true },
            )
        }
    }

    if (isFinishConfirmationVisible) {
        FinishWorkoutConfirmationDialog(
            onDismiss = { isFinishConfirmationVisible = false },
            onConfirm = {
                isFinishConfirmationVisible = false
                actions.onFinishWorkout()
            },
        )
    }

    if (isDiscardConfirmationVisible) {
        DiscardWorkoutConfirmationDialog(
            onDismiss = { isDiscardConfirmationVisible = false },
            onConfirm = {
                isDiscardConfirmationVisible = false
                actions.onDiscardWorkout()
            },
        )
    }

    editRequest?.let { request ->
        SetEditDialog(
            request = request,
            onDismiss = { editRequest = null },
            onSave = { setId, repsText, weightText, minutesText, secondsText, distanceText ->
                actions.onUpdateSetMetrics(setId, repsText, weightText, minutesText, secondsText, distanceText)
                editRequest = null
            },
        )
    }

    setNotesEditRequest?.let { request ->
        SetNotesDialog(
            request = request,
            onDismiss = { setNotesEditRequest = null },
            onSave = { setId, notesText ->
                actions.onUpdateSetNotes(setId, notesText)
                setNotesEditRequest = null
            },
        )
    }

    notesEditRequest?.let { request ->
        ExerciseNotesDialog(
            request = request,
            onDismiss = { notesEditRequest = null },
            onSave = { exerciseResultId, notesText ->
                actions.onUpdateExerciseNotes(exerciseResultId, notesText)
                notesEditRequest = null
            },
        )
    }

    if (isWorkoutNotesDialogVisible) {
        WorkoutNotesDialog(
            initialNotes = workout.notes,
            onDismiss = { isWorkoutNotesDialogVisible = false },
            onSave = { notesText ->
                actions.onUpdateWorkoutNotes(notesText)
                isWorkoutNotesDialogVisible = false
            },
        )
    }
}

@Composable
private fun ActiveWorkoutHeader(
    workout: ActiveWorkoutUiModel,
    nowMillis: Long,
    errorMessage: String?,
    onEditWorkoutNotes: () -> Unit,
    onAddExercise: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = workout.scheduleName,
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text =
                "Elapsed ${
                    formatDuration(
                        (nowMillis - workout.startedAt).coerceAtLeast(0L) / 1000L,
                    )
                }",
            style = MaterialTheme.typography.bodyMedium,
        )
        WorkoutProgress(progress = workout.progress)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = workout.notes?.takeIf { notes -> notes.isNotBlank() } ?: "Add a note about this workout",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color =
                    if (workout.notes.isNullOrBlank()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(
                onClick = onEditWorkoutNotes,
                modifier = Modifier.testTag("edit-workout-notes"),
            ) {
                Icon(
                    imageVector = GymbroIcons.Edit,
                    contentDescription =
                        if (workout.notes.isNullOrBlank()) {
                            "Add workout notes"
                        } else {
                            "Edit workout notes"
                        },
                )
            }
        }
        errorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(
            onClick = onAddExercise,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "Add Exercise")
        }
    }
}

@Composable
private fun WorkoutProgress(progress: WorkoutProgressUiModel) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Progress",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = progress.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { progress.fraction },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FinishWorkoutConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Finish workout?") },
        text = { Text(text = "This workout will move to Results.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Finish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Keep training")
            }
        },
    )
}

@Composable
private fun DiscardWorkoutConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Discard workout?") },
        text = { Text(text = "This active workout and its sets will be deleted.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Discard")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Keep workout")
            }
        },
    )
}

@Composable
private fun WorkoutActionRow(
    onFinishWorkout: () -> Unit,
    onDiscardWorkout: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Button(
            onClick = onFinishWorkout,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "Finish Workout")
        }
        TextButton(
            onClick = onDiscardWorkout,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(text = "Discard workout")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DismissibleExerciseItem(
    exercise: WorkoutExerciseUiModel,
    actions: ActiveWorkoutActions,
    onEditSet: (SetEditRequest) -> Unit,
    onEditSetNotes: (SetNotesEditRequest) -> Unit,
    onEditExerciseNotes: (ExerciseNotesEditRequest) -> Unit,
) {
    @Suppress("DEPRECATION")
    val dismissState =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value != SwipeToDismissBoxValue.Settled) {
                    actions.onDeleteExercise(exercise.exerciseResultId)
                }
                false
            },
        )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = "Delete",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
            tonalElevation = 1.dp,
        ) {
            ExerciseRoutineSection(
                exercise = exercise,
                actions = actions,
                onEditSet = onEditSet,
                onEditSetNotes = onEditSetNotes,
                onEditExerciseNotes = onEditExerciseNotes,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseRoutineSection(
    exercise: WorkoutExerciseUiModel,
    actions: ActiveWorkoutActions,
    onEditSet: (SetEditRequest) -> Unit,
    onEditSetNotes: (SetNotesEditRequest) -> Unit,
    onEditExerciseNotes: (ExerciseNotesEditRequest) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ExerciseRoutineHeader(
            exercise = exercise,
            onEditExerciseNotes = onEditExerciseNotes,
            onShowExerciseInfo = actions.onShowExerciseInfo,
        )

        RoutineHeaderRow(trackingMode = exercise.trackingMode)

        exercise.sets.forEach { set ->
            @Suppress("DEPRECATION")
            val dismissState =
                rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value != SwipeToDismissBoxValue.Settled) {
                            actions.onDeleteSet(set.id)
                        }
                        false
                    },
                )
            SwipeToDismissBox(
                state = dismissState,
                enableDismissFromStartToEnd = true,
                enableDismissFromEndToStart = true,
                backgroundContent = {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Delete",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                },
            ) {
                RoutineSetRow(
                    exercise = exercise,
                    set = set,
                    onEditSet = onEditSet,
                    onEditSetNotes = onEditSetNotes,
                    onUpdateSetCompletion = actions.onUpdateSetCompletion,
                )
            }
        }

        AddSetControls(
            exercise = exercise,
            onAddSet = actions.onAddSet,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                onClick = { actions.onAddEmptySet(exercise.exerciseResultId) },
                modifier = Modifier.weight(1f),
            ) {
                Text(text = ADD_SET_ROW_LABEL)
            }

            TextButton(
                onClick = { actions.onAddSetRow(exercise.exerciseResultId) },
                modifier = Modifier.weight(1f),
            ) {
                Text(text = "Copy Previous")
            }
        }
    }
}

@Composable
private fun ExerciseRoutineHeader(
    exercise: WorkoutExerciseUiModel,
    onEditExerciseNotes: (ExerciseNotesEditRequest) -> Unit,
    onShowExerciseInfo: (ExerciseInfoRequest) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val actionPlacement = routineHeaderActionPlacement(maxWidth.value.toInt())
        val titleContent: @Composable () -> Unit = {
            Text(
                text = exercise.exerciseName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = exercise.trackingMode.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            exercise.notes?.let { notes ->
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        val actionContent: @Composable () -> Unit = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(
                    onClick = {
                        onEditExerciseNotes(
                            ExerciseNotesEditRequest(
                                exerciseResultId = exercise.exerciseResultId,
                                exerciseName = exercise.exerciseName,
                                notes = exercise.notes,
                            ),
                        )
                    },
                ) {
                    Text(text = "Notes")
                }
                TextButton(
                    onClick = {
                        onShowExerciseInfo(
                            ExerciseInfoRequest(
                                exerciseResultId = exercise.exerciseResultId,
                                exerciseName = exercise.exerciseName,
                            ),
                        )
                    },
                ) {
                    Text(text = "Info")
                }
            }
        }

        if (actionPlacement == RoutineHeaderActionPlacement.BelowTitle) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                titleContent()
                actionContent()
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    titleContent()
                }
                actionContent()
            }
        }
    }
}

@Composable
private fun AddExerciseDialog(
    exercises: List<AvailableWorkoutExerciseUiModel>,
    errorMessage: String?,
    onSelectExercise: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val keyboardDismissal = rememberKeyboardDismissal()
    val filteredExercises = filterAvailableWorkoutExercises(exercises, query)

    AlertDialog(
        onDismissRequest = {
            keyboardDismissal.dismiss()
            onDismiss()
        },
        title = { Text(text = "Add Exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(text = "Search exercises") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardDismissal.dismiss() }),
                    modifier = Modifier.fillMaxWidth(),
                )
                addExerciseDialogErrorLines(errorMessage).forEach { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (exercises.isEmpty()) {
                    Text(text = "No exercises yet. Create one in Exercises first.")
                } else if (filteredExercises.isEmpty()) {
                    Text(text = "No matching exercises")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(
                            items = filteredExercises,
                            key = { exercise -> exercise.id },
                        ) { exercise ->
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                        .clickable(
                                            onClick = {
                                                keyboardDismissal.dismiss()
                                                onSelectExercise(exercise.id)
                                            },
                                        ),
                            ) {
                                Text(
                                    text = exercise.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    text = exercise.trackingMode.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = {
                    keyboardDismissal.dismiss()
                    onDismiss()
                },
            ) {
                Text(text = "Cancel")
            }
        },
    )
}

internal fun addExerciseDialogErrorLines(errorMessage: String?): List<String> =
    errorMessage
        ?.takeIf { message -> message.isNotBlank() }
        ?.let(::listOf)
        .orEmpty()

internal fun filterAvailableWorkoutExercises(
    exercises: List<AvailableWorkoutExerciseUiModel>,
    query: String,
): List<AvailableWorkoutExerciseUiModel> {
    val normalizedQuery = query.trim()
    return if (normalizedQuery.isEmpty()) {
        exercises
    } else {
        exercises.filter { exercise ->
            exercise.name.contains(normalizedQuery, ignoreCase = true)
        }
    }
}

@Composable
private fun ExerciseInfoDialog(
    exerciseInfo: ExerciseInfoUiModel,
    onDismiss: () -> Unit,
    onOpenExerciseStats: (Long) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = exerciseInfo.exerciseName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                exerciseInfoDialogLines(exerciseInfo).forEach { line ->
                    Text(text = line)
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                exerciseInfo.exerciseId?.let { exerciseId ->
                    TextButton(onClick = { onOpenExerciseStats(exerciseId) }) {
                        Text(text = "View full stats")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(text = "Close")
                }
            }
        },
    )
}

@Composable
private fun ExerciseNotesDialog(
    request: ExerciseNotesEditRequest,
    onDismiss: () -> Unit,
    onSave: (Long, String) -> Unit,
) {
    var notesText by remember(request.exerciseResultId) { mutableStateOf(request.notes.orEmpty()) }
    val keyboardDismissal = rememberKeyboardDismissal()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = request.exerciseName) },
        text = {
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text(text = "Exercise notes") },
                minLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardDismissal.dismiss() }),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    keyboardDismissal.dismiss()
                    onSave(request.exerciseResultId, notesText)
                },
            ) {
                Text(text = "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        },
    )
}

@Composable
private fun WorkoutNotesDialog(
    initialNotes: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var notesText by remember(initialNotes) { mutableStateOf(initialNotes.orEmpty()) }
    val keyboardDismissal = rememberKeyboardDismissal()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Workout notes") },
        text = {
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text(text = "Notes") },
                minLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardDismissal.dismiss() }),
                modifier = Modifier.fillMaxWidth().testTag("workout-notes-input"),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    keyboardDismissal.dismiss()
                    onSave(notesText)
                },
            ) {
                Text(text = "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        },
    )
}

@Composable
private fun SetNotesDialog(
    request: SetNotesEditRequest,
    onDismiss: () -> Unit,
    onSave: (Long, String) -> Unit,
) {
    var notesText by remember(request.setId) { mutableStateOf(request.notes.orEmpty()) }
    val keyboardDismissal = rememberKeyboardDismissal()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Set ${request.setNumber} notes") },
        text = {
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text(text = "Set notes") },
                minLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardDismissal.dismiss() }),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    keyboardDismissal.dismiss()
                    onSave(request.setId, notesText)
                },
            ) {
                Text(text = "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        },
    )
}

@Composable
private fun RoutineHeaderRow(trackingMode: TrackingMode) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "#",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(28.dp),
        )
        routineHeaderLabels(trackingMode).forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RoutineSetRow(
    exercise: WorkoutExerciseUiModel,
    set: WorkoutSetUiModel,
    onEditSet: (SetEditRequest) -> Unit,
    onEditSetNotes: (SetNotesEditRequest) -> Unit,
    onUpdateSetCompletion: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable {
                        onEditSet(
                            SetEditRequest(
                                setId = set.id,
                                exerciseResultId = exercise.exerciseResultId,
                                trackingMode = exercise.trackingMode,
                                reps = set.reps,
                                weight = set.weight,
                                durationSeconds = set.durationSeconds,
                                distance = set.distance,
                            ),
                        )
                    }.padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = (set.setOrder + 1).toString(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.width(28.dp),
            )
            set.metricCells(exercise.trackingMode).forEach { value ->
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }
            Checkbox(
                checked = set.isCompleted,
                onCheckedChange = { isChecked -> onUpdateSetCompletion(set.id, isChecked) },
                modifier =
                    Modifier
                        .weight(1f)
                        .testTag("set-${set.id}-completion"),
            )
        }
        SetNotesRow(
            set = set,
            onEditSetNotes = onEditSetNotes,
        )
    }
}

@Composable
private fun SetNotesRow(
    set: WorkoutSetUiModel,
    onEditSetNotes: (SetNotesEditRequest) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        set.notes?.let { notes ->
            Text(
                text = notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        TextButton(
            onClick = {
                onEditSetNotes(
                    SetNotesEditRequest(
                        setId = set.id,
                        setNumber = set.setOrder + 1,
                        notes = set.notes,
                    ),
                )
            },
        ) {
            Text(text = "Set note")
        }
    }
}

@Composable
private fun SetEditDialog(
    request: SetEditRequest,
    onDismiss: () -> Unit,
    onSave: (
        setId: Long,
        repsText: String,
        weightText: String,
        minutesText: String,
        secondsText: String,
        distanceText: String,
    ) -> Unit,
) {
    var repsText by remember(request.setId) { mutableStateOf(request.reps?.toString().orEmpty()) }
    var weightText by remember(request.setId) { mutableStateOf(request.weight?.trimmed().orEmpty()) }
    var minutesText by remember(request.setId) {
        mutableStateOf(request.durationSeconds?.let { (it / 60L).toString() }.orEmpty())
    }
    var secondsText by remember(request.setId) {
        mutableStateOf(request.durationSeconds?.let { (it % 60L).toString() }.orEmpty())
    }
    var distanceText by remember(request.setId) { mutableStateOf(request.distance?.trimmed().orEmpty()) }
    val keyboardDismissal = rememberKeyboardDismissal()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Edit Set") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (request.trackingMode) {
                    TrackingMode.Strength -> {
                        RoutineEditField(
                            value = repsText,
                            onValueChange = { repsText = it },
                            label = "Reps",
                            keyboardType = KeyboardType.Number,
                        )
                        RoutineEditField(
                            value = weightText,
                            onValueChange = { weightText = it },
                            label = "Weight",
                            keyboardType = KeyboardType.Decimal,
                        )
                    }
                    TrackingMode.Bodyweight -> {
                        RoutineEditField(
                            value = repsText,
                            onValueChange = { repsText = it },
                            label = "Reps",
                            keyboardType = KeyboardType.Number,
                        )
                    }
                    TrackingMode.Timed -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RoutineEditField(
                                value = minutesText,
                                onValueChange = { minutesText = it },
                                label = "Min",
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier.weight(1f),
                            )
                            RoutineEditField(
                                value = secondsText,
                                onValueChange = { secondsText = it },
                                label = "Sec",
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        RoutineEditField(
                            value = distanceText,
                            onValueChange = { distanceText = it },
                            label = "Distance",
                            keyboardType = KeyboardType.Decimal,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    keyboardDismissal.dismiss()
                    onSave(request.setId, repsText, weightText, minutesText, secondsText, distanceText)
                },
            ) {
                Text(text = "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        },
    )
}

@Composable
private fun RoutineEditField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
) {
    val keyboardDismissal = rememberKeyboardDismissal()

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { keyboardDismissal.dismiss() }),
        modifier = modifier.fillMaxWidth().testTag("workout-edit-input-$label"),
    )
}

internal const val ADD_SET_ROW_LABEL: String = "Add Set"

internal enum class RoutineHeaderActionPlacement {
    Inline,
    BelowTitle,
}

internal fun routineHeaderActionPlacement(availableWidthDp: Int): RoutineHeaderActionPlacement =
    if (availableWidthDp < 360) {
        RoutineHeaderActionPlacement.BelowTitle
    } else {
        RoutineHeaderActionPlacement.Inline
    }

internal fun exerciseInfoDialogLines(exerciseInfo: ExerciseInfoUiModel): List<String> =
    buildList {
        exerciseInfo.notes?.takeIf { notes -> notes.isNotBlank() }?.let(::add)
        exerciseInfo.headline?.takeIf { headline -> headline.isNotBlank() }?.let(::add)
        addAll(exerciseInfo.history.filter { historyLine -> historyLine.isNotBlank() })
        val hasHistory = exerciseInfo.history.any { historyLine -> historyLine.isNotBlank() }
        if (exerciseInfo.headline.isNullOrBlank() && !hasHistory) {
            add("No completed history yet")
        }
    }

internal fun routineHeaderLabels(trackingMode: TrackingMode): List<String> =
    when (trackingMode) {
        TrackingMode.Strength -> listOf("Reps", "Weight", "Done")
        TrackingMode.Bodyweight -> listOf("Reps", "Done")
        TrackingMode.Timed -> listOf("Time", "Distance", "Done")
    }

internal val WorkoutSetUiModel.completionActionLabel: String
    get() = if (isCompleted) "Checked" else "Done"

internal val WorkoutSetUiModel.completionStateLabel: String
    get() = if (isCompleted) "Checked" else "Unchecked"

private val TrackingMode.label: String
    get() =
        when (this) {
            TrackingMode.Strength -> "Strength"
            TrackingMode.Timed -> "Timed"
            TrackingMode.Bodyweight -> "Bodyweight"
        }

private fun WorkoutSetUiModel.metricCells(trackingMode: TrackingMode): List<String> =
    when (trackingMode) {
        TrackingMode.Strength -> listOf(reps?.toString().orDash(), weight?.trimmed().orDash())
        TrackingMode.Bodyweight -> listOf(reps?.toString().orDash())
        TrackingMode.Timed -> listOf(durationSeconds?.let(::formatDuration).orDash(), distance?.trimmed().orDash())
    }

private fun String?.orDash(): String = this ?: "-"

private fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "${hours}h ${minutes}m ${seconds}s"
    } else {
        "${minutes}m ${seconds}s"
    }
}

private fun Double.trimmed(): String =
    if (rem(1.0) == 0.0) {
        toLong().toString()
    } else {
        toString()
    }
