package com.brokenpip3.gymbro.ui.screens.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.brokenpip3.gymbro.data.repositories.WorkoutScheduleExercise

@Composable
internal fun FinishWorkoutConfirmationDialog(
    state: WorkoutFinishState,
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit,
) {
    val preview = state.scheduleUpdate
    var updateSchedule by remember(preview) { mutableStateOf(false) }
    val busy = state.isPreparing || state.isSaving
    AlertDialog(
        onDismissRequest = { if (!state.isSaving) onDismiss() },
        title = { Text(text = "Finish workout?") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("This workout will move to Results.")
                if (state.isPreparing) Text("Checking schedule changes...")
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (preview != null) {
                    Text("The exercise list has changed.", style = MaterialTheme.typography.titleSmall)
                    Text("Save these exercises and defaults to ${preview.schedule?.name ?: "the original schedule"}:")
                    if (preview.exercises.isEmpty()) Text("No exercises. The schedule will be empty.")
                    preview.exercises.forEachIndexed { index, exercise ->
                        Column {
                            Text("${index + 1}. ${exercise.name}", style = MaterialTheme.typography.bodyLarge)
                            Text(exercise.defaultSummary(), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .testTag("update-schedule-checkbox")
                                .toggleable(
                                    value = updateSchedule,
                                    enabled = preview.unavailableReason == null && !busy,
                                    role = Role.Checkbox,
                                    onValueChange = { updateSchedule = it },
                                ).padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = updateSchedule,
                            onCheckedChange = null,
                            enabled = preview.unavailableReason == null && !busy,
                        )
                        Text("Update schedule and defaults")
                    }
                    preview.unavailableReason?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(updateSchedule && preview?.unavailableReason == null) }, enabled = !busy) {
                Text(if (state.isSaving) "Finishing..." else "Finish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isSaving) {
                Text("Keep training")
            }
        },
    )
}

private fun WorkoutScheduleExercise.defaultSummary(): String =
    with(defaults) {
        if (targetSets == null) {
            "No targets"
        } else {
            listOfNotNull(
                "$targetSets sets",
                targetReps?.let { "$it reps" },
                targetWeight?.let { "$it weight" },
                targetDurationSeconds?.let { "${it}s" },
                targetDistance?.let { "$it distance" },
            ).joinToString(" · ")
        }
    }
