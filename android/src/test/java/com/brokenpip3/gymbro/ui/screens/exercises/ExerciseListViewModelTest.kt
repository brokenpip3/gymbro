package com.brokenpip3.gymbro.ui.screens.exercises

import com.brokenpip3.gymbro.data.entities.ExerciseEntity
import com.brokenpip3.gymbro.data.entities.ExerciseUsage
import com.brokenpip3.gymbro.domain.TrackingMode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseListViewModelTest {
    @Test
    fun exerciseRowsIncludeSessionCountAndLastCompletedTime() =
        runTest {
            val exercise =
                ExerciseEntity(
                    id = 7L,
                    name = "Back Squat",
                    notes = null,
                    trackingMode = TrackingMode.Strength.databaseValue,
                    createdAt = 100L,
                    updatedAt = 100L,
                )
            val viewModel =
                ExerciseListViewModel(
                    repository =
                        FakeExerciseRepository(
                            exercises = MutableStateFlow(listOf(exercise)),
                            usage =
                                MutableStateFlow(
                                    listOf(
                                        ExerciseUsage(
                                            exerciseId = 7L,
                                            sessionCount = 4,
                                            lastCompletedAt = 8_000L,
                                        ),
                                    ),
                                ),
                        ),
                    coroutineScope = backgroundScope,
                )
            val row = viewModel.exerciseRows.first { items -> items.isNotEmpty() }.single()

            assertEquals(4, row.sessionCount)
            assertEquals(8_000L, row.lastCompletedAt)
        }

    @Test
    fun blankNameShowsValidationErrorAndDoesNotSave() =
        runTest {
            val repository = FakeExerciseRepository()
            val viewModel =
                ExerciseListViewModel(
                    repository = repository,
                    nowMillis = { 1234L },
                    coroutineScope = backgroundScope,
                )

            viewModel.updateName("   ")
            viewModel.saveExercise(onSaved = {})

            assertEquals("Name is required", viewModel.formState.value.nameError)
            assertEquals(0, repository.createCount)
        }

    @Test
    fun savingExerciseTrimsNameAndNotes() =
        runTest {
            val repository = FakeExerciseRepository()
            var onSavedCalled = false
            val viewModel =
                ExerciseListViewModel(
                    repository = repository,
                    nowMillis = { 1234L },
                    coroutineScope = backgroundScope,
                )

            viewModel.updateName("  Squat  ")
            viewModel.updateNotes("  Low bar  ")
            viewModel.updateTrackingMode(TrackingMode.Strength)
            viewModel.saveExercise(onSaved = { onSavedCalled = true })

            assertEquals("Squat", repository.createdName)
            assertEquals("Low bar", repository.createdNotes)
            assertEquals("strength", repository.createdTrackingMode)
            assertTrue(onSavedCalled)
        }

    @Test
    fun deletingExerciseDelegatesToRepository() =
        runTest {
            val repository = FakeExerciseRepository()
            val viewModel =
                ExerciseListViewModel(
                    repository = repository,
                    coroutineScope = backgroundScope,
                )

            viewModel.deleteExercise(17)

            assertEquals(17L, repository.deletedExerciseId)
        }

    @Test
    fun loadExerciseForEditPrefillsForm() =
        runTest {
            val repository =
                FakeExerciseRepository(
                    exercise =
                        ExerciseEntity(
                            id = 4,
                            name = "Tempo Run",
                            notes = "Easy pace",
                            trackingMode = TrackingMode.Timed.databaseValue,
                            createdAt = 100,
                            updatedAt = 200,
                        ),
                )
            val viewModel =
                ExerciseListViewModel(
                    repository = repository,
                    coroutineScope = backgroundScope,
                )

            viewModel.loadExerciseForEdit(4)

            assertEquals("Tempo Run", viewModel.formState.value.name)
            assertEquals("Easy pace", viewModel.formState.value.notes)
            assertEquals(TrackingMode.Timed, viewModel.formState.value.trackingMode)
        }

    @Test
    fun editFormReportsLoadingUntilExistingExerciseIsLoaded() =
        runTest {
            val loadedExercise =
                ExerciseEntity(
                    id = 4,
                    name = "Tempo Run",
                    notes = "Easy pace",
                    trackingMode = TrackingMode.Timed.databaseValue,
                    createdAt = 100,
                    updatedAt = 200,
                )
            val pendingExercise = CompletableDeferred<ExerciseEntity?>()
            val viewModel =
                ExerciseListViewModel(
                    repository = FakeExerciseRepository(exerciseDeferred = pendingExercise),
                    coroutineScope =
                        CoroutineScope(
                            backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler),
                        ),
                )

            viewModel.loadExerciseForEdit(4)

            assertTrue(viewModel.formState.value.isLoading)
            pendingExercise.complete(loadedExercise)
            advanceUntilIdle()

            assertFalse(viewModel.formState.value.isLoading)
            assertEquals("Tempo Run", viewModel.formState.value.name)
        }

    @Test
    fun updateExerciseTrimsValuesAndCallsOnSaved() =
        runTest {
            val repository = FakeExerciseRepository()
            var onSavedCalled = false
            val viewModel =
                ExerciseListViewModel(
                    repository = repository,
                    nowMillis = { 5678L },
                    coroutineScope = backgroundScope,
                )

            viewModel.updateName("  Bench Press  ")
            viewModel.updateNotes("  Pause reps  ")
            viewModel.updateTrackingMode(TrackingMode.Strength)
            viewModel.updateExercise(exerciseId = 8, onSaved = { onSavedCalled = true })

            assertEquals(8L, repository.updatedExerciseId)
            assertEquals("Bench Press", repository.updatedName)
            assertEquals("Pause reps", repository.updatedNotes)
            assertEquals("strength", repository.updatedTrackingMode)
            assertEquals(5678L, repository.updatedNowMillis)
            assertTrue(onSavedCalled)
        }

    @Test
    fun missingExerciseOnLoadShowsExerciseNotFound() =
        runTest {
            val repository = FakeExerciseRepository()
            val viewModel =
                ExerciseListViewModel(
                    repository = repository,
                    coroutineScope = backgroundScope,
                )

            viewModel.loadExerciseForEdit(404)

            assertEquals("Exercise not found", viewModel.formState.value.saveError)
        }

    @Test
    fun missingExerciseBeforeSaveShowsErrorAndDoesNotCallOnSaved() =
        runTest {
            val repository =
                FakeExerciseRepository(
                    updateError = IllegalArgumentException("Exercise 404 was not found"),
                )
            var onSavedCalled = false
            val viewModel =
                ExerciseListViewModel(
                    repository = repository,
                    coroutineScope = backgroundScope,
                )

            viewModel.updateName("Squat")
            viewModel.updateExercise(exerciseId = 404, onSaved = { onSavedCalled = true })

            assertEquals("Exercise not found", viewModel.formState.value.saveError)
            assertEquals(false, onSavedCalled)
            assertEquals("Squat", viewModel.formState.value.name)
        }
}

