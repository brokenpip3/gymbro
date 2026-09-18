package com.brokenpip3.gymbro.ui.screens.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brokenpip3.gymbro.data.entities.ExerciseResultEntity
import com.brokenpip3.gymbro.data.entities.SetResultEntity
import com.brokenpip3.gymbro.data.entities.WorkoutRunEntity
import com.brokenpip3.gymbro.data.repositories.WorkoutRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

interface ResultsSource {
    fun observeWorkoutRuns(): Flow<List<WorkoutRunEntity>>

    fun observeResultChanges(): Flow<Unit> = flowOf(Unit)

    suspend fun getExerciseResults(workoutRunId: Long): List<ExerciseResultEntity>

    suspend fun getSetResults(exerciseResultId: Long): List<SetResultEntity>

    suspend fun updateExerciseNotes(
        exerciseResultId: Long,
        notes: String?,
    ) = Unit

    suspend fun updateSetNotes(
        setId: Long,
        notes: String?,
    ) = Unit

    suspend fun updateWorkoutNotes(
        workoutRunId: Long,
        notes: String?,
    ) = Unit
}

class WorkoutRepositoryResultsSource(
    private val repository: WorkoutRepository,
) : ResultsSource {
    override fun observeWorkoutRuns(): Flow<List<WorkoutRunEntity>> = repository.observeCompletedWorkoutRuns()

    override fun observeResultChanges(): Flow<Unit> =
        combine(
            repository.observeExerciseResultChanges(),
            repository.observeSetResultChanges(),
        ) { _, _ -> }

    override suspend fun getExerciseResults(workoutRunId: Long): List<ExerciseResultEntity> =
        repository
            .getExerciseResults(workoutRunId)

    override suspend fun getSetResults(exerciseResultId: Long): List<SetResultEntity> =
        repository
            .getSetResults(exerciseResultId)

    override suspend fun updateExerciseNotes(
        exerciseResultId: Long,
        notes: String?,
    ) {
        repository.updateExerciseResultNotes(
            exerciseResultId = exerciseResultId,
            notes = notes,
        )
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

    override suspend fun updateWorkoutNotes(
        workoutRunId: Long,
        notes: String?,
    ) {
        repository.updateWorkoutNotes(
            workoutRunId = workoutRunId,
            notes = notes,
        )
    }
}

class ResultsViewModel(
    private val source: ResultsSource,
    coroutineScope: CoroutineScope? = null,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val scope = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(ResultsUiState())
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()
    private val selectedStatsRange = MutableStateFlow(ResultsStatsRange.AllTime)
    private var selectedExerciseKey: ExerciseStatsSelectionKey? = null
    private var selectedWorkoutId: Long? = null

    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            source
                .observeWorkoutRuns()
                .combine(source.observeResultChanges()) { workoutRuns, _ -> workoutRuns }
                .combine(selectedStatsRange) { workoutRuns, statsRange -> workoutRuns to statsRange }
                .map { (workoutRuns, statsRange) -> workoutRuns.toResultsUiState(statsRange, clock()) }
                .collectLatest { state ->
                    _uiState.value =
                        state.copy(
                            selectedExerciseDetail =
                                selectedExerciseKey?.let { key ->
                                    state.exerciseDetails.firstOrNull { detail -> detail.matchesKey(key) }
                                },
                            selectedWorkoutDetail =
                                selectedWorkoutId?.let { runId ->
                                    state.workoutDetails.firstOrNull { detail -> detail.runId == runId }
                                },
                        )
                    if (_uiState.value.selectedExerciseDetail == null) {
                        selectedExerciseKey = null
                    }
                    if (_uiState.value.selectedWorkoutDetail == null) {
                        selectedWorkoutId = null
                    }
                }
        }
    }

    fun selectStatsRange(range: ResultsStatsRange) {
        selectedStatsRange.value = range
    }

    fun selectExerciseStats(stat: ExerciseStatsUiModel) {
        selectedExerciseKey = stat.toSelectionKey()
        val selectionKey = selectedExerciseKey
        _uiState.value =
            _uiState.value.copy(
                selectedExerciseDetail =
                    selectionKey?.let { key ->
                        _uiState.value.exerciseDetails.firstOrNull { detail -> detail.matchesKey(key) }
                    },
            )
    }

    fun dismissExerciseDetail() {
        selectedExerciseKey = null
        _uiState.value = _uiState.value.copy(selectedExerciseDetail = null)
    }

    fun selectWorkout(workout: WorkoutSummaryUiModel) {
        selectedWorkoutId = workout.runId
        _uiState.value =
            _uiState.value.copy(
                selectedWorkoutDetail =
                    _uiState.value.workoutDetails.firstOrNull { detail -> detail.runId == workout.runId },
            )
    }

    fun dismissWorkoutDetail() {
        selectedWorkoutId = null
        _uiState.value = _uiState.value.copy(selectedWorkoutDetail = null)
    }

    fun updateExerciseNotes(
        exerciseResultId: Long,
        notes: String,
    ) {
        scope.launch {
            source.updateExerciseNotes(
                exerciseResultId = exerciseResultId,
                notes = notes.trim().takeIf { value -> value.isNotEmpty() },
            )
        }
    }

    fun updateSetNotes(
        setId: Long,
        notes: String,
    ) {
        scope.launch {
            source.updateSetNotes(
                setId = setId,
                notes = notes.trim().takeIf { value -> value.isNotEmpty() },
            )
        }
    }

    fun updateWorkoutNotes(
        workoutRunId: Long,
        notes: String,
    ) {
        scope.launch {
            source.updateWorkoutNotes(
                workoutRunId = workoutRunId,
                notes = notes.trim().takeIf { value -> value.isNotEmpty() },
            )
        }
    }

    private suspend fun List<WorkoutRunEntity>.toResultsUiState(
        statsRange: ResultsStatsRange,
        now: Long,
    ): ResultsUiState {
        val completedRuns =
            filter { workoutRun -> workoutRun.finishedAt != null }
                .sortedByDescending { workoutRun -> workoutRun.finishedAt }
        val runsInRange =
            completedRuns.filter { workoutRun ->
                workoutRun.finishedAt?.let { finishedAt -> statsRange.includes(finishedAt, now) } == true
            }

        val runDetails =
            runsInRange.map { workoutRun ->
                val exerciseDetails =
                    source.getExerciseResults(workoutRun.id).map { exerciseResult ->
                        ResultExerciseDetail(
                            exerciseResult = exerciseResult,
                            setResults = source.getSetResults(exerciseResult.id),
                        )
                    }

                ResultRunDetail(
                    workoutRun = workoutRun,
                    exerciseDetails = exerciseDetails,
                )
            }

        val recentWorkouts = runDetails.map { runDetail -> runDetail.toWorkoutSummaryUiModel() }
        val exerciseStats = runDetails.toExerciseStats()
        val exerciseDetails = runDetails.toExerciseStatDetails()

        return ResultsUiState(
            recentWorkouts = recentWorkouts,
            exerciseStats = exerciseStats,
            insights =
                calculateResultsInsights(
                    recentWorkouts = recentWorkouts,
                    exerciseStats = exerciseStats,
                    exerciseDetails = exerciseDetails,
                ),
            exerciseDetails = exerciseDetails,
            workoutDetails = runDetails.map { runDetail -> runDetail.toWorkoutDetailUiModel() },
            statsRange = statsRange,
            hasHistoricalData = completedRuns.isNotEmpty(),
        )
    }
}

private fun ResultRunDetail.toWorkoutSummaryUiModel(): WorkoutSummaryUiModel =
    WorkoutSummaryUiModel(
        runId = workoutRun.id,
        scheduleName = workoutRun.scheduleNameSnapshot,
        startedAt = workoutRun.startedAt,
        durationSeconds = ((workoutRun.finishedAt ?: workoutRun.startedAt) - workoutRun.startedAt) / MILLIS_PER_SECOND,
        completedSetCount =
            exerciseDetails.sumOf { exerciseDetail ->
                exerciseDetail.setResults.count { setResult -> setResult.isCompleted }
            },
    )

private const val MILLIS_PER_SECOND = 1_000L
