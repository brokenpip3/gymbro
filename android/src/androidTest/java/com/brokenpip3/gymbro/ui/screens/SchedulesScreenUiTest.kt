package com.brokenpip3.gymbro.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.brokenpip3.gymbro.data.entities.ScheduleEntity
import com.brokenpip3.gymbro.ui.screens.schedules.ScheduleListItem
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SchedulesScreenUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun activeScheduleCannotBeDeletedWhileWorkoutIsInProgress() {
        var deletedScheduleId: Long? = null

        composeRule.setContent {
            SchedulesScreen(
                schedules =
                    listOf(
                        ScheduleListItem(
                            schedule =
                                ScheduleEntity(
                                    id = 7L,
                                    name = "Leg Day",
                                    notes = null,
                                    createdAt = 0L,
                                    updatedAt = 0L,
                                ),
                            exerciseCount = 3,
                            isActiveWorkout = true,
                            activeWorkoutRunId = 11L,
                            lastCompletedLabel = null,
                        ),
                    ),
                onCreateSchedule = {},
                onOpenSchedule = {},
                onDeleteSchedule = { deletedScheduleId = it },
            )
        }

        composeRule
            .onNodeWithContentDescription("Delete Leg Day")
            .performClick()
        composeRule.onNodeWithText("Workout in progress").assertIsDisplayed()
        composeRule
            .onNodeWithText("Finish or discard this workout before deleting its schedule.")
            .assertIsDisplayed()
        assertNull(deletedScheduleId)
    }

    @Test
    fun scheduleSearchFiltersByName() {
        composeRule.setContent {
            SchedulesScreen(
                schedules =
                    listOf(
                        ScheduleListItem(
                            schedule =
                                ScheduleEntity(
                                    id = 7L,
                                    name = "Leg Day",
                                    notes = "Quads and calves",
                                    createdAt = 0L,
                                    updatedAt = 0L,
                                ),
                            exerciseCount = 3,
                            isActiveWorkout = false,
                            activeWorkoutRunId = null,
                            lastCompletedLabel = null,
                        ),
                        ScheduleListItem(
                            schedule =
                                ScheduleEntity(
                                    id = 8L,
                                    name = "Upper Body",
                                    notes = "Push focus",
                                    createdAt = 0L,
                                    updatedAt = 0L,
                                ),
                            exerciseCount = 2,
                            isActiveWorkout = false,
                            activeWorkoutRunId = null,
                            lastCompletedLabel = null,
                        ),
                    ),
                onCreateSchedule = {},
                onOpenSchedule = {},
                onDeleteSchedule = {},
            )
        }

        composeRule.onNodeWithTag("schedule-search").performTextInput("upper")
        composeRule.onNodeWithText("Upper Body").assertIsDisplayed()
        composeRule.onNodeWithText("Leg Day").assertDoesNotExist()
    }
}
