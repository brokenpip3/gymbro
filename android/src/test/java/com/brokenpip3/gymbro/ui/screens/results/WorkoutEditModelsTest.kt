package com.brokenpip3.gymbro.ui.screens.results

import com.brokenpip3.gymbro.domain.TrackingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class WorkoutEditModelsTest {
    private val zone = ZoneId.of("Europe/Rome")

    @Test
    fun unchangedTimesPreserveMillisecondsAndLocalTimesParseStrictly() {
        val draft = WorkoutEditDraft.from(detail(), zone)
        val edit = draft.toEdit(zone)
        assertEquals(1001L, edit.startedAt)
        assertEquals(61001L, edit.finishedAt)
        assertEquals(80.0, edit.sets.single().weight)
        assertTrue(draft.copy(start = "2026-02-30 12:00").validationError(zone, Long.MAX_VALUE) != null)
        assertTrue(draft.copy(end = "nonsense").validationError(zone, Long.MAX_VALUE) != null)
    }

    @Test
    fun rejectsOverflowFractionalRepsAndNonFiniteNumbers() {
        val draft = WorkoutEditDraft.from(detail(), zone)
        for (bad in listOf("1.5", "999999999999999999999", "0", "-1")) {
            val changed = draft.copy(sets = listOf(draft.sets.single().copy(reps = bad)))
            assertTrue(changed.validationError(zone, Long.MAX_VALUE) != null)
        }
        for (bad in listOf("NaN", "Infinity", "-3", "abc")) {
            val changed = draft.copy(sets = listOf(draft.sets.single().copy(weight = bad)))
            assertTrue(changed.validationError(zone, Long.MAX_VALUE) != null)
        }
    }

    @Test
    fun incompleteSetsAllowMissingMetricsButCompletedSetsRequireThem() {
        val draft = WorkoutEditDraft.from(detail(), zone)
        val blank = draft.sets.single().copy(reps = "", weight = "", isCompleted = false)
        assertEquals(null, draft.copy(sets = listOf(blank)).validationError(zone, Long.MAX_VALUE))
        val completed = draft.copy(sets = listOf(blank.copy(isCompleted = true)))
        assertTrue(completed.validationError(zone, Long.MAX_VALUE) != null)
    }

    @Test
    fun rejectsFutureReversedTimesAndDaylightSavingGaps() {
        val draft = WorkoutEditDraft.from(detail(), zone)
        assertTrue(draft.validationError(zone, 1000) != null)
        assertTrue(draft.copy(end = "1970-01-01 00:00").validationError(zone, Long.MAX_VALUE) != null)
        assertTrue(draft.copy(start = "2026-03-29 02:30").validationError(zone, Long.MAX_VALUE) != null)
    }

    private fun detail() =
        WorkoutDetailUiModel(
            runId = 3,
            scheduleName = "Push",
            startedAt = 1001,
            durationSeconds = 60,
            notes = null,
            exercises =
                listOf(
                    WorkoutExerciseDetailUiModel(
                        5,
                        "Press",
                        TrackingMode.Strength,
                        null,
                        listOf(WorkoutSetDetailUiModel(24, 1, "8 × 80", true, null, reps = 8, weight = 80.0)),
                    ),
                ),
            finishedAt = 61001,
        )
}
