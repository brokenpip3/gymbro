package com.brokenpip3.gymbro.ui.screens.schedules

import com.brokenpip3.gymbro.data.entities.ExerciseEntity
import com.brokenpip3.gymbro.data.entities.ScheduleEntity
import com.brokenpip3.gymbro.data.entities.ScheduleExerciseEntity
import com.brokenpip3.gymbro.data.entities.WorkoutRunEntity
import com.brokenpip3.gymbro.data.repositories.MoveDirection
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleDetailViewModelTest {
    @Test
    fun assignExerciseDelegatesToRepositoryAndCallsOnAssigned() =
        runTest {
            val source = FakeScheduleDetailSource()
            var assigned = false
            val viewModel =
                ScheduleDetailViewModel(
                    scheduleId = 7L,
                    source = source,
                    workoutStarter = FakeWorkoutStarter(),
                    coroutineScope = viewModelScope(),
                )

            viewModel.assignExercise(42L) {
                assigned = true
            }

            assertEquals(7L, source.assignedScheduleId)
            assertEquals(42L, source.assignedExerciseId)
            assertTrue(assigned)
        }

    @Test
    fun assignExercisesPreservesTheUserSelectionOrder() =
        runTest {
            val source = FakeScheduleDetailSource()
            var assigned = false
            val viewModel =
                ScheduleDetailViewModel(
                    scheduleId = 7L,
                    source = source,
                    workoutStarter = FakeWorkoutStarter(),
                    coroutineScope = viewModelScope(),
                )

            viewModel.assignExercises(listOf(42L, 7L, 99L)) {
                assigned = true
            }

            assertEquals(listOf(42L, 7L, 99L), source.assignedExerciseIds)
            assertTrue(assigned)
        }

    @Test
    fun assignmentFailureSetsError() =
        runTest {
            val source = FakeScheduleDetailSource(assignError = IllegalStateException("failed"))
            var assigned = false
            val viewModel =
                ScheduleDetailViewModel(
                    scheduleId = 7L,
                    source = source,
                    workoutStarter = FakeWorkoutStarter(),
                    coroutineScope = viewModelScope(),
                )

            viewModel.assignExercise(42L) {
                assigned = true
            }

            assertEquals("Unable to add exercise", viewModel.uiState.value.assignmentError)
            assertFalse(assigned)
        }

    @Test
    fun startWorkoutDelegatesToStarterAndCallsOnStartedWithRunId() =
        runTest {
            val source =
                FakeScheduleDetailSource(
                    assignedExercises =
                        MutableStateFlow(
                            listOf(
                                scheduleExercise(id = 3, scheduleId = 7, exerciseId = 42),
                            ),
                        ),
                )
            val workoutStarter = FakeWorkoutStarter(startedRunId = 99)
            var startedRunId: Long? = null
            val viewModel =
                ScheduleDetailViewModel(
                    scheduleId = 7L,
                    source = source,
                    workoutStarter = workoutStarter,
                    nowMillis = { 1234L },
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            viewModel.startWorkout { runId ->
                startedRunId = runId
            }

            assertEquals(7L, workoutStarter.startedScheduleId)
            assertEquals(1234L, workoutStarter.startedNowMillis)
            assertEquals(99L, startedRunId)
            assertEquals(null, viewModel.uiState.value.startError)
        }

    @Test
    fun startWorkoutIsBlockedWhenScheduleHasNoAssignedExercises() =
        runTest {
            val workoutStarter = FakeWorkoutStarter(startedRunId = 99)
            var startedRunId: Long? = null
            val viewModel =
                ScheduleDetailViewModel(
                    scheduleId = 7L,
                    source = FakeScheduleDetailSource(),
                    workoutStarter = workoutStarter,
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            viewModel.startWorkout { runId ->
                startedRunId = runId
            }

            assertEquals(null, workoutStarter.startedScheduleId)
            assertEquals(null, startedRunId)
            assertEquals("Add at least one exercise to start a workout.", viewModel.uiState.value.startError)
            assertFalse(viewModel.uiState.value.canStartWorkout)
        }

    @Test
    fun startWorkoutIgnoresDuplicateInvocationsWhileStartIsInProgress() =
        runTest {
            val source =
                FakeScheduleDetailSource(
                    assignedExercises =
                        MutableStateFlow(
                            listOf(
                                scheduleExercise(id = 3, scheduleId = 7, exerciseId = 42),
                            ),
                        ),
                )
            val pendingRunId = CompletableDeferred<Long>()
            val workoutStarter = FakeWorkoutStarter(pendingRunId = pendingRunId)
            val startedRunIds = mutableListOf<Long>()
            val viewModel =
                ScheduleDetailViewModel(
                    scheduleId = 7L,
                    source = source,
                    workoutStarter = workoutStarter,
                    nowMillis = { 1234L },
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            viewModel.startWorkout { runId ->
                startedRunIds += runId
            }
            viewModel.startWorkout { runId ->
                startedRunIds += runId
            }

            assertEquals(1, workoutStarter.startCallCount)
            assertTrue(viewModel.uiState.value.isStarting)

            pendingRunId.complete(99L)
            advanceUntilIdle()

            assertEquals(listOf(99L), startedRunIds)
        }

    @Test
    fun activeWorkoutRunUsesResumeWorkoutActionLabel() =
        runTest {
            val activeRun =
                MutableStateFlow<WorkoutRunEntity?>(
                    workoutRun(id = 12, scheduleId = 7),
                )
            val viewModel =
                ScheduleDetailViewModel(
                    scheduleId = 7L,
                    source = FakeScheduleDetailSource(),
                    workoutStarter = FakeWorkoutStarter(activeWorkoutRun = activeRun),
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            assertEquals("Resume Workout", viewModel.uiState.value.workoutActionLabel)
            assertEquals(
                12L,
                viewModel.uiState.value.activeWorkoutRun
                    ?.id,
            )
        }

    @Test
    fun activeWorkoutFromAnotherScheduleDoesNotOfferResume() =
        runTest {
            val activeRun =
                MutableStateFlow<WorkoutRunEntity?>(
                    workoutRun(id = 12, scheduleId = 99),
                )
            val viewModel =
                ScheduleDetailViewModel(
                    scheduleId = 7L,
                    source = FakeScheduleDetailSource(),
                    workoutStarter = FakeWorkoutStarter(activeWorkoutRun = activeRun),
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            assertEquals(null, viewModel.uiState.value.activeWorkoutRun)
            assertEquals(
                12L,
                viewModel.uiState.value.otherActiveWorkoutRun
                    ?.id,
            )
            assertEquals("Resume Active Workout", viewModel.uiState.value.workoutActionLabel)
        }

    @Test
    fun startWorkoutWithActiveRunCallsOnStartedWithoutStartingNewRun() =
        runTest {
            val activeRun =
                MutableStateFlow<WorkoutRunEntity?>(
                    workoutRun(id = 12, scheduleId = 7),
                )
            val workoutStarter = FakeWorkoutStarter(activeWorkoutRun = activeRun)
            var startedRunId: Long? = null
            val viewModel =
                ScheduleDetailViewModel(
                    scheduleId = 7L,
                    source = FakeScheduleDetailSource(),
                    workoutStarter = workoutStarter,
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            viewModel.startWorkout { runId ->
                startedRunId = runId
            }

            assertEquals(12L, startedRunId)
            assertEquals(0, workoutStarter.startCallCount)
        }

    @Test
    fun startWorkoutFromAnotherScheduleResumesExistingRunWithoutStartingNewOne() =
        runTest {
            val activeRun =
                MutableStateFlow<WorkoutRunEntity?>(
                    workoutRun(id = 12, scheduleId = 99),
                )
            val workoutStarter = FakeWorkoutStarter(activeWorkoutRun = activeRun)
            var startedRunId: Long? = null
            val viewModel =
                ScheduleDetailViewModel(
                    scheduleId = 7L,
                    source = FakeScheduleDetailSource(),
                    workoutStarter = workoutStarter,
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            viewModel.startWorkout { runId ->
                startedRunId = runId
            }

            assertEquals(12L, startedRunId)
            assertEquals(0, workoutStarter.startCallCount)
        }

    @Test
    fun completedWorkoutRunStartsNewWorkoutInsteadOfResuming() =
        runTest {
            val source =
                FakeScheduleDetailSource(
                    assignedExercises =
                        MutableStateFlow(
                            listOf(
                                scheduleExercise(id = 3, scheduleId = 7, exerciseId = 42),
                            ),
                        ),
                )
            val activeRun =
                MutableStateFlow<WorkoutRunEntity?>(
                    workoutRun(id = 12, scheduleId = 7, finishedAt = 2000L),
                )
            val workoutStarter =
                FakeWorkoutStarter(
                    activeWorkoutRun = activeRun,
                    startedRunId = 99,
                )
            var startedRunId: Long? = null
            val viewModel =
                ScheduleDetailViewModel(
                    scheduleId = 7L,
                    source = source,
                    workoutStarter = workoutStarter,
                    nowMillis = { 3000L },
                    coroutineScope = viewModelScope(),
                )
            advanceUntilIdle()

            viewModel.startWorkout { runId ->
                startedRunId = runId
            }

            assertEquals("Start Workout", viewModel.uiState.value.workoutActionLabel)
            assertEquals(7L, workoutStarter.startedScheduleId)
            assertEquals(3000L, workoutStarter.startedNowMillis)
            assertEquals(99L, startedRunId)
        }

    @Test
    fun removeExerciseDelegatesToRepository() =
        runTest {
            val source = FakeScheduleDetailSource()
            val viewModel =
                ScheduleDetailViewModel(
                    scheduleId = 7L,
                    source = source,
                    workoutStarter = FakeWorkoutStarter(),
                    coroutineScope = viewModelScope(),
                )

            viewModel.removeExerciseFromSchedule(11)

            assertEquals(11L, source.removedScheduleExerciseId)
        }

    @Test
    fun moveExerciseDelegatesToRepository() =
        runTest {
            val source = FakeScheduleDetailSource()
            val viewModel =
                ScheduleDetailViewModel(
                    scheduleId = 7L,
                    source = source,
                    workoutStarter = FakeWorkoutStarter(),
                    coroutineScope = viewModelScope(),
                )

            viewModel.moveScheduleExercise(
                scheduleExerciseId = 11,
                direction = MoveDirection.Down,
            )

            assertEquals(7L, source.movedScheduleId)
            assertEquals(11L, source.movedScheduleExerciseId)
            assertEquals(MoveDirection.Down, source.movedDirection)
        }

    @Test
    fun updateScheduleExerciseTargetsDelegatesToSource() =
        runTest {
            val source = FakeScheduleDetailSource()
            val targets =
                ScheduleExerciseTargets(
                    targetSets = 3,
                    targetReps = 10,
                    targetWeight = 50.0,
                    targetDurationSeconds = null,
                    targetDistance = null,
                )
            val viewModel =
                ScheduleDetailViewModel(
                    scheduleId = 7L,
                    source = source,
                    workoutStarter = FakeWorkoutStarter(),
                    coroutineScope = viewModelScope(),
                )

            viewModel.updateScheduleExerciseTargets(11L, targets)

            assertEquals(11L, source.updatedTargetsScheduleExerciseId)
            assertEquals(targets, source.updatedTargets)
        }

    @Test
    fun removeExerciseFailureSetsError() =
        runTest {
            val source =
                FakeScheduleDetailSource(
                    removeError = IllegalStateException("failed"),
                )
            val viewModel =
                ScheduleDetailViewModel(
                    scheduleId = 7L,
                    source = source,
                    workoutStarter = FakeWorkoutStarter(),
                    coroutineScope = viewModelScope(),
                )

            viewModel.removeExerciseFromSchedule(11)

            assertEquals("Unable to update schedule exercises", viewModel.uiState.value.scheduleExerciseError)
        }

    @Test
    fun moveExerciseFailureSetsError() =
        runTest {
            val source =
                FakeScheduleDetailSource(
                    moveError = IllegalStateException("failed"),
                )
            val viewModel =
                ScheduleDetailViewModel(
                    scheduleId = 7L,
                    source = source,
                    workoutStarter = FakeWorkoutStarter(),
                    coroutineScope = viewModelScope(),
                )

            viewModel.moveScheduleExercise(11, MoveDirection.Up)

            assertEquals("Unable to update schedule exercises", viewModel.uiState.value.scheduleExerciseError)
        }

    @Test
    fun updateScheduleExerciseTargetsFailureSetsError() =
        runTest {
            val source =
                FakeScheduleDetailSource(
                    updateTargetsError = IllegalStateException("failed"),
                )
            val viewModel =
                ScheduleDetailViewModel(
                    scheduleId = 7L,
                    source = source,
                    workoutStarter = FakeWorkoutStarter(),
                    coroutineScope = viewModelScope(),
                )

            viewModel.updateScheduleExerciseTargets(
                11L,
                ScheduleExerciseTargets(
                    targetSets = 3,
                    targetReps = 10,
                    targetWeight = 50.0,
                    targetDurationSeconds = null,
                    targetDistance = null,
                ),
            )

            assertEquals("Unable to update schedule exercises", viewModel.uiState.value.scheduleExerciseError)
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun TestScope.viewModelScope(): CoroutineScope =
    CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler))

private class FakeScheduleDetailSource(
    private val schedule: Flow<ScheduleEntity?> = MutableStateFlow(null),
    private val assignedExercises: Flow<List<ScheduleExerciseEntity>> = MutableStateFlow(emptyList()),
    private val availableExercises: Flow<List<ExerciseEntity>> = MutableStateFlow(emptyList()),
    private val assignError: Throwable? = null,
    private val removeError: Throwable? = null,
    private val moveError: Throwable? = null,
    private val updateTargetsError: Throwable? = null,
) : ScheduleDetailSource {
    var assignedScheduleId: Long? = null
        private set
    var assignedExerciseId: Long? = null
        private set
    val assignedExerciseIds = mutableListOf<Long>()
    var removedScheduleExerciseId: Long? = null
        private set
    var movedScheduleId: Long? = null
        private set
    var movedScheduleExerciseId: Long? = null
        private set
    var movedDirection: MoveDirection? = null
        private set
    var updatedTargetsScheduleExerciseId: Long? = null
        private set
    var updatedTargets: ScheduleExerciseTargets? = null
        private set

    override fun observeSchedule(id: Long): Flow<ScheduleEntity?> = schedule

    override fun observeScheduleExercises(scheduleId: Long): Flow<List<ScheduleExerciseEntity>> = assignedExercises

    override fun observeAvailableExercises(): Flow<List<ExerciseEntity>> = availableExercises

    override suspend fun assignExercise(
        scheduleId: Long,
        exerciseId: Long,
    ): Long {
        assignError?.let { throw it }
        assignedScheduleId = scheduleId
        assignedExerciseId = exerciseId
        assignedExerciseIds += exerciseId
        return 1L
    }

    override suspend fun removeExerciseFromSchedule(scheduleExerciseId: Long) {
        removeError?.let { throw it }
        removedScheduleExerciseId = scheduleExerciseId
    }

    override suspend fun moveScheduleExercise(
        scheduleId: Long,
        scheduleExerciseId: Long,
        direction: MoveDirection,
    ) {
        moveError?.let { throw it }
        movedScheduleId = scheduleId
        movedScheduleExerciseId = scheduleExerciseId
        movedDirection = direction
    }

    override suspend fun updateScheduleExerciseTargets(
        scheduleExerciseId: Long,
        targets: ScheduleExerciseTargets,
    ) {
        updateTargetsError?.let { throw it }
        updatedTargetsScheduleExerciseId = scheduleExerciseId
        updatedTargets = targets
    }
}

private class FakeWorkoutStarter(
    private val activeWorkoutRun: MutableStateFlow<WorkoutRunEntity?> = MutableStateFlow(null),
    private val startedRunId: Long = 1,
    private val pendingRunId: CompletableDeferred<Long>? = null,
) : WorkoutStarter {
    var startedScheduleId: Long? = null
        private set
    var startedNowMillis: Long? = null
        private set
    var startCallCount = 0
        private set

    override fun observeActiveWorkoutRun(): Flow<WorkoutRunEntity?> = activeWorkoutRun

    override suspend fun startWorkout(
        scheduleId: Long,
        nowMillis: Long,
    ): Long {
        startCallCount += 1
        startedScheduleId = scheduleId
        startedNowMillis = nowMillis
        pendingRunId?.let { return it.await() }
        return startedRunId
    }
}

private fun scheduleExercise(
    id: Long = 1,
    scheduleId: Long = 7,
    exerciseId: Long = 42,
): ScheduleExerciseEntity =
    ScheduleExerciseEntity(
        id = id,
        scheduleId = scheduleId,
        exerciseId = exerciseId,
        sortOrder = 0,
        targetSets = null,
        targetReps = null,
        targetWeight = null,
        targetDurationSeconds = null,
        targetDistance = null,
    )

private fun workoutRun(
    id: Long = 1,
    scheduleId: Long = 7,
    finishedAt: Long? = null,
): WorkoutRunEntity =
    WorkoutRunEntity(
        id = id,
        scheduleId = scheduleId,
        scheduleNameSnapshot = "Push Day",
        startedAt = 1234L,
        finishedAt = finishedAt,
        notes = null,
    )
