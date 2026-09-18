package com.brokenpip3.gymbro.ui.screens.schedules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brokenpip3.gymbro.data.entities.ScheduleEntity
import com.brokenpip3.gymbro.data.entities.ScheduleExerciseEntity
import com.brokenpip3.gymbro.data.entities.WorkoutRunEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScheduleListItem(
    val schedule: ScheduleEntity,
    val exerciseCount: Int,
    val isActiveWorkout: Boolean,
    val activeWorkoutRunId: Long?,
    val lastCompletedLabel: String?,
) {
    val exerciseCountLabel: String
        get() =
            when (exerciseCount) {
                0 -> "No exercises"
                1 -> "1 exercise"
                else -> "$exerciseCount exercises"
            }
}

interface ScheduleCreator {
    fun observeSchedules(): Flow<List<ScheduleEntity>>

    fun observeSchedule(id: Long): Flow<ScheduleEntity?>

    fun observeScheduleExercises(scheduleId: Long): Flow<List<ScheduleExerciseEntity>>

    fun observeActiveWorkoutRun(): Flow<WorkoutRunEntity?>

    fun observeCompletedWorkoutRuns(): Flow<List<WorkoutRunEntity>>

    suspend fun createSchedule(
        name: String,
        notes: String?,
        nowMillis: Long,
    ): Long

    suspend fun updateSchedule(
        id: Long,
        name: String,
        notes: String?,
        nowMillis: Long,
    )

    suspend fun deleteSchedule(id: Long)
}

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleListViewModel(
    private val repository: ScheduleCreator,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {
    private val scope = coroutineScope ?: viewModelScope

    private val _schedules = MutableStateFlow<List<ScheduleEntity>>(emptyList())
    val schedules: StateFlow<List<ScheduleEntity>> = _schedules.asStateFlow()

    private val _scheduleRows = MutableStateFlow<List<ScheduleListItem>>(emptyList())
    val scheduleRows: StateFlow<List<ScheduleListItem>> = _scheduleRows.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _formState = MutableStateFlow(ScheduleFormState())
    val formState: StateFlow<ScheduleFormState> = _formState.asStateFlow()

    private val _listError = MutableStateFlow<String?>(null)
    val listError: StateFlow<String?> = _listError.asStateFlow()

    init {
        scope.launch {
            repository.observeSchedules().collectLatest { schedules ->
                _schedules.value = schedules
            }
        }

        scope.launch {
            observeScheduleRows().collectLatest { scheduleRows ->
                _scheduleRows.value = scheduleRows
                _isLoading.value = false
            }
        }
    }

    private fun observeScheduleRows(): Flow<List<ScheduleListItem>> =
        combine(
            repository.observeSchedules(),
            repository.observeActiveWorkoutRun(),
            repository.observeCompletedWorkoutRuns(),
        ) { schedules, activeWorkoutRun, completedWorkoutRuns ->
            ScheduleListInputs(
                schedules = schedules,
                activeWorkoutRun = activeWorkoutRun?.takeIf { it.finishedAt == null },
                completedWorkoutRuns = completedWorkoutRuns,
            )
        }.flatMapLatest { inputs ->
            if (inputs.schedules.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    inputs.schedules.map { schedule ->
                        repository.observeScheduleExercises(schedule.id)
                    },
                ) { exerciseLists ->
                    inputs.schedules.mapIndexed { index, schedule ->
                        schedule.toScheduleListItem(
                            exerciseCount = exerciseLists[index].size,
                            activeWorkoutRun = inputs.activeWorkoutRun,
                            completedWorkoutRuns = inputs.completedWorkoutRuns,
                            nowMillis = nowMillis(),
                        )
                    }
                }
            }
        }

    fun updateName(value: String) {
        _formState.update { state ->
            state.copy(name = value, nameError = null)
        }
    }

    fun updateNotes(value: String) {
        _formState.update { state ->
            state.copy(notes = value)
        }
    }

    fun loadScheduleForEdit(scheduleId: Long) {
        _formState.update { state ->
            state.copy(isSaving = true, saveError = null)
        }

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            val schedule = repository.observeSchedule(scheduleId).first()
            if (schedule == null) {
                _formState.update { state ->
                    state.copy(
                        isSaving = false,
                        saveError = "Schedule not found",
                    )
                }
                return@launch
            }

            _formState.value =
                ScheduleFormState(
                    name = schedule.name,
                    notes = schedule.notes.orEmpty(),
                    editingScheduleId = schedule.id,
                )
        }
    }

    fun saveSchedule(onSaved: (Long) -> Unit) {
        val state = _formState.value
        val trimmedName = state.name.trim()

        if (trimmedName.isBlank()) {
            _formState.update {
                it.copy(nameError = "Name is required")
            }
            return
        }

        val trimmedNotes = state.notes.trim().ifBlank { null }

        _formState.update {
            it.copy(isSaving = true, nameError = null, saveError = null)
        }

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                val editingScheduleId = state.editingScheduleId
                if (editingScheduleId == null) {
                    repository.createSchedule(
                        name = trimmedName,
                        notes = trimmedNotes,
                        nowMillis = nowMillis(),
                    )
                } else {
                    repository.updateSchedule(
                        id = editingScheduleId,
                        name = trimmedName,
                        notes = trimmedNotes,
                        nowMillis = nowMillis(),
                    )
                    editingScheduleId
                }
            }.onSuccess { scheduleId ->
                _formState.value = ScheduleFormState()
                onSaved(scheduleId)
            }.onFailure { error ->
                _formState.update { current ->
                    current.copy(
                        isSaving = false,
                        saveError = error.toScheduleSaveError(),
                    )
                }
            }
        }
    }

    fun deleteSchedule(scheduleId: Long) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                repository.deleteSchedule(scheduleId)
            }.onSuccess {
                _listError.value = null
            }.onFailure {
                _listError.value = "Unable to delete schedule"
            }
        }
    }

    fun clearListError() {
        _listError.value = null
    }

    fun clearSaveError() {
        _formState.update { state ->
            state.copy(saveError = null)
        }
    }

    private fun Throwable.toScheduleSaveError(): String =
        if (this is IllegalArgumentException && message?.contains("was not found") == true) {
            "Schedule not found"
        } else {
            "Unable to save schedule"
        }
}

