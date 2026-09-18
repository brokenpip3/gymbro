package com.brokenpip3.gymbro.ui.screens.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.brokenpip3.gymbro.domain.TrackingMode
import com.brokenpip3.gymbro.ui.rememberKeyboardDismissal
import com.brokenpip3.gymbro.ui.screens.ExerciseListViewModelFactory

@Composable
fun CreateExerciseScreenRoute(
    repository: ExerciseCreator,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    exerciseId: Long? = null,
) {
    val viewModel: ExerciseListViewModel =
        viewModel(
            factory =
                remember(repository, exerciseId) {
                    ExerciseListViewModelFactory(repository)
                },
        )
    val formState by viewModel.formState.collectAsState()

    LaunchedEffect(exerciseId) {
        if (exerciseId != null) {
            viewModel.loadExerciseForEdit(exerciseId)
        }
    }

    CreateExerciseScreen(
        formState = formState,
        onNameChange = { value ->
            viewModel.clearSaveError()
            viewModel.updateName(value)
        },
        onNotesChange = { value ->
            viewModel.clearSaveError()
            viewModel.updateNotes(value)
        },
        onTrackingModeChange = { value ->
            viewModel.clearSaveError()
            viewModel.updateTrackingMode(value)
        },
        onSave = {
            if (exerciseId == null) {
                viewModel.saveExercise(onSaved = onSaved)
            } else {
                viewModel.updateExercise(exerciseId = exerciseId, onSaved = onSaved)
            }
        },
        modifier = modifier,
    )
}

@Composable
fun CreateExerciseScreen(
    formState: ExerciseFormState,
    onNameChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onTrackingModeChange: (TrackingMode) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardDismissal = rememberKeyboardDismissal()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = formState.name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth().testTag("exercise-name-input"),
            enabled = !formState.isLoading,
            label = { Text(text = "Name") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { keyboardDismissal.dismiss() }),
            isError = formState.nameError != null,
            supportingText =
                formState.nameError?.let { error ->
                    { Text(text = error) }
                },
        )

        OutlinedTextField(
            value = formState.notes,
            onValueChange = onNotesChange,
            modifier = Modifier.fillMaxWidth().testTag("exercise-notes-input"),
            enabled = !formState.isLoading,
            label = { Text(text = "Notes") },
            minLines = 3,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { keyboardDismissal.dismiss() }),
        )

        TrackingModeChips(
            selectedTrackingMode = formState.trackingMode,
            onTrackingModeChange = onTrackingModeChange,
            enabled = !formState.isLoading,
        )

        if (formState.isLoading) {
            CircularProgressIndicator()
        }

        formState.saveError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Button(
            onClick = { keyboardDismissal.runBefore(onSave) },
            enabled = !formState.isLoading && !formState.isSaving,
            modifier = Modifier.testTag("exercise-save"),
        ) {
            Text(text = "Save")
        }
    }
}

@Composable
private fun TrackingModeChips(
    selectedTrackingMode: TrackingMode,
    onTrackingModeChange: (TrackingMode) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TrackingMode.entries.forEach { trackingMode ->
            FilterChip(
                selected = selectedTrackingMode == trackingMode,
                onClick = { onTrackingModeChange(trackingMode) },
                enabled = enabled,
                label = { Text(text = trackingMode.label) },
            )
        }
    }
}

private val TrackingMode.label: String
    get() =
        when (this) {
            TrackingMode.Strength -> "Strength"
            TrackingMode.Timed -> "Timed"
            TrackingMode.Bodyweight -> "Bodyweight"
        }
