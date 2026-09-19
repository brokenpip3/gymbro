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
                category = "Push",
                trackingMode = TrackingMode.Strength,
                saveError = "Unable to save exercise",
            ),
        onNameChange = {},
        onNotesChange = {},
        onCategoryChange = {},
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
    fun exerciseSearchMatchesCategoryIgnoringCase() {
        val rows =
            listOf(
                exerciseRow(id = 1L, name = "Back Squat", category = "Push"),
                exerciseRow(id = 2L, name = "Tempo Run", category = "Cardio"),
            )

        assertEquals(listOf(1L), filterExerciseRows(rows, "push").map { row -> row.exercise.id })
        assertEquals(listOf(2L), filterExerciseRows(rows, "CARDIO").map { row -> row.exercise.id })
    }

    @Test
    fun groupingDisabledKeepsFlatRowOrder() {
        val rows =
            listOf(
                exerciseRow(id = 1L, name = "A", category = "Push"),
                exerciseRow(id = 2L, name = "B", category = null),
            )

        assertEquals(
            listOf(
                ExerciseListEntry.Row(rows[0]),
                ExerciseListEntry.Row(rows[1]),
            ),
            buildExerciseListEntries(rows, groupByCategory = false),
        )
    }

    @Test
    fun groupingSortsCategoriesAlphabeticallyWithUncategorizedLast() {
        val rows =
            listOf(
                exerciseRow(id = 1L, name = "Squat", category = "legs"),
                exerciseRow(id = 2L, name = "Bench", category = "Push"),
                exerciseRow(id = 3L, name = "Run", category = null),
                exerciseRow(id = 4L, name = "Lat Pull", category = "Legs"),
            )

        assertEquals(
            listOf(
                ExerciseListEntry.Header("Legs"),
                ExerciseListEntry.Row(rows[3]),
                ExerciseListEntry.Row(rows[0]),
                ExerciseListEntry.Header("Push"),
                ExerciseListEntry.Row(rows[1]),
                ExerciseListEntry.Header(UNCATEGORIZED_GROUP_LABEL),
                ExerciseListEntry.Row(rows[2]),
            ),
            buildExerciseListEntries(rows, groupByCategory = true),
        )
    }

    @Test
    fun groupingPreservesNameOrderWithinCategory() {
        val rows =
            listOf(
                exerciseRow(id = 1L, name = "Cable Fly", category = "Push"),
                exerciseRow(id = 2L, name = "Bench", category = "Push"),
            )

        assertEquals(
            listOf(
                ExerciseListEntry.Header("Push"),
                ExerciseListEntry.Row(rows[1]),
                ExerciseListEntry.Row(rows[0]),
            ),
            buildExerciseListEntries(rows, groupByCategory = true),
        )
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
    category: String? = null,
): ExerciseListItem =
    ExerciseListItem(
        exercise =
            ExerciseEntity(
                id = id,
                name = name,
                notes = notes,
                category = category,
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
        category: String?,
        trackingMode: String,
        nowMillis: Long,
    ): Long = 1L

    override suspend fun getExercise(id: Long): ExerciseEntity? = null

    override suspend fun updateExercise(
        id: Long,
        name: String,
        notes: String?,
        category: String?,
        trackingMode: String,
        nowMillis: Long,
    ) = Unit

    override suspend fun deleteExercise(id: Long) = Unit
}
