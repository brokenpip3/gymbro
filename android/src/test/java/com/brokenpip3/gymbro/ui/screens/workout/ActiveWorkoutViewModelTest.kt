package com.brokenpip3.gymbro.ui.screens.workout

import com.brokenpip3.gymbro.data.entities.ExerciseEntity
import com.brokenpip3.gymbro.data.entities.ExerciseResultEntity
import com.brokenpip3.gymbro.data.entities.SetResultEntity
import com.brokenpip3.gymbro.data.entities.WorkoutRunEntity
import com.brokenpip3.gymbro.domain.TrackingMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("LargeClass")
class ActiveWorkoutViewModelTest {
    @Test
    fun noActiveWorkoutEmitsEmptyState() =
        runTest {
            val source = FakeActiveWorkoutSource()
            val viewModel =
                ActiveWorkoutViewModel(
                    source = source,
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            assertEquals(ActiveWorkoutUiState(isLoading = false), viewModel.uiState.value)
        }

    @Test
    fun activeWorkoutEmitsScheduleNameStartedTimeExercisesAndSetRows() =
        runTest {
            val viewModel =
                ActiveWorkoutViewModel(
                    source = activeWorkoutSourceWithSets(),
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            assertEquals(activeWorkoutUiStateWithSets(), viewModel.uiState.value)
        }

    @Test
    fun addStrengthSetWritesRepsAndWeight() =
        runTest {
            val source = FakeActiveWorkoutSource()
            val viewModel =
                ActiveWorkoutViewModel(
                    source = source,
                    coroutineScope = viewModelScope(),
                )

            viewModel.addSet(
                exerciseResultId = 10,
                reps = 8,
                weight = 80.0,
                durationSeconds = null,
                distance = null,
            )

            assertEquals(10L, source.addedExerciseResultId)
            assertEquals(8, source.addedReps)
            assertEquals(80.0, source.addedWeight)
            assertEquals(null, source.addedDurationSeconds)
            assertEquals(null, source.addedDistance)
        }

    @Test
    fun addTimedSetWritesDurationAndOptionalDistance() =
        runTest {
            val source = FakeActiveWorkoutSource()
            val viewModel =
                ActiveWorkoutViewModel(
                    source = source,
                    coroutineScope = viewModelScope(),
                )

            viewModel.addSet(
                exerciseResultId = 11,
                reps = null,
                weight = null,
                durationSeconds = 600L,
                distance = 2.5,
            )

            assertEquals(11L, source.addedExerciseResultId)
            assertEquals(null, source.addedReps)
            assertEquals(null, source.addedWeight)
            assertEquals(600L, source.addedDurationSeconds)
            assertEquals(2.5, source.addedDistance)
        }

    @Test
    fun invalidSetValuesSetErrorAndDoNotWrite() =
        runTest {
            val cases =
                listOf(
                    InvalidSetCase(reps = 0, expectedError = "Reps must be positive"),
                    InvalidSetCase(weight = 0.0, expectedError = "Weight must be positive"),
                    InvalidSetCase(weight = Double.NaN, expectedError = "Weight must be finite"),
                    InvalidSetCase(weight = Double.POSITIVE_INFINITY, expectedError = "Weight must be finite"),
                    InvalidSetCase(weight = Double.NEGATIVE_INFINITY, expectedError = "Weight must be finite"),
                    InvalidSetCase(durationSeconds = 0L, expectedError = "Duration must be positive"),
                    InvalidSetCase(distance = 0.0, expectedError = "Distance must be positive"),
                    InvalidSetCase(distance = Double.NaN, expectedError = "Distance must be finite"),
                    InvalidSetCase(distance = Double.POSITIVE_INFINITY, expectedError = "Distance must be finite"),
                    InvalidSetCase(distance = Double.NEGATIVE_INFINITY, expectedError = "Distance must be finite"),
                    InvalidSetCase(expectedError = "Set metrics are required"),
                )

            cases.forEach { invalidCase ->
                val source = FakeActiveWorkoutSource()
                val viewModel =
                    ActiveWorkoutViewModel(
                        source = source,
                        coroutineScope = viewModelScope(),
                    )

                viewModel.addSet(
                    exerciseResultId = 10,
                    reps = invalidCase.reps,
                    weight = invalidCase.weight,
                    durationSeconds = invalidCase.durationSeconds,
                    distance = invalidCase.distance,
                )

                assertEquals(invalidCase.expectedError, viewModel.uiState.value.errorMessage)
                assertEquals(0, source.addSetCallCount)
            }
        }

    @Test
    fun addSetRowDelegatesToSource() =
        runTest {
            val source = FakeActiveWorkoutSource()
            val viewModel =
                ActiveWorkoutViewModel(
                    source = source,
                    coroutineScope = viewModelScope(),
                )

            viewModel.addSetRow(exerciseResultId = 10L)

            assertEquals(10L, source.addedSetFromPreviousExerciseResultId)
        }

    @Test
    fun addEmptySetDelegatesToSource() =
        runTest {
            val source = FakeActiveWorkoutSource()
            val viewModel =
                ActiveWorkoutViewModel(
                    source = source,
                    coroutineScope = viewModelScope(),
                )

            viewModel.addEmptySet(exerciseResultId = 10L)

            assertEquals(10L, source.addedEmptySetExerciseResultId)
        }

    @Test
    fun availableExercisesAreExposedForAddDialog() =
        runTest {
            val source =
                FakeActiveWorkoutSource(
                    availableExercises =
                        listOf(
                            exercise(id = 2L, name = "Squat", trackingMode = TrackingMode.Strength.databaseValue),
                            exercise(id = 3L, name = "Plank", trackingMode = TrackingMode.Timed.databaseValue),
                        ),
                )
            val viewModel =
                ActiveWorkoutViewModel(
                    source = source,
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            assertEquals(
                listOf(
                    AvailableWorkoutExerciseUiModel(
                        id = 2L,
                        name = "Squat",
                        trackingMode = TrackingMode.Strength,
                    ),
                    AvailableWorkoutExerciseUiModel(
                        id = 3L,
                        name = "Plank",
                        trackingMode = TrackingMode.Timed,
                    ),
                ),
                viewModel.uiState.value.availableExercises,
            )
        }

    @Test
    fun addExerciseDelegatesToSourceClosesDialogAndClearsError() =
        runTest {
            val source =
                FakeActiveWorkoutSource(
                    activeWorkoutRun =
                        workoutRun(
                            id = 7L,
                            scheduleNameSnapshot = "Push Day",
                            startedAt = 1234L,
                        ),
                )
            val viewModel =
                ActiveWorkoutViewModel(
                    source = source,
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()
            viewModel.addSet(
                exerciseResultId = 10L,
                reps = null,
                weight = null,
                durationSeconds = null,
                distance = null,
            )
            viewModel.showAddExerciseDialog()

            viewModel.addExerciseToWorkout(exerciseId = 42L)
            advanceUntilIdle()

            assertEquals(7L to 42L, source.addedExerciseToWorkout)
            assertEquals(false, viewModel.uiState.value.isAddExerciseDialogVisible)
            assertEquals(null, viewModel.uiState.value.errorMessage)
        }

    @Test
    fun addExerciseFailureShowsErrorAndKeepsDialogOpen() =
        runTest {
            val source =
                FakeActiveWorkoutSource(
                    activeWorkoutRun =
                        workoutRun(
                            id = 7L,
                            scheduleNameSnapshot = "Push Day",
                            startedAt = 1234L,
                        ),
                    failAddExercise = true,
                )
            val viewModel =
                ActiveWorkoutViewModel(
                    source = source,
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()
            viewModel.showAddExerciseDialog()

            viewModel.addExerciseToWorkout(exerciseId = 42L)
            advanceUntilIdle()

            assertEquals("Unable to add exercise", viewModel.uiState.value.addExerciseErrorMessage)
            assertEquals(null, viewModel.uiState.value.errorMessage)
            assertEquals(true, viewModel.uiState.value.isAddExerciseDialogVisible)
        }

    @Test
    fun activeWorkoutBecomingNullClosesAddExerciseDialog() =
        runTest {
            val activeRun =
                MutableStateFlow<WorkoutRunEntity?>(
                    workoutRun(
                        id = 7L,
                        scheduleNameSnapshot = "Push Day",
                        startedAt = 1234L,
                    ),
                )
            val source = FakeActiveWorkoutSource(activeWorkoutRunFlow = activeRun)
            val viewModel =
                ActiveWorkoutViewModel(
                    source = source,
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()
            viewModel.showAddExerciseDialog()

            activeRun.value = null
            advanceUntilIdle()

            assertEquals(false, viewModel.uiState.value.isAddExerciseDialogVisible)
            assertEquals(null, viewModel.uiState.value.addExerciseErrorMessage)
        }

    @Test
    fun addExerciseWithNoActiveWorkoutClosesDialogAndShowsSafeState() =
        runTest {
            val source = FakeActiveWorkoutSource()
            val viewModel =
                ActiveWorkoutViewModel(
                    source = source,
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()
            viewModel.showAddExerciseDialog()

            viewModel.addExerciseToWorkout(exerciseId = 42L)

            assertEquals(false, viewModel.uiState.value.isAddExerciseDialogVisible)
            assertEquals("No active workout", viewModel.uiState.value.errorMessage)
        }

    @Test
    fun updateStrengthSetPersistsMetrics() =
        runTest {
            val source = FakeActiveWorkoutSource()
            val viewModel =
                ActiveWorkoutViewModel(
                    source = source,
                    coroutineScope = viewModelScope(),
                )

            viewModel.updateSetMetrics(
                setId = 20L,
                repsText = "12",
                weightText = "32.5",
                minutesText = "",
                secondsText = "",
                distanceText = "",
            )

            assertEquals(20L, source.updatedSetId)
            assertEquals(12, source.updatedReps)
            assertEquals(32.5, source.updatedWeight)
            assertEquals(null, source.updatedDurationSeconds)
            assertEquals(null, source.updatedDistance)
        }

    @Test
    fun toggleSetCompletionDelegatesToSource() =
        runTest {
            val source = FakeActiveWorkoutSource()
            val viewModel =
                ActiveWorkoutViewModel(
                    source = source,
                    coroutineScope = viewModelScope(),
                )

            viewModel.updateSetCompletion(setId = 20L, isCompleted = true)

            assertEquals(20L to true, source.updatedCompletion)
        }

    @Test
    fun completingStrengthSetWithoutBothMetricsShowsValidationError() =
        runTest {
            val source =
                FakeActiveWorkoutSource(
                    activeWorkoutRun =
                        workoutRun(
                            id = 7L,
                            scheduleNameSnapshot = "Push Day",
                            startedAt = 1234L,
                        ),
                    exerciseResults =
                        listOf(
                            exerciseResult(
                                id = 10L,
                                exerciseNameSnapshot = "Bench press",
                                trackingModeSnapshot = TrackingMode.Strength.databaseValue,
                                sortOrder = 0,
                            ),
                        ),
                    setResultsByExerciseResultId =
                        mapOf(
                            10L to
                                listOf(
                                    setResult(
                                        id = 20L,
                                        exerciseResultId = 10L,
                                        setOrder = 0,
                                        reps = 8,
                                        weight = null,
                                        isCompleted = false,
                                    ),
                                ),
                        ),
                )
            val viewModel =
                ActiveWorkoutViewModel(
                    source = source,
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            viewModel.updateSetCompletion(setId = 20L, isCompleted = true)

            assertEquals("Strength sets need reps and weight", viewModel.uiState.value.errorMessage)
            assertEquals(null, source.updatedCompletion)
        }

    @Test
    fun completingBodyweightAndTimedSetsRequiresTheirPrimaryMetric() =
        runTest {
            val source =
                FakeActiveWorkoutSource(
                    activeWorkoutRun =
                        workoutRun(
                            id = 7L,
                            scheduleNameSnapshot = "Mixed Day",
                            startedAt = 1234L,
                        ),
                    exerciseResults =
                        listOf(
                            exerciseResult(
                                id = 11L,
                                exerciseNameSnapshot = "Push-ups",
                                trackingModeSnapshot = TrackingMode.Bodyweight.databaseValue,
                                sortOrder = 0,
                            ),
                            exerciseResult(
                                id = 12L,
                                exerciseNameSnapshot = "Running",
                                trackingModeSnapshot = TrackingMode.Timed.databaseValue,
                                sortOrder = 1,
                            ),
                        ),
                    setResultsByExerciseResultId =
                        mapOf(
                            11L to
                                listOf(
                                    setResult(
                                        id = 21L,
                                        exerciseResultId = 11L,
                                        setOrder = 0,
                                        isCompleted = false,
                                    ),
                                ),
                            12L to
                                listOf(
                                    setResult(
                                        id = 22L,
                                        exerciseResultId = 12L,
                                        setOrder = 0,
                                        isCompleted = false,
                                    ),
                                ),
                        ),
                )
            val viewModel =
                ActiveWorkoutViewModel(
                    source = source,
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            viewModel.updateSetCompletion(setId = 21L, isCompleted = true)
            assertEquals("Bodyweight sets need reps", viewModel.uiState.value.errorMessage)
            viewModel.updateSetCompletion(setId = 22L, isCompleted = true)

            assertEquals("Timed sets need a duration", viewModel.uiState.value.errorMessage)
            assertEquals(null, source.updatedCompletion)
        }

    @Test
    fun updateExerciseNotesTrimsAndDelegatesToSource() =
        runTest {
            val source = FakeActiveWorkoutSource()
            val viewModel =
                ActiveWorkoutViewModel(
                    source = source,
                    coroutineScope = viewModelScope(),
                )

            viewModel.updateExerciseNotes(
                exerciseResultId = 10L,
                notesText = "  Keep shoulder blades tight.  ",
            )

            assertEquals(10L to "Keep shoulder blades tight.", source.updatedExerciseNotes)
            assertEquals(null, viewModel.uiState.value.errorMessage)
        }

    @Test
    fun updateExerciseNotesWithBlankTextClearsNotes() =
        runTest {
            val source = FakeActiveWorkoutSource()
            val viewModel =
                ActiveWorkoutViewModel(
                    source = source,
                    coroutineScope = viewModelScope(),
                )

            viewModel.updateExerciseNotes(
                exerciseResultId = 10L,
                notesText = "   ",
            )

            assertEquals(10L to null, source.updatedExerciseNotes)
            assertEquals(null, viewModel.uiState.value.errorMessage)
        }

    @Test
    fun updateSetNotesTrimsAndDelegatesToSource() =
        runTest {
            val source = FakeActiveWorkoutSource()
            val viewModel =
                ActiveWorkoutViewModel(
                    source = source,
                    coroutineScope = viewModelScope(),
                )

            viewModel.updateSetNotes(
                setId = 20L,
                notesText = "  RPE 8, clean reps.  ",
            )

            assertEquals(20L to "RPE 8, clean reps.", source.updatedSetNotes)
            assertEquals(null, viewModel.uiState.value.errorMessage)
        }

    @Test
    fun updateSetNotesWithBlankTextClearsNotes() =
        runTest {
            val source = FakeActiveWorkoutSource()
            val viewModel =
                ActiveWorkoutViewModel(
                    source = source,
                    coroutineScope = viewModelScope(),
                )

            viewModel.updateSetNotes(
                setId = 20L,
                notesText = "   ",
            )

            assertEquals(20L to null, source.updatedSetNotes)
            assertEquals(null, viewModel.uiState.value.errorMessage)
        }

    @Test
    fun updateWorkoutNotesTrimsAndDelegatesToSource() =
        runTest {
            val source =
                FakeActiveWorkoutSource(
                    activeWorkoutRun =
                        workoutRun(
                            id = 7L,
                            scheduleNameSnapshot = "Push Day",
                            startedAt = 1234L,
                        ),
                )
            val viewModel =
                ActiveWorkoutViewModel(
                    source = source,
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            viewModel.updateWorkoutNotes("  Felt strong today.  ")
            advanceUntilIdle()

            assertEquals(7L to "Felt strong today.", source.updatedWorkoutNotes)
            assertEquals(null, viewModel.uiState.value.errorMessage)
        }

    @Test
    fun blankWorkoutNotesClearExistingNotes() =
        runTest {
            val source =
                FakeActiveWorkoutSource(
                    activeWorkoutRun =
                        workoutRun(
                            id = 7L,
                            scheduleNameSnapshot = "Push Day",
                            startedAt = 1234L,
                        ),
                )
            val viewModel =
                ActiveWorkoutViewModel(
                    source = source,
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            viewModel.updateWorkoutNotes("   ")
            advanceUntilIdle()

            assertEquals(7L to null, source.updatedWorkoutNotes)
        }

    @Test
    fun invalidTextMetricsSetErrorAndDoNotWrite() =
        runTest {
            val source = FakeActiveWorkoutSource()
            val viewModel =
                ActiveWorkoutViewModel(
                    source = source,
                    coroutineScope = viewModelScope(),
                )

            viewModel.updateSetMetrics(
                setId = 20L,
                repsText = "abc",
                weightText = "",
                minutesText = "",
                secondsText = "",
                distanceText = "",
            )

            assertEquals("Reps must be a positive whole number", viewModel.uiState.value.errorMessage)
            assertEquals(0, source.updateSetMetricsCallCount)
        }

    @Test
    fun emptyTextMetricsAreAllowedForIncompleteRows() =
        runTest {
            val source = FakeActiveWorkoutSource()
            val viewModel =
                ActiveWorkoutViewModel(
                    source = source,
                    coroutineScope = viewModelScope(),
                )

            viewModel.updateSetMetrics(
                setId = 20L,
                repsText = "",
                weightText = "",
                minutesText = "",
                secondsText = "",
                distanceText = "",
            )

            assertEquals(20L, source.updatedSetId)
            assertEquals(null, source.updatedReps)
            assertEquals(null, source.updatedWeight)
            assertEquals(null, source.updatedDurationSeconds)
            assertEquals(null, source.updatedDistance)
            assertEquals(null, viewModel.uiState.value.errorMessage)
        }

    @Test
    fun oversizedDurationTextSetsErrorAndDoesNotWrite() =
        runTest {
            val source = FakeActiveWorkoutSource()
            val viewModel =
                ActiveWorkoutViewModel(
                    source = source,
                    coroutineScope = viewModelScope(),
                )

            viewModel.updateSetMetrics(
                setId = 20L,
                repsText = "",
                weightText = "",
                minutesText = Long.MAX_VALUE.toString(),
                secondsText = "59",
                distanceText = "",
            )

            assertEquals("Duration is too large", viewModel.uiState.value.errorMessage)
            assertEquals(0, source.updateSetMetricsCallCount)
        }

    @Test
    fun finishWorkoutCallsRepositoryAndClearsActiveState() =
        runTest {
            val activeRun =
                MutableStateFlow<WorkoutRunEntity?>(
                    workoutRun(
                        id = 7,
                        scheduleNameSnapshot = "Push Day",
                        startedAt = 1234L,
                    ),
                )
            val source = FakeActiveWorkoutSource(activeWorkoutRunFlow = activeRun)
            val viewModel =
                ActiveWorkoutViewModel(
                    source = source,
                    nowMillis = { 2000L },
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            viewModel.finishWorkout()
            advanceUntilIdle()

            assertEquals(7L, source.finishedWorkoutRunId)
            assertEquals(2000L, source.finishedNowMillis)
            assertEquals(
                ActiveWorkoutUiState(
                    isLoading = false,
                    finishedWorkoutRunId = 7L,
                ),
                viewModel.uiState.value,
            )
        }

    @Test
    fun acknowledgeWorkoutFinishedClearsFinishSignal() =
        runTest {
            val activeRun =
                MutableStateFlow<WorkoutRunEntity?>(
                    workoutRun(
                        id = 7,
                        scheduleNameSnapshot = "Push Day",
                        startedAt = 1234L,
                    ),
                )
            val source = FakeActiveWorkoutSource(activeWorkoutRunFlow = activeRun)
            val viewModel =
                ActiveWorkoutViewModel(
                    source = source,
                    nowMillis = { 2000L },
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            viewModel.finishWorkout()
            advanceUntilIdle()
            viewModel.acknowledgeWorkoutFinished()

            assertEquals(null, viewModel.uiState.value.finishedWorkoutRunId)
        }

    @Test
    fun discardWorkoutCallsRepositoryAndEmitsDiscardSignal() =
        runTest {
            val activeRun =
                workoutRun(
                    id = 7,
                    scheduleNameSnapshot = "Push Day",
                    startedAt = 1234L,
                )
            val source = FakeActiveWorkoutSource(activeWorkoutRun = activeRun)
            val viewModel =
                ActiveWorkoutViewModel(
                    source = source,
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            viewModel.discardWorkout()
            advanceUntilIdle()

            assertEquals(7L, source.discardedWorkoutRunId)
            assertEquals(
                ActiveWorkoutUiState(
                    isLoading = false,
                    discardedWorkoutRunId = 7L,
                ),
                viewModel.uiState.value,
            )
        }

    @Test
    fun acknowledgeWorkoutDiscardedClearsDiscardSignal() =
        runTest {
            val source =
                FakeActiveWorkoutSource(
                    activeWorkoutRun =
                        workoutRun(
                            id = 7,
                            scheduleNameSnapshot = "Push Day",
                            startedAt = 1234L,
                        ),
                )
            val viewModel =
                ActiveWorkoutViewModel(
                    source = source,
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            viewModel.discardWorkout()
            advanceUntilIdle()
            viewModel.acknowledgeWorkoutDiscarded()

            assertEquals(null, viewModel.uiState.value.discardedWorkoutRunId)
        }

    @Test
    fun showExerciseInfoLoadsInfo() =
        runTest {
            val expectedInfo =
                ExerciseInfoUiModel(
                    exerciseName = "Bench press",
                    notes = "Keep shoulder blades tight.",
                    headline = null,
                    history = emptyList(),
                )
            val source = FakeActiveWorkoutSource(exerciseInfo = expectedInfo)
            val viewModel =
                ActiveWorkoutViewModel(
                    source = source,
                    coroutineScope = viewModelScope(),
                )

            viewModel.showExerciseInfo(exerciseResultId = 10L)
            advanceUntilIdle()

            assertEquals(10L, source.loadedExerciseInfoResultId)
            assertEquals(expectedInfo, viewModel.uiState.value.exerciseInfo)
            assertEquals(null, viewModel.uiState.value.errorMessage)
        }

    @Test
    fun dismissExerciseInfoClearsInfo() =
        runTest {
            val source =
                FakeActiveWorkoutSource(
                    exerciseInfo =
                        ExerciseInfoUiModel(
                            exerciseName = "Bench press",
                            notes = "Keep shoulder blades tight.",
                            headline = null,
                            history = emptyList(),
                        ),
                )
            val viewModel =
                ActiveWorkoutViewModel(
                    source = source,
                    coroutineScope = viewModelScope(),
                )
            viewModel.showExerciseInfo(exerciseResultId = 10L)
            advanceUntilIdle()

            viewModel.dismissExerciseInfo()

            assertEquals(null, viewModel.uiState.value.exerciseInfo)
        }

    @Test
    fun showExerciseInfoFailureSetsError() =
        runTest {
            val source = FakeActiveWorkoutSource(failExerciseInfo = true)
            val viewModel =
                ActiveWorkoutViewModel(
                    source = source,
                    coroutineScope = viewModelScope(),
                )

            viewModel.showExerciseInfo(exerciseResultId = 10L)
            advanceUntilIdle()

            assertEquals("Unable to load exercise info", viewModel.uiState.value.errorMessage)
            assertEquals(null, viewModel.uiState.value.exerciseInfo)
        }

    @Test
    fun exerciseInfoSurvivesSetFlowEmission() =
        runTest {
            val expectedInfo =
                ExerciseInfoUiModel(
                    exerciseName = "Bench press",
                    notes = "Keep shoulder blades tight.",
                    headline = null,
                    history = emptyList(),
                )
            val source = activeWorkoutSourceWithSets().copyExerciseInfo(expectedInfo)
            val viewModel =
                ActiveWorkoutViewModel(
                    source = source,
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()
            viewModel.showExerciseInfo(exerciseResultId = 10L)
            advanceUntilIdle()

            source.emitSetResults(
                exerciseResultId = 10L,
                setResults =
                    listOf(
                        setResult(
                            id = 20L,
                            exerciseResultId = 10L,
                            setOrder = 0,
                            reps = 10,
                            weight = 82.5,
                        ),
                    ),
            )
            advanceUntilIdle()

            assertEquals(expectedInfo, viewModel.uiState.value.exerciseInfo)
        }

    @Test
    fun activeWorkoutObservationFailureSetsErrorState() =
        runTest {
            val viewModel =
                ActiveWorkoutViewModel(
                    source = FailingActiveWorkoutSource(failurePoint = FailurePoint.ActiveWorkout),
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            assertEquals(
                ActiveWorkoutUiState(
                    isLoading = false,
                    errorMessage = "Unable to load active workout",
                ),
                viewModel.uiState.value,
            )
        }

    @Test
    fun exerciseObservationFailureSetsErrorState() =
        runTest {
            val viewModel =
                ActiveWorkoutViewModel(
                    source = FailingActiveWorkoutSource(failurePoint = FailurePoint.Exercises),
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            assertEquals(
                ActiveWorkoutUiState(
                    isLoading = false,
                    errorMessage = "Unable to load active workout",
                ),
                viewModel.uiState.value,
            )
        }

    @Test
    fun setObservationFailureSetsErrorState() =
        runTest {
            val viewModel =
                ActiveWorkoutViewModel(
                    source = FailingActiveWorkoutSource(failurePoint = FailurePoint.Sets),
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            assertEquals(
                ActiveWorkoutUiState(
                    isLoading = false,
                    errorMessage = "Unable to load active workout",
                ),
                viewModel.uiState.value,
            )
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun TestScope.viewModelScope(): CoroutineScope =
    CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler))

private fun activeWorkoutSourceWithSets(): FakeActiveWorkoutSource =
    FakeActiveWorkoutSource(
        activeWorkoutRun =
            workoutRun(
                id = 7,
                scheduleNameSnapshot = "Push Day",
                startedAt = 1234L,
            ),
        exerciseResults =
            listOf(
                exerciseResult(
                    id = 10,
                    exerciseNameSnapshot = "Bench press",
                    trackingModeSnapshot = TrackingMode.Strength.databaseValue,
                    sortOrder = 0,
                ),
                exerciseResult(
                    id = 11,
                    exerciseNameSnapshot = "Run",
                    trackingModeSnapshot = TrackingMode.Timed.databaseValue,
                    sortOrder = 1,
                ),
            ),
        setResultsByExerciseResultId =
            mapOf(
                10L to
                    listOf(
                        setResult(
                            id = 20,
                            exerciseResultId = 10,
                            setOrder = 0,
                            reps = 8,
                            weight = 80.0,
                            isCompleted = false,
                        ),
                    ),
                11L to
                    listOf(
                        setResult(
                            id = 21,
                            exerciseResultId = 11,
                            setOrder = 0,
                            durationSeconds = 600L,
                            distance = 2.5,
                        ),
                    ),
            ),
    )

private fun FakeActiveWorkoutSource.copyExerciseInfo(exerciseInfo: ExerciseInfoUiModel): FakeActiveWorkoutSource =
    FakeActiveWorkoutSource(
        activeWorkoutRun =
            workoutRun(
                id = 7,
                scheduleNameSnapshot = "Push Day",
                startedAt = 1234L,
            ),
        exerciseResults =
            listOf(
                exerciseResult(
                    id = 10,
                    exerciseNameSnapshot = "Bench press",
                    trackingModeSnapshot = TrackingMode.Strength.databaseValue,
                    sortOrder = 0,
                ),
                exerciseResult(
                    id = 11,
                    exerciseNameSnapshot = "Run",
                    trackingModeSnapshot = TrackingMode.Timed.databaseValue,
                    sortOrder = 1,
                ),
            ),
        setResultsByExerciseResultId =
            mapOf(
                10L to
                    listOf(
                        setResult(
                            id = 20,
                            exerciseResultId = 10,
                            setOrder = 0,
                            reps = 8,
                            weight = 80.0,
                            isCompleted = false,
                        ),
                    ),
                11L to
                    listOf(
                        setResult(
                            id = 21,
                            exerciseResultId = 11,
                            setOrder = 0,
                            durationSeconds = 600L,
                            distance = 2.5,
                        ),
                    ),
            ),
        exerciseInfo = exerciseInfo,
    )

private fun activeWorkoutUiStateWithSets(): ActiveWorkoutUiState =
    ActiveWorkoutUiState(
        isLoading = false,
        activeWorkout =
            ActiveWorkoutUiModel(
                runId = 7,
                scheduleName = "Push Day",
                startedAt = 1234L,
                exercises =
                    listOf(
                        WorkoutExerciseUiModel(
                            exerciseResultId = 10,
                            exerciseName = "Bench press",
                            trackingMode = TrackingMode.Strength,
                            notes = null,
                            sets =
                                listOf(
                                    WorkoutSetUiModel(
                                        id = 20,
                                        setOrder = 0,
                                        reps = 8,
                                        weight = 80.0,
                                        durationSeconds = null,
                                        distance = null,
                                        notes = null,
                                        isCompleted = false,
                                    ),
                                ),
                        ),
                        WorkoutExerciseUiModel(
                            exerciseResultId = 11,
                            exerciseName = "Run",
                            trackingMode = TrackingMode.Timed,
                            notes = null,
                            sets =
                                listOf(
                                    WorkoutSetUiModel(
                                        id = 21,
                                        setOrder = 0,
                                        reps = null,
                                        weight = null,
                                        durationSeconds = 600L,
                                        distance = 2.5,
                                        notes = null,
                                        isCompleted = true,
                                    ),
                                ),
                        ),
                    ),
            ),
    )

private data class InvalidSetCase(
    val reps: Int? = null,
    val weight: Double? = null,
    val durationSeconds: Long? = null,
    val distance: Double? = null,
    val expectedError: String,
)

private enum class FailurePoint {
    ActiveWorkout,
    Exercises,
    Sets,
}

private class FailingActiveWorkoutSource(
    private val failurePoint: FailurePoint,
) : ActiveWorkoutSource {
    override fun observeActiveWorkoutRun(): Flow<WorkoutRunEntity?> =
        if (failurePoint == FailurePoint.ActiveWorkout) {
            flow { throw IllegalStateException("active failed") }
        } else {
            MutableStateFlow(
                workoutRun(
                    id = 7,
                    scheduleNameSnapshot = "Push Day",
                    startedAt = 1234L,
                ),
            )
        }

    override fun observeExerciseResults(id: Long): Flow<ExerciseResults> =
        if (failurePoint == FailurePoint.Exercises) {
            flow { throw IllegalStateException("exercises failed") }
        } else {
            MutableStateFlow(
                listOf(
                    exerciseResult(
                        id = 10,
                        exerciseNameSnapshot = "Bench press",
                        trackingModeSnapshot = TrackingMode.Strength.databaseValue,
                        sortOrder = 0,
                    ),
                ),
            )
        }

    override fun observeSetResults(id: Long): Flow<SetResults> =
        if (failurePoint == FailurePoint.Sets) {
            flow { throw IllegalStateException("sets failed") }
        } else {
            MutableStateFlow(emptyList())
        }

    override suspend fun addSet(
        exerciseResultId: Long,
        reps: Int?,
        weight: Double?,
        durationSeconds: Long?,
        distance: Double?,
    ): Long = 1L

    override suspend fun addSetFromPrevious(exerciseResultId: Long): Long = 1L

    override suspend fun updateSetMetrics(
        setId: Long,
        reps: Int?,
        weight: Double?,
        durationSeconds: Long?,
        distance: Double?,
    ) = Unit

    override suspend fun updateSetCompletion(
        setId: Long,
        isCompleted: Boolean,
    ) = Unit

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

@Suppress("LongParameterList")
private class FakeActiveWorkoutSource(
    private val activeWorkoutRunFlow: MutableStateFlow<WorkoutRunEntity?> = MutableStateFlow(null),
    activeWorkoutRun: WorkoutRunEntity? = null,
    exerciseResults: List<ExerciseResultEntity> = emptyList(),
    setResultsByExerciseResultId: Map<Long, List<SetResultEntity>> = emptyMap(),
    private val exerciseInfo: ExerciseInfoUiModel =
        ExerciseInfoUiModel(
            exerciseName = "Bench press",
            notes = null,
            headline = null,
            history = emptyList(),
        ),
    private val failExerciseInfo: Boolean = false,
    availableExercises: List<ExerciseEntity> = emptyList(),
    private val failAddExercise: Boolean = false,
) : ActiveWorkoutSource {
    private val exerciseResultFlow = MutableStateFlow(exerciseResults)
    private val availableExerciseFlow = MutableStateFlow(availableExercises)
    private val setResultFlows =
        setResultsByExerciseResultId.mapValuesTo(mutableMapOf()) { (_, sets) ->
            MutableStateFlow(sets)
        }
    private val emptySetResults = MutableStateFlow(emptyList<SetResultEntity>())

    var addedExerciseResultId: Long? = null
        private set
    var addedReps: Int? = null
        private set
    var addedWeight: Double? = null
        private set
    var addedDurationSeconds: Long? = null
        private set
    var addedDistance: Double? = null
        private set
    var finishedWorkoutRunId: Long? = null
        private set
    var finishedNowMillis: Long? = null
        private set
    var discardedWorkoutRunId: Long? = null
        private set
    var addSetCallCount = 0
        private set
    var addedSetFromPreviousExerciseResultId: Long? = null
        private set
    var addedEmptySetExerciseResultId: Long? = null
        private set
    var updatedSetId: Long? = null
        private set
    var updatedReps: Int? = null
        private set
    var updatedWeight: Double? = null
        private set
    var updatedDurationSeconds: Long? = null
        private set
    var updatedDistance: Double? = null
        private set
    var updatedCompletion: Pair<Long, Boolean>? = null
        private set
    var updatedExerciseNotes: Pair<Long, String?>? = null
        private set
    var updatedWorkoutNotes: Pair<Long, String?>? = null
        private set
    var updatedSetNotes: Pair<Long, String?>? = null
        private set
    var updateSetMetricsCallCount = 0
        private set
    var loadedExerciseInfoResultId: Long? = null
        private set
    var addedExerciseToWorkout: Pair<Long, Long>? = null
        private set

    init {
        activeWorkoutRun?.let { activeWorkoutRunFlow.value = it }
    }

    override fun observeActiveWorkoutRun(): Flow<WorkoutRunEntity?> = activeWorkoutRunFlow

    override fun observeExerciseResults(id: Long): Flow<ExerciseResults> = exerciseResultFlow

    override fun observeSetResults(id: Long): Flow<SetResults> = setResultFlows[id] ?: emptySetResults

    override fun observeAvailableExercises(): Flow<List<ExerciseEntity>> = availableExerciseFlow

    fun emitSetResults(
        exerciseResultId: Long,
        setResults: List<SetResultEntity>,
    ) {
        setResultFlows.getOrPut(exerciseResultId) { MutableStateFlow(emptyList()) }.value = setResults
    }

    override suspend fun getExerciseInfo(exerciseResultId: Long): ExerciseInfoUiModel {
        if (failExerciseInfo) {
            error("exercise info failed")
        }
        loadedExerciseInfoResultId = exerciseResultId
        return exerciseInfo
    }

    override suspend fun addSet(
        exerciseResultId: Long,
        reps: Int?,
        weight: Double?,
        durationSeconds: Long?,
        distance: Double?,
    ): Long {
        addSetCallCount += 1
        addedExerciseResultId = exerciseResultId
        addedReps = reps
        addedWeight = weight
        addedDurationSeconds = durationSeconds
        addedDistance = distance
        return 1L
    }

    override suspend fun addSetFromPrevious(exerciseResultId: Long): Long {
        addedSetFromPreviousExerciseResultId = exerciseResultId
        return 1L
    }

    override suspend fun addEmptySet(exerciseResultId: Long): Long {
        addedEmptySetExerciseResultId = exerciseResultId
        return 1L
    }

    override suspend fun addExerciseToActiveWorkout(
        workoutRunId: Long,
        exerciseId: Long,
    ): Long {
        if (failAddExercise) {
            error("add failed")
        }
        addedExerciseToWorkout = workoutRunId to exerciseId
        return 1L
    }

    override suspend fun updateSetMetrics(
        setId: Long,
        reps: Int?,
        weight: Double?,
        durationSeconds: Long?,
        distance: Double?,
    ) {
        updateSetMetricsCallCount += 1
        updatedSetId = setId
        updatedReps = reps
        updatedWeight = weight
        updatedDurationSeconds = durationSeconds
        updatedDistance = distance
    }

    override suspend fun updateSetCompletion(
        setId: Long,
        isCompleted: Boolean,
    ) {
        updatedCompletion = setId to isCompleted
    }

    override suspend fun updateExerciseResultNotes(
        exerciseResultId: Long,
        notes: String?,
    ) {
        updatedExerciseNotes = exerciseResultId to notes
    }

    override suspend fun updateWorkoutNotes(
        workoutRunId: Long,
        notes: String?,
    ) {
        updatedWorkoutNotes = workoutRunId to notes
    }

    override suspend fun updateSetNotes(
        setId: Long,
        notes: String?,
    ) {
        updatedSetNotes = setId to notes
    }

    override suspend fun deleteSet(setId: Long) = Unit

    override suspend fun deleteExerciseResult(exerciseResultId: Long) = Unit

    override suspend fun finishWorkout(
        workoutRunId: Long,
        nowMillis: Long,
    ) {
        finishedWorkoutRunId = workoutRunId
        finishedNowMillis = nowMillis
    }

    override suspend fun discardWorkout(workoutRunId: Long) {
        discardedWorkoutRunId = workoutRunId
    }
}

private fun exercise(
    id: Long,
    name: String,
    trackingMode: String,
): ExerciseEntity =
    ExerciseEntity(
        id = id,
        name = name,
        notes = null,
        trackingMode = trackingMode,
        createdAt = 100L,
        updatedAt = 100L,
    )

private fun workoutRun(
    id: Long,
    scheduleNameSnapshot: String,
    startedAt: Long,
): WorkoutRunEntity =
    WorkoutRunEntity(
        id = id,
        scheduleId = 1,
        scheduleNameSnapshot = scheduleNameSnapshot,
        startedAt = startedAt,
        finishedAt = null,
        notes = null,
    )

private fun exerciseResult(
    id: Long,
    exerciseNameSnapshot: String,
    trackingModeSnapshot: String,
    sortOrder: Int,
): ExerciseResultEntity =
    ExerciseResultEntity(
        id = id,
        workoutRunId = 7,
        exerciseId = id,
        exerciseNameSnapshot = exerciseNameSnapshot,
        trackingModeSnapshot = trackingModeSnapshot,
        sortOrder = sortOrder,
        notes = null,
    )

private fun setResult(
    id: Long,
    exerciseResultId: Long,
    setOrder: Int,
    reps: Int? = null,
    weight: Double? = null,
    durationSeconds: Long? = null,
    distance: Double? = null,
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
        notes = null,
        isCompleted = isCompleted,
    )
