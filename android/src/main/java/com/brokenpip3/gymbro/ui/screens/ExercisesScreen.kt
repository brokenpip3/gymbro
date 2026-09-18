package com.brokenpip3.gymbro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.brokenpip3.gymbro.data.entities.ExerciseEntity
import com.brokenpip3.gymbro.domain.TrackingMode
import com.brokenpip3.gymbro.ui.components.EmptyState
import com.brokenpip3.gymbro.ui.components.GymbroIcons
import com.brokenpip3.gymbro.ui.components.GymbroListTextRole
import com.brokenpip3.gymbro.ui.components.color
import com.brokenpip3.gymbro.ui.rememberKeyboardDismissal
import com.brokenpip3.gymbro.ui.screens.exercises.ExerciseCreator
import com.brokenpip3.gymbro.ui.screens.exercises.ExerciseListItem
import com.brokenpip3.gymbro.ui.screens.exercises.ExerciseListViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ExercisesRoute(
    repository: ExerciseCreator,
    onCreateExercise: () -> Unit,
    onEditExercise: (Long) -> Unit,
    onOpenExerciseStats: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val viewModel: ExerciseListViewModel =
        viewModel(
            factory =
                remember(repository) {
                    ExerciseListViewModelFactory(repository)
                },
        )
    val exercises by viewModel.exerciseRows.collectAsState()
    val listError by viewModel.listError.collectAsState()

    ExercisesScreen(
        exercises = exercises,
        onCreateExercise = onCreateExercise,
        onEditExercise = onEditExercise,
        onOpenExerciseStats = onOpenExerciseStats,
        onDeleteExercise = { exerciseId -> viewModel.deleteExercise(exerciseId) },
        onClearListError = viewModel::clearListError,
        listError = listError,
        modifier = modifier,
    )
}

internal class ExerciseListViewModelFactory(
    private val repository: ExerciseCreator,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExerciseListViewModel::class.java)) {
            return ExerciseListViewModel(repository = repository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

@Composable
fun ExercisesScreen(
    exercises: List<ExerciseListItem>,
    onCreateExercise: () -> Unit,
    onEditExercise: (Long) -> Unit,
    onOpenExerciseStats: (Long) -> Unit = {},
    onDeleteExercise: (Long) -> Unit,
    onClearListError: () -> Unit = {},
    modifier: Modifier = Modifier,
    listError: String? = null,
) {
    var pendingDelete by remember { mutableStateOf<ExerciseListItem?>(null) }

    pendingDelete?.let { exercise ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(text = "Delete exercise?") },
            text = { Text(text = "Existing workout history will keep its saved snapshots.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        onDeleteExercise(exercise.exercise.id)
                    },
                ) {
                    Text(text = "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(text = "Cancel")
                }
            },
        )
    }

    if (exercises.isEmpty()) {
        ExerciseEmptyState(
            onCreateExercise = onCreateExercise,
            listError = listError,
            onClearListError = onClearListError,
            modifier = modifier,
        )
    } else {
        ExerciseList(
            exercises = exercises,
            onCreateExercise = onCreateExercise,
            onEditExercise = onEditExercise,
            onOpenExerciseStats = onOpenExerciseStats,
            onRequestDelete = { exercise -> pendingDelete = exercise },
            onClearListError = onClearListError,
            listError = listError,
            modifier = modifier,
        )
    }
}

@Composable
private fun ExerciseEmptyState(
    onCreateExercise: () -> Unit,
    listError: String?,
    onClearListError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EmptyState(
        icon = GymbroIcons.Exercises,
        title = "No exercises yet",
        body = "Build your exercise library before creating schedules.",
        actionLabel = "Create Exercise",
        onAction = onCreateExercise,
        modifier = modifier.fillMaxSize(),
        errorMessage = listError,
        onDismissError = onClearListError,
    )
}

