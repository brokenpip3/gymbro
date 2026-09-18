package com.brokenpip3.gymbro.data.repositories

import com.brokenpip3.gymbro.data.dao.ExerciseDao
import com.brokenpip3.gymbro.data.entities.ExerciseEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseRepositoryTest {
    @Test
    fun observeExercisesReturnsDaoFlow() {
        val exerciseFlow = MutableStateFlow<List<ExerciseEntity>>(emptyList())
        val repository = ExerciseRepository(FakeExerciseDao(exerciseFlow = exerciseFlow))

        assertSame(exerciseFlow, repository.observeExercises())
    }

    @Test
    fun createExerciseInsertsExerciseWithCreatedAndUpdatedTimestamps() =
        runTest {
            val dao = FakeExerciseDao(insertId = 42)
            val repository = ExerciseRepository(dao)

            val id =
                repository.createExercise(
                    name = "Squat",
                    notes = "Low bar",
                    trackingMode = "strength",
                    nowMillis = 1234,
                )

            assertEquals(42, id)
            assertEquals(
                ExerciseEntity(
                    name = "Squat",
                    notes = "Low bar",
                    trackingMode = "strength",
                    createdAt = 1234,
                    updatedAt = 1234,
                ),
                dao.insertedExercise,
            )
        }

    @Test
    fun updateExercisePreservesCreatedAtAndSetsUpdatedAt() =
        runTest {
            val dao =
                FakeExerciseDao(
                    exercise =
                        ExerciseEntity(
                            id = 7,
                            name = "Squat",
                            notes = "Low bar",
                            trackingMode = "strength",
                            createdAt = 100,
                            updatedAt = 200,
                        ),
                )
            val repository = ExerciseRepository(dao)

            repository.updateExercise(
                id = 7,
                name = "Front Squat",
                notes = null,
                trackingMode = "strength",
                nowMillis = 300,
            )

            assertEquals(
                ExerciseEntity(
                    id = 7,
                    name = "Front Squat",
                    notes = null,
                    trackingMode = "strength",
                    createdAt = 100,
                    updatedAt = 300,
                ),
                dao.updatedExercise,
            )
        }

    @Test
    fun deleteExerciseDeletesById() =
        runTest {
            val dao = FakeExerciseDao()
            val repository = ExerciseRepository(dao)

            repository.deleteExercise(9)

            assertEquals(9L, dao.deletedScheduleAssignmentsForExerciseId)
            assertEquals(9L, dao.deletedExerciseId)
        }

    @Test
    fun deleteExerciseThrowsWhenExerciseDoesNotExist() =
        runTest {
            val repository = ExerciseRepository(FakeExerciseDao(deleteCount = 0))

            val error =
                runCatching {
                    repository.deleteExercise(404)
                }.exceptionOrNull()

            assertTrue(error is IllegalArgumentException)
            assertEquals("Exercise 404 was not found", error?.message)
        }

    @Test
    fun updateExerciseThrowsWhenExerciseDoesNotExist() =
        runTest {
            val repository = ExerciseRepository(FakeExerciseDao())

            val error =
                runCatching {
                    repository.updateExercise(
                        id = 404,
                        name = "Missing",
                        notes = null,
                        trackingMode = "strength",
                        nowMillis = 500,
                    )
                }.exceptionOrNull()

            assertTrue(error is IllegalArgumentException)
            assertEquals("Exercise 404 was not found", error?.message)
        }
}

private class FakeExerciseDao(
    private val exerciseFlow: Flow<List<ExerciseEntity>> = MutableStateFlow(emptyList()),
    private val insertId: Long = 1,
    private val exercise: ExerciseEntity? = null,
    private val deleteCount: Int = 1,
) : ExerciseDao {
    var insertedExercise: ExerciseEntity? = null
    var updatedExercise: ExerciseEntity? = null
    var deletedExerciseId: Long? = null
    var deletedScheduleAssignmentsForExerciseId: Long? = null

    override fun observeExercises(): Flow<List<ExerciseEntity>> = exerciseFlow

    override suspend fun getExercise(id: Long): ExerciseEntity? = exercise?.takeIf { it.id == id }

    override suspend fun getAllExercises(): List<ExerciseEntity> = listOfNotNull(exercise)

    override suspend fun insertExercise(exercise: ExerciseEntity): Long {
        insertedExercise = exercise
        return insertId
    }

    override suspend fun insertExercises(exercises: List<ExerciseEntity>) = Unit

    override suspend fun updateExercise(exercise: ExerciseEntity) {
        updatedExercise = exercise
    }

    override suspend fun deleteExercise(id: Long): Int {
        deletedExerciseId = id
        return deleteCount
    }

    override suspend fun deleteScheduleExercisesForExercise(exerciseId: Long) {
        deletedScheduleAssignmentsForExerciseId = exerciseId
    }

    override suspend fun deleteAllExercises() = Unit
}
