package com.brokenpip3.gymbro.ui.screens.results

import com.brokenpip3.gymbro.data.entities.ExerciseResultEntity
import com.brokenpip3.gymbro.data.entities.SetResultEntity
import com.brokenpip3.gymbro.data.entities.WorkoutRunEntity
import com.brokenpip3.gymbro.domain.TrackingMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("LargeClass")
class ResultsViewModelTest {
    @Test
    @Suppress("LongMethod")
    fun workoutDetailPreservesExerciseOrderAndShowsIncompleteSetsAndNotes() {
        val detail =
            ResultRunDetail(
                workoutRun =
                    workoutRun(
                        id = 7,
                        scheduleName = "Leg Day",
                        startedAt = 1_000L,
                        finishedAt = 61_000L,
                        notes = "Felt strong",
                    ),
                exerciseDetails =
                    listOf(
                        ResultExerciseDetail(
                            exerciseResult =
                                exerciseResult(
                                    id = 20,
                                    workoutRunId = 7,
                                    exerciseId = 2,
                                    exerciseName = "Leg Curl",
                                    trackingMode = TrackingMode.Strength,
                                    sortOrder = 1,
                                    notes = "Keep hips down",
                                ),
                            setResults =
                                listOf(
                                    setResult(
                                        id = 201,
                                        exerciseResultId = 20,
                                        reps = 10,
                                        weight = 30.0,
                                        setOrder = 0,
                                        notes = "Controlled",
                                    ),
                                    setResult(
                                        id = 202,
                                        exerciseResultId = 20,
                                        reps = null,
                                        weight = null,
                                        setOrder = 1,
                                        isCompleted = false,
                                    ),
                                ),
                        ),
                        ResultExerciseDetail(
                            exerciseResult =
                                exerciseResult(
                                    id = 10,
                                    workoutRunId = 7,
                                    exerciseId = 1,
                                    exerciseName = "Squat",
                                    trackingMode = TrackingMode.Strength,
                                    sortOrder = 0,
                                ),
                            setResults =
                                listOf(
                                    setResult(
                                        id = 101,
                                        exerciseResultId = 10,
                                        reps = 5,
                                        weight = 80.0,
                                    ),
                                ),
                        ),
                    ),
            )

        assertEquals(
            WorkoutDetailUiModel(
                runId = 7,
                scheduleName = "Leg Day",
                startedAt = 1_000L,
                durationSeconds = 60L,
                notes = "Felt strong",
                exercises =
                    listOf(
                        WorkoutExerciseDetailUiModel(
                            exerciseResultId = 10,
                            exerciseName = "Squat",
                            trackingMode = TrackingMode.Strength,
                            notes = null,
                            sets =
                                listOf(
                                    WorkoutSetDetailUiModel(
                                        setId = 101,
                                        setNumber = 1,
                                        value = "5 x 80",
                                        isCompleted = true,
                                        notes = null,
                                    ),
                                ),
                        ),
                        WorkoutExerciseDetailUiModel(
                            exerciseResultId = 20,
                            exerciseName = "Leg Curl",
                            trackingMode = TrackingMode.Strength,
                            notes = "Keep hips down",
                            sets =
                                listOf(
                                    WorkoutSetDetailUiModel(
                                        setId = 201,
                                        setNumber = 1,
                                        value = "10 x 30",
                                        isCompleted = true,
                                        notes = "Controlled",
                                    ),
                                    WorkoutSetDetailUiModel(
                                        setId = 202,
                                        setNumber = 2,
                                        value = null,
                                        isCompleted = false,
                                        notes = null,
                                    ),
                                ),
                        ),
                    ),
            ),
            detail.toWorkoutDetailUiModel(),
        )
    }

    @Test
    fun exerciseChartPointsUseWorkoutCompletionDates() {
        val stats =
            listOf(
                ResultRunDetail(
                    workoutRun = workoutRun(id = 1, startedAt = 1_000L, finishedAt = 86_400_000L),
                    exerciseDetails =
                        listOf(
                            ResultExerciseDetail(
                                exerciseResult = strengthExerciseResult(id = 10, workoutRunId = 1),
                                setResults =
                                    listOf(
                                        setResult(id = 100, exerciseResultId = 10, reps = 5, weight = 80.0),
                                    ),
                            ),
                        ),
                ),
            ).toExerciseStats()

        assertEquals(
            "Jan 2",
            stats
                .single()
                .points
                .single()
                .label,
        )
    }

    @Test
    fun strengthDetailsExposeMaxWeightTrendAlongsideVolume() {
        val details =
            listOf(
                ResultRunDetail(
                    workoutRun = workoutRun(id = 1, startedAt = 1_000L, finishedAt = 86_400_000L),
                    exerciseDetails =
                        listOf(
                            ResultExerciseDetail(
                                exerciseResult = strengthExerciseResult(id = 10, workoutRunId = 1),
                                setResults =
                                    listOf(
                                        setResult(id = 100, exerciseResultId = 10, reps = 5, weight = 80.0),
                                    ),
                            ),
                        ),
                ),
                ResultRunDetail(
                    workoutRun = workoutRun(id = 2, startedAt = 2_000L, finishedAt = 172_800_000L),
                    exerciseDetails =
                        listOf(
                            ResultExerciseDetail(
                                exerciseResult = strengthExerciseResult(id = 20, workoutRunId = 2),
                                setResults =
                                    listOf(
                                        setResult(id = 200, exerciseResultId = 20, reps = 5, weight = 90.0),
                                    ),
                            ),
                        ),
                ),
            ).toExerciseStatDetails()

        assertEquals(
            ExerciseChartUiModel(
                title = "Max weight",
                points =
                    listOf(
                        ChartPoint(label = "Jan 2", value = 80.0),
                        ChartPoint(label = "Jan 3", value = 90.0),
                    ),
            ),
            details.single().secondaryChart,
        )
    }

    @Test
    fun exerciseDetailsIncludeRecentSetNotes() {
        val details =
            listOf(
                ResultRunDetail(
                    workoutRun = workoutRun(id = 1, startedAt = 1_000L, finishedAt = 86_400_000L),
                    exerciseDetails =
                        listOf(
                            ResultExerciseDetail(
                                exerciseResult = strengthExerciseResult(id = 10, workoutRunId = 1),
                                setResults =
                                    listOf(
                                        setResult(
                                            id = 100,
                                            exerciseResultId = 10,
                                            reps = 5,
                                            weight = 80.0,
                                            notes = "RPE 8",
                                        ),
                                    ),
                            ),
                        ),
                ),
            ).toExerciseStatDetails()

        assertEquals(listOf("RPE 8"), details.single().notes)
    }

    @Test
    fun exerciseDetailsExposeRecentSessionsNewestFirst() {
        val details =
            listOf(
                ResultRunDetail(
                    workoutRun =
                        workoutRun(
                            id = 1,
                            scheduleName = "Push Day",
                            startedAt = 1_000L,
                            finishedAt = 86_400_000L,
                        ),
                    exerciseDetails =
                        listOf(
                            ResultExerciseDetail(
                                exerciseResult = strengthExerciseResult(id = 10, workoutRunId = 1),
                                setResults =
                                    listOf(
                                        setResult(id = 100, exerciseResultId = 10, reps = 5, weight = 80.0),
                                    ),
                            ),
                        ),
                ),
                ResultRunDetail(
                    workoutRun =
                        workoutRun(
                            id = 2,
                            scheduleName = "Push Day",
                            startedAt = 2_000L,
                            finishedAt = 172_800_000L,
                        ),
                    exerciseDetails =
                        listOf(
                            ResultExerciseDetail(
                                exerciseResult = strengthExerciseResult(id = 20, workoutRunId = 2),
                                setResults =
                                    listOf(
                                        setResult(id = 200, exerciseResultId = 20, reps = 5, weight = 90.0),
                                        setResult(
                                            id = 201,
                                            exerciseResultId = 20,
                                            reps = 5,
                                            weight = 90.0,
                                            setOrder = 1,
                                        ),
                                    ),
                            ),
                        ),
                ),
            ).toExerciseStatDetails()

        assertEquals(
            listOf(
                RecentSessionUiModel(
                    dateLabel = "Jan 3",
                    scheduleName = "Push Day",
                    value = "900 volume",
                    setCount = 2,
                    comparison =
                        SessionComparisonUiModel(
                            label = "+500 volume",
                            trend = TrendDirection.Up,
                        ),
                ),
                RecentSessionUiModel(
                    dateLabel = "Jan 2",
                    scheduleName = "Push Day",
                    value = "400 volume",
                    setCount = 1,
                ),
            ),
            details.single().recentSessions,
        )
    }

