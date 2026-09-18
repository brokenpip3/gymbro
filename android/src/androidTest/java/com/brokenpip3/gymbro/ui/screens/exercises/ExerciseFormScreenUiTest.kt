package com.brokenpip3.gymbro.ui.screens.exercises

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.brokenpip3.gymbro.domain.TrackingMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseFormScreenUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun editFormShowsPrefilledValuesAndSelectedTrackingMode() {
        composeRule.setContent {
            CreateExerciseScreen(
                formState =
                    ExerciseFormState(
                        name = "Tempo Run",
                        notes = "Easy pace",
                        trackingMode = TrackingMode.Timed,
                    ),
                onNameChange = {},
                onNotesChange = {},
                onTrackingModeChange = {},
                onSave = {},
            )
        }

        composeRule.onNodeWithText("Tempo Run").assertIsDisplayed()
        composeRule.onNodeWithText("Easy pace").assertIsDisplayed()
        composeRule.onNodeWithText("Timed").assertIsDisplayed()
    }

    @Test
    fun saveEmitsEditedValues() {
        var name = "Old name"
        var notes = "Old notes"
        var saved = false
        composeRule.setContent {
            var formState by
                remember {
                    mutableStateOf(
                        ExerciseFormState(
                            name = name,
                            notes = notes,
                            trackingMode = TrackingMode.Strength,
                        ),
                    )
                }
            CreateExerciseScreen(
                formState = formState,
                onNameChange = {
                    name = it
                    formState = formState.copy(name = it)
                },
                onNotesChange = {
                    notes = it
                    formState = formState.copy(notes = it)
                },
                onTrackingModeChange = {},
                onSave = { saved = true },
            )
        }

        composeRule.onNodeWithTag("exercise-name-input").performTextClearance()
        composeRule.onNodeWithTag("exercise-name-input").performTextInput("New name")
        composeRule.onNodeWithTag("exercise-notes-input").performTextClearance()
        composeRule.onNodeWithTag("exercise-notes-input").performTextInput("New notes")
        composeRule.onNodeWithTag("exercise-save").performClick()

        assertEquals("New name", name)
        assertEquals("New notes", notes)
        assertEquals(true, saved)
    }
}