@Composable
private fun ExerciseList(
    exercises: List<ExerciseListItem>,
    onCreateExercise: () -> Unit,
    onEditExercise: (Long) -> Unit,
    onOpenExerciseStats: (Long) -> Unit,
    onRequestDelete: (ExerciseListItem) -> Unit,
    onClearListError: () -> Unit,
    listError: String?,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }
    val keyboardDismissal = rememberKeyboardDismissal()
    val visibleExercises = filterExerciseRows(exercises, searchQuery)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
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

                Button(
                    onClick = onCreateExercise,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "Create Exercise")
                }

                listError?.let { error ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onClearListError) {
                            Text(text = "Dismiss")
                        }
                    }
                }

                if (visibleExercises.isEmpty()) {
                    Text(
                        text = "No exercises match your search.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        items(
            items = visibleExercises,
            key = { row -> row.exercise.id },
        ) { row ->
            ExerciseListItemRow(
                row = row,
                onEditExercise = onEditExercise,
                onOpenExerciseStats = onOpenExerciseStats,
                onRequestDelete = onRequestDelete,
            )
        }
    }
}

@Composable
private fun ExerciseListItemRow(
    row: ExerciseListItem,
    onEditExercise: (Long) -> Unit,
    onOpenExerciseStats: (Long) -> Unit,
    onRequestDelete: (ExerciseListItem) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
        tonalElevation = 1.dp,
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = row.exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = GymbroListTextRole.Title.color(MaterialTheme.colorScheme),
                )
            },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = row.exercise.trackingModeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = GymbroListTextRole.Subtitle.color(MaterialTheme.colorScheme),
                    )
                    ExerciseUsageText(row)
                    row.exercise.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = GymbroListTextRole.Notes.color(MaterialTheme.colorScheme),
                        )
                    }
                }
            },
            trailingContent = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { onOpenExerciseStats(row.exercise.id) }) {
                        Icon(
                            imageVector = GymbroIcons.Results,
                            contentDescription = "View stats for ${row.exercise.name}",
                        )
                    }
                    IconButton(onClick = { onEditExercise(row.exercise.id) }) {
                        Icon(
                            imageVector = GymbroIcons.Edit,
                            contentDescription = "Edit ${row.exercise.name}",
                        )
                    }
                    IconButton(onClick = { onRequestDelete(row) }) {
                        Icon(
                            imageVector = GymbroIcons.Delete,
                            contentDescription = "Delete ${row.exercise.name}",
                        )
                    }
                }
            },
            colors =
                ListItemDefaults.colors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    headlineColor = GymbroListTextRole.Title.color(MaterialTheme.colorScheme),
                    supportingColor = GymbroListTextRole.Metadata.color(MaterialTheme.colorScheme),
                ),
        )
    }
}

internal fun filterExerciseRows(
    rows: List<ExerciseListItem>,
    query: String,
): List<ExerciseListItem> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isBlank()) return rows

    return rows.filter { row ->
        row.exercise.name.contains(normalizedQuery, ignoreCase = true) ||
            row.exercise.notes?.contains(normalizedQuery, ignoreCase = true) == true
    }
}

@Composable
private fun ExerciseUsageText(row: ExerciseListItem) {
    when {
        row.sessionCount > 0 && row.lastCompletedAt != null -> {
            Text(
                text = "${row.sessionCount} sessions · Last ${row.lastCompletedAt.formatExerciseDate()}",
                style = MaterialTheme.typography.bodySmall,
                color = GymbroListTextRole.Metadata.color(MaterialTheme.colorScheme),
            )
        }

        row.sessionCount > 0 -> {
            Text(
                text = "${row.sessionCount} sessions",
                style = MaterialTheme.typography.bodySmall,
                color = GymbroListTextRole.Metadata.color(MaterialTheme.colorScheme),
            )
        }

        else -> {
            Text(
                text = "Not used yet",
                style = MaterialTheme.typography.bodySmall,
                color = GymbroListTextRole.Metadata.color(MaterialTheme.colorScheme),
            )
        }
    }
}

private val ExerciseEntity.trackingModeLabel: String
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

private val EXERCISE_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d")

@Suppress("ktlint:standard:function-expression-body")
private fun Long.formatExerciseDate(): String {
    val instant = Instant.ofEpochMilli(this)
    val zonedDate = instant.atZone(ZoneId.systemDefault())
    return EXERCISE_DATE_FORMATTER.format(zonedDate)
}
