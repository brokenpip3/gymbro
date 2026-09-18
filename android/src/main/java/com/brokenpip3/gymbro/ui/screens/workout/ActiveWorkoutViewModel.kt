@file:Suppress("TooManyFunctions")

package com.brokenpip3.gymbro.ui.screens.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brokenpip3.gymbro.data.dao.WorkoutRunDao
import com.brokenpip3.gymbro.data.entities.ExerciseEntity
import com.brokenpip3.gymbro.data.entities.ExerciseResultEntity
import com.brokenpip3.gymbro.data.entities.SetResultEntity
import com.brokenpip3.gymbro.data.entities.WorkoutRunEntity
import com.brokenpip3.gymbro.data.repositories.WorkoutRepository
import com.brokenpip3.gymbro.domain.TrackingMode
import com.brokenpip3.gymbro.ui.screens.results.ResultExerciseDetail
import com.brokenpip3.gymbro.ui.screens.results.ResultRunDetail
import com.brokenpip3.gymbro.ui.screens.results.detailFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

typealias ExerciseResults = List<ExerciseResultEntity>
typealias SetResults = List<SetResultEntity>

private const val UNSUPPORTED_EXERCISE_INFO = "Exercise info is not supported"
private const val UNSUPPORTED_ADD_SET_ROW = "Adding set rows is not supported"
private const val UNSUPPORTED_ADD_EMPTY_SET = "Adding empty sets is not supported"
private const val UNSUPPORTED_ADD_EXERCISE = "Adding exercises is not supported"
private const val UNSUPPORTED_SET_METRICS_UPDATE = "Updating set metrics is not supported"
private const val UNSUPPORTED_SET_COMPLETION_UPDATE = "Updating set completion is not supported"

private fun unsupportedExerciseInfo(): Nothing = throw UnsupportedOperationException(UNSUPPORTED_EXERCISE_INFO)

private fun unsupportedAddSetRow(): Nothing = throw UnsupportedOperationException(UNSUPPORTED_ADD_SET_ROW)

private fun unsupportedAddEmptySet(): Nothing = throw UnsupportedOperationException(UNSUPPORTED_ADD_EMPTY_SET)

private fun unsupportedAddExercise(): Nothing = throw UnsupportedOperationException(UNSUPPORTED_ADD_EXERCISE)

private fun unsupportedSetMetricsUpdate(): Nothing = throw UnsupportedOperationException(UNSUPPORTED_SET_METRICS_UPDATE)

@Suppress("ktlint:standard:function-expression-body")
private fun unsupportedSetCompletionUpdate(): Nothing {
    throw UnsupportedOperationException(UNSUPPORTED_SET_COMPLETION_UPDATE)
}

@Suppress("ktlint:standard:function-expression-body")
private suspend fun WorkoutRepository.copyPreviousSet(exerciseResultId: Long): Long {
    return addSetFromPrevious(exerciseResultId)
}

interface ActiveWorkoutSource {
    fun observeActiveWorkoutRun(): Flow<WorkoutRunEntity?>

    fun observeExerciseResults(id: Long): Flow<ExerciseResults>

    fun observeSetResults(id: Long): Flow<SetResults>

    fun observeAvailableExercises(): Flow<List<ExerciseEntity>> = flowOf(emptyList())

    suspend fun getExerciseInfo(exerciseResultId: Long): ExerciseInfoUiModel = unsupportedExerciseInfo()

    suspend fun addSet(
        exerciseResultId: Long,
        reps: Int?,
        weight: Double?,
        durationSeconds: Long?,
        distance: Double?,
    ): Long

    suspend fun addSetFromPrevious(exerciseResultId: Long): Long = unsupportedAddSetRow()

    suspend fun addEmptySet(exerciseResultId: Long): Long = unsupportedAddEmptySet()

    suspend fun addExerciseToActiveWorkout(
        workoutRunId: Long,
        exerciseId: Long,
    ): Long = unsupportedAddExercise()

    suspend fun updateSetMetrics(
        setId: Long,
        reps: Int?,
        weight: Double?,
        durationSeconds: Long?,
        distance: Double?,
    ): Unit = unsupportedSetMetricsUpdate()

