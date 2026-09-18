package com.brokenpip3.gymbro.ui.screens.schedules

import com.brokenpip3.gymbro.data.entities.ScheduleEntity
import com.brokenpip3.gymbro.data.entities.ScheduleExerciseEntity
import com.brokenpip3.gymbro.data.entities.WorkoutRunEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleListViewModelTest {
    @Test
    fun scheduleRowsExposeLoadingStateUntilFirstMetadataEmission() =
        runTest {
            val schedules = MutableSharedFlow<List<ScheduleEntity>>(replay = 1)
            val viewModel =
                ScheduleListViewModel(
                    repository = FakeScheduleRepository(schedules = schedules),
                    coroutineScope = backgroundScope,
                )

            runCurrent()
            assertEquals(true, viewModel.isLoading.value)
            schedules.emit(emptyList())
            runCurrent()

            assertEquals(false, viewModel.isLoading.value)
        }

    @Test
    fun scheduleRowsIncludeExerciseCountMetadata() =
        runTest {
            val schedulesFlow =
                MutableStateFlow(
                    listOf(
                        scheduleEntity(id = 7, name = "Leg Day"),
                        scheduleEntity(id = 8, name = "Push Day"),
                    ),
                )
            val repository =
                FakeScheduleRepository(
                    schedules = schedulesFlow,
                    scheduleExercisesBySchedule =
                        mapOf(
                            7L to
                                MutableStateFlow(
                                    listOf(
                                        scheduleExercise(scheduleId = 7, id = 1),
                                        scheduleExercise(scheduleId = 7, id = 2),
                                    ),
                                ),
                            8L to MutableStateFlow(emptyList()),
                        ),
                )

            val viewModel =
                ScheduleListViewModel(
                    repository = repository,
                    coroutineScope = backgroundScope,
                )
            val rows = viewModel.scheduleRows.first { it.isNotEmpty() }

            assertEquals(
                listOf("2 exercises", "No exercises"),
                rows.map { it.exerciseCountLabel },
            )
        }

    @Test
    fun scheduleRowsMarkActiveWorkoutSchedule() =
        runTest {
            val repository =
                FakeScheduleRepository(
                    schedules =
                        MutableStateFlow(
                            listOf(
                                scheduleEntity(id = 7, name = "Leg Day"),
                                scheduleEntity(id = 8, name = "Push Day"),
                            ),
                        ),
                    activeWorkoutRun =
                        MutableStateFlow(
                            workoutRun(
                                id = 99,
                                scheduleId = 8,
                                finishedAt = null,
                            ),
                        ),
                )

            val viewModel =
                ScheduleListViewModel(
                    repository = repository,
                    coroutineScope = backgroundScope,
                )
            val rows = viewModel.scheduleRows.first { it.isNotEmpty() }

            assertEquals(
                listOf(false, true),
                rows.map { it.isActiveWorkout },
            )
        }

    @Test
    fun scheduleRowsIncludeLatestCompletedWorkoutSummary() =
        runTest {
            val repository =
                FakeScheduleRepository(
                    schedules = MutableStateFlow(listOf(scheduleEntity(id = 7, name = "Leg Day"))),
                    completedWorkoutRuns =
                        MutableStateFlow(
                            listOf(
                                workoutRun(
                                    id = 1,
                                    scheduleId = 7,
                                    startedAt = 30L * 60L * 1000L,
                                    finishedAt = 60L * 60L * 1000L,
                                ),
                                workoutRun(
                                    id = 2,
                                    scheduleId = 7,
                                    startedAt = 2L * 60L * 60L * 1000L,
                                    finishedAt = 3L * 60L * 60L * 1000L,
                                ),
                            ),
                        ),
                )

            val viewModel =
                ScheduleListViewModel(
                    repository = repository,
                    nowMillis = { 4L * 60L * 60L * 1000L },
                    coroutineScope = backgroundScope,
                )
            val rows = viewModel.scheduleRows.first { it.isNotEmpty() }

            assertEquals("Last: today · 60 min", rows.single().lastCompletedLabel)
        }

    @Test
    fun blankNameShowsValidationErrorAndDoesNotSave() =
        runTest {
            val repository = FakeScheduleRepository()
            val viewModel =
                ScheduleListViewModel(
                    repository = repository,
                    nowMillis = { 1234L },
                    coroutineScope = backgroundScope,
                )

            viewModel.updateName("   ")
            viewModel.saveSchedule(onSaved = {})

            assertEquals("Name is required", viewModel.formState.value.nameError)
            assertEquals(0, repository.createCount)
        }

    @Test
    fun savingScheduleTrimsNameAndNotes() =
        runTest {
            val repository = FakeScheduleRepository(createdId = 42L)
            var savedId: Long? = null
            val viewModel =
                ScheduleListViewModel(
                    repository = repository,
                    nowMillis = { 1234L },
                    coroutineScope = backgroundScope,
                )

            viewModel.updateName("  Push Day  ")
            viewModel.updateNotes("  Chest  ")
            viewModel.saveSchedule(onSaved = { savedId = it })

            assertEquals("Push Day", repository.createdName)
            assertEquals("Chest", repository.createdNotes)
            assertEquals(42L, savedId)
        }

    @Test
    fun loadScheduleForEditPrefillsForm() =
        runTest {
            val repository =
                FakeScheduleRepository(
                    schedule =
                        scheduleEntity(
                            id = 7,
                            name = "Leg Day",
                            notes = "Quads",
                        ),
                )
            val viewModel =
                ScheduleListViewModel(
                    repository = repository,
                    coroutineScope = backgroundScope,
                )

            viewModel.loadScheduleForEdit(7)
            advanceUntilIdle()

            assertEquals("Leg Day", viewModel.formState.value.name)
            assertEquals("Quads", viewModel.formState.value.notes)
        }

    @Test
    fun savingLoadedScheduleUpdatesExistingScheduleAndCallsOnSaved() =
        runTest {
            val repository =
                FakeScheduleRepository(
                    schedule = scheduleEntity(id = 7),
                )
            var savedId: Long? = null
            val viewModel =
                ScheduleListViewModel(
                    repository = repository,
                    nowMillis = { 1234L },
                    coroutineScope = backgroundScope,
                )
            viewModel.loadScheduleForEdit(7)
            advanceUntilIdle()

            viewModel.updateName("  Pull Day  ")
            viewModel.updateNotes("  Back  ")
            viewModel.saveSchedule(onSaved = { savedId = it })

            assertEquals(7L, repository.updatedId)
            assertEquals("Pull Day", repository.updatedName)
            assertEquals("Back", repository.updatedNotes)
            assertEquals(1234L, repository.updatedNowMillis)
            assertEquals(7L, savedId)
        }

    @Test
    fun savingLoadedScheduleThatDisappearedKeepsFormOpenAndShowsNotFound() =
        runTest {
            val repository =
                FakeScheduleRepository(
                    schedule = scheduleEntity(id = 7),
                    updateError = IllegalArgumentException("Schedule 7 was not found"),
                )
            var savedId: Long? = null
            val viewModel =
                ScheduleListViewModel(
                    repository = repository,
                    nowMillis = { 1234L },
                    coroutineScope = backgroundScope,
                )
            viewModel.loadScheduleForEdit(7)
            advanceUntilIdle()

            viewModel.updateName("Leg Day")
            viewModel.saveSchedule(onSaved = { savedId = it })

            assertEquals(null, savedId)
            assertEquals(7L, viewModel.formState.value.editingScheduleId)
            assertEquals("Leg Day", viewModel.formState.value.name)
            assertEquals("Schedule not found", viewModel.formState.value.saveError)
            assertFalse(viewModel.formState.value.isSaving)
        }

    @Test
    fun loadMissingScheduleForEditShowsError() =
        runTest {
            val viewModel =
                ScheduleListViewModel(
                    repository = FakeScheduleRepository(schedule = null),
                    coroutineScope = backgroundScope,
                )

            viewModel.loadScheduleForEdit(7)
            advanceUntilIdle()

            assertEquals("Schedule not found", viewModel.formState.value.saveError)
            assertFalse(viewModel.formState.value.isSaving)
        }

    @Test
    fun deleteScheduleDelegatesToRepository() =
        runTest {
            val repository = FakeScheduleRepository()
            val viewModel =
                ScheduleListViewModel(
                    repository = repository,
                    coroutineScope = backgroundScope,
                )

            viewModel.deleteSchedule(7)

            assertEquals(7L, repository.deletedId)
        }

    @Test
    fun deleteScheduleFailureShowsListError() =
        runTest {
            val repository = FakeScheduleRepository(deleteError = IllegalStateException("failed"))
            val viewModel =
                ScheduleListViewModel(
                    repository = repository,
                    coroutineScope = backgroundScope,
                )

            viewModel.deleteSchedule(7)

            assertEquals("Unable to delete schedule", viewModel.listError.value)
        }
}

