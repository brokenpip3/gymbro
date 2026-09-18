package com.brokenpip3.gymbro.ui.screens.results

import com.brokenpip3.gymbro.domain.TrackingMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ResultsInsightsCalculatorTest {
    @Test
    fun reportsConsistencyMomentumAndPersonalRecords() {
        val insights =
            calculateResultsInsights(
                recentWorkouts =
                    listOf(
                        workout(id = 1L),
                        workout(id = 2L),
                        workout(id = 3L),
                    ),
                exerciseStats =
                    listOf(
                        exerciseStat(
                            id = 1L,
                            name = "Back Squat",
                            previousValue = 100.0,
                            latestValue = 120.0,
                            personalRecordLabel = "PR",
                        ),
                        exerciseStat(
                            id = 2L,
                            name = "Leg Curl",
                            previousValue = 100.0,
                            latestValue = 105.0,
                        ),
                    ),
            )

        assertEquals(
            listOf(
                ResultsInsightUiModel(
                    title = "Consistency",
                    message = "3 workouts completed in this range.",
                    tone = ResultsInsightTone.Positive,
                ),
                ResultsInsightUiModel(
                    title = "Momentum",
                    message = "Back Squat is up 20% from the previous session.",
                    tone = ResultsInsightTone.Positive,
                ),
                ResultsInsightUiModel(
                    title = "Personal records",
                    message = "1 exercise set a new personal record.",
                    tone = ResultsInsightTone.Positive,
                ),
            ),
            insights,
        )
    }

    @Test
    fun asksForAnotherWorkoutWhenThereIsNoComparisonBaseline() {
        assertEquals(
            listOf(
                ResultsInsightUiModel(
                    title = "Build your baseline",
                    message = "Complete another workout to unlock progress comparisons.",
                    tone = ResultsInsightTone.Neutral,
                ),
            ),
            calculateResultsInsights(
                recentWorkouts = listOf(workout(id = 1L)),
                exerciseStats =
                    listOf(
                        exerciseStat(
                            id = 1L,
                            name = "Plank",
                            previousValue = null,
                            latestValue = 60.0,
                        ),
                    ),
            ),
        )
    }

    @Test
    fun returnsNoInsightsWithoutCompletedWorkouts() {
        assertEquals(emptyList<ResultsInsightUiModel>(), calculateResultsInsights(emptyList(), emptyList()))
    }

    @Test
    fun doesNotTreatLongerTimedDurationAsPositiveMomentum() {
        val insights =
            calculateResultsInsights(
                recentWorkouts = listOf(workout(id = 1L), workout(id = 2L)),
                exerciseStats =
                    listOf(
                        timedExerciseStat(
                            previousValue = 300.0,
                            latestValue = 600.0,
                        ),
                    ),
            )

        assertEquals(listOf("Consistency"), insights.map { insight -> insight.title })
    }

    @Test
    fun reportsFasterTimedProgressWhenPaceImproves() {
        val insights =
            calculateResultsInsights(
                recentWorkouts = listOf(workout(id = 1L), workout(id = 2L)),
                exerciseStats =
                    listOf(
                        timedExerciseStat(
                            previousValue = 600.0,
                            latestValue = 500.0,
                        ),
                    ),
                exerciseDetails =
                    listOf(
                        ExerciseStatsDetailUiModel(
                            exerciseId = 3L,
                            exerciseName = "Run",
                            trackingMode = TrackingMode.Timed,
                            latest = "8m 20s",
                            best = "8m 20s",
                            average = "9m 10s avg",
                            totalSessions = 2,
                            totalSets = 2,
                            points =
                                listOf(
                                    ChartPoint(label = "Previous", value = 600.0),
                                    ChartPoint(label = "Latest", value = 500.0),
                                ),
                            recentSets = emptyList(),
                            additionalCharts =
                                listOf(
                                    ExerciseChartUiModel(
                                        title = "Average pace",
                                        points =
                                            listOf(
                                                ChartPoint(label = "Previous", value = 300.0),
                                                ChartPoint(label = "Latest", value = 250.0),
                                            ),
                                        valueFormat = ChartValueFormat.Duration,
                                    ),
                                ),
                        ),
                    ),
            )

        assertEquals(
            "Run is faster by 16.67% from the previous session.",
            insights.single { insight -> insight.title == "Momentum" }.message,
        )
    }
}

private fun workout(id: Long): WorkoutSummaryUiModel =
    WorkoutSummaryUiModel(
        runId = id,
        scheduleName = "Workout",
        startedAt = id,
        durationSeconds = 1_800L,
        completedSetCount = 6,
    )

private fun exerciseStat(
    id: Long,
    name: String,
    previousValue: Double?,
    latestValue: Double,
    personalRecordLabel: String? = null,
): ExerciseStatsUiModel =
    ExerciseStatsUiModel(
        exerciseId = id,
        exerciseName = name,
        trackingMode = TrackingMode.Strength,
        headline = "${latestValue.formatStat()} volume",
        progressDelta = "New",
        personalRecordLabel = personalRecordLabel,
        trend = if (previousValue == null) TrendDirection.Unknown else TrendDirection.Up,
        points =
            listOfNotNull(
                previousValue?.let { value -> ChartPoint(label = "Previous", value = value) },
                ChartPoint(label = "Latest", value = latestValue),
            ),
    )

private fun timedExerciseStat(
    previousValue: Double,
    latestValue: Double,
): ExerciseStatsUiModel =
    exerciseStat(
        id = 3L,
        name = "Run",
        previousValue = previousValue,
        latestValue = latestValue,
    ).copy(trackingMode = TrackingMode.Timed)