    suspend fun updateSetCompletion(
        setId: Long,
        isCompleted: Boolean,
    ): Unit = unsupportedSetCompletionUpdate()

    suspend fun updateSetNotes(
        setId: Long,
        notes: String?,
    )

    suspend fun updateExerciseResultNotes(
        exerciseResultId: Long,
        notes: String?,
    )

    suspend fun updateWorkoutNotes(
        workoutRunId: Long,
        notes: String?,
    )

    suspend fun deleteSet(setId: Long)

    suspend fun deleteExerciseResult(exerciseResultId: Long)

    suspend fun finishWorkout(
        workoutRunId: Long,
        nowMillis: Long,
    )

    suspend fun discardWorkout(workoutRunId: Long)
}

class WorkoutRepositoryActiveWorkoutSource(
    private val repository: WorkoutRepository,
    private val dao: WorkoutRunDao,
) : ActiveWorkoutSource {
    override fun observeActiveWorkoutRun(): Flow<WorkoutRunEntity?> = repository.observeActiveWorkoutRun()

    override fun observeExerciseResults(id: Long): Flow<ExerciseResults> = dao.observeExerciseResults(id)

    override fun observeSetResults(id: Long): Flow<SetResults> = dao.observeSetResults(id)

    override fun observeAvailableExercises(): Flow<List<ExerciseEntity>> = repository.observeAvailableExercises()

    override suspend fun getExerciseInfo(exerciseResultId: Long): ExerciseInfoUiModel {
        val exerciseResult =
            repository.getExerciseResult(exerciseResultId)
                ?: error("Exercise result not found")
        val completedRunDetails =
            repository
                .observeCompletedWorkoutRuns()
                .first()
                .filter { workoutRun -> workoutRun.finishedAt != null }
                .map { workoutRun ->
                    ResultRunDetail(
                        workoutRun = workoutRun,
                        exerciseDetails =
                            repository.getExerciseResults(workoutRun.id).map { completedExercise ->
                                ResultExerciseDetail(
                                    exerciseResult = completedExercise,
                                    setResults = repository.getSetResults(completedExercise.id),
                                )
                            },
                    )
                }
        val detail = completedRunDetails.detailFor(exerciseResult)

        return ExerciseInfoUiModel(
            exerciseName = exerciseResult.exerciseNameSnapshot,
            notes = exerciseResult.notes?.takeIf { notes -> notes.isNotBlank() },
            headline = detail?.let { "${it.latest} latest / ${it.best} best" },
            history =
                detail
                    ?.let { stats ->
                        buildList {
                            add(stats.average)
                            add("${stats.totalSessions} sessions / ${stats.totalSets} sets")
                            addAll(stats.recentSets.map { set -> "${set.label}: ${set.value}" })
                        }
                    }.orEmpty(),
            exerciseId = exerciseResult.exerciseId,
        )
    }

    override suspend fun addSet(
        exerciseResultId: Long,
        reps: Int?,
        weight: Double?,
        durationSeconds: Long?,
        distance: Double?,
    ): Long =
        repository.addSet(
            exerciseResultId = exerciseResultId,
            reps = reps,
            weight = weight,
            durationSeconds = durationSeconds,
            distance = distance,
            notes = null,
        )

    override suspend fun addSetFromPrevious(exerciseResultId: Long): Long = repository.copyPreviousSet(exerciseResultId)

    override suspend fun addEmptySet(exerciseResultId: Long): Long = repository.addEmptySet(exerciseResultId)

    override suspend fun addExerciseToActiveWorkout(
        workoutRunId: Long,
        exerciseId: Long,
    ): Long =
        repository.addExerciseToActiveWorkout(
            workoutRunId = workoutRunId,
            exerciseId = exerciseId,
        )

    override suspend fun updateSetMetrics(
        setId: Long,
        reps: Int?,
        weight: Double?,
        durationSeconds: Long?,
        distance: Double?,
    ) {
        repository.updateSetMetrics(
            setId = setId,
            reps = reps,
            weight = weight,
            durationSeconds = durationSeconds,
            distance = distance,
        )
    }

    override suspend fun updateSetCompletion(
        setId: Long,
        isCompleted: Boolean,
    ) {
        repository.updateSetCompletion(setId = setId, isCompleted = isCompleted)
    }

    override suspend fun updateSetNotes(
        setId: Long,
        notes: String?,
    ) {
        repository.updateSetNotes(
            setId = setId,
            notes = notes,
        )
    }

    override suspend fun updateExerciseResultNotes(
        exerciseResultId: Long,
        notes: String?,
    ) {
        repository.updateExerciseResultNotes(
            exerciseResultId = exerciseResultId,
            notes = notes,
        )
    }

    override suspend fun updateWorkoutNotes(
        workoutRunId: Long,
        notes: String?,
    ) {
        repository.updateWorkoutNotes(
            workoutRunId = workoutRunId,
            notes = notes,
        )
    }

    override suspend fun deleteSet(setId: Long) {
        repository.deleteSet(setId)
    }

    override suspend fun deleteExerciseResult(exerciseResultId: Long) {
        repository.deleteExerciseFromWorkout(exerciseResultId)
    }

    override suspend fun finishWorkout(
        workoutRunId: Long,
        nowMillis: Long,
    ) {
        repository.finishWorkout(workoutRunId, nowMillis)
    }

    override suspend fun discardWorkout(workoutRunId: Long) {
        repository.discardWorkout(workoutRunId)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveWorkoutViewModel(
    private val source: ActiveWorkoutSource,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {
    private val scope = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            source
                .observeAvailableExercises()
                .catch {
                    _uiState.update { state ->
                        state.copy(errorMessage = "Unable to load exercises")
                    }
                    emit(emptyList())
                }.collectLatest { exercises ->
                    _uiState.update { state ->
                        state.copy(availableExercises = exercises.map { exercise -> exercise.toAvailableUiModel() })
                    }
                }
        }

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            source
                .observeActiveWorkoutRun()
                .flatMapLatest { workoutRun ->
                    workoutRun?.let(::observeWorkoutUiState)
                        ?: flowOf(ActiveWorkoutUiState(isLoading = false))
                }.catch {
                    emit(
                        ActiveWorkoutUiState(
                            isLoading = false,
                            errorMessage = "Unable to load active workout",
                        ),
                    )
                }.collectLatest { state ->
                    val hasActiveWorkout = state.activeWorkout != null
                    _uiState.value =
                        if (state.errorMessage == null) {
                            state.copy(
                                errorMessage = _uiState.value.errorMessage,
                                exerciseInfo = _uiState.value.exerciseInfo,
                                finishedWorkoutRunId = _uiState.value.finishedWorkoutRunId,
                                discardedWorkoutRunId = _uiState.value.discardedWorkoutRunId,
                                availableExercises = _uiState.value.availableExercises,
                                isAddExerciseDialogVisible =
                                    hasActiveWorkout && _uiState.value.isAddExerciseDialogVisible,
                                addExerciseErrorMessage =
                                    _uiState.value.addExerciseErrorMessage.takeIf { hasActiveWorkout },
                            )
                        } else {
                            state.copy(
                                availableExercises = _uiState.value.availableExercises,
                                finishedWorkoutRunId = _uiState.value.finishedWorkoutRunId,
                                discardedWorkoutRunId = _uiState.value.discardedWorkoutRunId,
                                isAddExerciseDialogVisible =
                                    hasActiveWorkout && _uiState.value.isAddExerciseDialogVisible,
                                addExerciseErrorMessage =
                                    _uiState.value.addExerciseErrorMessage.takeIf { hasActiveWorkout },
                            )
                        }
                }
        }
    }

    fun addSet(
        exerciseResultId: Long,
        reps: Int?,
        weight: Double?,
        durationSeconds: Long?,
        distance: Double?,
    ) {
        val validationError = validateSet(reps, weight, durationSeconds, distance)
        if (validationError != null) {
            _uiState.update { state ->
                state.copy(errorMessage = validationError)
            }
            return
        }

        _uiState.update { state ->
            state.copy(errorMessage = null)
        }

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                source.addSet(
                    exerciseResultId = exerciseResultId,
                    reps = reps,
                    weight = weight,
                    durationSeconds = durationSeconds,
                    distance = distance,
                )
            }.onFailure {
                _uiState.update { state ->
                    state.copy(errorMessage = "Unable to add set")
                }
            }
        }
    }

    fun addSetRow(exerciseResultId: Long) {
        _uiState.update { state ->
            state.copy(errorMessage = null)
        }

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                source.addSetFromPrevious(exerciseResultId)
            }.onFailure {
                _uiState.update { state ->
                    state.copy(errorMessage = "Unable to add set")
                }
            }
        }
    }

    fun addEmptySet(exerciseResultId: Long) {
        _uiState.update { state ->
            state.copy(errorMessage = null)
        }

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                source.addEmptySet(exerciseResultId)
            }.onFailure {
                _uiState.update { state ->
                    state.copy(errorMessage = "Unable to add set")
                }
            }
        }
    }

    fun showAddExerciseDialog() {
        _uiState.update { state ->
            state.copy(
                isAddExerciseDialogVisible = true,
                errorMessage = null,
                addExerciseErrorMessage = null,
            )
        }
    }

    fun dismissAddExerciseDialog() {
        _uiState.update { state ->
            state.copy(
                isAddExerciseDialogVisible = false,
                addExerciseErrorMessage = null,
            )
        }
    }

    fun addExerciseToWorkout(exerciseId: Long) {
        val workoutRunId =
            _uiState.value.activeWorkout?.runId
                ?: run {
                    _uiState.update { state ->
                        state.copy(
                            isAddExerciseDialogVisible = false,
                            addExerciseErrorMessage = null,
                            errorMessage = "No active workout",
                        )
                    }
                    return
                }

        _uiState.update { state ->
            state.copy(
                errorMessage = null,
                addExerciseErrorMessage = null,
            )
        }

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                source.addExerciseToActiveWorkout(
                    workoutRunId = workoutRunId,
                    exerciseId = exerciseId,
                )
            }.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        isAddExerciseDialogVisible = false,
                        errorMessage = null,
                        addExerciseErrorMessage = null,
                    )
                }
            }.onFailure {
                _uiState.update { state ->
                    state.copy(addExerciseErrorMessage = "Unable to add exercise")
                }
            }
        }
    }

    fun updateSetMetrics(
        setId: Long,
        repsText: String,
        weightText: String,
        minutesText: String,
        secondsText: String,
        distanceText: String,
    ) {
        val metrics =
            parseSetMetrics(
                repsText = repsText,
                weightText = weightText,
                minutesText = minutesText,
                secondsText = secondsText,
                distanceText = distanceText,
            )

        if (metrics.errorMessage != null) {
            _uiState.update { state ->
                state.copy(errorMessage = metrics.errorMessage)
            }
            return
        }

        _uiState.update { state ->
            state.copy(errorMessage = null)
        }

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                source.updateSetMetrics(
                    setId = setId,
                    reps = metrics.reps,
                    weight = metrics.weight,
                    durationSeconds = metrics.durationSeconds,
                    distance = metrics.distance,
                )
            }.onFailure {
                _uiState.update { state ->
                    state.copy(errorMessage = "Unable to update set")
                }
            }
        }
    }

    fun updateSetCompletion(
        setId: Long,
        isCompleted: Boolean,
    ) {
        val validationError = validateSetCompletion(_uiState.value, setId, isCompleted)
        if (validationError != null) {
            _uiState.update { state ->
                state.copy(errorMessage = validationError)
            }
            return
        }

        _uiState.update { state ->
            state.copy(errorMessage = null)
        }

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                source.updateSetCompletion(setId = setId, isCompleted = isCompleted)
            }.onFailure {
                _uiState.update { state ->
                    state.copy(errorMessage = "Unable to update completion")
                }
            }
        }
    }

    fun updateExerciseNotes(
        exerciseResultId: Long,
        notesText: String,
    ) {
        val notes = notesText.trim().takeIf { it.isNotEmpty() }

        _uiState.update { state ->
            state.copy(errorMessage = null)
        }

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                source.updateExerciseResultNotes(
                    exerciseResultId = exerciseResultId,
                    notes = notes,
                )
            }.onFailure {
                _uiState.update { state ->
                    state.copy(errorMessage = "Unable to update exercise notes")
                }
            }
        }
    }

    fun updateSetNotes(
        setId: Long,
        notesText: String,
    ) {
        val notes = notesText.trim().takeIf { it.isNotEmpty() }

        _uiState.update { state ->
            state.copy(errorMessage = null)
        }

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                source.updateSetNotes(
                    setId = setId,
                    notes = notes,
                )
            }.onFailure {
                _uiState.update { state ->
                    state.copy(errorMessage = "Unable to update set notes")
                }
            }
        }
    }

    fun updateWorkoutNotes(notesText: String) {
        val workoutRunId = _uiState.value.activeWorkout?.runId ?: return
        val notes = notesText.trim().takeIf { it.isNotEmpty() }

        _uiState.update { state ->
            state.copy(errorMessage = null)
        }

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                source.updateWorkoutNotes(
                    workoutRunId = workoutRunId,
                    notes = notes,
                )
            }.onFailure {
                _uiState.update { state ->
                    state.copy(errorMessage = "Unable to update workout notes")
                }
            }
        }
    }

    fun deleteSet(setId: Long) {
        _uiState.update { state ->
            state.copy(errorMessage = null)
        }

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                source.deleteSet(setId)
            }.onFailure {
                _uiState.update { state ->
                    state.copy(errorMessage = "Unable to delete set")
                }
            }
        }
    }

    fun deleteExerciseResult(exerciseResultId: Long) {
        _uiState.update { state ->
            state.copy(errorMessage = null)
        }

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                source.deleteExerciseResult(exerciseResultId)
            }.onFailure {
                _uiState.update { state ->
                    state.copy(errorMessage = "Unable to delete exercise")
                }
            }
        }
    }

    fun showExerciseInfo(exerciseResultId: Long) {
        _uiState.update { state ->
            state.copy(errorMessage = null)
        }

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                source.getExerciseInfo(exerciseResultId)
            }.onSuccess { exerciseInfo ->
                _uiState.update { state ->
                    state.copy(
                        exerciseInfo = exerciseInfo,
                        errorMessage = null,
                    )
                }
            }.onFailure {
                _uiState.update { state ->
                    state.copy(
                        exerciseInfo = null,
                        errorMessage = "Unable to load exercise info",
                    )
                }
            }
        }
    }

    fun dismissExerciseInfo() {
        _uiState.update { state ->
            state.copy(exerciseInfo = null)
        }
    }

    fun acknowledgeWorkoutFinished() {
        _uiState.update { state ->
            state.copy(finishedWorkoutRunId = null)
        }
    }

    fun acknowledgeWorkoutDiscarded() {
        _uiState.update { state ->
            state.copy(discardedWorkoutRunId = null)
        }
    }

    fun finishWorkout() {
        val workoutRunId = _uiState.value.activeWorkout?.runId ?: return

        _uiState.update { state ->
            state.copy(errorMessage = null)
        }

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                source.finishWorkout(workoutRunId, nowMillis())
            }.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        activeWorkout = null,
                        exerciseInfo = null,
                        finishedWorkoutRunId = workoutRunId,
                    )
                }
            }.onFailure {
                _uiState.update { state ->
                    state.copy(errorMessage = "Unable to finish workout")
                }
            }
        }
    }

    fun discardWorkout() {
        val workoutRunId = _uiState.value.activeWorkout?.runId ?: return

        _uiState.update { state ->
            state.copy(errorMessage = null)
        }

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                source.discardWorkout(workoutRunId)
            }.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        activeWorkout = null,
                        exerciseInfo = null,
                        discardedWorkoutRunId = workoutRunId,
                    )
                }
            }.onFailure {
                _uiState.update { state ->
                    state.copy(errorMessage = "Unable to discard workout")
                }
            }
        }
    }

    private fun observeWorkoutUiState(workoutRun: WorkoutRunEntity): Flow<ActiveWorkoutUiState> =
        source.observeExerciseResults(workoutRun.id).flatMapLatest { exerciseResults ->
            if (exerciseResults.isEmpty()) {
                flowOf(
                    ActiveWorkoutUiState(
                        isLoading = false,
                        activeWorkout = workoutRun.toUiModel(exercises = emptyList()),
                    ),
                )
            } else {
                combine(
                    exerciseResults.map { exerciseResult ->
                        source.observeSetResults(exerciseResult.id).map { setResults ->
                            exerciseResult.toUiModel(setResults)
                        }
                    },
                ) { exercises ->
                    ActiveWorkoutUiState(
                        isLoading = false,
                        activeWorkout = workoutRun.toUiModel(exercises = exercises.toList()),
                    )
                }
            }
        }
}

