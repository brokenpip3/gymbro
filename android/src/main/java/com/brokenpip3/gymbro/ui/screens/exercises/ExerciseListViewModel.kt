package com.brokenpip3.gymbro.ui.screens.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brokenpip3.gymbro.data.entities.ExerciseEntity
import com.brokenpip3.gymbro.data.entities.ExerciseUsage
import com.brokenpip3.gymbro.domain.TrackingMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExerciseListItem(
    val exercise: ExerciseEntity,
    val sessionCount: Int = 0,
    val lastCompletedAt: Long? = null,
)

interface ExerciseCreator {
    fun observeExercises(): Flow<List<ExerciseEntity>>

    fun observeExerciseUsage(): Flow<List<ExerciseUsage>> = flowOf(emptyList())

    suspend fun getExercise(id: Long): ExerciseEntity?

    suspend fun createExercise(
        name: String,
        notes: String?,
        trackingMode: String,
        nowMillis: Long,
    ): Long

    suspend fun updateExercise(
        id: Long,
        name: String,
        notes: String?,
        trackingMode: String,
        nowMillis: Long,
    )

    suspend fun deleteExercise(id: Long)
}

class ExerciseListViewModel(
    private val repository: ExerciseCreator,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {
    private val scope = coroutineScope ?: viewModelScope

    private val _exercises = MutableStateFlow<List<ExerciseEntity>>(emptyList())
    val exercises: StateFlow<List<ExerciseEntity>> = _exercises.asStateFlow()

    private val _exerciseRows = MutableStateFlow<List<ExerciseListItem>>(emptyList())
    val exerciseRows: StateFlow<List<ExerciseListItem>> = _exerciseRows.asStateFlow()

    private val _formState = MutableStateFlow(ExerciseFormState())
    val formState: StateFlow<ExerciseFormState> = _formState.asStateFlow()

    private val _listError = MutableStateFlow<String?>(null)
    val listError: StateFlow<String?> = _listError.asStateFlow()

    init {
        scope.launch {
            combine(
                repository.observeExercises(),
                repository.observeExerciseUsage(),
            ) { exercises, usage ->
                val usageByExerciseId = usage.associateBy { item -> item.exerciseId }
                exercises.map { exercise ->
                    val exerciseUsage = usageByExerciseId[exercise.id]
                    ExerciseListItem(
                        exercise = exercise,
                        sessionCount = exerciseUsage?.sessionCount ?: 0,
                        lastCompletedAt = exerciseUsage?.lastCompletedAt,
                    )
                }
            }.collectLatest { rows ->
                _exerciseRows.value = rows
                _exercises.value = rows.map { row -> row.exercise }
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

    fun updateTrackingMode(value: TrackingMode) {
        _formState.update { state ->
            state.copy(trackingMode = value)
        }
    }

    fun saveExercise(onSaved: () -> Unit) {
        saveExercise(exerciseId = null, onSaved = onSaved)
    }

    fun loadExerciseForEdit(exerciseId: Long) {
        _formState.update {
            it.copy(isLoading = true, saveError = null)
        }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                repository.getExercise(exerciseId)
            }.onSuccess { exercise ->
                if (exercise == null) {
                    _formState.update {
                        it.copy(isLoading = false, saveError = "Exercise not found")
                    }
                    return@onSuccess
                }

                _formState.value =
                    ExerciseFormState(
                        name = exercise.name,
                        notes = exercise.notes.orEmpty(),
                        trackingMode = trackingModeFromDatabaseValue(exercise.trackingMode),
                        isLoading = false,
                    )
            }.onFailure {
                _formState.update {
                    it.copy(isLoading = false, saveError = "Unable to load exercise")
                }
            }
        }
    }

    fun updateExercise(
        exerciseId: Long,
        onSaved: () -> Unit,
    ) {
        saveExercise(exerciseId = exerciseId, onSaved = onSaved)
    }

    private fun saveExercise(
        exerciseId: Long?,
        onSaved: () -> Unit,
    ) {
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
                val now = nowMillis()
                if (exerciseId == null) {
                    repository.createExercise(
                        name = trimmedName,
                        notes = trimmedNotes,
                        trackingMode = state.trackingMode.databaseValue,
                        nowMillis = now,
                    )
                } else {
                    repository.updateExercise(
                        id = exerciseId,
                        name = trimmedName,
                        notes = trimmedNotes,
                        trackingMode = state.trackingMode.databaseValue,
                        nowMillis = now,
                    )
                }
            }.onSuccess {
                _formState.value = ExerciseFormState()
                onSaved()
            }.onFailure {
                _formState.update { current ->
                    current.copy(
                        isSaving = false,
                        saveError = it.exerciseSaveErrorMessage(),
                    )
                }
            }
        }
    }

    fun deleteExercise(id: Long) {
        _listError.value = null
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                repository.deleteExercise(id)
            }.onFailure {
                _listError.value = "Unable to delete exercise"
            }
        }
    }

    fun clearSaveError() {
        _formState.update { state ->
            state.copy(saveError = null)
        }
    }

    fun clearListError() {
        _listError.value = null
    }
}

private fun trackingModeFromDatabaseValue(value: String): TrackingMode =
    TrackingMode.entries.firstOrNull { trackingMode ->
        trackingMode.databaseValue == value
    } ?: TrackingMode.Strength

private fun Throwable.exerciseSaveErrorMessage(): String =
    if (this is IllegalArgumentException && message?.contains("was not found") == true) {
        "Exercise not found"
    } else {
        "Unable to save exercise"
    }
