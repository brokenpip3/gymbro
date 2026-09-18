package com.brokenpip3.gymbro.ui.screens.results

import com.brokenpip3.gymbro.domain.TrackingMode

data class ResultsUiState(
    val recentWorkouts: List<WorkoutSummaryUiModel> = emptyList(),
    val exerciseStats: List<ExerciseStatsUiModel> = emptyList(),
    val insights: List<ResultsInsightUiModel> = emptyList(),
    val exerciseDetails: List<ExerciseStatsDetailUiModel> = emptyList(),
    val selectedExerciseDetail: ExerciseStatsDetailUiModel? = null,
    val workoutDetails: List<WorkoutDetailUiModel> = emptyList(),
    val selectedWorkoutDetail: WorkoutDetailUiModel? = null,
    val statsRange: ResultsStatsRange = ResultsStatsRange.AllTime,
    val hasHistoricalData: Boolean = false,
)

data class ResultsInsightUiModel(
    val title: String,
    val message: String,
    val tone: ResultsInsightTone,
)

enum class ResultsInsightTone {
    Positive,
    Neutral,
}

enum class ResultsStatsRange(
    val label: String,
    val days: Long?,
) {
    AllTime(label = "All time", days = null),
    Last90Days(label = "90 days", days = 90L),
    Last30Days(label = "30 days", days = 30L),
    ;

    fun includes(
        finishedAt: Long,
        now: Long,
    ): Boolean = days == null || finishedAt >= now - (days * MILLIS_PER_DAY)
}

private const val MILLIS_PER_DAY = 86_400_000L

data class WorkoutSummaryUiModel(
    val runId: Long,
    val scheduleName: String,
    val startedAt: Long,
    val durationSeconds: Long,
    val completedSetCount: Int,
)

data class WorkoutDetailUiModel(
    val runId: Long,
    val scheduleName: String,
    val startedAt: Long,
    val durationSeconds: Long,
    val notes: String?,
    val exercises: List<WorkoutExerciseDetailUiModel>,
)

data class WorkoutExerciseDetailUiModel(
    val exerciseResultId: Long,
    val exerciseName: String,
    val trackingMode: TrackingMode,
    val notes: String?,
    val sets: List<WorkoutSetDetailUiModel>,
)

data class WorkoutSetDetailUiModel(
    val setId: Long,
    val setNumber: Int,
    val value: String?,
    val isCompleted: Boolean,
    val notes: String?,
)

data class ExerciseStatsUiModel(
    val exerciseId: Long?,
    val exerciseName: String,
    val trackingMode: TrackingMode,
    val deletedScope: DeletedExerciseScope? = null,
    val headline: String,
    val progressDelta: String = "New",
    val lastTrainedAt: Long = 0L,
    val progressScore: Double? = null,
    val personalRecordLabel: String? = null,
    val trend: TrendDirection,
    val points: List<ChartPoint>,
)

enum class ExerciseStatsFilter(
    val label: String,
    val trackingMode: TrackingMode?,
) {
    All(label = "All", trackingMode = null),
    Strength(label = "Strength", trackingMode = TrackingMode.Strength),
    Timed(label = "Timed", trackingMode = TrackingMode.Timed),
    Bodyweight(label = "Bodyweight", trackingMode = TrackingMode.Bodyweight),
}

enum class ExerciseStatsSort(
    val label: String,
) {
    Recent(label = "Recent"),
    Progress(label = "Progress"),
    Records(label = "Records"),
    Name(label = "Name"),
}