private fun validateSet(
    reps: Int?,
    weight: Double?,
    durationSeconds: Long?,
    distance: Double?,
): String? =
    when {
        reps == null && weight == null && durationSeconds == null && distance == null -> "Set metrics are required"
        repsError(reps) != null -> repsError(reps)
        weightError(weight) != null -> weightError(weight)
        durationError(durationSeconds) != null -> durationError(durationSeconds)
        distanceError(distance) != null -> distanceError(distance)
        else -> null
    }

private fun validateSetCompletion(
    state: ActiveWorkoutUiState,
    setId: Long,
    isCompleted: Boolean,
): String? {
    val exercise =
        state.activeWorkout
            ?.exercises
            ?.firstOrNull { workoutExercise ->
                workoutExercise.sets.any { set -> set.id == setId }
            }
    val set = exercise?.sets?.firstOrNull { workoutSet -> workoutSet.id == setId }

    return when {
        !isCompleted || exercise == null || set == null -> null
        exercise.trackingMode == TrackingMode.Strength && (set.reps == null || set.weight == null) ->
            "Strength sets need reps and weight"
        exercise.trackingMode == TrackingMode.Bodyweight && set.reps == null ->
            "Bodyweight sets need reps"
        exercise.trackingMode == TrackingMode.Timed && set.durationSeconds == null ->
            "Timed sets need a duration"
        else -> null
    }
}

