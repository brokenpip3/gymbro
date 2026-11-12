package com.brokenpip3.gymbro.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun SettingsDataSection(
    isBusy: Boolean,
    onRequestHistoryReset: () -> Unit,
) {
    Text(text = "Data", style = MaterialTheme.typography.titleMedium)
    Text(
        text = "Exercises, schedules, and settings stay. Use Export everything to back up your history first.",
        style = MaterialTheme.typography.bodyMedium,
    )
    OutlinedButton(
        onClick = onRequestHistoryReset,
        enabled = !isBusy,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = "Reset workout history and statistics")
    }
}

@Composable
internal fun HistoryResetConfirmation(
    isVisible: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!isVisible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset workout history?") },
        text = { Text("This permanently deletes all completed workouts and their statistics.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Reset", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