    @Test
    fun timedProgressDeltaKeepsNegativeSignWhenLatestSessionIsShorter() {
        val stats =
            listOf(
                ResultRunDetail(
                    workoutRun = workoutRun(id = 1, startedAt = 1_000L, finishedAt = 86_400_000L),
                    exerciseDetails =
                        listOf(
                            ResultExerciseDetail(
                                exerciseResult = timedExerciseResult(id = 10, workoutRunId = 1),
                                setResults =
                                    listOf(
                                        setResult(id = 100, exerciseResultId = 10, durationSeconds = 900L),
                                    ),
                            ),
                        ),
                ),
                ResultRunDetail(
                    workoutRun = workoutRun(id = 2, startedAt = 2_000L, finishedAt = 172_800_000L),
                    exerciseDetails =
                        listOf(
                            ResultExerciseDetail(
                                exerciseResult = timedExerciseResult(id = 20, workoutRunId = 2),
                                setResults =
                                    listOf(
                                        setResult(id = 200, exerciseResultId = 20, durationSeconds = 600L),
                                    ),
                            ),
                        ),
                ),
            ).toExerciseStats()

        assertEquals("-5m 0s", stats.single().progressDelta)
    }

    @Test
    fun recentWorkoutsExcludeUnfinishedRunsAndUseFinishedDurationNewestFirst() =
        runTest {
            val viewModel =
                ResultsViewModel(
                    source =
                        FakeResultsSource(
                            workoutRuns =
                                listOf(
                                    workoutRun(id = 1, scheduleName = "Unfinished", startedAt = 4_000L),
                                    workoutRun(
                                        id = 2,
                                        scheduleName = "Push Day",
                                        startedAt = 1_000L,
                                        finishedAt = 61_000L,
                                    ),
                                    workoutRun(
                                        id = 3,
                                        scheduleName = "Leg Day",
                                        startedAt = 3_000L,
                                        finishedAt = 183_000L,
                                    ),
                                ),
                            exerciseResultsByRunId =
                                mapOf(
                                    2L to listOf(exerciseResult(id = 20, workoutRunId = 2)),
                                    3L to listOf(exerciseResult(id = 30, workoutRunId = 3)),
                                ),
                            setResultsByExerciseResultId =
                                mapOf(
                                    20L to listOf(setResult(id = 200, exerciseResultId = 20)),
                                    30L to
                                        listOf(
                                            setResult(id = 300, exerciseResultId = 30),
                                            setResult(id = 301, exerciseResultId = 30),
                                        ),
                                ),
                        ),
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            assertEquals(
                listOf(
                    WorkoutSummaryUiModel(
                        runId = 3,
                        scheduleName = "Leg Day",
                        startedAt = 3_000L,
                        durationSeconds = 180,
                        completedSetCount = 2,
                    ),
                    WorkoutSummaryUiModel(
                        runId = 2,
                        scheduleName = "Push Day",
                        startedAt = 1_000L,
                        durationSeconds = 60,
                        completedSetCount = 1,
                    ),
                ),
                viewModel.uiState.value.recentWorkouts,
            )
        }

    @Test
    fun selectingStatsRangeFiltersWorkoutHistoryAndExerciseStatsTogether() =
        runTest {
            val now = 200L * MILLIS_PER_DAY
            val viewModel =
                ResultsViewModel(
                    source =
                        FakeResultsSource(
                            workoutRuns =
                                listOf(
                                    workoutRun(
                                        id = 1L,
                                        scheduleName = "Recent",
                                        startedAt = now - (10L * MILLIS_PER_DAY) - 60_000L,
                                        finishedAt = now - (10L * MILLIS_PER_DAY),
                                    ),
                                    workoutRun(
                                        id = 2L,
                                        scheduleName = "Older",
                                        startedAt = now - (100L * MILLIS_PER_DAY) - 60_000L,
                                        finishedAt = now - (100L * MILLIS_PER_DAY),
                                    ),
                                ),
                            exerciseResultsByRunId =
                                mapOf(
                                    1L to listOf(strengthExerciseResult(id = 10L, workoutRunId = 1L)),
                                    2L to listOf(strengthExerciseResult(id = 20L, workoutRunId = 2L)),
                                ),
                            setResultsByExerciseResultId =
                                mapOf(
                                    10L to
                                        listOf(
                                            setResult(
                                                id = 100L,
                                                exerciseResultId = 10L,
                                                reps = 5,
                                                weight = 80.0,
                                            ),
                                        ),
                                    20L to
                                        listOf(
                                            setResult(
                                                id = 200L,
                                                exerciseResultId = 20L,
                                                reps = 5,
                                                weight = 60.0,
                                            ),
                                        ),
                                ),
                        ),
                    coroutineScope = viewModelScope(),
                    clock = { now },
                )
            advanceUntilIdle()

            viewModel.selectStatsRange(ResultsStatsRange.Last90Days)
            advanceUntilIdle()

            assertEquals(ResultsStatsRange.Last90Days, viewModel.uiState.value.statsRange)
            assertEquals(
                listOf("Recent"),
                viewModel.uiState.value.recentWorkouts
                    .map { it.scheduleName },
            )
            assertEquals(
                1,
                viewModel.uiState.value.exerciseDetails
                    .single()
                    .totalSessions,
            )
            assertTrue(viewModel.uiState.value.hasHistoricalData)
        }

    @Test
    fun emptyStatsRangeKeepsHistoricalDataFlagForRangeSpecificEmptyState() =
        runTest {
            val now = 200L * MILLIS_PER_DAY
            val viewModel =
                ResultsViewModel(
                    source =
                        FakeResultsSource(
                            workoutRuns =
                                listOf(
                                    workoutRun(
                                        id = 1L,
                                        scheduleName = "Older",
                                        startedAt = now - (100L * MILLIS_PER_DAY) - 60_000L,
                                        finishedAt = now - (100L * MILLIS_PER_DAY),
                                    ),
                                ),
                            exerciseResultsByRunId =
                                mapOf(1L to listOf(strengthExerciseResult(id = 10L, workoutRunId = 1L))),
                            setResultsByExerciseResultId =
                                mapOf(
                                    10L to
                                        listOf(
                                            setResult(
                                                id = 100L,
                                                exerciseResultId = 10L,
                                                reps = 5,
                                                weight = 80.0,
                                            ),
                                        ),
                                ),
                        ),
                    coroutineScope = viewModelScope(),
                    clock = { now },
                )
            advanceUntilIdle()

            viewModel.selectStatsRange(ResultsStatsRange.Last30Days)
            advanceUntilIdle()

            assertEquals(ResultsStatsRange.Last30Days, viewModel.uiState.value.statsRange)
            assertTrue(
                viewModel.uiState.value.recentWorkouts
                    .isEmpty(),
            )
            assertTrue(
                viewModel.uiState.value.exerciseStats
                    .isEmpty(),
            )
            assertTrue(viewModel.uiState.value.hasHistoricalData)
        }

    @Test
    fun selectingRecentWorkoutExposesItsDetail() =
        runTest {
            val viewModel =
                ResultsViewModel(
                    source =
                        FakeResultsSource(
                            workoutRuns =
                                listOf(
                                    workoutRun(
                                        id = 7L,
                                        scheduleName = "Leg Day",
                                        startedAt = 1_000L,
                                        finishedAt = 61_000L,
                                    ),
                                ),
                            exerciseResultsByRunId =
                                mapOf(
                                    7L to listOf(exerciseResult(id = 70L, workoutRunId = 7L, exerciseName = "Squat")),
                                ),
                            setResultsByExerciseResultId =
                                mapOf(
                                    70L to listOf(setResult(id = 700L, exerciseResultId = 70L, reps = 5)),
                                ),
                        ),
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            viewModel.selectWorkout(
                viewModel.uiState.value
                    .recentWorkouts
                    .single(),
            )

            assertEquals(
                7L,
                viewModel.uiState.value
                    .selectedWorkoutDetail
                    ?.runId,
            )
            assertEquals(
                "Squat",
                viewModel.uiState.value.selectedWorkoutDetail
                    ?.exercises
                    ?.single()
                    ?.exerciseName,
            )
        }

    @Test
    fun editingHistoricalNotesTrimsTextBeforeDelegatingToSource() =
        runTest {
            val source = FakeResultsSource()
            val viewModel = ResultsViewModel(source = source, coroutineScope = viewModelScope())
            advanceUntilIdle()

            viewModel.updateExerciseNotes(exerciseResultId = 10L, notes = "  Form cue  ")
            viewModel.updateSetNotes(setId = 20L, notes = "  RPE 8  ")
            advanceUntilIdle()

            assertEquals(listOf(10L to "Form cue"), source.exerciseNoteUpdates)
            assertEquals(listOf(20L to "RPE 8"), source.setNoteUpdates)
        }

    @Test
    fun editingHistoricalWorkoutNotesTrimsTextBeforeDelegatingToSource() =
        runTest {
            val source = FakeResultsSource()
            val viewModel = ResultsViewModel(source = source, coroutineScope = viewModelScope())
            advanceUntilIdle()

            viewModel.updateWorkoutNotes(workoutRunId = 7L, notes = "  Great session  ")
            advanceUntilIdle()

            assertEquals(listOf(7L to "Great session"), source.workoutNoteUpdates)
        }

    @Test
    fun strengthStatsUseTotalVolumeAndCompareLatestWorkoutToPreviousWorkout() =
        runTest {
            val viewModel =
                ResultsViewModel(
                    source =
                        FakeResultsSource(
                            workoutRuns =
                                listOf(
                                    workoutRun(
                                        id = 1,
                                        scheduleName = "Workout 1",
                                        startedAt = 1_000L,
                                        finishedAt = 61_000L,
                                    ),
                                    workoutRun(
                                        id = 2,
                                        scheduleName = "Workout 2",
                                        startedAt = 2_000L,
                                        finishedAt = 62_000L,
                                    ),
                                    workoutRun(id = 3, startedAt = 3_000L, finishedAt = 63_000L),
                                ),
                            exerciseResultsByRunId =
                                mapOf(
                                    1L to
                                        listOf(
                                            exerciseResult(
                                                id = 10,
                                                workoutRunId = 1,
                                                exerciseId = 5,
                                                exerciseName = "Bench press",
                                                trackingMode = TrackingMode.Strength,
                                            ),
                                        ),
                                    2L to
                                        listOf(
                                            exerciseResult(
                                                id = 20,
                                                workoutRunId = 2,
                                                exerciseId = 5,
                                                exerciseName = "Bench press",
                                                trackingMode = TrackingMode.Strength,
                                            ),
                                        ),
                                    3L to
                                        listOf(
                                            exerciseResult(
                                                id = 30,
                                                workoutRunId = 3,
                                                exerciseId = 5,
                                                exerciseName = "Bench press",
                                                trackingMode = TrackingMode.Strength,
                                            ),
                                        ),
                                ),
                            setResultsByExerciseResultId =
                                mapOf(
                                    10L to listOf(setResult(id = 100, exerciseResultId = 10, reps = 8, weight = 50.0)),
                                    20L to
                                        listOf(
                                            setResult(id = 200, exerciseResultId = 20, reps = 5, weight = 80.0),
                                            setResult(id = 201, exerciseResultId = 20, reps = 3, weight = null),
                                        ),
                                    30L to listOf(setResult(id = 300, exerciseResultId = 30, reps = 6, weight = 100.0)),
                                ),
                        ),
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            assertEquals(
                ExerciseStatsUiModel(
                    exerciseId = 5,
                    exerciseName = "Bench press",
                    trackingMode = TrackingMode.Strength,
                    headline = "600 volume",
                    progressDelta = "+200 volume",
                    lastTrainedAt = 63_000L,
                    progressScore = 50.0,
                    personalRecordLabel = "PR",
                    trend = TrendDirection.Up,
                    points =
                        listOf(
                            ChartPoint(label = "Jan 1", value = 400.0),
                            ChartPoint(label = "Jan 1", value = 400.0),
                            ChartPoint(label = "Jan 1", value = 600.0),
                        ),
                ),
                viewModel.uiState.value.exerciseStats
                    .single(),
            )
        }

    @Test
    fun exerciseStatsUseFinishedWorkoutOrderWhenStartAndFinishOrderDiffer() =
        runTest {
            val viewModel =
                ResultsViewModel(
                    source =
                        FakeResultsSource(
                            workoutRuns =
                                listOf(
                                    workoutRun(id = 1, startedAt = 1_000L, finishedAt = 121_000L),
                                    workoutRun(id = 2, startedAt = 2_000L, finishedAt = 62_000L),
                                ),
                            exerciseResultsByRunId =
                                mapOf(
                                    1L to
                                        listOf(
                                            exerciseResult(
                                                id = 10,
                                                workoutRunId = 1,
                                                exerciseId = 5,
                                                exerciseName = "Bench press",
                                                trackingMode = TrackingMode.Strength,
                                            ),
                                        ),
                                    2L to
                                        listOf(
                                            exerciseResult(
                                                id = 20,
                                                workoutRunId = 2,
                                                exerciseId = 5,
                                                exerciseName = "Bench press",
                                                trackingMode = TrackingMode.Strength,
                                            ),
                                        ),
                                ),
                            setResultsByExerciseResultId =
                                mapOf(
                                    10L to listOf(setResult(id = 100, exerciseResultId = 10, reps = 6, weight = 100.0)),
                                    20L to listOf(setResult(id = 200, exerciseResultId = 20, reps = 9, weight = 100.0)),
                                ),
                        ),
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            assertEquals(
                ExerciseStatsUiModel(
                    exerciseId = 5,
                    exerciseName = "Bench press",
                    trackingMode = TrackingMode.Strength,
                    headline = "600 volume",
                    progressDelta = "-300 volume",
                    lastTrainedAt = 121_000L,
                    progressScore = -33.33333333333333,
                    trend = TrendDirection.Down,
                    points =
                        listOf(
                            ChartPoint(label = "Jan 1", value = 900.0),
                            ChartPoint(label = "Jan 1", value = 600.0),
                        ),
                ),
                viewModel.uiState.value.exerciseStats
                    .single(),
            )
        }

    @Test
    fun bodyweightStatsUseTotalRepsAndFlatTrend() =
        runTest {
            val viewModel =
                ResultsViewModel(
                    source =
                        FakeResultsSource(
                            workoutRuns =
                                listOf(
                                    workoutRun(
                                        id = 1,
                                        scheduleName = "Workout 1",
                                        startedAt = 1_000L,
                                        finishedAt = 61_000L,
                                    ),
                                    workoutRun(
                                        id = 2,
                                        scheduleName = "Workout 2",
                                        startedAt = 2_000L,
                                        finishedAt = 62_000L,
                                    ),
                                ),
                            exerciseResultsByRunId =
                                mapOf(
                                    1L to
                                        listOf(
                                            exerciseResult(
                                                id = 10,
                                                workoutRunId = 1,
                                                exerciseId = 7,
                                                exerciseName = "Push-up",
                                                trackingMode = TrackingMode.Bodyweight,
                                            ),
                                        ),
                                    2L to
                                        listOf(
                                            exerciseResult(
                                                id = 20,
                                                workoutRunId = 2,
                                                exerciseId = 7,
                                                exerciseName = "Push-up",
                                                trackingMode = TrackingMode.Bodyweight,
                                            ),
                                        ),
                                ),
                            setResultsByExerciseResultId =
                                mapOf(
                                    10L to listOf(setResult(id = 100, exerciseResultId = 10, reps = 12)),
                                    20L to listOf(setResult(id = 200, exerciseResultId = 20, reps = 12)),
                                ),
                        ),
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            val stats =
                viewModel.uiState.value.exerciseStats
                    .single()

            assertEquals(TrendDirection.Flat, stats.trend)
            assertEquals("12 reps", stats.headline)
        }

    @Test
    fun timedStatsIncludeDurationAndOptionalDistance() =
        runTest {
            val viewModel =
                ResultsViewModel(
                    source =
                        FakeResultsSource(
                            workoutRuns =
                                listOf(
                                    workoutRun(id = 1, startedAt = 1_000L, finishedAt = 61_000L),
                                    workoutRun(id = 2, startedAt = 2_000L, finishedAt = 62_000L),
                                ),
                            exerciseResultsByRunId =
                                mapOf(
                                    1L to
                                        listOf(
                                            exerciseResult(
                                                id = 10,
                                                workoutRunId = 1,
                                                exerciseId = 9,
                                                exerciseName = "Run",
                                                trackingMode = TrackingMode.Timed,
                                            ),
                                        ),
                                    2L to
                                        listOf(
                                            exerciseResult(
                                                id = 20,
                                                workoutRunId = 2,
                                                exerciseId = 9,
                                                exerciseName = "Run",
                                                trackingMode = TrackingMode.Timed,
                                            ),
                                        ),
                                ),
                            setResultsByExerciseResultId =
                                mapOf(
                                    10L to listOf(setResult(id = 100, exerciseResultId = 10, durationSeconds = 600L)),
                                    20L to
                                        listOf(
                                            setResult(
                                                id = 200,
                                                exerciseResultId = 20,
                                                durationSeconds = 900L,
                                                distance = 2.5,
                                            ),
                                        ),
                                ),
                        ),
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            assertEquals(
                ExerciseStatsUiModel(
                    exerciseId = 9,
                    exerciseName = "Run",
                    trackingMode = TrackingMode.Timed,
                    headline = "900 sec / 2.5 distance",
                    progressDelta = "+5m 0s",
                    lastTrainedAt = 62_000L,
                    progressScore = 50.0,
                    personalRecordLabel = "PR",
                    trend = TrendDirection.Up,
                    points =
                        listOf(
                            ChartPoint(label = "Jan 1", value = 600.0),
                            ChartPoint(label = "Jan 1", value = 900.0),
                        ),
                ),
                viewModel.uiState.value.exerciseStats
                    .single(),
            )
        }

    @Test
    fun strengthStatsIgnoreCompletedRowsWithIncompleteStrengthMetrics() =
        runTest {
            val viewModel =
                ResultsViewModel(
                    source =
                        FakeResultsSource(
                            workoutRuns =
                                listOf(
                                    workoutRun(
                                        id = 1L,
                                        startedAt = 1_000L,
                                        finishedAt = 61_000L,
                                    ),
                                ),
                            exerciseResultsByRunId =
                                mapOf(
                                    1L to listOf(strengthExerciseResult(id = 10L, workoutRunId = 1L)),
                                ),
                            setResultsByExerciseResultId =
                                mapOf(
                                    10L to
                                        listOf(
                                            setResult(id = 100L, exerciseResultId = 10L, reps = 5, weight = 80.0),
                                            setResult(id = 101L, exerciseResultId = 10L, weight = 500.0),
                                            setResult(id = 102L, exerciseResultId = 10L, reps = 20),
                                        ),
                                ),
                        ),
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            val detail =
                viewModel.uiState.value.exerciseDetails
                    .single()

            assertEquals("80", detail.summaryMetrics.first { it.label == "Max weight" }.value)
            assertEquals("5", detail.summaryMetrics.first { it.label == "Total reps" }.value)
            assertEquals("400", detail.summaryMetrics.first { it.label == "Total volume" }.value)
        }

    @Test
    fun strengthDetailStatsUseCompletedSetsAndExposeLatestBestAverageTrendAndRecentSets() =
        runTest {
            val viewModel =
                ResultsViewModel(
                    source =
                        FakeResultsSource(
                            workoutRuns =
                                listOf(
                                    workoutRun(
                                        id = 1,
                                        scheduleName = "Workout 1",
                                        startedAt = 1_000L,
                                        finishedAt = 61_000L,
                                    ),
                                    workoutRun(
                                        id = 2,
                                        scheduleName = "Workout 2",
                                        startedAt = 2_000L,
                                        finishedAt = 62_000L,
                                    ),
                                ),
                            exerciseResultsByRunId =
                                mapOf(
                                    1L to listOf(strengthExerciseResult(id = 10, workoutRunId = 1)),
                                    2L to listOf(strengthExerciseResult(id = 20, workoutRunId = 2)),
                                ),
                            setResultsByExerciseResultId =
                                mapOf(
                                    10L to
                                        listOf(
                                            setResult(id = 100, exerciseResultId = 10, reps = 5, weight = 80.0),
                                            setResult(
                                                id = 101,
                                                exerciseResultId = 10,
                                                reps = 1,
                                                weight = 500.0,
                                                isCompleted = false,
                                            ),
                                        ),
                                    20L to
                                        listOf(
                                            setResult(id = 200, exerciseResultId = 20, reps = 6, weight = 90.0),
                                            setResult(id = 201, exerciseResultId = 20, reps = 4, weight = 100.0),
                                        ),
                                ),
                        ),
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            val stat =
                viewModel.uiState.value.exerciseStats
                    .single()
            viewModel.selectExerciseStats(stat)

            val detail = viewModel.uiState.value.selectedExerciseDetail
            assertStrengthDetailAnalytics(detail)
            assertEquals(
                listOf(
                    RecentSetUiModel(label = "Workout 2 Set 1", value = "6 x 90"),
                    RecentSetUiModel(label = "Workout 2 Set 2", value = "4 x 100"),
                    RecentSetUiModel(label = "Workout 1 Set 1", value = "5 x 80"),
                ),
                detail?.recentSets,
            )
            assertEquals(TrendDirection.Up, stat.trend)
        }

    @Test
    fun bodyweightDetailStatsUseTotalRepsBestSetAndAverageRepsPerWorkout() =
        runTest {
            val viewModel =
                ResultsViewModel(
                    source =
                        FakeResultsSource(
                            workoutRuns =
                                listOf(
                                    workoutRun(id = 1, startedAt = 1_000L, finishedAt = 61_000L),
                                    workoutRun(id = 2, startedAt = 2_000L, finishedAt = 62_000L),
                                ),
                            exerciseResultsByRunId =
                                mapOf(
                                    1L to listOf(bodyweightExerciseResult(id = 10, workoutRunId = 1)),
                                    2L to listOf(bodyweightExerciseResult(id = 20, workoutRunId = 2)),
                                ),
                            setResultsByExerciseResultId =
                                mapOf(
                                    10L to listOf(setResult(id = 100, exerciseResultId = 10, reps = 8)),
                                    20L to
                                        listOf(
                                            setResult(id = 200, exerciseResultId = 20, reps = 10),
                                            setResult(id = 201, exerciseResultId = 20, reps = 12),
                                        ),
                                ),
                        ),
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            val selectedExercise =
                viewModel.uiState.value.exerciseStats
                    .single()
            viewModel.selectExerciseStats(selectedExercise)

            assertEquals(
                "22 reps",
                viewModel.uiState.value.selectedExerciseDetail
                    ?.latest,
            )
            assertEquals(
                "12 reps",
                viewModel.uiState.value.selectedExerciseDetail
                    ?.best,
            )
            assertEquals(
                "22 best reps",
                viewModel.uiState.value.selectedExerciseDetail
                    ?.bestSession,
            )
            assertEquals(
                null,
                viewModel.uiState.value.selectedExerciseDetail
                    ?.bestEstimatedOneRepMax,
            )
            assertEquals(
                "15 avg reps",
                viewModel.uiState.value.selectedExerciseDetail
                    ?.average,
            )
            assertEquals(
                "+14 reps",
                viewModel.uiState.value.selectedExerciseDetail
                    ?.progressDelta,
            )
            assertEquals(
                "PR",
                viewModel.uiState.value.selectedExerciseDetail
                    ?.personalRecordLabel,
            )
            assertEquals(
                2,
                viewModel.uiState.value.selectedExerciseDetail
                    ?.totalSessions,
            )
            assertEquals(
                3,
                viewModel.uiState.value.selectedExerciseDetail
                    ?.totalSets,
            )
            assertEquals(
                listOf(
                    SummaryMetricUiModel(label = "Total reps", value = "30"),
                    SummaryMetricUiModel(label = "Best set", value = "12 reps"),
                    SummaryMetricUiModel(label = "Average", value = "15 avg reps"),
                    SummaryMetricUiModel(label = "Frequency", value = "2 sessions"),
                ),
                viewModel.uiState.value.selectedExerciseDetail
                    ?.summaryMetrics,
            )
        }

    @Test
    fun timedDetailStatsUseTotalDurationBestDurationAverageAndDistanceWhenPresent() =
        runTest {
            val viewModel =
                ResultsViewModel(
                    source =
                        FakeResultsSource(
                            workoutRuns =
                                listOf(
                                    workoutRun(id = 1, startedAt = 1_000L, finishedAt = 61_000L),
                                    workoutRun(id = 2, startedAt = 2_000L, finishedAt = 62_000L),
                                ),
                            exerciseResultsByRunId =
                                mapOf(
                                    1L to listOf(timedExerciseResult(id = 10, workoutRunId = 1)),
                                    2L to listOf(timedExerciseResult(id = 20, workoutRunId = 2)),
                                ),
                            setResultsByExerciseResultId =
                                mapOf(
                                    10L to listOf(setResult(id = 100, exerciseResultId = 10, durationSeconds = 300L)),
                                    20L to
                                        listOf(
                                            setResult(
                                                id = 200,
                                                exerciseResultId = 20,
                                                durationSeconds = 600L,
                                                distance = 2.5,
                                            ),
                                            setResult(
                                                id = 201,
                                                exerciseResultId = 20,
                                                durationSeconds = 120L,
                                                distance = 0.5,
                                            ),
                                        ),
                                ),
                        ),
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            val selectedExercise =
                viewModel.uiState.value.exerciseStats
                    .single()
            viewModel.selectExerciseStats(selectedExercise)

            assertEquals(
                "12m 0s / 3 distance",
                viewModel.uiState.value.selectedExerciseDetail
                    ?.latest,
            )
            assertEquals(
                "10m 0s / 2.5 distance",
                viewModel.uiState.value.selectedExerciseDetail
                    ?.best,
            )
            assertEquals(
                "12m 0s best",
                viewModel.uiState.value.selectedExerciseDetail
                    ?.bestSession,
            )
            assertEquals(
                null,
                viewModel.uiState.value.selectedExerciseDetail
                    ?.bestEstimatedOneRepMax,
            )
            assertEquals(
                "8m 30s avg",
                viewModel.uiState.value.selectedExerciseDetail
                    ?.average,
            )
            assertEquals(
                "+7m 0s",
                viewModel.uiState.value.selectedExerciseDetail
                    ?.progressDelta,
            )
            assertEquals(
                "PR",
                viewModel.uiState.value.selectedExerciseDetail
                    ?.personalRecordLabel,
            )
            assertEquals(
                2,
                viewModel.uiState.value.selectedExerciseDetail
                    ?.totalSessions,
            )
            assertEquals(
                3,
                viewModel.uiState.value.selectedExerciseDetail
                    ?.totalSets,
            )
            assertEquals(
                listOf(
                    SummaryMetricUiModel(label = "Total time", value = "17m 0s"),
                    SummaryMetricUiModel(label = "Total distance", value = "3"),
                    SummaryMetricUiModel(label = "Avg pace", value = "5m 40s / distance"),
                    SummaryMetricUiModel(label = "Best set", value = "10m 0s / 2.5 distance"),
                    SummaryMetricUiModel(label = "Frequency", value = "2 sessions"),
                ),
                viewModel.uiState.value.selectedExerciseDetail
                    ?.summaryMetrics,
            )
        }

    @Test
    fun exerciseDetailExposesRecentDistinctExerciseNotes() =
        runTest {
            val viewModel =
                ResultsViewModel(
                    source =
                        FakeResultsSource(
                            workoutRuns =
                                listOf(
                                    workoutRun(id = 1, startedAt = 1_000L, finishedAt = 61_000L),
                                    workoutRun(id = 2, startedAt = 2_000L, finishedAt = 62_000L),
                                ),
                            exerciseResultsByRunId =
                                mapOf(
                                    1L to
                                        listOf(
                                            exerciseResult(
                                                id = 10,
                                                workoutRunId = 1,
                                                exerciseId = 5L,
                                                notes = "Keep elbows tucked",
                                            ),
                                        ),
                                    2L to
                                        listOf(
                                            exerciseResult(
                                                id = 20,
                                                workoutRunId = 2,
                                                exerciseId = 5L,
                                                notes = "Felt strong",
                                            ),
                                        ),
                                ),
                            setResultsByExerciseResultId =
                                mapOf(
                                    10L to listOf(setResult(id = 100, exerciseResultId = 10, reps = 8)),
                                    20L to listOf(setResult(id = 200, exerciseResultId = 20, reps = 10)),
                                ),
                        ),
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            viewModel.selectExerciseStats(
                viewModel.uiState.value.exerciseStats
                    .single(),
            )

            assertEquals(
                listOf("Felt strong", "Keep elbows tucked"),
                viewModel.uiState.value.selectedExerciseDetail
                    ?.notes,
            )
        }

    @Test
    fun exerciseDetailLabelsRecentNotesWithDateAndSource() =
        runTest {
            val viewModel =
                ResultsViewModel(
                    source =
                        FakeResultsSource(
                            workoutRuns =
                                listOf(
                                    workoutRun(
                                        id = 1,
                                        scheduleName = "Push Day",
                                        startedAt = 1_000L,
                                        finishedAt = 86_400_000L,
                                    ),
                                ),
                            exerciseResultsByRunId =
                                mapOf(
                                    1L to
                                        listOf(
                                            exerciseResult(
                                                id = 10,
                                                workoutRunId = 1,
                                                exerciseId = 5L,
                                                notes = "Keep elbows tucked",
                                            ),
                                        ),
                                ),
                            setResultsByExerciseResultId =
                                mapOf(
                                    10L to
                                        listOf(
                                            setResult(
                                                id = 100,
                                                exerciseResultId = 10,
                                                reps = 8,
                                                notes = "RPE 8",
                                            ),
                                        ),
                                ),
                        ),
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            val selectedExercise =
                viewModel.uiState.value.exerciseStats
                    .single()
            viewModel.selectExerciseStats(selectedExercise)

            val noteDetails =
                viewModel.uiState.value.selectedExerciseDetail
                    ?.noteDetails

            assertEquals(
                listOf(
                    ExerciseNoteUiModel(
                        dateLabel = "Jan 2",
                        contextLabel = "Push Day · Exercise",
                        text = "Keep elbows tucked",
                    ),
                    ExerciseNoteUiModel(
                        dateLabel = "Jan 2",
                        contextLabel = "Push Day · Set 1",
                        text = "RPE 8",
                    ),
                ),
                noteDetails,
            )
        }

    @Test
    fun exerciseDetailIncludesNotesFromIncompleteSets() =
        runTest {
            val viewModel =
                ResultsViewModel(
                    source =
                        FakeResultsSource(
                            workoutRuns =
                                listOf(
                                    workoutRun(
                                        id = 1,
                                        scheduleName = "Push Day",
                                        startedAt = 1_000L,
                                        finishedAt = 86_400_000L,
                                    ),
                                ),
                            exerciseResultsByRunId =
                                mapOf(
                                    1L to listOf(strengthExerciseResult(id = 10, workoutRunId = 1)),
                                ),
                            setResultsByExerciseResultId =
                                mapOf(
                                    10L to
                                        listOf(
                                            setResult(id = 100, exerciseResultId = 10, reps = 8, weight = 60.0),
                                            setResult(
                                                id = 101,
                                                exerciseResultId = 10,
                                                setOrder = 1,
                                                notes = "Stopped early",
                                                isCompleted = false,
                                            ),
                                        ),
                                ),
                        ),
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            viewModel.selectExerciseStats(
                viewModel.uiState.value.exerciseStats
                    .single(),
            )

            assertEquals(
                listOf(
                    ExerciseNoteUiModel(
                        dateLabel = "Jan 2",
                        contextLabel = "Push Day · Set 2",
                        text = "Stopped early",
                    ),
                ),
                viewModel.uiState.value.selectedExerciseDetail
                    ?.noteDetails,
            )
        }

    @Test
    fun timedExerciseSummaryIncludesAveragePaceWhenDistanceIsLogged() =
        runTest {
            val viewModel =
                ResultsViewModel(
                    source =
                        FakeResultsSource(
                            workoutRuns =
                                listOf(
                                    workoutRun(id = 1, startedAt = 1_000L, finishedAt = 61_000L),
                                ),
                            exerciseResultsByRunId =
                                mapOf(
                                    1L to listOf(timedExerciseResult(id = 10, workoutRunId = 1)),
                                ),
                            setResultsByExerciseResultId =
                                mapOf(
                                    10L to
                                        listOf(
                                            setResult(
                                                id = 100,
                                                exerciseResultId = 10,
                                                durationSeconds = 300L,
                                                distance = 1.0,
                                            ),
                                            setResult(
                                                id = 101,
                                                exerciseResultId = 10,
                                                setOrder = 1,
                                                durationSeconds = 300L,
                                                distance = 1.5,
                                            ),
                                        ),
                                ),
                        ),
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            viewModel.selectExerciseStats(
                viewModel.uiState.value.exerciseStats
                    .single(),
            )

            assertEquals(
                SummaryMetricUiModel(label = "Avg pace", value = "4m 0s / distance"),
                viewModel.uiState.value.selectedExerciseDetail
                    ?.summaryMetrics
                    ?.single { metric -> metric.label == "Avg pace" },
            )
        }

    @Test
    fun sameExerciseIdGroupsRenamedHistoryIntoOneStatAndUsesLatestSnapshotForDisplay() =
        runTest {
            val viewModel =
                ResultsViewModel(
                    source =
                        FakeResultsSource(
                            workoutRuns =
                                listOf(
                                    workoutRun(id = 1, startedAt = 1_000L, finishedAt = 61_000L),
                                    workoutRun(id = 2, startedAt = 2_000L, finishedAt = 62_000L),
                                ),
                            exerciseResultsByRunId =
                                mapOf(
                                    1L to
                                        listOf(
                                            exerciseResult(
                                                id = 10,
                                                workoutRunId = 1,
                                                exerciseId = 5L,
                                                exerciseName = "Bench press",
                                                trackingMode = TrackingMode.Strength,
                                            ),
                                        ),
                                    2L to
                                        listOf(
                                            exerciseResult(
                                                id = 20,
                                                workoutRunId = 2,
                                                exerciseId = 5L,
                                                exerciseName = "Barbell bench press",
                                                trackingMode = TrackingMode.Strength,
                                            ),
                                        ),
                                ),
                            setResultsByExerciseResultId =
                                mapOf(
                                    10L to listOf(setResult(id = 100, exerciseResultId = 10, reps = 5, weight = 80.0)),
                                    20L to listOf(setResult(id = 200, exerciseResultId = 20, reps = 6, weight = 90.0)),
                                ),
                        ),
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            assertEquals(1, viewModel.uiState.value.exerciseStats.size)
            val stat =
                viewModel.uiState.value.exerciseStats
                    .single()
            assertEquals("Barbell bench press", stat.exerciseName)
            assertEquals(
                listOf(
                    ChartPoint(label = "Jan 1", value = 400.0),
                    ChartPoint(label = "Jan 1", value = 540.0),
                ),
                stat.points,
            )
            viewModel.selectExerciseStats(stat)
            assertEquals(
                2,
                viewModel.uiState.value.selectedExerciseDetail
                    ?.totalSessions,
            )
        }

    @Test
    fun nullExerciseIdFallbackGroupsBySnapshotNameAndTrackingMode() =
        runTest {
            val viewModel =
                ResultsViewModel(
                    source =
                        FakeResultsSource(
                            workoutRuns =
                                listOf(
                                    workoutRun(id = 1, startedAt = 1_000L, finishedAt = 61_000L),
                                    workoutRun(id = 2, startedAt = 2_000L, finishedAt = 62_000L),
                                    workoutRun(id = 3, startedAt = 3_000L, finishedAt = 63_000L),
                                ),
                            exerciseResultsByRunId =
                                mapOf(
                                    1L to
                                        listOf(
                                            exerciseResult(
                                                id = 10,
                                                workoutRunId = 1,
                                                exerciseId = null,
                                                exerciseName = "Deleted squat",
                                                trackingMode = TrackingMode.Strength,
                                            ),
                                        ),
                                    2L to
                                        listOf(
                                            exerciseResult(
                                                id = 20,
                                                workoutRunId = 2,
                                                exerciseId = null,
                                                exerciseName = "Deleted squat",
                                                trackingMode = TrackingMode.Strength,
                                            ),
                                        ),
                                    3L to
                                        listOf(
                                            exerciseResult(
                                                id = 30,
                                                workoutRunId = 3,
                                                exerciseId = null,
                                                exerciseName = "Deleted squat",
                                                trackingMode = TrackingMode.Bodyweight,
                                            ),
                                        ),
                                ),
                            setResultsByExerciseResultId =
                                mapOf(
                                    10L to listOf(setResult(id = 100, exerciseResultId = 10, reps = 5, weight = 80.0)),
                                    20L to listOf(setResult(id = 200, exerciseResultId = 20, reps = 6, weight = 90.0)),
                                    30L to listOf(setResult(id = 300, exerciseResultId = 30, reps = 20)),
                                ),
                        ),
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            val strengthStat =
                viewModel.uiState.value.exerciseStats.single { stat ->
                    stat.exerciseName == "Deleted squat" &&
                        stat.trackingMode == TrackingMode.Strength
                }
            assertEquals(
                listOf(
                    ChartPoint(label = "Jan 1", value = 400.0),
                    ChartPoint(label = "Jan 1", value = 540.0),
                ),
                strengthStat.points,
            )
            assertEquals(2, viewModel.uiState.value.exerciseStats.size)
        }

    @Test
    fun deletedSnapshotsWithSameNameAndModeUseScheduleSlotAsSeparateStatsGroups() {
        val runDetails =
            listOf(
                ResultRunDetail(
                    workoutRun = workoutRun(id = 1, startedAt = 1_000L, finishedAt = 61_000L),
                    exerciseDetails =
                        listOf(
                            ResultExerciseDetail(
                                exerciseResult =
                                    exerciseResult(
                                        id = 10,
                                        workoutRunId = 1,
                                        exerciseId = null,
                                        exerciseName = "Deleted movement",
                                        trackingMode = TrackingMode.Strength,
                                    ).copy(sortOrder = 0),
                                setResults =
                                    listOf(
                                        setResult(id = 100, exerciseResultId = 10, reps = 5, weight = 80.0),
                                    ),
                            ),
                        ),
                ),
                ResultRunDetail(
                    workoutRun = workoutRun(id = 2, startedAt = 2_000L, finishedAt = 62_000L),
                    exerciseDetails =
                        listOf(
                            ResultExerciseDetail(
                                exerciseResult =
                                    exerciseResult(
                                        id = 20,
                                        workoutRunId = 2,
                                        exerciseId = null,
                                        exerciseName = "Deleted movement",
                                        trackingMode = TrackingMode.Strength,
                                    ).copy(sortOrder = 1),
                                setResults =
                                    listOf(
                                        setResult(id = 200, exerciseResultId = 20, reps = 6, weight = 90.0),
                                    ),
                            ),
                        ),
                ),
            )

        assertEquals(2, runDetails.toExerciseStats().size)
    }

    @Test
    fun selectedExerciseDetailStaysSelectedAndUpdatesAfterResultsRefresh() =
        runTest {
            val source =
                FakeResultsSource(
                    workoutRuns = listOf(workoutRun(id = 1, startedAt = 1_000L, finishedAt = 61_000L)),
                    exerciseResultsByRunId =
                        mapOf(
                            1L to listOf(strengthExerciseResult(id = 10, workoutRunId = 1)),
                        ),
                    setResultsByExerciseResultId =
                        mapOf(
                            10L to listOf(setResult(id = 100, exerciseResultId = 10, reps = 5, weight = 80.0)),
                        ),
                )
            val viewModel =
                ResultsViewModel(
                    source = source,
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()
            viewModel.selectExerciseStats(
                viewModel.uiState.value.exerciseStats
                    .single(),
            )

            source.replace(
                workoutRuns =
                    listOf(
                        workoutRun(id = 1, startedAt = 1_000L, finishedAt = 61_000L),
                        workoutRun(id = 2, startedAt = 2_000L, finishedAt = 62_000L),
                    ),
                exerciseResultsByRunId =
                    mapOf(
                        1L to listOf(strengthExerciseResult(id = 10, workoutRunId = 1)),
                        2L to
                            listOf(
                                exerciseResult(
                                    id = 20,
                                    workoutRunId = 2,
                                    exerciseId = 5L,
                                    exerciseName = "Barbell bench press",
                                    trackingMode = TrackingMode.Strength,
                                ),
                            ),
                    ),
                setResultsByExerciseResultId =
                    mapOf(
                        10L to listOf(setResult(id = 100, exerciseResultId = 10, reps = 5, weight = 80.0)),
                        20L to listOf(setResult(id = 200, exerciseResultId = 20, reps = 6, weight = 90.0)),
                    ),
            )
            advanceUntilIdle()

            assertEquals(
                "Barbell bench press",
                viewModel.uiState.value.selectedExerciseDetail
                    ?.exerciseName,
            )
            assertEquals(
                "540 volume",
                viewModel.uiState.value.selectedExerciseDetail
                    ?.latest,
            )
        }

    @Test
    fun selectedExerciseDetailUpdatesWhenOnlySetResultsChange() =
        runTest {
            val source =
                FakeResultsSource(
                    workoutRuns = listOf(workoutRun(id = 1, startedAt = 1_000L, finishedAt = 61_000L)),
                    exerciseResultsByRunId =
                        mapOf(
                            1L to listOf(strengthExerciseResult(id = 10, workoutRunId = 1)),
                        ),
                    setResultsByExerciseResultId =
                        mapOf(
                            10L to listOf(setResult(id = 100, exerciseResultId = 10, reps = 5, weight = 80.0)),
                        ),
                )
            val viewModel =
                ResultsViewModel(
                    source = source,
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()
            viewModel.selectExerciseStats(
                viewModel.uiState.value.exerciseStats
                    .single(),
            )

            source.replaceResultData(
                exerciseResultsByRunId =
                    mapOf(
                        1L to listOf(strengthExerciseResult(id = 10, workoutRunId = 1)),
                    ),
                setResultsByExerciseResultId =
                    mapOf(
                        10L to listOf(setResult(id = 100, exerciseResultId = 10, reps = 6, weight = 100.0)),
                    ),
            )
            source.emitResultChange()
            advanceUntilIdle()

            assertEquals(
                "600 volume",
                viewModel.uiState.value.selectedExerciseDetail
                    ?.latest,
            )
        }

    @Test
    fun detailForUsesSameExerciseIdBeforeFallbackAndExcludesIncompleteRuns() {
        val detail =
            listOf(
                ResultRunDetail(
                    workoutRun = workoutRun(id = 1, startedAt = 1_000L, finishedAt = 61_000L),
                    exerciseDetails =
                        listOf(
                            ResultExerciseDetail(
                                exerciseResult =
                                    exerciseResult(
                                        id = 10,
                                        workoutRunId = 1,
                                        exerciseId = 5L,
                                        exerciseName = "Old bench",
                                        trackingMode = TrackingMode.Strength,
                                    ),
                                setResults =
                                    listOf(
                                        setResult(id = 100, exerciseResultId = 10, reps = 5, weight = 80.0),
                                    ),
                            ),
                        ),
                ),
                ResultRunDetail(
                    workoutRun = workoutRun(id = 2, startedAt = 2_000L, finishedAt = 62_000L),
                    exerciseDetails =
                        listOf(
                            ResultExerciseDetail(
                                exerciseResult =
                                    exerciseResult(
                                        id = 20,
                                        workoutRunId = 2,
                                        exerciseId = null,
                                        exerciseName = "Bench press",
                                        trackingMode = TrackingMode.Strength,
                                    ),
                                setResults =
                                    listOf(
                                        setResult(id = 200, exerciseResultId = 20, reps = 20, weight = 200.0),
                                    ),
                            ),
                        ),
                ),
                ResultRunDetail(
                    workoutRun = workoutRun(id = 3, startedAt = 3_000L, finishedAt = null),
                    exerciseDetails =
                        listOf(
                            ResultExerciseDetail(
                                exerciseResult =
                                    exerciseResult(
                                        id = 30,
                                        workoutRunId = 3,
                                        exerciseId = 5L,
                                        exerciseName = "Bench press",
                                        trackingMode = TrackingMode.Strength,
                                    ),
                                setResults =
                                    listOf(
                                        setResult(id = 300, exerciseResultId = 30, reps = 30, weight = 300.0),
                                    ),
                            ),
                        ),
                ),
            ).detailFor(
                exerciseResult(
                    id = 40,
                    workoutRunId = 4,
                    exerciseId = 5L,
                    exerciseName = "Bench press",
                    trackingMode = TrackingMode.Strength,
                ),
            )

        assertEquals("400 volume", detail?.latest)
        assertEquals(1, detail?.totalSessions)
    }

    @Test
    fun detailForFallsBackBySnapshotWhenCurrentExerciseIdIsNull() {
        val detail =
            listOf(
                ResultRunDetail(
                    workoutRun = workoutRun(id = 1, startedAt = 1_000L, finishedAt = 61_000L),
                    exerciseDetails =
                        listOf(
                            ResultExerciseDetail(
                                exerciseResult =
                                    exerciseResult(
                                        id = 10,
                                        workoutRunId = 1,
                                        exerciseId = null,
                                        exerciseName = "Deleted plank",
                                        trackingMode = TrackingMode.Timed,
                                    ),
                                setResults =
                                    listOf(
                                        setResult(id = 100, exerciseResultId = 10, durationSeconds = 120L),
                                    ),
                            ),
                        ),
                ),
            ).detailFor(
                exerciseResult(
                    id = 20,
                    workoutRunId = 2,
                    exerciseId = null,
                    exerciseName = "Deleted plank",
                    trackingMode = TrackingMode.Timed,
                ),
            )

        assertEquals("2m 0s", detail?.latest)
        assertEquals(1, detail?.totalSessions)
    }

    @Test
    fun detailForCurrentNullExerciseIdFallsBackToHistoricalNonNullIdBySnapshot() {
        val detail =
            listOf(
                ResultRunDetail(
                    workoutRun = workoutRun(id = 1, startedAt = 1_000L, finishedAt = 61_000L),
                    exerciseDetails =
                        listOf(
                            ResultExerciseDetail(
                                exerciseResult =
                                    exerciseResult(
                                        id = 10,
                                        workoutRunId = 1,
                                        exerciseId = 44L,
                                        exerciseName = "Goblet squat",
                                        trackingMode = TrackingMode.Strength,
                                    ),
                                setResults =
                                    listOf(
                                        setResult(id = 100, exerciseResultId = 10, reps = 8, weight = 30.0),
                                    ),
                            ),
                        ),
                ),
            ).detailFor(
                exerciseResult(
                    id = 20,
                    workoutRunId = 2,
                    exerciseId = null,
                    exerciseName = "Goblet squat",
                    trackingMode = TrackingMode.Strength,
                ),
            )

        assertEquals("240 volume", detail?.latest)
        assertEquals(1, detail?.totalSessions)
    }

    @Test
    fun detailForCurrentNonNullExerciseIdFallsBackToDifferentHistoricalIdBySnapshotWhenSameIdMissing() {
        val detail =
            listOf(
                ResultRunDetail(
                    workoutRun = workoutRun(id = 1, startedAt = 1_000L, finishedAt = 61_000L),
                    exerciseDetails =
                        listOf(
                            ResultExerciseDetail(
                                exerciseResult =
                                    exerciseResult(
                                        id = 10,
                                        workoutRunId = 1,
                                        exerciseId = 44L,
                                        exerciseName = "Goblet squat",
                                        trackingMode = TrackingMode.Strength,
                                    ),
                                setResults =
                                    listOf(
                                        setResult(id = 100, exerciseResultId = 10, reps = 8, weight = 30.0),
                                    ),
                            ),
                        ),
                ),
            ).detailFor(
                exerciseResult(
                    id = 20,
                    workoutRunId = 2,
                    exerciseId = 99L,
                    exerciseName = "Goblet squat",
                    trackingMode = TrackingMode.Strength,
                ),
            )

        assertEquals("240 volume", detail?.latest)
        assertEquals(1, detail?.totalSessions)
    }
}

private fun assertStrengthDetailAnalytics(detail: ExerciseStatsDetailUiModel?) {
    assertEquals("940 volume", detail?.latest)
    assertEquals("6 x 90", detail?.best)
    assertEquals("940 best volume", detail?.bestSession)
    assertEquals("113.3 est. 1RM", detail?.bestEstimatedOneRepMax)
    assertEquals("670 avg volume", detail?.average)
    assertEquals("+540 volume", detail?.progressDelta)
    assertEquals("+135%", detail?.progressPercent)
    assertEquals("PR", detail?.personalRecordLabel)
    assertEquals(2, detail?.totalSessions)
    assertEquals(3, detail?.totalSets)
    assertEquals(
        ChartSummaryUiModel(
            rangeLabel = "Jan 1 - Jan 1",
            minLabel = "Min 400 volume",
            maxLabel = "Max 940 volume",
        ),
        detail?.chartSummary,
    )
    assertEquals(
        listOf(
            PersonalRecordUiModel(dateLabel = "Jan 1", metric = "Volume", value = "400 volume"),
            PersonalRecordUiModel(dateLabel = "Jan 1", metric = "Max weight", value = "80"),
            PersonalRecordUiModel(dateLabel = "Jan 1", metric = "Volume", value = "940 volume"),
            PersonalRecordUiModel(dateLabel = "Jan 1", metric = "Max weight", value = "100"),
        ),
        detail?.personalRecords,
    )
    assertEquals(
        listOf(
            SummaryMetricUiModel(label = "Max weight", value = "100"),
            SummaryMetricUiModel(label = "Total volume", value = "1340"),
            SummaryMetricUiModel(label = "Total reps", value = "15"),
            SummaryMetricUiModel(label = "Frequency", value = "2 sessions"),
        ),
        detail?.summaryMetrics,
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun TestScope.viewModelScope(): CoroutineScope =
    CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler))

private const val MILLIS_PER_DAY = 86_400_000L

private class FakeResultsSource(
    workoutRuns: List<WorkoutRunEntity> = emptyList(),
    private var exerciseResultsByRunId: Map<Long, List<ExerciseResultEntity>> = emptyMap(),
    private var setResultsByExerciseResultId: Map<Long, List<SetResultEntity>> = emptyMap(),
) : ResultsSource {
    private val workoutRuns = MutableStateFlow(workoutRuns)
    private val resultChanges = MutableSharedFlow<Unit>(replay = 1)
    val exerciseNoteUpdates = mutableListOf<Pair<Long, String?>>()
    val setNoteUpdates = mutableListOf<Pair<Long, String?>>()
    val workoutNoteUpdates = mutableListOf<Pair<Long, String?>>()

    init {
        resultChanges.tryEmit(Unit)
    }

    override fun observeWorkoutRuns(): Flow<List<WorkoutRunEntity>> = workoutRuns

    override fun observeResultChanges(): Flow<Unit> = resultChanges

    override suspend fun getExerciseResults(workoutRunId: Long): List<ExerciseResultEntity> =
        exerciseResultsByRunId[workoutRunId]
            .orEmpty()

    override suspend fun getSetResults(exerciseResultId: Long): List<SetResultEntity> =
        setResultsByExerciseResultId[exerciseResultId].orEmpty()

    override suspend fun updateExerciseNotes(
        exerciseResultId: Long,
        notes: String?,
    ) {
        exerciseNoteUpdates += exerciseResultId to notes
    }

    override suspend fun updateSetNotes(
        setId: Long,
        notes: String?,
    ) {
        setNoteUpdates += setId to notes
    }

    override suspend fun updateWorkoutNotes(
        workoutRunId: Long,
        notes: String?,
    ) {
        workoutNoteUpdates += workoutRunId to notes
    }

    fun replace(
        workoutRuns: List<WorkoutRunEntity>,
        exerciseResultsByRunId: Map<Long, List<ExerciseResultEntity>>,
        setResultsByExerciseResultId: Map<Long, List<SetResultEntity>>,
    ) {
        this.exerciseResultsByRunId = exerciseResultsByRunId
        this.setResultsByExerciseResultId = setResultsByExerciseResultId
        this.workoutRuns.value = workoutRuns
    }

    fun replaceResultData(
        exerciseResultsByRunId: Map<Long, List<ExerciseResultEntity>>,
        setResultsByExerciseResultId: Map<Long, List<SetResultEntity>>,
    ) {
        this.exerciseResultsByRunId = exerciseResultsByRunId
        this.setResultsByExerciseResultId = setResultsByExerciseResultId
    }

    fun emitResultChange() {
        resultChanges.tryEmit(Unit)
    }
}

private fun workoutRun(
    id: Long,
    scheduleName: String = "Workout",
    startedAt: Long,
    finishedAt: Long? = null,
    notes: String? = null,
): WorkoutRunEntity =
    WorkoutRunEntity(
        id = id,
        scheduleId = 1,
        scheduleNameSnapshot = scheduleName,
        startedAt = startedAt,
        finishedAt = finishedAt,
        notes = notes,
    )

private fun exerciseResult(
    id: Long,
    workoutRunId: Long,
    exerciseId: Long? = id,
    exerciseName: String = "Exercise",
    trackingMode: TrackingMode = TrackingMode.Bodyweight,
    sortOrder: Int = 0,
    notes: String? = null,
): ExerciseResultEntity =
    ExerciseResultEntity(
        id = id,
        workoutRunId = workoutRunId,
        exerciseId = exerciseId,
        exerciseNameSnapshot = exerciseName,
        trackingModeSnapshot = trackingMode.databaseValue,
        sortOrder = sortOrder,
        notes = notes,
    )

private fun strengthExerciseResult(
    id: Long,
    workoutRunId: Long,
): ExerciseResultEntity =
    exerciseResult(
        id = id,
        workoutRunId = workoutRunId,
        exerciseId = 5L,
        exerciseName = "Bench press",
        trackingMode = TrackingMode.Strength,
    )

private fun bodyweightExerciseResult(
    id: Long,
    workoutRunId: Long,
): ExerciseResultEntity =
    exerciseResult(
        id = id,
        workoutRunId = workoutRunId,
        exerciseId = 7L,
        exerciseName = "Push-up",
        trackingMode = TrackingMode.Bodyweight,
    )

private fun timedExerciseResult(
    id: Long,
    workoutRunId: Long,
): ExerciseResultEntity =
    exerciseResult(
        id = id,
        workoutRunId = workoutRunId,
        exerciseId = 9L,
        exerciseName = "Run",
        trackingMode = TrackingMode.Timed,
    )

@Suppress("LongParameterList")
private fun setResult(
    id: Long,
    exerciseResultId: Long,
    setOrder: Int = 0,
    reps: Int? = null,
    weight: Double? = null,
    durationSeconds: Long? = null,
    distance: Double? = null,
    notes: String? = null,
    isCompleted: Boolean = true,
): SetResultEntity =
    SetResultEntity(
        id = id,
        exerciseResultId = exerciseResultId,
        setOrder = setOrder,
        reps = reps,
        weight = weight,
        durationSeconds = durationSeconds,
        distance = distance,
        notes = notes,
        isCompleted = isCompleted,
    )
