package com.brokenpip3.gymbro.ui.screens.workout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.brokenpip3.gymbro.domain.TrackingMode
import com.brokenpip3.gymbro.ui.theme.GymbroTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActiveWorkoutScreenUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun activeWorkoutShowsCoreActionsAndStrengthFields() {
        composeRule.setContent {
            GymbroTheme {
                ActiveWorkoutScreen(uiState = activeWorkoutState())
            }
        }

        composeRule.onNodeWithText("Leg Day").assertIsDisplayed()
        composeRule.onNodeWithText("Add Exercise").assertIsDisplayed()
        composeRule
            .onNodeWithTag("active-workout-list")
            .performScrollToNode(hasText("Finish Workout"))
            .assertIsDisplayed()
        composeRule.onNodeWithTag("workout-input-Reps").assertIsDisplayed()
        composeRule.onNodeWithTag("workout-input-Weight").assertIsDisplayed()
        composeRule.onNodeWithText("Add Set").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun activeWorkoutShowsSetAndExerciseProgress() {
        composeRule.setContent {
            GymbroTheme {
                ActiveWorkoutScreen(uiState = activeWorkoutState())
            }
        }

        composeRule.onNodeWithText("Progress").assertIsDisplayed()
        composeRule.onNodeWithText("1 of 1 sets · 1 of 1 exercises").assertIsDisplayed()
    }

    @Test
    fun workoutNotesCanBeEditedFromTheHeader() {
        var savedNotes: String? = null
        composeRule.setContent {
            GymbroTheme {
                ActiveWorkoutScreen(
                    uiState = activeWorkoutState(),
                    onUpdateWorkoutNotes = { savedNotes = it },
                )
            }
        }

        composeRule.onNodeWithTag("edit-workout-notes").performClick()
        composeRule.onNodeWithTag("workout-notes-input").performTextInput("Felt strong today")
        composeRule.onNodeWithText("Save").performClick()

        assertEquals("Felt strong today", savedNotes)
    }

    @Test
    fun finishingWorkoutRequiresConfirmation() {
        var finishCount = 0
        composeRule.setContent {
            GymbroTheme {
                ActiveWorkoutScreen(
                    uiState = activeWorkoutState(),
                    onFinishWorkout = { finishCount += 1 },
                )
            }
        }

        composeRule
            .onNodeWithTag("active-workout-list")
            .performScrollToNode(hasText("Finish Workout"))
        composeRule.onNodeWithText("Finish Workout").performClick()

        assertEquals(0, finishCount)
        composeRule.onNodeWithText("Finish workout?").assertIsDisplayed()
        composeRule.onNodeWithText("Keep training").performClick()
        assertEquals(0, finishCount)

        composeRule
            .onNodeWithTag("active-workout-list")
            .performScrollToNode(hasText("Finish Workout"))
        composeRule.onNodeWithText("Finish Workout").performClick()
        composeRule.onNodeWithText("Finish").performClick()

        assertEquals(1, finishCount)
    }

    @Test
    fun discardingWorkoutRequiresConfirmation() {
        var discardCount = 0
        composeRule.setContent {
            GymbroTheme {
                ActiveWorkoutScreen(
                    uiState = activeWorkoutState(),
                    onDiscardWorkout = { discardCount += 1 },
                )
            }
        }

        composeRule
            .onNodeWithTag("active-workout-list")
            .performScrollToNode(hasText("Discard workout"))
        composeRule.onNodeWithText("Discard workout").performClick()

        assertEquals(0, discardCount)
        composeRule.onNodeWithText("Discard workout?").assertIsDisplayed()
        composeRule.onNodeWithText("Keep workout").performClick()
        assertEquals(0, discardCount)

        composeRule
            .onNodeWithTag("active-workout-list")
            .performScrollToNode(hasText("Discard workout"))
        composeRule.onNodeWithText("Discard workout").performClick()
        composeRule.onNodeWithText("Discard").performClick()

        assertEquals(1, discardCount)
    }

    @Test
    fun completingASetInvokesTheCompletionCallback() {
        var completedSetId: Long? = null
        var completed: Boolean? = null
        composeRule.setContent {
            GymbroTheme {
                ActiveWorkoutScreen(
                    uiState = activeWorkoutState(),
                    onUpdateSetCompletion = { setId, isCompleted ->
                        completedSetId = setId
                        completed = isCompleted
                    },
                )
            }
        }

        composeRule.onNodeWithTag("set-101-completion").performClick()

        assertEquals(101L, completedSetId)
        assertEquals(false, completed)
    }

    @Test
    fun tappingASetOpensTheEditDialogWithItsCurrentValues() {
        composeRule.setContent {
            GymbroTheme {
                ActiveWorkoutScreen(uiState = activeWorkoutState())
            }
        }

        composeRule.onNodeWithText("8").performClick()

        composeRule.onNodeWithText("Edit Set").assertIsDisplayed()
        composeRule.onNodeWithTag("workout-edit-input-Reps").assertIsDisplayed()
        composeRule.onNodeWithTag("workout-edit-input-Weight").assertIsDisplayed()
    }

    @Test
    fun invalidSetInputShowsInlineValidation() {
        composeRule.setContent {
            GymbroTheme {
                ActiveWorkoutScreen(uiState = activeWorkoutState())
            }
        }

        composeRule.onNodeWithTag("workout-input-Reps").performTextInput("0")
        composeRule.onNodeWithTag("workout-input-Weight").performTextInput("20")
        composeRule.onNodeWithTag("workout-input-Reps").assertTextContains("0")
        composeRule.onNodeWithTag("workout-input-Weight").assertTextContains("20")
        composeRule.onNodeWithTag("workout-done-10").performScrollTo().performClick()

        composeRule.onNodeWithTag("workout-input-Reps").performScrollTo()
        composeRule.onNodeWithText("Reps must be positive.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun numericFieldsAcceptTheDoneImeAction() {
        composeRule.setContent {
            GymbroTheme {
                ActiveWorkoutScreen(uiState = activeWorkoutState())
            }
        }

        composeRule.onNodeWithTag("workout-input-Reps").performImeAction()
    }

    @Test
    fun workoutActionsRemainVisibleAtAConstrainedPhoneWidth() {
        composeRule.setContent {
            GymbroTheme {
                Box(
                    modifier =
                        Modifier
                            .width(320.dp)
                            .height(800.dp),
                ) {
                    ActiveWorkoutScreen(uiState = activeWorkoutState())
                }
            }
        }

        composeRule.onNodeWithText("Add Exercise").assertIsDisplayed()
        composeRule
            .onNodeWithTag("active-workout-list")
            .performScrollToNode(hasText("Finish Workout"))
            .assertIsDisplayed()
        composeRule.onNodeWithTag("workout-done-10").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun addExerciseDialogFiltersAndSelectsAnExercise() {
        var selectedExerciseId: Long? = null
        composeRule.setContent {
            GymbroTheme {
                ActiveWorkoutScreen(
                    uiState =
                        activeWorkoutState(
                            isAddExerciseDialogVisible = true,
                            availableExercises =
                                listOf(
                                    AvailableWorkoutExerciseUiModel(20L, "Hip Thrust", TrackingMode.Strength),
                                    AvailableWorkoutExerciseUiModel(21L, "Running", TrackingMode.Timed),
                                ),
                        ),
                    onAddExerciseToWorkout = { selectedExerciseId = it },
                )
            }
        }

        composeRule.onNodeWithText("Hip Thrust").performClick()

        assertEquals(20L, selectedExerciseId)
    }

    @Test
    fun exerciseInfoCanOpenFullStatsForSavedExercise() {
        var openedExerciseId: Long? = null
        composeRule.setContent {
            GymbroTheme {
                ActiveWorkoutScreen(
                    uiState =
                        activeWorkoutState(
                            exerciseInfo =
                                ExerciseInfoUiModel(
                                    exerciseId = 13L,
                                    exerciseName = "Back Squat",
                                    notes = null,
                                    headline = "640 volume latest / 720 volume best",
                                    history = listOf("2 sessions / 2 sets"),
                                ),
                        ),
                    onOpenExerciseStats = { openedExerciseId = it },
                )
            }
        }

        composeRule.onNodeWithText("View full stats").performClick()

        assertEquals(13L, openedExerciseId)
    }
}

private fun activeWorkoutState(
    isAddExerciseDialogVisible: Boolean = false,
    availableExercises: List<AvailableWorkoutExerciseUiModel> = emptyList(),
    exerciseInfo: ExerciseInfoUiModel? = null,
): ActiveWorkoutUiState =
    ActiveWorkoutUiState(
        isLoading = false,
        activeWorkout =
            ActiveWorkoutUiModel(
                runId = 1L,
                scheduleName = "Leg Day",
                startedAt = 0L,
                exercises =
                    listOf(
                        WorkoutExerciseUiModel(
                            exerciseResultId = 10L,
                            exerciseName = "Back Squat",
                            trackingMode = TrackingMode.Strength,
                            notes = null,
                            sets =
                                listOf(
                                    WorkoutSetUiModel(
                                        id = 101L,
                                        setOrder = 0,
                                        reps = 8,
                                        weight = 80.0,
                                        durationSeconds = null,
                                        distance = null,
                                        notes = null,
                                        isCompleted = true,
                                    ),
                                ),
                        ),
                    ),
            ),
        isAddExerciseDialogVisible = isAddExerciseDialogVisible,
        availableExercises = availableExercises,
        exerciseInfo = exerciseInfo,
    )