private fun repsError(reps: Int?): String? =
    when {
        reps != null && reps <= 0 -> "Reps must be positive"
        else -> null
    }

private fun weightError(weight: Double?): String? =
    when {
        weight != null && !weight.isFinite() -> "Weight must be finite"
        weight != null && weight <= 0.0 -> "Weight must be positive"
        else -> null
    }

private fun durationError(durationSeconds: Long?): String? =
    when {
        durationSeconds != null && durationSeconds <= 0L -> "Duration must be positive"
        else -> null
    }

private fun distanceError(distance: Double?): String? =
    when {
        distance != null && !distance.isFinite() -> "Distance must be finite"
        distance != null && distance <= 0.0 -> "Distance must be positive"
        else -> null
    }

private fun WorkoutRunEntity.toUiModel(exercises: List<WorkoutExerciseUiModel>): ActiveWorkoutUiModel =
    ActiveWorkoutUiModel(
        runId = id,
        scheduleName = scheduleNameSnapshot,
        startedAt = startedAt,
        notes = notes?.takeIf { note -> note.isNotBlank() },
        exercises = exercises,
    )

private fun ExerciseResultEntity.toUiModel(setResults: List<SetResultEntity>): WorkoutExerciseUiModel =
    WorkoutExerciseUiModel(
        exerciseResultId = id,
        exerciseName = exerciseNameSnapshot,
        trackingMode = trackingModeSnapshot.toTrackingMode(),
        notes = notes?.takeIf { note -> note.isNotBlank() },
        sets = setResults.map { setResult -> setResult.toUiModel() },
    )

