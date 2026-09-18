package com.brokenpip3.gymbro.ui.screens.results

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.brokenpip3.gymbro.data.entities.ExerciseResultEntity
import com.brokenpip3.gymbro.data.entities.SetResultEntity
import com.brokenpip3.gymbro.data.entities.WorkoutRunEntity
import com.brokenpip3.gymbro.domain.TrackingMode
import com.brokenpip3.gymbro.ui.components.TrendChart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class ResultsScreenContractTest {
    @Test
    fun signaturesCompile() = Unit

    @Test
    fun exerciseStatsSearchMatchesNamesIgnoringCaseAndKeepsOrder() {
        val stats =
            listOf(
                exerciseStat(id = 2L, name = "Tempo Run"),
                exerciseStat(id = 1L, name = "Back Squat"),
            )

        assertEquals(listOf(1L), filterExerciseStats(stats, "SQUAT").map { stat -> stat.exerciseId })
        assertEquals(listOf(2L), filterExerciseStats(stats, "run").map { stat -> stat.exerciseId })
        assertEquals(listOf(2L, 1L), filterExerciseStats(stats, " ").map { stat -> stat.exerciseId })
    }

    @Test
    fun exerciseStatsCanFilterByModeAndSortByRecentProgressRecordsOrName() {
        val stats =
            listOf(
                exerciseStat(
                    id = 1L,
                    name = "Back Squat",
                    mode = TrackingMode.Strength,
                    lastTrainedAt = 10L,
                    progressScore = 4.0,
                    personalRecordLabel = null,
                ),
                exerciseStat(
                    id = 2L,
                    name = "Tempo Run",
                    mode = TrackingMode.Timed,
                    lastTrainedAt = 30L,
                    progressScore = 2.0,
                    personalRecordLabel = "PR",
                ),
                exerciseStat(
                    id = 3L,
                    name = "Bench Press",
                    mode = TrackingMode.Strength,
                    lastTrainedAt = 20L,
                    progressScore = 8.0,
                    personalRecordLabel = null,
                ),
            )

        assertEquals(
            listOf(3L, 1L),
            filterAndSortExerciseStats(
                stats = stats,
                query = "",
                filter = ExerciseStatsFilter.Strength,
                sort = ExerciseStatsSort.Progress,
            ).map { stat -> stat.exerciseId },
        )
        assertEquals(
            listOf(2L, 3L, 1L),
            filterAndSortExerciseStats(
                stats = stats,
                query = "",
                filter = ExerciseStatsFilter.All,
                sort = ExerciseStatsSort.Recent,
            ).map { stat -> stat.exerciseId },
        )
        assertEquals(
            listOf(2L, 3L, 1L),
            filterAndSortExerciseStats(
                stats = stats,
                query = "",
                filter = ExerciseStatsFilter.All,
                sort = ExerciseStatsSort.Records,
            ).map { stat -> stat.exerciseId },
        )
        assertEquals(
            listOf(1L, 3L, 2L),
            filterAndSortExerciseStats(
                stats = stats,
                query = "",
                filter = ExerciseStatsFilter.All,
                sort = ExerciseStatsSort.Name,
            ).map { stat -> stat.exerciseId },
        )
    }

    @Test
    fun summaryMetricRowsKeepEveryMetricAndPairByDisplayOrder() {
        val metrics =
            listOf(
                SummaryMetricUiModel(label = "Total time", value = "17m 0s"),
                SummaryMetricUiModel(label = "Total distance", value = "3"),
                SummaryMetricUiModel(label = "Avg pace", value = "5m 40s / distance"),
                SummaryMetricUiModel(label = "Best set", value = "10m 0s"),
                SummaryMetricUiModel(label = "Frequency", value = "2 sessions"),
            )

        assertEquals(
            listOf(
                listOf("Total time", "Total distance"),
                listOf("Avg pace", "Best set"),
                listOf("Frequency"),
            ),
            summaryMetricRows(metrics).map { row -> row.map(SummaryMetricUiModel::label) },
        )
        assertEquals(metrics, summaryMetricRows(metrics).flatten())
    }
}

private fun exerciseStat(
    id: Long,
    name: String,
    mode: TrackingMode = TrackingMode.Strength,
    lastTrainedAt: Long = 0L,
    progressScore: Double? = null,
    personalRecordLabel: String? = null,
): ExerciseStatsUiModel =
    ExerciseStatsUiModel(
        exerciseId = id,
        exerciseName = name,
        trackingMode = mode,
        headline = "100 volume",
        personalRecordLabel = personalRecordLabel,
        lastTrainedAt = lastTrainedAt,
        progressScore = progressScore,
        trend = TrendDirection.Unknown,
        points = listOf(ChartPoint(label = "Today", value = 100.0)),
    )

@Composable
private fun ResultsRouteSignatureContract() {
    ResultsRoute(
        repository = ContractResultsSource(),
        initialExerciseId = 7L,
        onDismissExerciseDetail = {},
    )
}

@Composable
private fun ResultsScreenSignatureContract() {
    ResultsScreen(
        uiState =
            ResultsUiState(
                recentWorkouts =
                    listOf(
                        WorkoutSummaryUiModel(
                            runId = 1L,
                            scheduleName = "Leg Day",
                            startedAt = 100L,
                            durationSeconds = 3_600L,
                            completedSetCount = 8,
                        ),
                    ),
                exerciseStats =
                    listOf(
                        ExerciseStatsUiModel(
                            exerciseId = 2L,
                            exerciseName = "Squat",
                            trackingMode = TrackingMode.Strength,
                            headline = "1200 volume",
                            trend = TrendDirection.Up,
                            points =
                                listOf(
                                    ChartPoint(label = "Workout 1", value = 1000.0),
                                    ChartPoint(label = "Workout 2", value = 1200.0),
                                ),
                        ),
                    ),
            ),
    )
}

@Composable
private fun TrendChartSignatureContract() {
    TrendChart(
        points =
            listOf(
                ChartPoint(label = "Workout 1", value = 1.0),
                ChartPoint(label = "Workout 2", value = 2.0),
            ),
        height = 128.dp,
    )
}

private class ContractResultsSource : ResultsSource {
    override fun observeWorkoutRuns(): Flow<List<WorkoutRunEntity>> = MutableStateFlow(emptyList())

    override suspend fun getExerciseResults(workoutRunId: Long): List<ExerciseResultEntity> = emptyList()

    override suspend fun getSetResults(exerciseResultId: Long): List<SetResultEntity> = emptyList()
}
