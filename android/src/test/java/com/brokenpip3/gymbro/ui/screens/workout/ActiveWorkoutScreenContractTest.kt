package com.brokenpip3.gymbro.ui.screens.workout

import androidx.compose.runtime.Composable
import com.brokenpip3.gymbro.data.entities.WorkoutRunEntity
import com.brokenpip3.gymbro.domain.TrackingMode
import com.brokenpip3.gymbro.ui.screens.WorkoutScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActiveWorkoutScreenContractTest {
    @Test
    fun signaturesCompile() = Unit

    @Test
    fun workoutProgressCountsCompletedSetsAndExercises() {
        val workout =
            ActiveWorkoutUiModel(
                runId = 1L,
                scheduleName = "Leg Day",
                startedAt = 1L,
                exercises =
                    listOf(
                        workoutExercise(
                            id = 1L,
                            sets = listOf(true, true),
                        ),
                        workoutExercise(
                            id = 2L,
                            sets = listOf(true, false),
                        ),
                    ),
            )

        assertEquals(
            WorkoutProgressUiModel(
                completedSets = 3,
                totalSets = 4,
                completedExercises = 1,
                totalExercises = 2,
            ),
            workout.progress,
        )
        assertEquals("3 of 4 sets · 1 of 2 exercises", workout.progress.label)
    }

    @Test
    fun strengthRoutineContractIncludesExpectedTableText() {
        assertEquals(listOf("Reps", "Weight", "Done"), routineHeaderLabels(TrackingMode.Strength))
        assertEquals("Add Set", ADD_SET_ROW_LABEL)
    }

    @Test
    fun setControlActionStacksOnlyWhenThePhoneIsTooNarrow() {
        assertEquals(false, shouldStackSetAction(360))
        assertEquals(true, shouldStackSetAction(320))
    }

    @Test
    fun exerciseHeaderActionsMoveBelowLongTitlesOnNarrowScreens() {
        assertEquals(RoutineHeaderActionPlacement.Inline, routineHeaderActionPlacement(400))
        assertEquals(RoutineHeaderActionPlacement.BelowTitle, routineHeaderActionPlacement(320))
    }

    @Test
    fun timedRoutineContractIncludesExpectedTableText() {
        assertEquals(listOf("Time", "Distance", "Done"), routineHeaderLabels(TrackingMode.Timed))
    }

    @Test
    fun rowCompletionContractExposesDoneAndCheckStates() {
        val completed =
            WorkoutSetUiModel(
                id = 1L,
                setOrder = 0,
                reps = 8,
                weight = 80.0,
                durationSeconds = null,
                distance = null,
                notes = null,
                isCompleted = true,
            )
        val incomplete =
            completed.copy(
                id = 2L,
                isCompleted = false,
            )

        assertEquals("Checked", completed.completionStateLabel)
        assertEquals("Checked", completed.completionActionLabel)
        assertEquals("Unchecked", incomplete.completionStateLabel)
        assertEquals("Done", incomplete.completionActionLabel)
    }

    @Test
    fun exerciseInfoDialogContractIncludesNameNotesAndEmptyHistoryText() {
        val info =
            ExerciseInfoUiModel(
                exerciseName = "Bench Press",
                notes = "Keep shoulder blades tight.",
                headline = null,
                history = emptyList(),
            )

        assertEquals("Bench Press", info.exerciseName)
        assertEquals(
            listOf("Keep shoulder blades tight.", "No completed history yet"),
            exerciseInfoDialogLines(info),
        )
    }

    @Test
    fun addExerciseDialogContractExposesErrorMessage() {
        assertEquals(
            listOf("Unable to add exercise"),
            addExerciseDialogErrorLines("Unable to add exercise"),
        )
        assertEquals(emptyList<String>(), addExerciseDialogErrorLines(null))
    }

    @Test
    fun availableExerciseFilterMatchesNamesIgnoringCaseAndWhitespace() {
        val exercises =
            listOf(
                AvailableWorkoutExerciseUiModel(1L, "Back Squat", TrackingMode.Strength),
                AvailableWorkoutExerciseUiModel(2L, "Bench Press", TrackingMode.Strength),
            )

        assertEquals(
            listOf(exercises[0]),
            filterAvailableWorkoutExercises(exercises, "  SQUAT "),
        )
        assertEquals(exercises, filterAvailableWorkoutExercises(exercises, "   "))
        assertEquals(emptyList<AvailableWorkoutExerciseUiModel>(), filterAvailableWorkoutExercises(exercises, "row"))
    }

    @Test
    fun strengthParserRejectsInvalidNonEmptyRepsAndPreservesWeight() {
        val result = parseStrengthSetInput(repsText = "ten", weightText = "80.5")

        assertEquals("Enter whole-number reps.", result.repsError)
        assertNull(result.weightError)
        assertNull(result.metrics)
    }

    @Test
    fun strengthParserAcceptsBlankOptionalWeight() {
        val result = parseStrengthSetInput(repsText = "8", weightText = "")

        assertEquals(
            ParsedSetMetrics(reps = 8, weight = null, durationSeconds = null, distance = null),
            result.metrics,
        )
    }

    @Test
    fun strengthParserRejectsZeroAndNegativeValues() {
        val zero = parseStrengthSetInput(repsText = "0", weightText = "0")
        val negative = parseStrengthSetInput(repsText = "-1", weightText = "-10")

        assertEquals("Reps must be positive.", zero.repsError)
        assertEquals("Weight must be positive.", zero.weightError)
        assertNull(zero.metrics)
        assertEquals("Reps must be positive.", negative.repsError)
        assertEquals("Weight must be positive.", negative.weightError)
        assertNull(negative.metrics)
    }

    @Test
    fun strengthParserRejectsNonFiniteWeight() {
        val nan = parseStrengthSetInput(repsText = "8", weightText = "NaN")
        val infinity = parseStrengthSetInput(repsText = "8", weightText = "Infinity")

        assertEquals("Weight must be finite.", nan.weightError)
        assertNull(nan.metrics)
        assertEquals("Weight must be finite.", infinity.weightError)
        assertNull(infinity.metrics)
    }

    @Test
    fun bodyweightParserRejectsZeroAndNegativeReps() {
        val zero = parseBodyweightSetInput(repsText = "0")
        val negative = parseBodyweightSetInput(repsText = "-5")

        assertEquals("Reps must be positive.", zero.repsError)
        assertNull(zero.metrics)
        assertEquals("Reps must be positive.", negative.repsError)
        assertNull(negative.metrics)
    }

    @Test
    fun timedParserRejectsInvalidDistanceWithoutDroppingDuration() {
        val result = parseTimedSetInput(minutesText = "1", secondsText = "30", distanceText = "far")

        assertEquals("Enter a decimal distance.", result.distanceError)
        assertNull(result.metrics)
    }

    @Test
    fun timedParserAcceptsBlankOptionalDistance() {
        val result = parseTimedSetInput(minutesText = "1", secondsText = "30", distanceText = "")

        assertEquals(
            ParsedSetMetrics(reps = null, weight = null, durationSeconds = 90L, distance = null),
            result.metrics,
        )
    }

    @Test
    fun timedParserRejectsZeroDuration() {
        val result = parseTimedSetInput(minutesText = "0", secondsText = "0", distanceText = "")

        assertEquals("Duration must be positive.", result.minutesError)
        assertNull(result.metrics)
    }

    @Test
    fun timedParserRejectsNegativeDurationInputs() {
        val result = parseTimedSetInput(minutesText = "-1", secondsText = "-30", distanceText = "")

        assertEquals("Minutes must not be negative.", result.minutesError)
        assertEquals("Seconds must not be negative.", result.secondsError)
        assertNull(result.metrics)
    }

    @Test
    fun timedParserRejectsZeroNegativeAndNonFiniteDistance() {
        val zero = parseTimedSetInput(minutesText = "1", secondsText = "", distanceText = "0")
        val negative = parseTimedSetInput(minutesText = "1", secondsText = "", distanceText = "-1")
        val nan = parseTimedSetInput(minutesText = "1", secondsText = "", distanceText = "NaN")
        val infinity = parseTimedSetInput(minutesText = "1", secondsText = "", distanceText = "Infinity")

        assertEquals("Distance must be positive.", zero.distanceError)
        assertNull(zero.metrics)
        assertEquals("Distance must be positive.", negative.distanceError)
        assertNull(negative.metrics)
        assertEquals("Distance must be finite.", nan.distanceError)
        assertNull(nan.metrics)
        assertEquals("Distance must be finite.", infinity.distanceError)
        assertNull(infinity.metrics)
    }
}