private class FakeExerciseRepository(
    private val exercises: Flow<List<ExerciseEntity>> = MutableStateFlow(emptyList()),
    private val usage: Flow<List<ExerciseUsage>> = MutableStateFlow(emptyList()),
    private val exercise: ExerciseEntity? = null,
    private val exerciseDeferred: CompletableDeferred<ExerciseEntity?>? = null,
    private val updateError: Throwable? = null,
) : ExerciseCreator {
    var createCount = 0
        private set
    var createdName: String? = null
        private set
    var createdNotes: String? = null
        private set
    var createdTrackingMode: String? = null
        private set
    var createdNowMillis: Long? = null
        private set
    var deletedExerciseId: Long? = null
        private set
    var updatedExerciseId: Long? = null
        private set
    var updatedName: String? = null
        private set
    var updatedNotes: String? = null
        private set
    var updatedTrackingMode: String? = null
        private set
    var updatedNowMillis: Long? = null
        private set

    override fun observeExercises(): Flow<List<ExerciseEntity>> = exercises

    override fun observeExerciseUsage(): Flow<List<ExerciseUsage>> = usage

    override suspend fun createExercise(
        name: String,
        notes: String?,
        trackingMode: String,
        nowMillis: Long,
    ): Long {
        createCount += 1
        createdName = name
        createdNotes = notes
        createdTrackingMode = trackingMode
        createdNowMillis = nowMillis
        return 1L
    }

    @Suppress("ktlint:standard:function-expression-body")
    override suspend fun getExercise(id: Long): ExerciseEntity? {
        return exerciseDeferred?.await() ?: exercise?.takeIf { it.id == id }
    }

    override suspend fun updateExercise(
        id: Long,
        name: String,
        notes: String?,
        trackingMode: String,
        nowMillis: Long,
    ) {
        updateError?.let { throw it }
        updatedExerciseId = id
        updatedName = name
        updatedNotes = notes
        updatedTrackingMode = trackingMode
        updatedNowMillis = nowMillis
    }

    override suspend fun deleteExercise(id: Long) {
        deletedExerciseId = id
    }
}
