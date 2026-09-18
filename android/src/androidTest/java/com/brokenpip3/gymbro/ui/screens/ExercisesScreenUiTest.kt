package com.brokenpip3.gymbro.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.brokenpip3.gymbro.data.entities.ExerciseEntity
import com.brokenpip3.gymbro.domain.TrackingMode
import com.brokenpip3.gymbro.ui.screens.exercises.ExerciseListItem
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExercisesScreenUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun exerciseRowExposesAccessibleEditAndDeleteActions() {
        composeRule.setContent {
            ExercisesScreen(
                exercises = listOf(exerciseRow()),
                onCreateExercise = {},
                onEditExercise = {},
                onDeleteExercise = {},
            )
        }

        composeRule.onNodeWithContentDescription("Edit Bench Press").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Delete Bench Press").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("View stats for Bench Press").assertIsDisplayed()
    }

    @Test
    fun statsActionCallsTheExerciseCallback() {
        var statsExerciseId: Long? = null
        composeRule.setContent {
            ExercisesScreen(
                exercises = listOf(exerciseRow()),
                onCreateExercise = {},
                onEditExercise = {},
                onDeleteExercise = {},
                onOpenExerciseStats = { statsExerciseId = it },
            )
        }

        composeRule.onNodeWithContentDescription("View stats for Bench Press").performClick()

        assertEquals(7L, statsExerciseId)
    }

    @Test
    fun editActionCallsTheExerciseCallback() {
        var editedExerciseId: Long? = null
        composeRule.setContent {
            ExercisesScreen(
                exercises = listOf(exerciseRow()),
                onCreateExercise = {},
                onEditExercise = { editedExerciseId = it },
                onDeleteExercise = {},
            )
        }

        composeRule.onNodeWithContentDescription("Edit Bench Press").performClick()

        assertEquals(7L, editedExerciseId)
    }

    @Test
    fun deleteActionRequiresConfirmationBeforeCallingTheCallback() {
        var deletedExerciseId: Long? = null
        composeRule.setContent {
            ExercisesScreen(
                exercises = listOf(exerciseRow()),
                onCreateExercise = {},
                onEditExercise = {},
                onDeleteExercise = { deletedExerciseId = it },
            )
        }

        composeRule.onNodeWithContentDescription("Delete Bench Press").performClick()
        composeRule.onNodeWithText("Delete exercise?").assertIsDisplayed()
        assertEquals(null, deletedExerciseId)

        composeRule.onNodeWithText("Delete").performClick()

        assertEquals(7L, deletedExerciseId)
    }
}

private fun exerciseRow() =
    ExerciseListItem(
        exercise =
            ExerciseEntity(
                id = 7L,
                name = "Bench Press",
                notes = "Pause",
                trackingMode = TrackingMode.Strength.databaseValue,
                createdAt = 0L,
                updatedAt = 0L,
            ),
    )
