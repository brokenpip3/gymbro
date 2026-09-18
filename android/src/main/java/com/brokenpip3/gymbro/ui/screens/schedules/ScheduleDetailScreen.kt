@file:Suppress("TooManyFunctions")

package com.brokenpip3.gymbro.ui.screens.schedules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.brokenpip3.gymbro.data.entities.ExerciseEntity
import com.brokenpip3.gymbro.data.entities.ScheduleEntity
import com.brokenpip3.gymbro.data.entities.ScheduleExerciseEntity
import com.brokenpip3.gymbro.data.repositories.MoveDirection
import com.brokenpip3.gymbro.domain.TrackingMode
import com.brokenpip3.gymbro.ui.components.EmptyState
import com.brokenpip3.gymbro.ui.components.GymbroIcons
import com.brokenpip3.gymbro.ui.components.GymbroListTextRole
import com.brokenpip3.gymbro.ui.components.color
import com.brokenpip3.gymbro.ui.rememberKeyboardDismissal

@Composable
fun ScheduleDetailRoute(
    scheduleId: Long,
    repository: ScheduleDetailSource,
    workoutStarter: WorkoutStarter,
    onAddExercise: () -> Unit,
    onOpenWorkout: (Long) -> Unit,
    onOpenExerciseStats: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val viewModel: ScheduleDetailViewModel =
        viewModel(
            factory =
                remember(scheduleId, repository, workoutStarter) {
                    ScheduleDetailViewModelFactory(
                        scheduleId = scheduleId,
                        source = repository,
                        workoutStarter = workoutStarter,
                    )
                },
        )
    val uiState by viewModel.uiState.collectAsState()

    ScheduleDetailScreen(
        uiState = uiState,
        onAddExercise = onAddExercise,
        onOpenExerciseStats = onOpenExerciseStats,
        onStartWorkout = {
            viewModel.startWorkout(onOpenWorkout)
        },
        onRemoveExercise = viewModel::removeExerciseFromSchedule,
        onMoveExercise = viewModel::moveScheduleExercise,
        onUpdateTargets = viewModel::updateScheduleExerciseTargets,
        modifier = modifier,
    )
}