internal fun filterAndSortExerciseStats(
    stats: List<ExerciseStatsUiModel>,
    query: String,
    filter: ExerciseStatsFilter,
    sort: ExerciseStatsSort,
): List<ExerciseStatsUiModel> {
    val normalizedQuery = query.trim()
    val filtered =
        stats.filter { stat ->
            (filter.trackingMode == null || stat.trackingMode == filter.trackingMode) &&
                (normalizedQuery.isBlank() || stat.exerciseName.contains(normalizedQuery, ignoreCase = true))
        }

    return when (sort) {
        ExerciseStatsSort.Recent ->
            filtered.sortedWith(
                compareByDescending<ExerciseStatsUiModel> { stat -> stat.lastTrainedAt }
                    .thenBy { stat -> stat.exerciseName.lowercase() },
            )

        ExerciseStatsSort.Progress ->
            filtered.sortedWith(
                compareByDescending<ExerciseStatsUiModel> { stat ->
                    stat.progressScore ?: Double.NEGATIVE_INFINITY
                }.thenByDescending { stat -> stat.lastTrainedAt }
                    .thenBy { stat -> stat.exerciseName.lowercase() },
            )

        ExerciseStatsSort.Records ->
            filtered.sortedWith(
                compareByDescending<ExerciseStatsUiModel> { stat -> stat.personalRecordLabel != null }
                    .thenByDescending { stat -> stat.lastTrainedAt }
                    .thenBy { stat -> stat.exerciseName.lowercase() },
            )

        ExerciseStatsSort.Name ->
            filtered.sortedWith(
                compareBy<ExerciseStatsUiModel> { stat -> stat.exerciseName.lowercase() }
                    .thenBy { stat -> stat.trackingMode.ordinal },
            )
    }
}

data class ExerciseStatsDetailUiModel(
    val exerciseId: Long?,
    val exerciseName: String,
    val trackingMode: TrackingMode,
    val deletedScope: DeletedExerciseScope? = null,
    val latest: String,
    val best: String,
    val bestSession: String = "No sessions",
    val bestEstimatedOneRepMax: String? = null,
    val average: String,
    val progressDelta: String = "New",
    val progressPercent: String? = null,
    val personalRecordLabel: String? = null,
    val totalSessions: Int,
    val totalSets: Int,
    val summaryMetrics: List<SummaryMetricUiModel> = emptyList(),
    val chartSummary: ChartSummaryUiModel? = null,
    val personalRecords: List<PersonalRecordUiModel> = emptyList(),
    val points: List<ChartPoint>,
    val recentSessions: List<RecentSessionUiModel> = emptyList(),
    val recentSets: List<RecentSetUiModel>,
    val notes: List<String> = emptyList(),
    val noteDetails: List<ExerciseNoteUiModel> = emptyList(),
    val secondaryChart: ExerciseChartUiModel? = null,
    val additionalCharts: List<ExerciseChartUiModel> = emptyList(),
)

enum class ChartValueFormat {
    Number,
    Duration,
}

data class ExerciseChartUiModel(
    val title: String,
    val points: List<ChartPoint>,
    val valueFormat: ChartValueFormat = ChartValueFormat.Number,
)

data class ChartSummaryUiModel(
    val rangeLabel: String,
    val minLabel: String,
    val maxLabel: String,
)

data class SummaryMetricUiModel(
    val label: String,
    val value: String,
)

data class PersonalRecordUiModel(
    val dateLabel: String,
    val metric: String,
    val value: String,
)

data class DeletedExerciseScope(
    val scheduleId: Long,
    val sortOrder: Int,
)

internal data class ExerciseStatsSelectionKey(
    val exerciseId: Long?,
    val exerciseName: String?,
    val trackingMode: TrackingMode?,
    val deletedScope: DeletedExerciseScope?,
)

data class RecentSetUiModel(
    val label: String,
    val value: String,
)

data class RecentSessionUiModel(
    val dateLabel: String,
    val scheduleName: String,
    val value: String,
    val setCount: Int,
    val comparison: SessionComparisonUiModel? = null,
)

data class SessionComparisonUiModel(
    val label: String,
    val trend: TrendDirection,
)

data class ExerciseNoteUiModel(
    val dateLabel: String,
    val contextLabel: String,
    val text: String,
)

enum class TrendDirection {
    Up,
    Flat,
    Down,
    Unknown,
}

data class ChartPoint(
    val label: String,
    val value: Double,
)