private fun scheduleEntity(
    id: Long = 1,
    name: String = "Push Day",
    notes: String? = null,
): ScheduleEntity =
    ScheduleEntity(
        id = id,
        name = name,
        notes = notes,
        createdAt = 100L,
        updatedAt = 200L,
    )

private fun scheduleExercise(
    scheduleId: Long,
    id: Long = 1,
): ScheduleExerciseEntity =
    ScheduleExerciseEntity(
        id = id,
        scheduleId = scheduleId,
        exerciseId = id,
        sortOrder = id.toInt(),
        targetSets = null,
        targetReps = null,
        targetWeight = null,
        targetDurationSeconds = null,
        targetDistance = null,
    )

private fun workoutRun(
    id: Long,
    scheduleId: Long,
    startedAt: Long = 100L,
    finishedAt: Long?,
): WorkoutRunEntity =
    WorkoutRunEntity(
        id = id,
        scheduleId = scheduleId,
        scheduleNameSnapshot = "Schedule $scheduleId",
        startedAt = startedAt,
        finishedAt = finishedAt,
        notes = null,
    )

@Suppress("LongParameterList")
private class FakeScheduleRepository(
    private val createdId: Long = 1L,
    private val schedules: Flow<List<ScheduleEntity>> = MutableStateFlow(emptyList()),
    private val schedule: ScheduleEntity? = scheduleEntity(),
    private val scheduleExercisesBySchedule: Map<Long, Flow<List<ScheduleExerciseEntity>>> = emptyMap(),
    private val activeWorkoutRun: Flow<WorkoutRunEntity?> = MutableStateFlow(null),
    private val completedWorkoutRuns: Flow<List<WorkoutRunEntity>> = MutableStateFlow(emptyList()),
    private val updateError: Throwable? = null,
    private val deleteError: Throwable? = null,
) : ScheduleCreator {
    var createCount = 0
        private set
    var createdName: String? = null
        private set
    var createdNotes: String? = null
        private set
    var createdNowMillis: Long? = null
        private set
    var updatedId: Long? = null
        private set
    var updatedName: String? = null
        private set
    var updatedNotes: String? = null
        private set
    var updatedNowMillis: Long? = null
        private set
    var deletedId: Long? = null
        private set

    override fun observeSchedules(): Flow<List<ScheduleEntity>> = schedules

    override fun observeSchedule(id: Long): Flow<ScheduleEntity?> = MutableStateFlow(schedule?.takeIf { it.id == id })

    override fun observeScheduleExercises(scheduleId: Long): Flow<List<ScheduleExerciseEntity>> =
        scheduleExercisesBySchedule[scheduleId] ?: MutableStateFlow(emptyList())

    override fun observeActiveWorkoutRun(): Flow<WorkoutRunEntity?> = activeWorkoutRun

    override fun observeCompletedWorkoutRuns(): Flow<List<WorkoutRunEntity>> = completedWorkoutRuns

    override suspend fun createSchedule(
        name: String,
        notes: String?,
        nowMillis: Long,
    ): Long {
        createCount += 1
        createdName = name
        createdNotes = notes
        createdNowMillis = nowMillis
        return createdId
    }

    override suspend fun updateSchedule(
        id: Long,
        name: String,
        notes: String?,
        nowMillis: Long,
    ) {
        updateError?.let { throw it }
        updatedId = id
        updatedName = name
        updatedNotes = notes
        updatedNowMillis = nowMillis
    }

    override suspend fun deleteSchedule(id: Long) {
        deleteError?.let { throw it }
        deletedId = id
    }
}
