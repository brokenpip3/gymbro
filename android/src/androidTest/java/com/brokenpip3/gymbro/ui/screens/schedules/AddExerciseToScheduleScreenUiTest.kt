package com.brokenpip3.gymbro.ui.screens.schedules

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.brokenpip3.gymbro.data.entities.ExerciseEntity
import com.brokenpip3.gymbro.domain.TrackingMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddExerciseToScheduleScreenUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectingExercisesShowsOrderAndCommitsThatOrder() {
        var selectedExerciseIds = emptyList<Long>()
        composeRule.setContent {
            AddExerciseToScheduleScreen(
                uiState =
                    ScheduleDetailUiState(
                        availableExercises =
                            listOf(
                                exercise(id = 1L, name = "Bench Press"),
                                exercise(id = 2L, name = "Back Squat"),
                            ),
                    ),
                onExercisesSelected = { selectedExerciseIds = it },
            )
        }

        composeRule.onNodeWithText("Back Squat").performClick()
        composeRule.onNodeWithText("Bench Press").performClick()

        composeRule.onNodeWithText("Selected 1").assertIsDisplayed()
        composeRule.onNodeWithText("Selected 2").assertIsDisplayed()
        composeRule.onNodeWithText("Add 2 exercises").performClick()

        assertEquals(listOf(2L, 1L), selectedExerciseIds)
    }
}

private fun exercise(
    id: Long,
    name: String,
): ExerciseEntity =
    ExerciseEntity(
        id = id,
        name = name,
        notes = null,
        trackingMode = TrackingMode.Strength.databaseValue,
        createdAt = 0L,
        updatedAt = 0L,
    )
