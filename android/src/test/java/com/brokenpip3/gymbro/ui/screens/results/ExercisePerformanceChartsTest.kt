package com.brokenpip3.gymbro.ui.screens.results

import com.brokenpip3.gymbro.data.entities.ExerciseResultEntity
import com.brokenpip3.gymbro.data.entities.SetResultEntity
import com.brokenpip3.gymbro.data.entities.WorkoutRunEntity
import com.brokenpip3.gymbro.domain.TrackingMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ExercisePerformanceChartsTest {
    @Test
    fun strengthDetailsIncludeEstimatedOneRepMaxTrend() {
        val detail =
            strengthRuns(
                listOf(
                    10 to 50.0,
                    8 to 60.0,
                ),
            ).toExerciseStatDetails().single()

        val chart = detail.additionalCharts.first { chart -> chart.title == "Estimated 1RM" }
        assertEquals(
            listOf(
                ChartPoint(label = "Jan 2", value = 66.66666666666666),
                ChartPoint(label = "Jan 2", value = 76.0),
            ),
            chart.points,
        )
    }

    @Test
    fun strengthDetailsIncludeTotalRepsTrend() {
        val detail =
            strengthRuns(
                listOf(
                    10 to 50.0,
                    8 to 60.0,
                ),
            ).toExerciseStatDetails().single()

        val chart = detail.additionalCharts.first { chart -> chart.title == "Total reps" }

        assertEquals(
            listOf(
                ChartPoint(label = "Jan 2", value = 10.0),
                ChartPoint(label = "Jan 2", value = 8.0),
            ),
            chart.points,
        )
    }

    @Test
    fun recentStrengthSessionsExplainChangeFromThePreviousSession() {
        val detail =
            strengthRuns(
                listOf(
                    10 to 50.0,
                    10 to 60.0,
                    10 to 70.0,
                ),
            ).toExerciseStatDetails().single()

        assertEquals(
            listOf("+100 volume", "+100 volume", null),
            detail.recentSessions.map { session -> session.comparison?.label },
        )
        assertEquals(
            listOf(TrendDirection.Up, TrendDirection.Up, null),
            detail.recentSessions.map { session -> session.comparison?.trend },
        )
    }

    @Test
    fun timedDetailsIncludePaceTrendWhenDistanceIsLogged() {
        val detail =
            timedRuns(
                listOf(
                    600L to 2.0,
                    540L to 2.0,
                ),
            ).toExerciseStatDetails().single()

        assertEquals("Average pace", detail.additionalCharts.single().title)
        assertEquals(ChartValueFormat.Duration, detail.additionalCharts.single().valueFormat)
        assertEquals(
            listOf(
                ChartPoint(label = "Jan 2", value = 300.0),
                ChartPoint(label = "Jan 2", value = 270.0),
            ),
            detail.additionalCharts.single().points,
        )
    }

    @Test
    fun timedProgressUsesPaceWhenBothSessionsHaveDistance() {
        val runs =
            timedRuns(
                listOf(
                    600L to 2.0,
                    600L to 3.0,
                ),
            )

        val stat = runs.toExerciseStats().single()
        val detail = runs.toExerciseStatDetails().single()

        assertEquals(TrendDirection.Up, stat.trend)
        assertEquals("-1m 40s / distance", stat.progressDelta)
        assertEquals("-33.3%", detail.progressPercent)
        assertEquals("PR", detail.personalRecordLabel)
    }

    @Test
    fun recentTimedSessionsDescribeFasterPaceAsImprovement() {
        val detail =
            timedRuns(
                listOf(
                    600L to 2.0,
                    540L to 2.0,
                ),
            ).toExerciseStatDetails().single()
        val comparison = detail.recentSessions.first().comparison

        assertEquals(
            "Faster 0m 30s / distance",
            comparison?.label,
        )
        assertEquals(
            TrendDirection.Up,
            comparison?.trend,
        )
    }

    @Test
    fun timedDistanceRecordIsShownWhenPaceIsUnchanged() {
        val stat =
            timedRuns(
                listOf(
                    600L to 2.0,
                    900L to 3.0,
                ),
            ).toExerciseStats().single()

        assertEquals("PR", stat.personalRecordLabel)
    }

    @Test
    fun timedSetDetailsIncludeDistanceWhenItIsLogged() {
        val set =
            SetResultEntity(
                id = 1L,
                exerciseResultId = 1L,
                setOrder = 0,
                reps = null,
                weight = null,
                durationSeconds = 300L,
                distance = 2.0,
                notes = null,
            )

        assertEquals("5m 0s / 2 distance", set.formatSet(TrackingMode.Timed))
    }

    @Test
    fun timedAverageUsesPaceWhenEverySessionHasDistance() {
        val detail =
            timedRuns(
                listOf(
                    600L to 2.0,
                    540L to 3.0,
                ),
            ).toExerciseStatDetails().single()

        assertEquals("4m 0s avg pace / distance", detail.average)
    }

    @Test
    fun timedDistanceDetailsUseFastestSetPaceAndDistanceRecords() {
        val detail =
            timedRunsWithSets(
                listOf(
                    listOf(600L to 2.0, 360L to 1.0),
                    listOf(540L to 2.0, 300L to 1.0),
                ),
            ).toExerciseStatDetails().single()

        assertEquals("4m 30s / distance", detail.best)
        assertEquals(
            listOf("Distance", "Pace", "Pace"),
            detail.personalRecords.map { record -> record.metric },
        )
    }

    @Test
    fun timedAveragePaceUsesOnlyTheRecentAverageWindow() {
        val detail =
            timedRuns(
                listOf(
                    3_600L to 1.0,
                    300L to 1.0,
                    300L to 1.0,
                    300L to 1.0,
                    300L to 1.0,
                    300L to 1.0,
                ),
            ).toExerciseStatDetails().single()

        assertEquals("5m 0s avg pace / distance", detail.average)
    }

    @Test
    fun exerciseStatsExposeLatestSessionAndImprovementScore() {
        val stat =
            strengthRuns(
                listOf(
                    10 to 50.0,
                    10 to 60.0,
                ),
            ).toExerciseStats().single()

        assertEquals(86_400_002L, stat.lastTrainedAt)
        assertEquals(20.0, stat.progressScore!!, 0.001)
        assertEquals(2, stat.points.size)
    }

    private fun strengthRuns(values: List<Pair<Int, Double>>): List<ResultRunDetail> =
        values.mapIndexed { index, (reps, weight) ->
            val runId = index + 1L
            ResultRunDetail(
                workoutRun = run(runId = runId, finishedAt = 86_400_000L + runId),
                exerciseDetails =
                    listOf(
                        ResultExerciseDetail(
                            exerciseResult = exerciseResult(runId = runId, mode = TrackingMode.Strength),
                            setResults =
                                listOf(
                                    SetResultEntity(
                                        id = runId,
                                        exerciseResultId = runId,
                                        setOrder = 0,
                                        reps = reps,
                                        weight = weight,
                                        durationSeconds = null,
                                        distance = null,
                                        notes = null,
                                    ),
                                ),
                        ),
                    ),
            )
        }

    private fun timedRuns(values: List<Pair<Long, Double>>): List<ResultRunDetail> =
        values.mapIndexed { index, (duration, distance) ->
            val runId = index + 1L
            ResultRunDetail(
                workoutRun = run(runId = runId, finishedAt = 86_400_000L + runId),
                exerciseDetails =
                    listOf(
                        ResultExerciseDetail(
                            exerciseResult = exerciseResult(runId = runId, mode = TrackingMode.Timed),
                            setResults =
                                listOf(
                                    SetResultEntity(
                                        id = runId,
                                        exerciseResultId = runId,
                                        setOrder = 0,
                                        reps = null,
                                        weight = null,
                                        durationSeconds = duration,
                                        distance = distance,
                                        notes = null,
                                    ),
                                ),
                        ),
                    ),
            )
        }

    private fun timedRunsWithSets(values: List<List<Pair<Long, Double>>>): List<ResultRunDetail> =
        values.mapIndexed { index, sets ->
            val runId = index + 1L
            ResultRunDetail(
                workoutRun = run(runId = runId, finishedAt = 86_400_000L + runId),
                exerciseDetails =
                    listOf(
                        ResultExerciseDetail(
                            exerciseResult = exerciseResult(runId = runId, mode = TrackingMode.Timed),
                            setResults =
                                sets.mapIndexed { setIndex, (duration, distance) ->
                                    SetResultEntity(
                                        id = runId * 10L + setIndex,
                                        exerciseResultId = runId,
                                        setOrder = setIndex,
                                        reps = null,
                                        weight = null,
                                        durationSeconds = duration,
                                        distance = distance,
                                        notes = null,
                                    )
                                },
                        ),
                    ),
            )
        }

    private fun run(
        runId: Long,
        finishedAt: Long,
    ): WorkoutRunEntity =
        WorkoutRunEntity(
            id = runId,
            scheduleId = 1L,
            scheduleNameSnapshot = "Run",
            startedAt = finishedAt - 60_000L,
            finishedAt = finishedAt,
            notes = null,
        )

    private fun exerciseResult(
        runId: Long,
        mode: TrackingMode,
    ): ExerciseResultEntity =
        ExerciseResultEntity(
            id = runId,
            workoutRunId = runId,
            exerciseId = 1L,
            exerciseNameSnapshot = if (mode == TrackingMode.Timed) "Run" else "Squat",
            trackingModeSnapshot = mode.databaseValue,
            sortOrder = 0,
            notes = null,
        )
}
