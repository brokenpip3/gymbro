package com.brokenpip3.gymbro.ui.screens.results

import com.brokenpip3.gymbro.domain.TrackingMode
import kotlin.math.abs

internal fun calculateResultsInsights(
    recentWorkouts: List<WorkoutSummaryUiModel>,
    exerciseStats: List<ExerciseStatsUiModel>,
    exerciseDetails: List<ExerciseStatsDetailUiModel> = emptyList(),
): List<ResultsInsightUiModel> {
    if (recentWorkouts.isEmpty()) return emptyList()

    val insights =
        buildList {
            if (recentWorkouts.size == 1) {
                add(
                    ResultsInsightUiModel(
                        title = "Build your baseline",
                        message = "Complete another workout to unlock progress comparisons.",
                        tone = ResultsInsightTone.Neutral,
                    ),
                )
            } else {
                add(
                    ResultsInsightUiModel(
                        title = "Consistency",
                        message = "${recentWorkouts.size} workouts completed in this range.",
                        tone = ResultsInsightTone.Positive,
                    ),
                )
            }

            exerciseStats
                .mapNotNull { stat -> stat.toPositiveProgress(exerciseDetails) }
                .maxByOrNull { progress -> progress.percent }
                ?.let { progress ->
                    add(
                        ResultsInsightUiModel(
                            title = "Momentum",
                            message =
                                progress.message,
                            tone = ResultsInsightTone.Positive,
                        ),
                    )
                }

            val personalRecordCount = exerciseStats.count { stat -> stat.personalRecordLabel != null }
            if (personalRecordCount > 0) {
                add(
                    ResultsInsightUiModel(
                        title = "Personal records",
                        message =
                            "$personalRecordCount ${if (personalRecordCount == 1) "exercise" else "exercises"} " +
                                "set a new personal record.",
                        tone = ResultsInsightTone.Positive,
                    ),
                )
            } else if (recentWorkouts.size > 1 && exerciseStats.none { stat -> stat.points.size >= 2 }) {
                add(
                    ResultsInsightUiModel(
                        title = "Build your baseline",
                        message = "Repeat an exercise to unlock progress comparisons.",
                        tone = ResultsInsightTone.Neutral,
                    ),
                )
            }
        }

    return insights.take(MAX_RESULTS_INSIGHTS)
}

private data class PositiveProgress(
    val percent: Double,
    val message: String,
)

private typealias ExerciseDetails = List<ExerciseStatsDetailUiModel>

private fun ExerciseStatsUiModel.toPositiveProgress(exerciseDetails: ExerciseDetails): PositiveProgress? =
    if (trackingMode == TrackingMode.Timed) {
        exerciseDetails
            .firstOrNull { detail -> detail.matches(this) }
            ?.toPositivePaceProgress()
    } else {
        points.toPositiveProgress(exerciseName)
    }

private fun List<ChartPoint>.toPositiveProgress(exerciseName: String): PositiveProgress? =
    takeIf { values -> values.size >= 2 }
        ?.let { values ->
            val previous = values[values.lastIndex - 1].value
            if (previous == 0.0) {
                null
            } else {
                val percent = ((values.last().value - previous) / abs(previous)) * 100.0
                PositiveProgress(
                    percent = percent,
                    message = "$exerciseName is up ${percent.formatStat()}% from the previous session.",
                ).takeIf { progress -> progress.percent > 0.0 }
            }
        }

private fun ExerciseStatsDetailUiModel.toPositivePaceProgress(): PositiveProgress? =
    additionalCharts
        .firstOrNull { chart -> chart.title == "Average pace" }
        ?.points
        ?.takeIf { points -> points.size >= 2 }
        ?.let { pacePoints ->
            val previous = pacePoints[pacePoints.lastIndex - 1].value
            if (previous <= 0.0) {
                null
            } else {
                val latest = pacePoints.last().value
                val percent = ((previous - latest) / previous) * 100.0
                PositiveProgress(
                    percent = percent,
                    message = "$exerciseName is faster by ${percent.formatStat()}% from the previous session.",
                ).takeIf { progress -> progress.percent > 0.0 }
            }
        }

private fun ExerciseStatsDetailUiModel.matches(stat: ExerciseStatsUiModel): Boolean =
    exerciseId == stat.exerciseId &&
        exerciseName == stat.exerciseName &&
        trackingMode == stat.trackingMode &&
        deletedScope == stat.deletedScope

private const val MAX_RESULTS_INSIGHTS = 3
