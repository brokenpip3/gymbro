package com.brokenpip3.gymbro.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.brokenpip3.gymbro.ui.components.EmptyState
import com.brokenpip3.gymbro.ui.components.GymbroIcons
import com.brokenpip3.gymbro.ui.components.GymbroListTextRole
import com.brokenpip3.gymbro.ui.components.color
import com.brokenpip3.gymbro.ui.rememberKeyboardDismissal
import com.brokenpip3.gymbro.ui.screens.schedules.ScheduleCreator
import com.brokenpip3.gymbro.ui.screens.schedules.ScheduleListItem
import com.brokenpip3.gymbro.ui.screens.schedules.ScheduleListViewModel

@Composable
fun SchedulesRoute(
    repository: ScheduleCreator,
    onCreateSchedule: () -> Unit,
    onOpenSchedule: (Long) -> Unit,
    onEditSchedule: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ScheduleListViewModel =
        viewModel(
            factory =
                remember(repository) {
                    ScheduleListViewModelFactory(repository)
                },
        )
    val scheduleRows by viewModel.scheduleRows.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val listError by viewModel.listError.collectAsState()

    SchedulesScreen(
        schedules = scheduleRows,
        onCreateSchedule = onCreateSchedule,
        onOpenSchedule = onOpenSchedule,
        onEditSchedule = onEditSchedule,
        onDeleteSchedule = viewModel::deleteSchedule,
        onClearListError = viewModel::clearListError,
        isLoading = isLoading,
        listError = listError,
        modifier = modifier,
    )
}

internal class ScheduleListViewModelFactory(
    private val repository: ScheduleCreator,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScheduleListViewModel::class.java)) {
            return ScheduleListViewModel(repository = repository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

@Composable
@Suppress("LongParameterList")
fun SchedulesScreen(
    schedules: List<ScheduleListItem>,
    onCreateSchedule: () -> Unit,
    onOpenSchedule: (Long) -> Unit,
    onEditSchedule: (Long) -> Unit = {},
    onDeleteSchedule: (Long) -> Unit = {},
    onClearListError: () -> Unit = {},
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
    listError: String? = null,
) {
    var pendingDelete by remember { mutableStateOf<ScheduleListItem?>(null) }

    pendingDelete?.let { row ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = {
                Text(
                    text =
                        if (row.isActiveWorkout) {
                            "Workout in progress"
                        } else {
                            "Delete schedule?"
                        },
                )
            },
            text = {
                Text(
                    text =
                        if (row.isActiveWorkout) {
                            "Finish or discard this workout before deleting its schedule."
                        } else {
                            "Workout history will keep its saved snapshots."
                        },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (row.isActiveWorkout) {
                            pendingDelete = null
                        } else {
                            pendingDelete = null
                            onDeleteSchedule(row.schedule.id)
                        }
                    },
                ) {
                    Text(text = if (row.isActiveWorkout) "Close" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(text = if (row.isActiveWorkout) "Keep workout" else "Cancel")
                }
            },
        )
    }

    if (isLoading) {
        ScheduleLoadingState(modifier = modifier)
    } else if (schedules.isEmpty()) {
        ScheduleEmptyState(
            onCreateSchedule = onCreateSchedule,
            listError = listError,
            onClearListError = onClearListError,
            modifier = modifier,
        )
    } else {
        ScheduleList(
            schedules = schedules,
            onCreateSchedule = onCreateSchedule,
            onOpenSchedule = onOpenSchedule,
            onEditSchedule = onEditSchedule,
            onRequestDelete = { schedule -> pendingDelete = schedule },
            onClearListError = onClearListError,
            listError = listError,
            modifier = modifier,
        )
    }
}

@Composable
private fun ScheduleLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ScheduleEmptyState(
    onCreateSchedule: () -> Unit,
    listError: String?,
    onClearListError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EmptyState(
        icon = GymbroIcons.Schedules,
        title = "No schedules yet",
        body = "Create your first schedule to plan your workouts.",
        actionLabel = "Create Schedule",
        onAction = onCreateSchedule,
        modifier = modifier.fillMaxSize(),
        errorMessage = listError,
        onDismissError = onClearListError,
    )
}

@Composable
private fun ScheduleList(
    schedules: List<ScheduleListItem>,
    onCreateSchedule: () -> Unit,
    onOpenSchedule: (Long) -> Unit,
    onEditSchedule: (Long) -> Unit,
    onRequestDelete: (ScheduleListItem) -> Unit,
    onClearListError: () -> Unit,
    listError: String?,
    modifier: Modifier = Modifier,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val keyboardDismissal = rememberKeyboardDismissal()
    val visibleSchedules = filterScheduleRows(schedules, searchQuery)

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
                    modifier = Modifier.fillMaxWidth().testTag("schedule-search"),
                    label = { Text(text = "Search schedules") },
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
                    onClick = onCreateSchedule,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "Create Schedule")
                }

                if (visibleSchedules.isEmpty()) {
                    Text(
                        text = "No schedules match your search.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
            }
        }

        items(
            items = visibleSchedules,
            key = { row -> row.schedule.id },
        ) { row ->
            ScheduleListRow(
                row = row,
                onOpenSchedule = onOpenSchedule,
                onEditSchedule = onEditSchedule,
                onRequestDelete = onRequestDelete,
            )
        }
    }
}

@Composable
private fun ScheduleListRow(
    row: ScheduleListItem,
    onOpenSchedule: (Long) -> Unit,
    onEditSchedule: (Long) -> Unit,
    onRequestDelete: (ScheduleListItem) -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onOpenSchedule(row.schedule.id) },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
        tonalElevation = 1.dp,
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = row.schedule.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = GymbroListTextRole.Title.color(MaterialTheme.colorScheme),
                )
            },
            supportingContent = { ScheduleRowSupportingText(row) },
            trailingContent = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { onEditSchedule(row.schedule.id) }) {
                        Icon(
                            imageVector = GymbroIcons.Edit,
                            contentDescription = "Edit ${row.schedule.name}",
                        )
                    }
                    IconButton(onClick = { onRequestDelete(row) }) {
                        Icon(
                            imageVector = GymbroIcons.Delete,
                            contentDescription = "Delete ${row.schedule.name}",
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

@Composable
private fun ScheduleRowSupportingText(row: ScheduleListItem) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        row.schedule.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            Text(
                text = notes,
                style = MaterialTheme.typography.bodyMedium,
                color = GymbroListTextRole.Notes.color(MaterialTheme.colorScheme),
            )
        }

        Text(
            text = scheduleRowMetadata(row),
            style = MaterialTheme.typography.bodySmall,
            color = GymbroListTextRole.Metadata.color(MaterialTheme.colorScheme),
        )

        scheduleRowStatusLabel(row)?.let { statusLabel ->
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelMedium,
                color = GymbroListTextRole.Status.color(MaterialTheme.colorScheme),
            )
        }
    }
}

internal fun scheduleRowMetadata(row: ScheduleListItem): String =
    listOfNotNull(
        row.exerciseCountLabel,
        row.lastCompletedLabel,
    ).joinToString(separator = " · ")

internal fun scheduleRowStatusLabel(row: ScheduleListItem): String? = "Active workout".takeIf { row.isActiveWorkout }

internal fun filterScheduleRows(
    rows: List<ScheduleListItem>,
    query: String,
): List<ScheduleListItem> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isBlank()) return rows

    return rows.filter { row ->
        row.schedule.name.contains(normalizedQuery, ignoreCase = true) ||
            row.schedule.notes?.contains(normalizedQuery, ignoreCase = true) == true
    }
}
