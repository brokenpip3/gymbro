package com.brokenpip3.gymbro.ui.screens.schedules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brokenpip3.gymbro.data.entities.ExerciseEntity
import com.brokenpip3.gymbro.data.entities.ScheduleEntity
import com.brokenpip3.gymbro.data.entities.ScheduleExerciseEntity
import com.brokenpip3.gymbro.data.entities.WorkoutRunEntity
import com.brokenpip3.gymbro.data.repositories.MoveDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScheduleDetailUiState(
    val schedule: ScheduleEntity? = null,
    val assignedExercises: List<ScheduleExerciseEntity> = emptyList(),
    val availableExercises: List<ExerciseEntity> = emptyList(),
    val assignmentError: String? = null,
    val scheduleExerciseError: String? = null,
    val isAssigning: Boolean = false,
    val activeWorkoutRun: WorkoutRunEntity? = null,
    val otherActiveWorkoutRun: WorkoutRunEntity? = null,
    val startError: String? = null,
    val isStarting: Boolean = false,
) {
    val canStartWorkout: Boolean
        get() = activeWorkoutRun != null || otherActiveWorkoutRun != null || assignedExercises.isNotEmpty()

    val workoutActionLabel: String
        get() =
            when {
                activeWorkoutRun != null -> "Resume Workout"
                otherActiveWorkoutRun != null -> "Resume Active Workout"
                else -> "Start Workout"
            }
}

data class ScheduleExerciseTargets(
    val targetSets: Int?,
    val targetReps: Int?,
    val targetWeight: Double?,
    val targetDurationSeconds: Long?,
    val targetDistance: Double?,
)

interface ScheduleDetailSource {
    fun observeSchedule(id: Long): Flow<ScheduleEntity?>

    fun observeScheduleExercises(scheduleId: Long): Flow<List<ScheduleExerciseEntity>>

    fun observeAvailableExercises(): Flow<List<ExerciseEntity>>

    suspend fun assignExercise(
        scheduleId: Long,
        exerciseId: Long,
    ): Long

    suspend fun assignExercises(
        scheduleId: Long,
        exerciseIds: List<Long>,
    ) {
        exerciseIds.forEach { exerciseId ->
            assignExercise(scheduleId, exerciseId)
        }
    }

    suspend fun removeExerciseFromSchedule(scheduleExerciseId: Long)

    suspend fun moveScheduleExercise(
        scheduleId: Long,
        scheduleExerciseId: Long,
        direction: MoveDirection,
    )

    suspend fun updateScheduleExerciseTargets(
        scheduleExerciseId: Long,
        targets: ScheduleExerciseTargets,
    )
}

interface WorkoutStarter {
    fun observeActiveWorkoutRun(): Flow<WorkoutRunEntity?>

    suspend fun startWorkout(
        scheduleId: Long,
        nowMillis: Long,
    ): Long
}

internal object NoActiveWorkoutStarter : WorkoutStarter {
    override fun observeActiveWorkoutRun(): Flow<WorkoutRunEntity?> = flowOf(null)

    override suspend fun startWorkout(
        scheduleId: Long,
        nowMillis: Long,
    ): Long = error("Workout start is not available")
}