internal class ScheduleDetailViewModelFactory(
    private val scheduleId: Long,
    private val source: ScheduleDetailSource,
    private val workoutStarter: WorkoutStarter = NoActiveWorkoutStarter,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScheduleDetailViewModel::class.java)) {
            return ScheduleDetailViewModel(
                scheduleId = scheduleId,
                source = source,
                workoutStarter = workoutStarter,
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

@Composable
fun ScheduleDetailScreen(
    uiState: ScheduleDetailUiState,
    onAddExercise: () -> Unit,
    onStartWorkout: () -> Unit,
    onOpenExerciseStats: (Long) -> Unit = {},
    onRemoveExercise: (Long) -> Unit = {},
    onMoveExercise: (Long, MoveDirection) -> Unit = { _, _ -> },
    onUpdateTargets: (Long, ScheduleExerciseTargets) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    var pendingRemove by remember { mutableStateOf<ScheduleExerciseEntity?>(null) }
    var pendingTargetEdit by remember { mutableStateOf<ScheduleTargetEditRequest?>(null) }
    val schedule = uiState.schedule
    if (schedule == null) {
        Text(
            text = "Schedule not found",
            modifier = modifier.padding(16.dp),
        )
        return
    }

    pendingRemove?.let { scheduleExercise ->
        RemoveScheduleExerciseDialog(
            onDismiss = { pendingRemove = null },
            onConfirm = {
                pendingRemove = null
                onRemoveExercise(scheduleExercise.id)
            },
        )
    }

    pendingTargetEdit?.let { request ->
        ScheduleTargetDialog(
            request = request,
            onDismiss = { pendingTargetEdit = null },
            onSave = { targets ->
                pendingTargetEdit = null
                onUpdateTargets(request.scheduleExercise.id, targets)
            },
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScheduleDetailHeader(
                schedule = schedule,
                uiState = uiState,
                onAddExercise = onAddExercise,
                onStartWorkout = onStartWorkout,
            )
        }

        if (uiState.assignedExercises.isEmpty()) {
            item {
                EmptyState(
                    icon = GymbroIcons.Exercises,
                    title = "No exercises assigned",
                    body = "Add exercises to build this schedule.",
                    actionLabel = "Add Exercise",
                    onAction = onAddExercise,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                )
            }
        } else {
            itemsIndexed(
                items = uiState.assignedExercises,
                key = { _, scheduleExercise -> scheduleExercise.id },
            ) { index, scheduleExercise ->
                val exercise =
                    uiState.availableExercises.firstOrNull { exercise ->
                        exercise.id == scheduleExercise.exerciseId
                    }
                AssignedExerciseRow(
                    position = index,
                    scheduleExercise = scheduleExercise,
                    exercise = exercise,
                    canMoveUp = index > 0,
                    canMoveDown = index < uiState.assignedExercises.lastIndex,
                    actions =
                        AssignedExerciseActions(
                            onMoveUp = {
                                onMoveExercise(scheduleExercise.id, MoveDirection.Up)
                            },
                            onMoveDown = {
                                onMoveExercise(scheduleExercise.id, MoveDirection.Down)
                            },
                            onEditTargets = {
                                pendingTargetEdit =
                                    ScheduleTargetEditRequest(
                                        scheduleExercise = scheduleExercise,
                                        trackingMode = exercise?.trackingMode.toTrackingMode(),
                                    )
                            },
                            onOpenExerciseStats = {
                                onOpenExerciseStats(exercise?.id ?: scheduleExercise.exerciseId)
                            },
                            onRequestRemove = { pendingRemove = scheduleExercise },
                        ),
                )
            }
        }
    }
}

@Composable
private fun RemoveScheduleExerciseDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Remove exercise?") },
        text = { Text(text = "Workout history will keep its saved snapshots.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Remove")
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
private fun ScheduleDetailHeader(
    schedule: ScheduleEntity,
    uiState: ScheduleDetailUiState,
    onAddExercise: () -> Unit,
    onStartWorkout: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = schedule.name,
            style = MaterialTheme.typography.headlineSmall,
        )

        Text(
            text =
                "${uiState.assignedExercises.size} " +
                    if (uiState.assignedExercises.size == 1) "exercise" else "exercises",
            style = MaterialTheme.typography.bodySmall,
            color = GymbroListTextRole.Metadata.color(MaterialTheme.colorScheme),
        )

        schedule.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            Text(
                text = notes,
                style = MaterialTheme.typography.bodyMedium,
                color = GymbroListTextRole.Notes.color(MaterialTheme.colorScheme),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onAddExercise,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = GymbroIcons.Add,
                    contentDescription = null,
                )
                Text(text = "Add Exercise")
            }

            Button(
                onClick = onStartWorkout,
                enabled = uiState.canStartWorkout && !uiState.isStarting,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = GymbroIcons.Workout,
                    contentDescription = null,
                )
                Text(text = uiState.workoutActionLabel)
            }
        }

        ScheduleDetailErrors(uiState)
    }
}

@Composable
private fun ScheduleDetailErrors(uiState: ScheduleDetailUiState) {
    if (!uiState.canStartWorkout || uiState.startError != null) {
        Text(
            text = uiState.startError ?: "Add at least one exercise to start a workout.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }

    uiState.scheduleExerciseError?.let { error ->
        Text(
            text = error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun AssignedExerciseRow(
    position: Int,
    scheduleExercise: ScheduleExerciseEntity,
    exercise: ExerciseEntity?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    actions: AssignedExerciseActions,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AssignedExerciseSummary(
                position = position,
                scheduleExercise = scheduleExercise,
                exercise = exercise,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = actions.onEditTargets,
                ) {
                    Text(text = "Targets")
                }
                IconButton(
                    onClick = actions.onOpenExerciseStats,
                ) {
                    Icon(
                        imageVector = GymbroIcons.Results,
                        contentDescription = "View stats for ${exercise?.name ?: "exercise"}",
                    )
                }
                IconButton(
                    onClick = actions.onRequestRemove,
                ) {
                    Icon(
                        imageVector = GymbroIcons.Delete,
                        contentDescription = "Remove ${exercise?.name ?: "exercise"} from schedule",
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(
                    onClick = actions.onMoveUp,
                    enabled = canMoveUp,
                ) {
                    Icon(
                        imageVector = GymbroIcons.ArrowUp,
                        contentDescription = "Move ${exercise?.name ?: "exercise"} up",
                    )
                }
                IconButton(
                    onClick = actions.onMoveDown,
                    enabled = canMoveDown,
                ) {
                    Icon(
                        imageVector = GymbroIcons.ArrowDown,
                        contentDescription = "Move ${exercise?.name ?: "exercise"} down",
                    )
                }
            }
        }
    }
}

@Composable
private fun AssignedExerciseSummary(
    position: Int,
    scheduleExercise: ScheduleExerciseEntity,
    exercise: ExerciseEntity?,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "${position + 1}",
            style = MaterialTheme.typography.titleMedium,
            color = GymbroListTextRole.Value.color(MaterialTheme.colorScheme),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = exercise?.name ?: "Exercise #${scheduleExercise.exerciseId}",
                style = MaterialTheme.typography.bodyLarge,
            )
            if (exercise != null) {
                Text(
                    text = exercise.trackingModeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = GymbroListTextRole.Subtitle.color(MaterialTheme.colorScheme),
                )
                exercise.notes?.takeIf { notes -> notes.isNotBlank() }?.let { notes ->
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = GymbroListTextRole.Notes.color(MaterialTheme.colorScheme),
                    )
                }
            }
            Text(
                text = scheduleExercise.targetLabel(exercise?.trackingMode.toTrackingMode()),
                style = MaterialTheme.typography.bodySmall,
                color = GymbroListTextRole.Metadata.color(MaterialTheme.colorScheme),
            )
        }
    }
}

