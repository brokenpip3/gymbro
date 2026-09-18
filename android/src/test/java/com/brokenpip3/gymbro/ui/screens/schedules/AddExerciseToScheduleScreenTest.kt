package com.brokenpip3.gymbro.ui.screens.schedules

import com.brokenpip3.gymbro.data.entities.ExerciseEntity
import com.brokenpip3.gymbro.domain.TrackingMode
import org.junit.Assert.assertEquals
import org.junit.Test

class AddExerciseToScheduleScreenTest {
    @Test
    fun availableExerciseSearchMatchesNameAndNotesIgnoringCase() {
        val exercises =
            listOf(
                exercise(id = 1L, name = "Back Squat", notes = "Low bar"),
                exercise(id = 2L, name = "Tempo Run", notes = "Easy pace"),
            )

        assertEquals(listOf(1L), filterAvailableExercises(exercises, "low").map { it.id })
        assertEquals(listOf(2L), filterAvailableExercises(exercises, "RUN").map { it.id })
    }

    @Test
    fun blankAvailableExerciseSearchKeepsExistingOrder() {
        val exercises = listOf(exercise(id = 2L, name = "Tempo Run"), exercise(id = 1L, name = "Back Squat"))

        assertEquals(listOf(2L, 1L), filterAvailableExercises(exercises, " ").map { it.id })
    }

    @Test
    fun exerciseSelectionAppendsAndRemovesWithoutChangingSelectionOrder() {
        val selected = toggleExerciseSelection(emptyList(), 2L)
        val reordered = toggleExerciseSelection(toggleExerciseSelection(selected, 1L), 2L)

        assertEquals(listOf(2L, 1L), selected + 1L)
        assertEquals(listOf(1L), reordered)
    }
}

private fun exercise(
    id: Long,
    name: String,
    notes: String? = null,
): ExerciseEntity =
    ExerciseEntity(
        id = id,
        name = name,
        notes = notes,
        trackingMode = TrackingMode.Strength.databaseValue,
        createdAt = 0L,
        updatedAt = 0L,
    )
