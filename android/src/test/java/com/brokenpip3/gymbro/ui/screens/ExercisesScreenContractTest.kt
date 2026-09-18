package com.brokenpip3.gymbro.ui.screens

import androidx.compose.runtime.Composable
import com.brokenpip3.gymbro.data.entities.ExerciseEntity
import com.brokenpip3.gymbro.domain.TrackingMode
import com.brokenpip3.gymbro.ui.screens.exercises.CreateExerciseScreen
import com.brokenpip3.gymbro.ui.screens.exercises.ExerciseCreator
import com.brokenpip3.gymbro.ui.screens.exercises.ExerciseFormState
import com.brokenpip3.gymbro.ui.screens.exercises.ExerciseListItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

@Composable
private fun ExercisesScreenSignatureContract() {
    ExercisesScreen(
        exercises =
            listOf(
                ExerciseListItem(
                    exercise =
                        ExerciseEntity(
                            id = 1L,
                            name = "Back Squat",
                            notes = "Low bar",
                            trackingMode = TrackingMode.Strength.databaseValue,
                            createdAt = 100L,
                            updatedAt = 100L,
                        ),
                    sessionCount = 3,
                    lastCompletedAt = 500L,
                ),
            ),
        onCreateExercise = {},
        onEditExercise = {},
        onDeleteExercise = {},
        onClearListError = {},
    )
}

@Composable
private fun ExercisesRouteSignatureContract() {
    ExercisesRoute(
        repository = ContractExerciseRepository(),
        onCreateExercise = {},
        onEditExercise = {},
    )
}

@Composable
private fun ExercisesEmptyStateErrorContract() {
    ExercisesScreen(
        exercises = emptyList(),
        onCreateExercise = {},
        onEditExercise = {},
        onDeleteExercise = {},
        onClearListError = {},
        listError = "Unable to delete exercise",
    )
}

@Composable
private fun ExerciseEditFormContract() {
    CreateExerciseScreen(
        formState =
            ExerciseFormState(
                name = "Bench Press",
                notes = "Pause on chest",
                trackingMode = TrackingMode.Strength,
                saveError = "Unable to save exercise",
            ),
        onNameChange = {},
        onNotesChange = {},
        onTrackingModeChange = {},
        onSave = {},
    )
}

class ExercisesScreenContractTest {
    @Test
    fun exerciseSearchMatchesNameAndNotesIgnoringCase() {
        val rows =
            listOf(
                exerciseRow(id = 1L, name = "Back Squat", notes = "Low bar"),
                exerciseRow(id = 2L, name = "Tempo Run", notes = "Easy pace"),
            )

        assertEquals(listOf(1L), filterExerciseRows(rows, "LOW").map { row -> row.exercise.id })
        assertEquals(listOf(2L), filterExerciseRows(rows, "run").map { row -> row.exercise.id })
    }

    @Test
    fun blankExerciseSearchKeepsExistingOrder() {
        val rows =
            listOf(
                exerciseRow(id = 2L, name = "Tempo Run"),
                exerciseRow(id = 1L, name = "Back Squat"),
            )

        assertEquals(listOf(2L, 1L), filterExerciseRows(rows, "  ").map { row -> row.exercise.id })
    }
}

private fun exerciseRow(
    id: Long,
    name: String,
    notes: String? = null,
): ExerciseListItem =
    ExerciseListItem(
        exercise =
            ExerciseEntity(
                id = id,
                name = name,
                notes = notes,
                trackingMode = TrackingMode.Strength.databaseValue,
                createdAt = 0L,
                updatedAt = 0L,
            ),
    )

private class ContractExerciseRepository : ExerciseCreator {
    override fun observeExercises(): Flow<List<ExerciseEntity>> = MutableStateFlow(emptyList())

    override suspend fun createExercise(
        name: String,
        notes: String?,
        trackingMode: String,
        nowMillis: Long,
    ): Long = 1L

    override suspend fun getExercise(id: Long): ExerciseEntity? = null

    override suspend fun updateExercise(
        id: Long,
        name: String,
        notes: String?,
        trackingMode: String,
        nowMillis: Long,
    ) = Unit

    override suspend fun deleteExercise(id: Long) = Unit
}
