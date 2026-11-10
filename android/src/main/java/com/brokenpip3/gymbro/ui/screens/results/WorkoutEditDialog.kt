package com.brokenpip3.gymbro.ui.screens.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.brokenpip3.gymbro.domain.TrackingMode

@Composable
internal fun WorkoutEditDialog(
    state: WorkoutEditorState,
    onDraftChanged: (WorkoutEditDraft) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val draft = state.draft
    AlertDialog(
        onDismissRequest = { if (!state.isSaving) onDismiss() },
        title = { Text("Edit workout") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(draft.detail.scheduleName, style = MaterialTheme.typography.titleMedium)
                Text("Local time (${draft.zone.id}) · YYYY-MM-DD HH:MM", style = MaterialTheme.typography.bodySmall)
                EditField("Start", draft.start, "workout-edit-start", !state.isSaving) {
                    onDraftChanged(draft.copy(start = it))
                }
                EditField("End", draft.end, "workout-edit-end", !state.isSaving) {
                    onDraftChanged(draft.copy(end = it))
                }
                val duration = runCatching { draft.toEdit().let { (it.finishedAt - it.startedAt) / 1000L } }.getOrNull()
                duration?.takeIf { it >= 0 }?.let {
                    Text("Duration: ${it / 3600}h ${(it % 3600) / 60}m ${it % 60}s")
                }
                draft.sets.forEach { set ->
                    HorizontalDivider()
                    WorkoutSetEditFields(set, !state.isSaving) { changed ->
                        val updatedSets = draft.sets.map { if (it.setId == changed.setId) changed else it }
                        onDraftChanged(draft.copy(sets = updatedSets))
                    }
                }
                val error = state.error ?: draft.validationError()
                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("workout-edit-error"),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = !state.isSaving && draft.validationError() == null) {
                Text(if (state.isSaving) "Saving…" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isSaving) { Text("Cancel") }
        },
    )
}

@Composable
private fun WorkoutSetEditFields(
    set: WorkoutSetEditDraft,
    enabled: Boolean,
    onChange: (WorkoutSetEditDraft) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("${set.exerciseName} · Set ${set.setNumber}", style = MaterialTheme.typography.titleSmall)
        if (set.mode != TrackingMode.Timed) {
            EditField("Reps", set.reps, "workout-edit-reps-${set.setId}", enabled, KeyboardType.Number) {
                onChange(set.copy(reps = it))
            }
        }
        if (set.mode == TrackingMode.Strength) {
            EditField("Weight", set.weight, "workout-edit-weight-${set.setId}", enabled, KeyboardType.Decimal) {
                onChange(set.copy(weight = it))
            }
        }
        if (set.mode == TrackingMode.Timed) {
            EditField(
                "Duration (seconds)",
                set.duration,
                "workout-edit-duration-${set.setId}",
                enabled,
                KeyboardType.Number,
            ) {
                onChange(set.copy(duration = it))
            }
            EditField(
                "Distance (optional)",
                set.distance,
                "workout-edit-distance-${set.setId}",
                enabled,
                KeyboardType.Decimal,
            ) {
                onChange(set.copy(distance = it))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = set.isCompleted,
                onCheckedChange = { onChange(set.copy(isCompleted = it)) },
                enabled = enabled,
                modifier =
                    Modifier.semantics {
                        contentDescription = "${set.exerciseName}, set ${set.setNumber}, completed"
                    },
            )
            Text("Completed")
        }
        set.validationError()?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun EditField(
    label: String,
    value: String,
    tag: String,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth().testTag(tag),
    )
}
