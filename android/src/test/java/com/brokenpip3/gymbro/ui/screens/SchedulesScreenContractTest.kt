package com.brokenpip3.gymbro.ui.screens

import androidx.compose.runtime.Composable
import com.brokenpip3.gymbro.data.entities.ScheduleEntity
import com.brokenpip3.gymbro.ui.screens.schedules.ScheduleListItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@Composable
private fun SchedulesScreenSignatureContract() {
    SchedulesScreen(
        schedules =
            listOf(
                ScheduleListItem(
                    schedule =
                        ScheduleEntity(
                            id = 7,
                            name = "Leg Day",
                            notes = "Quads",
                            createdAt = 100L,
                            updatedAt = 200L,
                        ),
                    exerciseCount = 4,
                    isActiveWorkout = true,
                    activeWorkoutRunId = 99L,
                    lastCompletedLabel = "Last: today · 45 min",
                ),
            ),
        onCreateSchedule = {},
        onOpenSchedule = {},
        onClearListError = {},
    )
}

@Composable
private fun SchedulesEmptyStateErrorContract() {
    SchedulesScreen(
        schedules = emptyList(),
        onCreateSchedule = {},
        onOpenSchedule = {},
        onClearListError = {},
        listError = "Unable to delete schedule",
    )
}

class SchedulesScreenContractTest {
    @Test
    fun scheduleRowMetadataIncludesExerciseCountAndLastCompletion() {
        val row =
            ScheduleListItem(
                schedule = schedule(id = 1L),
                exerciseCount = 4,
                isActiveWorkout = false,
                activeWorkoutRunId = null,
                lastCompletedLabel = "Last: today · 45 min",
            )

        assertEquals("4 exercises · Last: today · 45 min", scheduleRowMetadata(row))
    }

    @Test
    fun scheduleRowMetadataDoesNotAddAnEmptySeparator() {
        val row =
            ScheduleListItem(
                schedule = schedule(id = 2L),
                exerciseCount = 0,
                isActiveWorkout = false,
                activeWorkoutRunId = null,
                lastCompletedLabel = null,
            )

        assertEquals("No exercises", scheduleRowMetadata(row))
        assertNull(scheduleRowStatusLabel(row))
    }

    @Test
    fun activeScheduleRowExposesAStatusLabel() {
        val row =
            ScheduleListItem(
                schedule = schedule(id = 3L),
                exerciseCount = 2,
                isActiveWorkout = true,
                activeWorkoutRunId = 8L,
                lastCompletedLabel = null,
            )

        assertEquals("Active workout", scheduleRowStatusLabel(row))
    }

    @Test
    fun scheduleSearchMatchesNamesAndNotesIgnoringCaseAndWhitespace() {
        val rows =
            listOf(
                scheduleRow(id = 1L, name = "Leg Day", notes = "Quads and calves"),
                scheduleRow(id = 2L, name = "Upper Body", notes = "Push focus"),
            )

        assertEquals(
            listOf(1L),
            filterScheduleRows(rows, "  CALVES ").map { row -> row.schedule.id },
        )
        assertEquals(
            listOf(2L),
            filterScheduleRows(rows, "upper").map { row -> row.schedule.id },
        )
        assertEquals(rows, filterScheduleRows(rows, "   "))
    }
}

private fun schedule(id: Long) =
    ScheduleEntity(
        id = id,
        name = "Leg Day",
        notes = null,
        createdAt = 0L,
        updatedAt = 0L,
    )

private fun scheduleRow(
    id: Long,
    name: String,
    notes: String?,
) = ScheduleListItem(
    schedule = schedule(id).copy(name = name, notes = notes),
    exerciseCount = 2,
    isActiveWorkout = false,
    activeWorkoutRunId = null,
    lastCompletedLabel = null,
)