private data class ScheduleListInputs(
    val schedules: List<ScheduleEntity>,
    val activeWorkoutRun: WorkoutRunEntity?,
    val completedWorkoutRuns: List<WorkoutRunEntity>,
)

private fun ScheduleEntity.toScheduleListItem(
    exerciseCount: Int,
    activeWorkoutRun: WorkoutRunEntity?,
    completedWorkoutRuns: List<WorkoutRunEntity>,
    nowMillis: Long,
): ScheduleListItem {
    val lastCompletedWorkout =
        completedWorkoutRuns
            .filter { workoutRun -> workoutRun.scheduleId == id && workoutRun.finishedAt != null }
            .maxByOrNull { workoutRun -> workoutRun.finishedAt ?: Long.MIN_VALUE }

    return ScheduleListItem(
        schedule = this,
        exerciseCount = exerciseCount,
        isActiveWorkout = activeWorkoutRun?.scheduleId == id,
        activeWorkoutRunId = activeWorkoutRun?.takeIf { it.scheduleId == id }?.id,
        lastCompletedLabel = lastCompletedWorkout?.lastCompletedLabel(nowMillis),
    )
}

private fun WorkoutRunEntity.lastCompletedLabel(nowMillis: Long): String? {
    val finishedAt = finishedAt ?: return null
    val durationMinutes =
        ((finishedAt - startedAt).coerceAtLeast(0L) + MILLIS_PER_MINUTE - 1)
            .div(MILLIS_PER_MINUTE)
            .coerceAtLeast(1L)

    return "Last: ${finishedAt.relativeDayLabel(nowMillis)} · $durationMinutes min"
}

private fun Long.relativeDayLabel(nowMillis: Long): String {
    val ageMillis = nowMillis - this
    return when {
        ageMillis < 0 -> "soon"
        ageMillis < MILLIS_PER_DAY -> "today"
        ageMillis < 2 * MILLIS_PER_DAY -> "yesterday"
        else -> "${ageMillis / MILLIS_PER_DAY}d ago"
    }
}

private const val MILLIS_PER_MINUTE = 60_000L
private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
