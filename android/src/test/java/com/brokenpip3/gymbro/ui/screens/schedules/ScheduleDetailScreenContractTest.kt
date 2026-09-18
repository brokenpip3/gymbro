package com.brokenpip3.gymbro.ui.screens.schedules

import androidx.compose.runtime.Composable
import com.brokenpip3.gymbro.data.entities.ExerciseEntity
import com.brokenpip3.gymbro.data.entities.ScheduleEntity
import com.brokenpip3.gymbro.data.entities.ScheduleExerciseEntity
import com.brokenpip3.gymbro.domain.TrackingMode
import org.junit.Assert.assertEquals
import org.junit.Test

@Composable
private fun ScheduleDetailActionsContract() {
    ScheduleDetailScreen(
        uiState =
            ScheduleDetailUiState(
                schedule =
                    ScheduleEntity(
                        id = 7L,
                        name = "Leg Day",
                        notes = null,
                        createdAt = 0L,
                        updatedAt = 0L,
                    ),
                assignedExercises =
                    listOf(
                        ScheduleExerciseEntity(
                            id = 11L,
                            scheduleId = 7L,
                            exerciseId = 13L,
                            sortOrder = 0,
                            targetSets = 3,
                            targetReps = 10,
                            targetWeight = 80.0,
                            targetDurationSeconds = null,
                            targetDistance = null,
                        ),
                    ),
                availableExercises =
                    listOf(
                        ExerciseEntity(
                            id = 13L,
                            name = "Squat",
                            notes = null,
                            trackingMode = TrackingMode.Strength.databaseValue,
                            createdAt = 0L,
                            updatedAt = 0L,
                        ),
                    ),
            ),
        onAddExercise = {},
        onStartWorkout = {},
        onRemoveExercise = {},
        onMoveExercise = { _, _ -> },
        onUpdateTargets = { _, _ -> },
    )
}

class ScheduleDetailScreenContractTest {
    @Test
    fun assignedExerciseActionsAreGroupedForCompactLayouts() {
        assertEquals(
            listOf(
                listOf("Targets", "Stats", "Delete"),
                listOf("Move up", "Move down"),
            ),
            assignedExerciseActionRows(),
        )
    }
}
