package com.brokenpip3.gymbro.ui.screens.schedules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.brokenpip3.gymbro.ui.rememberKeyboardDismissal
import com.brokenpip3.gymbro.ui.screens.ScheduleListViewModelFactory

@Composable
fun CreateScheduleScreenRoute(
    repository: ScheduleCreator,
    onSaved: (Long) -> Unit,
    modifier: Modifier = Modifier,
    scheduleId: Long? = null,
) {
    val viewModel: ScheduleListViewModel =
        viewModel(
            factory =
                remember(repository) {
                    ScheduleListViewModelFactory(repository)
                },
        )
    val formState by viewModel.formState.collectAsState()

    LaunchedEffect(scheduleId) {
        scheduleId?.let { viewModel.loadScheduleForEdit(it) }
    }

    CreateScheduleScreen(
        formState = formState,
        onNameChange = { value ->
            viewModel.clearSaveError()
            viewModel.updateName(value)
        },
        onNotesChange = { value ->
            viewModel.clearSaveError()
            viewModel.updateNotes(value)
        },
        onSave = { viewModel.saveSchedule(onSaved = onSaved) },
        modifier = modifier,
    )
}

@Composable
fun CreateScheduleScreen(
    formState: ScheduleFormState,
    onNameChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
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
            modifier = Modifier.fillMaxWidth(),
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
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = "Notes") },
            minLines = 3,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { keyboardDismissal.dismiss() }),
        )

        formState.saveError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Button(
            onClick = { keyboardDismissal.runBefore(onSave) },
            enabled = !formState.isSaving,
        ) {
            Text(text = "Save")
        }
    }
}