private data class AssignedExerciseActions(
    val onMoveUp: () -> Unit,
    val onMoveDown: () -> Unit,
    val onEditTargets: () -> Unit,
    val onOpenExerciseStats: () -> Unit,
    val onRequestRemove: () -> Unit,
)

internal fun assignedExerciseActionRows(): List<List<String>> =
    listOf(
        listOf("Targets", "Stats", "Delete"),
        listOf("Move up", "Move down"),
    )

private data class ScheduleTargetEditRequest(
    val scheduleExercise: ScheduleExerciseEntity,
    val trackingMode: TrackingMode,
)

@Composable
private fun ScheduleTargetDialog(
    request: ScheduleTargetEditRequest,
    onDismiss: () -> Unit,
    onSave: (ScheduleExerciseTargets) -> Unit,
) {
    val scheduleExercise = request.scheduleExercise
    var setsText by remember(scheduleExercise.id) {
        mutableStateOf(scheduleExercise.targetSets?.toString().orEmpty())
    }
    var repsText by remember(scheduleExercise.id) {
        mutableStateOf(scheduleExercise.targetReps?.toString().orEmpty())
    }
    var weightText by remember(scheduleExercise.id) {
        mutableStateOf(scheduleExercise.targetWeight?.trimmed().orEmpty())
    }
    var minutesText by remember(scheduleExercise.id) {
        mutableStateOf(scheduleExercise.targetDurationSeconds?.let { (it / 60L).toString() }.orEmpty())
    }
    var secondsText by remember(scheduleExercise.id) {
        mutableStateOf(scheduleExercise.targetDurationSeconds?.let { (it % 60L).toString() }.orEmpty())
    }
    var distanceText by remember(scheduleExercise.id) {
        mutableStateOf(scheduleExercise.targetDistance?.trimmed().orEmpty())
    }
    val keyboardDismissal = rememberKeyboardDismissal()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Workout targets") },
        text = {
            ScheduleTargetFields(
                trackingMode = request.trackingMode,
                setsText = setsText,
                onSetsTextChange = { setsText = it },
                repsText = repsText,
                onRepsTextChange = { repsText = it },
                weightText = weightText,
                onWeightTextChange = { weightText = it },
                minutesText = minutesText,
                onMinutesTextChange = { minutesText = it },
                secondsText = secondsText,
                onSecondsTextChange = { secondsText = it },
                distanceText = distanceText,
                onDistanceTextChange = { distanceText = it },
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    keyboardDismissal.runBefore {
                        onSave(
                            request.toTargets(
                                setsText = setsText,
                                repsText = repsText,
                                weightText = weightText,
                                minutesText = minutesText,
                                secondsText = secondsText,
                                distanceText = distanceText,
                            ),
                        )
                    }
                },
            ) {
                Text(text = "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = { keyboardDismissal.runBefore(onDismiss) }) {
                Text(text = "Cancel")
            }
        },
    )
}

@Composable
@Suppress("LongParameterList")
private fun ScheduleTargetFields(
    trackingMode: TrackingMode,
    setsText: String,
    onSetsTextChange: (String) -> Unit,
    repsText: String,
    onRepsTextChange: (String) -> Unit,
    weightText: String,
    onWeightTextChange: (String) -> Unit,
    minutesText: String,
    onMinutesTextChange: (String) -> Unit,
    secondsText: String,
    onSecondsTextChange: (String) -> Unit,
    distanceText: String,
    onDistanceTextChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TargetEditField(setsText, onSetsTextChange, "Sets", KeyboardType.Number)
        if (trackingMode != TrackingMode.Timed) {
            TargetEditField(repsText, onRepsTextChange, "Reps", KeyboardType.Number)
        }
        if (trackingMode == TrackingMode.Strength) {
            TargetEditField(weightText, onWeightTextChange, "Weight", KeyboardType.Decimal)
        }
        if (trackingMode == TrackingMode.Timed) {
            TimedTargetFields(
                minutesText = minutesText,
                onMinutesTextChange = onMinutesTextChange,
                secondsText = secondsText,
                onSecondsTextChange = onSecondsTextChange,
                distanceText = distanceText,
                onDistanceTextChange = onDistanceTextChange,
            )
        }
    }
}

