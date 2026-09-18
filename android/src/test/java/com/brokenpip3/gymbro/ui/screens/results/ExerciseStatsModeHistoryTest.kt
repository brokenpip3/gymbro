package com.brokenpip3.gymbro.ui.screens.results

import com.brokenpip3.gymbro.data.entities.ExerciseResultEntity
import com.brokenpip3.gymbro.data.entities.SetResultEntity
import com.brokenpip3.gymbro.data.entities.WorkoutRunEntity
import com.brokenpip3.gymbro.domain.TrackingMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseStatsModeHistoryTest {
    @Test
    fun changingTrackingModeKeepsHistoricalSeriesSeparate() {
        val runs =
            listOf(
                resultRun(
                    runId = 1L,
                    finishedAt = 1_000L,
                    exerciseResult =
                        exerciseResult(
                            id = 11L,
                            runId = 1L,
                            trackingMode = TrackingMode.Strength,
                        ),
                    setResult =
                        SetResultEntity(
                            id = 111L,
                            exerciseResultId = 11L,
                            setOrder = 0,
                            reps = 5,
                            weight = 100.0,
                            durationSeconds = null,
                            distance = null,
                            notes = null,
                        ),
                ),
                resultRun(
                    runId = 2L,
                    finishedAt = 2_000L,
                    exerciseResult =
                        exerciseResult(
                            id = 22L,
                            runId = 2L,
                            trackingMode = TrackingMode.Timed,
                        ),
                    setResult =
                        SetResultEntity(
                            id = 222L,
                            exerciseResultId = 22L,
                            setOrder = 0,
                            reps = null,
                            weight = null,
                            durationSeconds = 60L,
                            distance = null,
                            notes = null,
                        ),
                ),
            )

        val stats = runs.toExerciseStats()

        assertEquals(2, stats.size)
        assertEquals(setOf(TrackingMode.Strength, TrackingMode.Timed), stats.map { it.trackingMode }.toSet())
        assertEquals("500 volume", stats.single { it.trackingMode == TrackingMode.Strength }.headline)
        assertEquals("60 sec", stats.single { it.trackingMode == TrackingMode.Timed }.headline)
    }
}

private fun resultRun(
    runId: Long,
    finishedAt: Long,
    exerciseResult: ExerciseResultEntity,
    setResult: SetResultEntity,
): ResultRunDetail =
    ResultRunDetail(
        workoutRun =
            WorkoutRunEntity(
                id = runId,
                scheduleId = 1L,
                scheduleNameSnapshot = "Routine",
                startedAt = finishedAt - 500L,
                finishedAt = finishedAt,
                notes = null,
            ),
        exerciseDetails =
            listOf(
                ResultExerciseDetail(
                    exerciseResult = exerciseResult,
                    setResults = listOf(setResult),
                ),
            ),
    )

private fun exerciseResult(
    id: Long,
    runId: Long,
    trackingMode: TrackingMode,
): ExerciseResultEntity =
    ExerciseResultEntity(
        id = id,
        workoutRunId = runId,
        exerciseId = 7L,
        exerciseNameSnapshot = "Adaptable Exercise",
        trackingModeSnapshot = trackingMode.databaseValue,
        sortOrder = 0,
        notes = null,
    )
