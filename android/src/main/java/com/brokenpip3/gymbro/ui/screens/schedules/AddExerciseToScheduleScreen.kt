package com.brokenpip3.gymbro.ui.screens.schedules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.brokenpip3.gymbro.data.entities.ExerciseEntity
import com.brokenpip3.gymbro.ui.components.EmptyState
import com.brokenpip3.gymbro.ui.components.GymbroIcons
import com.brokenpip3.gymbro.ui.rememberKeyboardDismissal

@Composable
fun AddExerciseToScheduleRoute(
    scheduleId: Long,
    repository: ScheduleDetailSource,
    onAssigned: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ScheduleDetailViewModel =
        viewModel(
            factory =
                remember(scheduleId, repository) {
                    ScheduleDetailViewModelFactory(
                        scheduleId = scheduleId,
                        source = repository,
                    )
                },
        )
    val uiState by viewModel.uiState.collectAsState()

    AddExerciseToScheduleScreen(
        uiState = uiState,
        onExercisesSelected = { exerciseIds ->
            viewModel.clearAssignmentError()
            viewModel.assignExercises(
                exerciseIds = exerciseIds,
                onAssigned = onAssigned,
            )
        },
        modifier = modifier,
    )
}

@Composable
fun AddExerciseToScheduleScreen(
    uiState: ScheduleDetailUiState,
    onExercisesSelected: (List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.availableExercises.isEmpty()) {
        EmptyState(
            icon = GymbroIcons.Exercises,
            title = "No exercises available",
            body = "Create exercises before adding them to a schedule.",
            actionLabel = null,
            onAction = null,
            modifier = modifier.fillMaxSize(),
        )
        return
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedExerciseIds by remember { mutableStateOf(emptyList<Long>()) }
    val keyboardDismissal = rememberKeyboardDismissal()
    val visibleExercises = filterAvailableExercises(uiState.availableExercises, searchQuery)

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = "Search exercises") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboardDismissal.dismiss() }),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                TextButton(onClick = { searchQuery = "" }) {
                                    Text(text = "Clear")
                                }
                            }
                        },
                    )
                    uiState.assignmentError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    if (visibleExercises.isEmpty()) {
                        Text(
                            text = "No exercises match your search.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            items(
                items = visibleExercises,
                key = { exercise -> exercise.id },
            ) { exercise ->
                val selectionOrder = selectedExerciseIds.indexOf(exercise.id).takeIf { it >= 0 }?.plus(1)
                AddExerciseRow(
                    exercise = exercise,
                    enabled = !uiState.isAssigning,
                    selected = selectionOrder != null,
                    selectionOrder = selectionOrder,
                    onExerciseSelected = {
                        selectedExerciseIds = toggleExerciseSelection(selectedExerciseIds, exercise.id)
                    },
                )
            }
        }

        Button(
            onClick = { onExercisesSelected(selectedExerciseIds) },
            enabled = selectedExerciseIds.isNotEmpty() && !uiState.isAssigning,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text =
                    "Add ${selectedExerciseIds.size} " +
                        if (selectedExerciseIds.size == 1) "exercise" else "exercises",
            )
        }
    }
}

internal fun toggleExerciseSelection(
    selectedExerciseIds: List<Long>,
    exerciseId: Long,
): List<Long> =
    if (exerciseId in selectedExerciseIds) {
        selectedExerciseIds.filterNot { selectedId -> selectedId == exerciseId }
    } else {
        selectedExerciseIds + exerciseId
    }

internal fun filterAvailableExercises(
    exercises: List<ExerciseEntity>,
    query: String,
): List<ExerciseEntity> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isBlank()) return exercises

    return exercises.filter { exercise ->
        exercise.name.contains(normalizedQuery, ignoreCase = true) ||
            exercise.notes?.contains(normalizedQuery, ignoreCase = true) == true
    }
}

@Composable
private fun AddExerciseRow(
    exercise: ExerciseEntity,
    enabled: Boolean,
    selected: Boolean,
    selectionOrder: Int?,
    onExerciseSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(text = exercise.name) },
        supportingContent = {
            Text(
                text =
                    buildString {
                        append(exercise.trackingModeLabel)
                        selectionOrder?.let { order -> append(" · Selected $order") }
                    },
            )
        },
        leadingContent = {
            Checkbox(
                checked = selected,
                onCheckedChange = { onExerciseSelected() },
                enabled = enabled,
            )
        },
        modifier =
            modifier.clickable(enabled = enabled) {
                onExerciseSelected()
            },
    )
}