private fun workoutExercise(
    id: Long,
    sets: List<Boolean>,
): WorkoutExerciseUiModel =
    WorkoutExerciseUiModel(
        exerciseResultId = id,
        exerciseName = "Exercise $id",
        trackingMode = TrackingMode.Strength,
        notes = null,
        sets =
            sets.mapIndexed { index, isCompleted ->
                WorkoutSetUiModel(
                    id = id * 10 + index,
                    setOrder = index,
                    reps = 8,
                    weight = 40.0,
                    durationSeconds = null,
                    distance = null,
                    notes = null,
                    isCompleted = isCompleted,
                )
            },
    )

@Composable
private fun ActiveWorkoutScreenSignatureContract() {
    ActiveWorkoutScreen(
        uiState =
            ActiveWorkoutUiState(
                isLoading = false,
                activeWorkout =
                    ActiveWorkoutUiModel(
                        runId = 1L,
                        scheduleName = "Push Day",
                        startedAt = 100L,
                        exercises =
                            listOf(
                                WorkoutExerciseUiModel(
                                    exerciseResultId = 2L,
                                    exerciseName = "Bench Press",
                                    trackingMode = TrackingMode.Strength,
                                    notes = null,
                                    sets =
                                        listOf(
                                            WorkoutSetUiModel(
                                                id = 3L,
                                                setOrder = 1,
                                                reps = 8,
                                                weight = 80.0,
                                                durationSeconds = null,
                                                distance = null,
                                                notes = null,
                                            ),
                                        ),
                                ),
                            ),
                    ),
                isAddExerciseDialogVisible = true,
                addExerciseErrorMessage = "Unable to add exercise",
            ),
        onAddSetRow = {},
        onUpdateSetMetrics = { _, _, _, _, _, _ -> },
        onUpdateSetCompletion = { _, _ -> },
        onDeleteSet = {},
        onFinishWorkout = {},
        onShowAddExerciseDialog = {},
        onDismissAddExerciseDialog = {},
        onAddExerciseToWorkout = {},
        onShowExerciseInfo = {},
        onDismissExerciseInfo = {},
    )
}