class ScheduleDetailViewModel(
    private val scheduleId: Long,
    private val source: ScheduleDetailSource,
    private val workoutStarter: WorkoutStarter,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {
    private val scope = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(ScheduleDetailUiState())
    val uiState: StateFlow<ScheduleDetailUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            source.observeSchedule(scheduleId).collectLatest { schedule ->
                _uiState.update { state ->
                    state.copy(schedule = schedule)
                }
            }
        }

        scope.launch {
            source.observeScheduleExercises(scheduleId).collectLatest { assignedExercises ->
                _uiState.update { state ->
                    state.copy(assignedExercises = assignedExercises)
                }
            }
        }

        scope.launch {
            source.observeAvailableExercises().collectLatest { availableExercises ->
                _uiState.update { state ->
                    state.copy(availableExercises = availableExercises)
                }
            }
        }

        scope.launch {
            workoutStarter.observeActiveWorkoutRun().collectLatest { activeWorkoutRun ->
                _uiState.update { state ->
                    val unfinishedRun = activeWorkoutRun?.takeIf { workoutRun -> workoutRun.finishedAt == null }
                    state.copy(
                        activeWorkoutRun =
                            unfinishedRun?.takeIf { workoutRun -> workoutRun.scheduleId == scheduleId },
                        otherActiveWorkoutRun =
                            unfinishedRun?.takeIf { workoutRun -> workoutRun.scheduleId != scheduleId },
                    )
                }
            }
        }
    }

    fun assignExercise(
        exerciseId: Long,
        onAssigned: () -> Unit,
    ) {
        assignExercises(listOf(exerciseId), onAssigned)
    }

    fun assignExercises(
        exerciseIds: List<Long>,
        onAssigned: () -> Unit,
    ) {
        val distinctExerciseIds = exerciseIds.distinct()
        if (distinctExerciseIds.isEmpty()) return

        _uiState.update { state ->
            state.copy(isAssigning = true, assignmentError = null, scheduleExerciseError = null)
        }

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                source.assignExercises(scheduleId, distinctExerciseIds)
            }.onSuccess {
                _uiState.update { state ->
                    state.copy(isAssigning = false)
                }
                onAssigned()
            }.onFailure {
                _uiState.update { state ->
                    state.copy(
                        isAssigning = false,
                        assignmentError = "Unable to add exercise",
                    )
                }
            }
        }
    }

    fun clearAssignmentError() {
        _uiState.update { state ->
            state.copy(assignmentError = null)
        }
    }

    fun removeExerciseFromSchedule(scheduleExerciseId: Long) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                source.removeExerciseFromSchedule(scheduleExerciseId)
            }.onFailure {
                _uiState.update { state ->
                    state.copy(scheduleExerciseError = "Unable to update schedule exercises")
                }
            }
        }
    }

    fun moveScheduleExercise(
        scheduleExerciseId: Long,
        direction: MoveDirection,
    ) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                source.moveScheduleExercise(
                    scheduleId = scheduleId,
                    scheduleExerciseId = scheduleExerciseId,
                    direction = direction,
                )
            }.onFailure {
                _uiState.update { state ->
                    state.copy(scheduleExerciseError = "Unable to update schedule exercises")
                }
            }
        }
    }

    fun clearScheduleExerciseError() {
        _uiState.update { state ->
            state.copy(scheduleExerciseError = null)
        }
    }

    fun updateScheduleExerciseTargets(
        scheduleExerciseId: Long,
        targets: ScheduleExerciseTargets,
    ) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                source.updateScheduleExerciseTargets(scheduleExerciseId, targets)
            }.onFailure {
                _uiState.update { state ->
                    state.copy(scheduleExerciseError = "Unable to update schedule exercises")
                }
            }
        }
    }

    fun startWorkout(onStarted: (Long) -> Unit) {
        val state = _uiState.value
        when {
            state.isStarting -> Unit
            state.activeWorkoutRun != null -> onStarted(state.activeWorkoutRun.id)
            state.otherActiveWorkoutRun != null -> onStarted(state.otherActiveWorkoutRun.id)
            state.assignedExercises.isEmpty() -> {
                _uiState.update { state ->
                    state.copy(startError = "Add at least one exercise to start a workout.")
                }
            }
            else -> startNewWorkout(onStarted)
        }
    }

    private fun startNewWorkout(onStarted: (Long) -> Unit) {
        _uiState.update { state ->
            state.copy(isStarting = true, startError = null)
        }

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                workoutStarter.startWorkout(scheduleId, nowMillis())
            }.onSuccess { workoutRunId ->
                _uiState.update { state ->
                    state.copy(
                        isStarting = false,
                    )
                }
                onStarted(workoutRunId)
            }.onFailure {
                _uiState.update { state ->
                    state.copy(
                        isStarting = false,
                        startError = "Unable to start workout",
                    )
                }
            }
        }
    }
}