private fun SetResultEntity.toUiModel(): WorkoutSetUiModel =
    WorkoutSetUiModel(
        id = id,
        setOrder = setOrder,
        reps = reps,
        weight = weight,
        durationSeconds = durationSeconds,
        distance = distance,
        notes = notes?.takeIf { note -> note.isNotBlank() },
        isCompleted = isCompleted,
    )

private fun ExerciseEntity.toAvailableUiModel(): AvailableWorkoutExerciseUiModel =
    AvailableWorkoutExerciseUiModel(
        id = id,
        name = name,
        trackingMode = trackingMode.toTrackingMode(),
    )

private fun String.toTrackingMode(): TrackingMode =
    TrackingMode.entries.firstOrNull { trackingMode ->
        trackingMode.databaseValue == this
    } ?: TrackingMode.Bodyweight

private data class EditableSetMetricsParseResult(
    val reps: Int? = null,
    val weight: Double? = null,
    val durationSeconds: Long? = null,
    val distance: Double? = null,
    val errorMessage: String? = null,
)

private fun parseSetMetrics(
    repsText: String,
    weightText: String,
    minutesText: String,
    secondsText: String,
    distanceText: String,
): EditableSetMetricsParseResult {
    val reps = parsePositiveInt(repsText, "Reps must be a positive whole number")
    val weight = parsePositiveFiniteDouble(weightText, "Weight must be a positive number")
    val duration = parseDurationSeconds(minutesText = minutesText, secondsText = secondsText)
    val distance = parsePositiveFiniteDouble(distanceText, "Distance must be a positive number")
    val errorMessage =
        reps.errorMessage
            ?: weight.errorMessage
            ?: duration.errorMessage
            ?: distance.errorMessage

    if (errorMessage != null) {
        return EditableSetMetricsParseResult(errorMessage = errorMessage)
    }

    return EditableSetMetricsParseResult(
        reps = reps.value,
        weight = weight.value,
        durationSeconds = duration.value,
        distance = distance.value,
    )
}

