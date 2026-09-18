package com.brokenpip3.gymbro.ui.components

import androidx.compose.runtime.Composable
import org.junit.Assert.assertEquals
import org.junit.Test

class UiComponentsContractTest {
    @Test
    fun gymbroIconsExposeExpectedLocalIcons() {
        assertEquals("Schedules", GymbroIcons.Schedules.name)
        assertEquals("Exercises", GymbroIcons.Exercises.name)
        assertEquals("Workout", GymbroIcons.Workout.name)
        assertEquals("Results", GymbroIcons.Results.name)
        assertEquals("Settings", GymbroIcons.Settings.name)
        assertEquals("ArrowUp", GymbroIcons.ArrowUp.name)
        assertEquals("ArrowDown", GymbroIcons.ArrowDown.name)
    }
}

@Composable
private fun EmptyStateSignatureContract() {
    EmptyState(
        icon = GymbroIcons.Schedules,
        title = "No schedules",
        body = "Create your first training schedule.",
        actionLabel = "Create",
        onAction = {},
    )
}

@Composable
private fun MetricComponentsSignatureContract() {
    MetricTile(label = "Workouts", value = "4")
    SectionHeader(text = "Exercise stats")
}