@Composable
private fun WorkoutScreenWrapperSignatureContract() {
    WorkoutScreen(
        uiState = ActiveWorkoutUiState(isLoading = false),
        onShowExerciseInfo = {},
        onDismissExerciseInfo = {},
    )
}

@Composable
private fun WorkoutRouteSignatureContract() {
    WorkoutRoute(
        repository = ContractActiveWorkoutSource(),
    )
}

private class ContractActiveWorkoutSource : ActiveWorkoutSource {
    override fun observeActiveWorkoutRun(): Flow<WorkoutRunEntity?> = MutableStateFlow(null)

    override fun observeExerciseResults(id: Long): Flow<ExerciseResults> = MutableStateFlow(emptyList())

    override fun observeSetResults(id: Long): Flow<SetResults> = MutableStateFlow(emptyList())

    override suspend fun getExerciseInfo(exerciseResultId: Long): ExerciseInfoUiModel =
        ExerciseInfoUiModel(
            exerciseName = "Bench Press",
            notes = null,
            headline = null,
            history = emptyList(),
        )

    override suspend fun addSet(
        exerciseResultId: Long,
        reps: Int?,
        weight: Double?,
        durationSeconds: Long?,
        distance: Double?,
    ): Long = 1L

    override suspend fun updateExerciseResultNotes(
        exerciseResultId: Long,
        notes: String?,
    ) = Unit

    override suspend fun updateWorkoutNotes(
        workoutRunId: Long,
        notes: String?,
    ) = Unit

    override suspend fun updateSetNotes(
        setId: Long,
        notes: String?,
    ) = Unit

    override suspend fun deleteSet(setId: Long) = Unit

    override suspend fun deleteExerciseResult(exerciseResultId: Long) = Unit

    override suspend fun finishWorkout(
        workoutRunId: Long,
        nowMillis: Long,
    ) = Unit

    override suspend fun discardWorkout(workoutRunId: Long) = Unit
}