private data class ParseResult<T>(
    val value: T? = null,
    val errorMessage: String? = null,
)

private fun parsePositiveInt(
    text: String,
    errorMessage: String,
): ParseResult<Int> {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return ParseResult()

    val value = trimmed.toIntOrNull()
    return if (value != null && value > 0) {
        ParseResult(value = value)
    } else {
        ParseResult(errorMessage = errorMessage)
    }
}

private fun parsePositiveFiniteDouble(
    text: String,
    errorMessage: String,
): ParseResult<Double> {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return ParseResult()

    val value = trimmed.toDoubleOrNull()
    return if (value != null && value.isFinite() && value > 0.0) {
        ParseResult(value = value)
    } else {
        ParseResult(errorMessage = errorMessage)
    }
}

private fun parseDurationSeconds(
    minutesText: String,
    secondsText: String,
): ParseResult<Long> {
    val trimmedMinutes = minutesText.trim()
    val trimmedSeconds = secondsText.trim()
    if (trimmedMinutes.isEmpty() && trimmedSeconds.isEmpty()) return ParseResult()

    val minutes = trimmedMinutes.takeIf { it.isNotEmpty() }?.toLongOrNull() ?: 0L
    val seconds = trimmedSeconds.takeIf { it.isNotEmpty() }?.toLongOrNull() ?: 0L

    return when {
        trimmedMinutes.isNotEmpty() && minutesText.trim().toLongOrNull() == null ->
            ParseResult(errorMessage = "Minutes must be a whole number")
        trimmedSeconds.isNotEmpty() && secondsText.trim().toLongOrNull() == null ->
            ParseResult(errorMessage = "Seconds must be 0-59")
        seconds !in 0L..59L -> ParseResult(errorMessage = "Seconds must be 0-59")
        minutes < 0L -> ParseResult(errorMessage = "Duration must be positive")
        minutes > (Long.MAX_VALUE - seconds) / 60L -> ParseResult(errorMessage = "Duration is too large")
        minutes * 60L + seconds <= 0L -> ParseResult(errorMessage = "Duration must be positive")
        else -> ParseResult(value = minutes * 60L + seconds)
    }
}
