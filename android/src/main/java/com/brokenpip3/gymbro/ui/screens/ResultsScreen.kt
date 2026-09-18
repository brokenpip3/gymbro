package com.brokenpip3.gymbro.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.brokenpip3.gymbro.ui.components.EmptyState
import com.brokenpip3.gymbro.ui.components.GymbroIcons

@Composable
fun ResultsScreen(modifier: Modifier = Modifier) {
    EmptyState(
        icon = GymbroIcons.Results,
        title = "No results yet",
        body = "Completed workouts will appear here with your insights.",
        actionLabel = null,
        onAction = null,
        modifier = modifier.fillMaxSize(),
    )
}