@Composable
private fun TimedTargetFields(
    minutesText: String,
    onMinutesTextChange: (String) -> Unit,
    secondsText: String,
    onSecondsTextChange: (String) -> Unit,
    distanceText: String,
    onDistanceTextChange: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TargetEditField(
            value = minutesText,
            onValueChange = onMinutesTextChange,
            label = "Min",
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(1f),
        )
        TargetEditField(
            value = secondsText,
            onValueChange = onSecondsTextChange,
            label = "Sec",
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(1f),
        )
    }
    TargetEditField(distanceText, onDistanceTextChange, "Distance", KeyboardType.Decimal)
}

@Composable
private fun TargetEditField(
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
        modifier = modifier.fillMaxWidth(),
    )
}

private fun ScheduleTargetEditRequest.toTargets(
    setsText: String,
    repsText: String,
    weightText: String,
    minutesText: String,
    secondsText: String,
    distanceText: String,
): ScheduleExerciseTargets =
    ScheduleExerciseTargets(
        targetSets = setsText.toPositiveIntOrNull(),
        targetReps =
            when (trackingMode) {
                TrackingMode.Strength,
                TrackingMode.Bodyweight,
                -> repsText.toPositiveIntOrNull()
                TrackingMode.Timed -> null
            },
        targetWeight = weightText.toPositiveDoubleOrNull().takeIf { trackingMode == TrackingMode.Strength },
        targetDurationSeconds =
            if (trackingMode == TrackingMode.Timed) {
                minutesText.toPositiveLongOrZero() * 60L + secondsText.toPositiveLongOrZero()
            } else {
                null
            }?.takeIf { seconds -> seconds > 0L },
        targetDistance = distanceText.toPositiveDoubleOrNull().takeIf { trackingMode == TrackingMode.Timed },
    )

private fun ScheduleExerciseEntity.targetLabel(trackingMode: TrackingMode): String {
    val sets = targetSets?.let { "$it sets" }
    val metric =
        when (trackingMode) {
            TrackingMode.Strength ->
                listOfNotNull(
                    targetReps?.let { "$it reps" },
                    targetWeight?.let { "${it.trimmed()} weight" },
                ).joinToString(" / ")
            TrackingMode.Bodyweight -> targetReps?.let { "$it reps" }.orEmpty()
            TrackingMode.Timed ->
                listOfNotNull(
                    targetDurationSeconds?.let(::formatDurationTarget),
                    targetDistance?.let { "${it.trimmed()} distance" },
                ).joinToString(" / ")
        }
    val label = listOfNotNull(sets, metric.takeIf { it.isNotBlank() }).joinToString(" · ")
    return label.ifBlank { "No targets" }
}

private fun String?.toTrackingMode(): TrackingMode =
    TrackingMode.entries.firstOrNull { trackingMode -> trackingMode.databaseValue == this } ?: TrackingMode.Bodyweight

private fun String.toPositiveIntOrNull(): Int? = trim().toIntOrNull()?.takeIf { it > 0 }

private fun String.toPositiveLongOrZero(): Long = trim().toLongOrNull()?.takeIf { it > 0L } ?: 0L

private fun String.toPositiveDoubleOrNull(): Double? = trim().toDoubleOrNull()?.takeIf { it > 0.0 }

private fun Double.trimmed(): String =
    if (rem(1.0) == 0.0) {
        toLong().toString()
    } else {
        toString()
    }

private fun formatDurationTarget(totalSeconds: Long): String {
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return if (minutes > 0L) {
        "${minutes}m ${seconds}s"
    } else {
        "${seconds}s"
    }
}

internal val ExerciseEntity.trackingModeLabel: String
    get() =
        when (trackingMode) {
            TrackingMode.Strength.databaseValue -> "Strength"
            TrackingMode.Timed.databaseValue -> "Timed"
            TrackingMode.Bodyweight.databaseValue -> "Bodyweight"
            else ->
                trackingMode.replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase() else char.toString()
                }
        }
