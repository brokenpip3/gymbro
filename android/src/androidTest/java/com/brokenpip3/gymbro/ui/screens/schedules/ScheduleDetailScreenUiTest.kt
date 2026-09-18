package com.brokenpip3.gymbro.ui.screens.schedules

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.brokenpip3.gymbro.data.entities.ExerciseEntity
import com.brokenpip3.gymbro.data.entities.ScheduleEntity
import com.brokenpip3.gymbro.data.entities.ScheduleExerciseEntity
import com.brokenpip3.gymbro.domain.TrackingMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduleDetailScreenUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun scheduleExerciseExposesCompactAccessibleActions() {
        composeRule.setContent {
            ScheduleDetailScreen(
                uiState = scheduleDetailState(),
                onAddExercise = {},
                onStartWorkout = {},
            )
        }

        composeRule.onNodeWithText("Targets").assertIsDisplayed()
        composeRule.onNodeWithText("Pause at bottom").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("View stats for Squat")
            .assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Remove Squat from schedule")
            .assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Move Squat up")
            .assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Move Squat down")
            .assertIsDisplayed()
    }

    @Test
    fun scheduleDetailShowsExerciseCountAndPrimaryActions() {
        composeRule.setContent {
            ScheduleDetailScreen(
                uiState = scheduleDetailState(),
                onAddExercise = {},
                onStartWorkout = {},
            )
        }

        composeRule.onNodeWithText("1 exercise").assertIsDisplayed()
        composeRule.onNodeWithText("Add Exercise").assertIsDisplayed()
        composeRule.onNodeWithText("Start Workout").assertIsDisplayed()
        composeRule.onNodeWithText("1").assertIsDisplayed()
    }

    @Test
    fun removingScheduleExerciseRequiresConfirmation() {
        var removedScheduleExerciseId: Long? = null
        composeRule.setContent {
            ScheduleDetailScreen(
                uiState = scheduleDetailState(),
                onAddExercise = {},
                onStartWorkout = {},
                onRemoveExercise = { removedScheduleExerciseId = it },
            )
        }

        composeRule
            .onNodeWithContentDescription("Remove Squat from schedule")
            .performClick()
        composeRule.onNodeWithText("Remove exercise?").assertIsDisplayed()
        assertEquals(null, removedScheduleExerciseId)

        composeRule.onNodeWithText("Remove").performClick()

        assertEquals(11L, removedScheduleExerciseId)
    }

    @Test
    fun statsActionCallsTheExerciseCallback() {
        var statsExerciseId: Long? = null
        composeRule.setContent {
            ScheduleDetailScreen(
                uiState = scheduleDetailState(),
                onAddExercise = {},
                onStartWorkout = {},
                onOpenExerciseStats = { statsExerciseId = it },
            )
        }

        composeRule.onNodeWithContentDescription("View stats for Squat").performClick()

        assertEquals(13L, statsExerciseId)
    }
}

private fun scheduleDetailState() =
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
                    notes = "Pause at bottom",
                    trackingMode = TrackingMode.Strength.databaseValue,
                    createdAt = 0L,
                    updatedAt = 0L,
                ),
            ),
    )
