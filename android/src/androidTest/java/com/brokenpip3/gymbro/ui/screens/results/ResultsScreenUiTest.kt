package com.brokenpip3.gymbro.ui.screens.results

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.brokenpip3.gymbro.domain.TrackingMode
import com.brokenpip3.gymbro.ui.theme.GymbroTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResultsScreenUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun exerciseStatsSearchFiltersRenderedRows() {
        composeRule.setContent {
            GymbroTheme {
                ResultsScreen(
                    uiState =
                        ResultsUiState(
                            exerciseStats =
                                listOf(
                                    exerciseStat(1L, "Back Squat"),
                                    exerciseStat(2L, "Tempo Run"),
                                ),
                        ),
                    modifier = Modifier,
                )
            }
        }

        composeRule.onNodeWithTag("exercise-stats-search").performTextInput("squat")

        composeRule.onNodeWithText("Back Squat").assertIsDisplayed()
        composeRule.onNodeWithTag("exercise-stats-filter-Strength").assertIsDisplayed()
    }

    @Test
    fun exerciseStatsFilterShowsOnlyTheSelectedTrackingMode() {
        composeRule.setContent {
            GymbroTheme {
                ResultsScreen(
                    uiState =
                        ResultsUiState(
                            exerciseStats =
                                listOf(
                                    exerciseStat(1L, "Back Squat"),
                                    exerciseStat(
                                        id = 2L,
                                        name = "Tempo Run",
                                        mode = TrackingMode.Timed,
                                    ),
                                ),
                        ),
                    modifier = Modifier,
                )
            }
        }

        composeRule.onNodeWithTag("exercise-stats-filter-Timed").performClick()

        composeRule.onNodeWithTag("exercise-stat-2").assertIsDisplayed()
        composeRule.onNodeWithTag("exercise-stat-1").assertDoesNotExist()
    }

    @Test
    fun resultsOverviewShowsEvidenceBasedInsights() {
        composeRule.setContent {
            GymbroTheme {
                ResultsScreen(
                    uiState =
                        ResultsUiState(
                            recentWorkouts =
                                listOf(
                                    WorkoutSummaryUiModel(
                                        runId = 1L,
                                        scheduleName = "Leg Day",
                                        startedAt = 100L,
                                        durationSeconds = 1_800L,
                                        completedSetCount = 8,
                                    ),
                                ),
                            insights =
                                listOf(
                                    ResultsInsightUiModel(
                                        title = "Build your baseline",
                                        message = "Complete another workout to unlock progress comparisons.",
                                        tone = ResultsInsightTone.Neutral,
                                    ),
                                ),
                        ),
                    modifier = Modifier,
                )
            }
        }

        composeRule.onNodeWithText("Insights").assertIsDisplayed()
        composeRule.onNodeWithText("Build your baseline").assertIsDisplayed()
        composeRule.onNodeWithText("Complete another workout to unlock progress comparisons.").assertIsDisplayed()
    }

    @Test
    fun exerciseDetailShowsReadableChartScale() {
        composeRule.setContent {
            GymbroTheme {
                ResultsScreen(
                    uiState =
                        ResultsUiState(
                            exerciseStats = listOf(exerciseStat(1L, "Back Squat")),
                            selectedExerciseDetail =
                                ExerciseStatsDetailUiModel(
                                    exerciseId = 1L,
                                    exerciseName = "Back Squat",
                                    trackingMode = TrackingMode.Strength,
                                    latest = "1,050 volume",
                                    best = "5 x 120",
                                    average = "900 avg volume",
                                    progressPercent = "+50%",
                                    summaryMetrics =
                                        listOf(
                                            SummaryMetricUiModel(label = "Max weight", value = "120"),
                                        ),
                                    totalSessions = 2,
                                    totalSets = 6,
                                    chartSummary =
                                        ChartSummaryUiModel(
                                            rangeLabel = "Jan 1 - Jan 8",
                                            minLabel = "Min 800 volume",
                                            maxLabel = "Max 1,200 volume",
                                        ),
                                    points =
                                        listOf(
                                            ChartPoint(label = "Jan 1", value = 800.0),
                                            ChartPoint(label = "Jan 8", value = 1_200.0),
                                        ),
                                    recentSessions =
                                        listOf(
                                            RecentSessionUiModel(
                                                dateLabel = "Jan 8",
                                                scheduleName = "Push Day",
                                                value = "1,200 volume",
                                                setCount = 4,
                                                comparison =
                                                    SessionComparisonUiModel(
                                                        label = "+400 volume",
                                                        trend = TrendDirection.Up,
                                                    ),
                                            ),
                                        ),
                                    recentSets = emptyList(),
                                ),
                        ),
                    modifier = Modifier,
                )
            }
        }

        composeRule.onNodeWithTag("exercise-stats-detail").assertIsDisplayed()
        composeRule.onAllNodesWithText("1,200 volume").get(0).assertIsDisplayed()
        composeRule.onAllNodesWithText("800 volume").get(0).assertIsDisplayed()
        composeRule.onNodeWithText("Push Day").assertIsDisplayed()
        composeRule.onNodeWithText("4 sets").assertIsDisplayed()
        composeRule.onNodeWithText("+400 volume").assertIsDisplayed()
        composeRule.onNodeWithText("+50%").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun exerciseDetailShowsModeSpecificPerformanceCharts() {
        composeRule.setContent {
            GymbroTheme {
                ResultsScreen(
                    uiState =
                        ResultsUiState(
                            exerciseStats = listOf(exerciseStat(1L, "Back Squat")),
                            selectedExerciseDetail =
                                ExerciseStatsDetailUiModel(
                                    exerciseId = 1L,
                                    exerciseName = "Back Squat",
                                    trackingMode = TrackingMode.Strength,
                                    latest = "1,050 volume",
                                    best = "5 x 120",
                                    average = "900 avg volume",
                                    totalSessions = 2,
                                    totalSets = 6,
                                    points =
                                        listOf(
                                            ChartPoint(label = "Jan 1", value = 800.0),
                                            ChartPoint(label = "Jan 8", value = 1_200.0),
                                        ),
                                    recentSets = emptyList(),
                                    additionalCharts =
                                        listOf(
                                            ExerciseChartUiModel(
                                                title = "Total reps",
                                                points =
                                                    listOf(
                                                        ChartPoint(label = "Jan 1", value = 20.0),
                                                        ChartPoint(label = "Jan 8", value = 24.0),
                                                    ),
                                            ),
                                            ExerciseChartUiModel(
                                                title = "Estimated 1RM",
                                                points =
                                                    listOf(
                                                        ChartPoint(label = "Jan 1", value = 100.0),
                                                        ChartPoint(label = "Jan 8", value = 120.0),
                                                    ),
                                            ),
                                        ),
                                ),
                        ),
                    modifier = Modifier,
                )
            }
        }

        composeRule.onNodeWithText("Total reps").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("24").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Estimated 1RM").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("120").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun exerciseDetailShowsDatedNoteContext() {
        composeRule.setContent {
            GymbroTheme {
                ResultsScreen(
                    uiState =
                        ResultsUiState(
                            exerciseStats = listOf(exerciseStat(1L, "Back Squat")),
                            selectedExerciseDetail =
                                ExerciseStatsDetailUiModel(
                                    exerciseId = 1L,
                                    exerciseName = "Back Squat",
                                    trackingMode = TrackingMode.Strength,
                                    latest = "400 volume",
                                    best = "5 x 80",
                                    average = "400 avg volume",
                                    totalSessions = 1,
                                    totalSets = 1,
                                    points = listOf(ChartPoint(label = "Jan 8", value = 400.0)),
                                    recentSets = emptyList(),
                                    noteDetails =
                                        listOf(
                                            ExerciseNoteUiModel(
                                                dateLabel = "Jan 8",
                                                contextLabel = "Push Day · Set 1",
                                                text = "RPE 8",
                                            ),
                                        ),
                                ),
                        ),
                )
            }
        }

        composeRule.onNodeWithText("Push Day · Set 1").assertIsDisplayed()
        composeRule.onAllNodesWithText("Jan 8").get(0).assertIsDisplayed()
        composeRule.onNodeWithText("RPE 8").assertIsDisplayed()
    }

    @Test
    fun statsRangeCanBeChanged() {
        composeRule.setContent {
            GymbroTheme {
                var selectedRange by remember { mutableStateOf(ResultsStatsRange.AllTime) }
                ResultsScreen(
                    uiState =
                        ResultsUiState(
                            statsRange = selectedRange,
                            recentWorkouts =
                                listOf(
                                    WorkoutSummaryUiModel(
                                        runId = 1L,
                                        scheduleName = "Leg Day",
                                        startedAt = 100L,
                                        durationSeconds = 60L,
                                        completedSetCount = 1,
                                    ),
                                ),
                        ),
                    onStatsRangeSelected = { selectedRange = it },
                    modifier = Modifier,
                )
                Text(text = "Selected: ${selectedRange.label}")
            }
        }

        composeRule.onNodeWithTag("results-range-Last30Days").performClick()

        composeRule.onNodeWithText("Selected: 30 days").assertIsDisplayed()
    }

    @Test
    fun recentWorkoutOpensExerciseAndSetDetails() {
        composeRule.setContent {
            GymbroTheme {
                var uiState by remember {
                    mutableStateOf(
                        ResultsUiState(
                            recentWorkouts =
                                listOf(
                                    WorkoutSummaryUiModel(
                                        runId = 1L,
                                        scheduleName = "Leg Day",
                                        startedAt = 100L,
                                        durationSeconds = 60L,
                                        completedSetCount = 1,
                                    ),
                                ),
                            workoutDetails =
                                listOf(
                                    WorkoutDetailUiModel(
                                        runId = 1L,
                                        scheduleName = "Leg Day",
                                        startedAt = 100L,
                                        durationSeconds = 60L,
                                        notes = "Felt strong",
                                        exercises =
                                            listOf(
                                                WorkoutExerciseDetailUiModel(
                                                    exerciseResultId = 2L,
                                                    exerciseName = "Squat",
                                                    trackingMode = TrackingMode.Strength,
                                                    notes = "Keep the chest up",
                                                    sets =
                                                        listOf(
                                                            WorkoutSetDetailUiModel(
                                                                setId = 3L,
                                                                setNumber = 1,
                                                                value = "5 x 80",
                                                                isCompleted = true,
                                                                notes = "Smooth reps",
                                                            ),
                                                        ),
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                    )
                }
                ResultsScreen(
                    uiState = uiState,
                    onWorkoutSelected = { workout ->
                        uiState =
                            uiState.copy(
                                selectedWorkoutDetail =
                                    uiState.workoutDetails.first { detail -> detail.runId == workout.runId },
                            )
                    },
                    modifier = Modifier,
                )
            }
        }

        composeRule.onNodeWithTag("workout-summary-1").performClick()

        composeRule.onNodeWithText("Squat").assertIsDisplayed()
        composeRule.onNodeWithText("5 x 80").assertIsDisplayed()
        composeRule.onNodeWithText("Keep the chest up").assertIsDisplayed()
        composeRule.onNodeWithText("Felt strong").assertIsDisplayed()
    }

    @Test
    fun completedExerciseNotesCanBeEdited() {
        composeRule.setContent {
            GymbroTheme {
                var uiState by remember {
                    mutableStateOf(
                        ResultsUiState(
                            recentWorkouts =
                                listOf(
                                    WorkoutSummaryUiModel(
                                        runId = 1L,
                                        scheduleName = "Leg Day",
                                        startedAt = 100L,
                                        durationSeconds = 60L,
                                        completedSetCount = 1,
                                    ),
                                ),
                            workoutDetails =
                                listOf(
                                    WorkoutDetailUiModel(
                                        runId = 1L,
                                        scheduleName = "Leg Day",
                                        startedAt = 100L,
                                        durationSeconds = 60L,
                                        notes = null,
                                        exercises =
                                            listOf(
                                                WorkoutExerciseDetailUiModel(
                                                    exerciseResultId = 2L,
                                                    exerciseName = "Squat",
                                                    trackingMode = TrackingMode.Strength,
                                                    notes = null,
                                                    sets = emptyList(),
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                    )
                }
                var savedNotes by remember { mutableStateOf<String?>(null) }
                ResultsScreen(
                    uiState = uiState,
                    onWorkoutSelected = { workout ->
                        uiState =
                            uiState.copy(
                                selectedWorkoutDetail =
                                    uiState.workoutDetails.first { detail -> detail.runId == workout.runId },
                            )
                    },
                    onUpdateExerciseNotes = { _, notes -> savedNotes = notes },
                    modifier = Modifier,
                )
                if (savedNotes != null) {
                    Text(text = "Saved: $savedNotes")
                }
            }
        }

        composeRule.onNodeWithTag("workout-summary-1").performClick()
        composeRule.onNodeWithTag("edit-exercise-notes-2").performClick()
        composeRule.onNodeWithTag("result-note-input").performTextInput("Form cue")
        composeRule.onNodeWithText("Save").performClick()

        composeRule.onNodeWithText("Saved: Form cue").assertIsDisplayed()
    }

    @Test
    fun completedWorkoutNotesCanBeEdited() {
        composeRule.setContent {
            GymbroTheme {
                var uiState by remember {
                    mutableStateOf(
                        ResultsUiState(
                            recentWorkouts =
                                listOf(
                                    WorkoutSummaryUiModel(
                                        runId = 1L,
                                        scheduleName = "Leg Day",
                                        startedAt = 100L,
                                        durationSeconds = 60L,
                                        completedSetCount = 1,
                                    ),
                                ),
                            workoutDetails =
                                listOf(
                                    WorkoutDetailUiModel(
                                        runId = 1L,
                                        scheduleName = "Leg Day",
                                        startedAt = 100L,
                                        durationSeconds = 60L,
                                        notes = null,
                                        exercises = emptyList(),
                                    ),
                                ),
                        ),
                    )
                }
                var savedNotes by remember { mutableStateOf<String?>(null) }
                ResultsScreen(
                    uiState = uiState,
                    onWorkoutSelected = { workout ->
                        uiState =
                            uiState.copy(
                                selectedWorkoutDetail =
                                    uiState.workoutDetails.first { detail -> detail.runId == workout.runId },
                            )
                    },
                    onUpdateWorkoutNotes = { _, notes -> savedNotes = notes },
                    modifier = Modifier,
                )
                if (savedNotes != null) {
                    Text(text = "Saved: $savedNotes")
                }
            }
        }

        composeRule.onNodeWithTag("workout-summary-1").performClick()
        composeRule.onNodeWithTag("edit-workout-notes-1").performClick()
        composeRule.onNodeWithTag("result-note-input").performTextInput("Great session")
        composeRule.onNodeWithText("Save").performClick()

        composeRule.onNodeWithText("Saved: Great session").assertIsDisplayed()
    }
}

private fun exerciseStat(
    id: Long,
    name: String,
    mode: TrackingMode = TrackingMode.Strength,
): ExerciseStatsUiModel =
    ExerciseStatsUiModel(
        exerciseId = id,
        exerciseName = name,
        trackingMode = mode,
        headline = "100 volume",
        trend = TrendDirection.Unknown,
        points = listOf(ChartPoint(label = "Today", value = 100.0)),
    )
