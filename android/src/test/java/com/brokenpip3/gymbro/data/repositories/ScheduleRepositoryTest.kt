package com.brokenpip3.gymbro.data.repositories

import com.brokenpip3.gymbro.data.dao.ExerciseDao
import com.brokenpip3.gymbro.data.dao.ScheduleDao
import com.brokenpip3.gymbro.data.entities.ExerciseEntity
import com.brokenpip3.gymbro.data.entities.ScheduleEntity
import com.brokenpip3.gymbro.data.entities.ScheduleExerciseEntity
import com.brokenpip3.gymbro.ui.screens.schedules.ScheduleExerciseTargets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class ScheduleRepositoryTest {
    @Test
    fun createScheduleInsertsScheduleWithTimestamps() =
        runTest {
            val scheduleDao = FakeScheduleDao(insertScheduleId = 99)
            val repository = ScheduleRepository(scheduleDao, FakeScheduleExerciseDao())

            val id = repository.createSchedule("Push Day", "Chest and shoulders", 1234L)

            assertEquals(99, id)
            assertEquals(
                ScheduleEntity(
                    name = "Push Day",
                    notes = "Chest and shoulders",
                    createdAt = 1234L,
                    updatedAt = 1234L,
                ),
                scheduleDao.insertedSchedule,
            )
        }

    @Test
    fun assignExerciseAppendsAfterCurrentAssignments() =
        runTest {
            val scheduleDao =
                FakeScheduleDao(
                    scheduleExercises =
                        listOf(
                            scheduleExercise(sortOrder = 0),
                            scheduleExercise(sortOrder = 1),
                        ),
                )
            val repository = ScheduleRepository(scheduleDao, FakeScheduleExerciseDao())

            repository.assignExercise(scheduleId = 7, exerciseId = 42)

            assertEquals(
                ScheduleExerciseEntity(
                    scheduleId = 7,
                    exerciseId = 42,
                    sortOrder = 2,
                    targetSets = null,
                    targetReps = null,
                    targetWeight = null,
                    targetDurationSeconds = null,
                    targetDistance = null,
                ),
                scheduleDao.insertedScheduleExercise,
            )
        }

    @Test
    fun updateSchedulePreservesIdAndCreatedAtAndUpdatesEditableFields() =
        runTest {
            val scheduleDao =
                FakeScheduleDao(
                    schedule =
                        ScheduleEntity(
                            id = 7,
                            name = "Old name",
                            notes = "Old notes",
                            createdAt = 100L,
                            updatedAt = 200L,
                        ),
                )
            val repository = ScheduleRepository(scheduleDao, FakeScheduleExerciseDao())

            repository.updateSchedule(
                id = 7,
                name = "Leg Day",
                notes = null,
                nowMillis = 300L,
            )

            assertEquals(
                ScheduleEntity(
                    id = 7,
                    name = "Leg Day",
                    notes = null,
                    createdAt = 100L,
                    updatedAt = 300L,
                ),
                scheduleDao.updatedSchedule,
            )
        }

    @Test
    fun updateMissingScheduleThrows() =
        runTest {
            val repository = ScheduleRepository(FakeScheduleDao(schedule = null), FakeScheduleExerciseDao())

            try {
                repository.updateSchedule(
                    id = 7,
                    name = "Leg Day",
                    notes = null,
                    nowMillis = 300L,
                )
                fail("Expected missing schedule update to fail")
            } catch (error: IllegalArgumentException) {
                assertEquals("Schedule 7 was not found", error.message)
            }
        }

    @Test
    fun deleteScheduleDeletesAssignedExercisesBeforeSchedule() =
        runTest {
            val scheduleDao =
                FakeScheduleDao(
                    schedule =
                        ScheduleEntity(
                            id = 7L,
                            name = "Leg Day",
                            notes = null,
                            createdAt = 0L,
                            updatedAt = 0L,
                        ),
                )
            val repository = ScheduleRepository(scheduleDao, FakeScheduleExerciseDao())

            repository.deleteSchedule(7)

            assertEquals(7L, scheduleDao.deletedScheduleExercisesForScheduleId)
            assertEquals(7L, scheduleDao.deletedScheduleId)
        }

    @Test
    fun deleteMissingScheduleThrows() =
        runTest {
            val repository =
                ScheduleRepository(
                    scheduleDao = FakeScheduleDao(schedule = null),
                    exerciseDao = FakeScheduleExerciseDao(),
                )

            try {
                repository.deleteSchedule(7)
                fail("Expected missing schedule delete to fail")
            } catch (error: IllegalArgumentException) {
                assertEquals("Schedule 7 was not found", error.message)
            }
        }

    @Test
    fun removeExerciseFromScheduleDeletesScheduleExercise() =
        runTest {
            val scheduleDao = FakeScheduleDao()
            val repository = ScheduleRepository(scheduleDao, FakeScheduleExerciseDao())

            repository.removeExerciseFromSchedule(11)

            assertEquals(11L, scheduleDao.deletedScheduleExerciseId)
        }

    @Test
    fun removeMissingExerciseFromScheduleThrows() =
        runTest {
            val repository = ScheduleRepository(FakeScheduleDao(scheduleExercise = null), FakeScheduleExerciseDao())

            try {
                repository.removeExerciseFromSchedule(11)
                fail("Expected missing schedule exercise removal to fail")
            } catch (error: IllegalArgumentException) {
                assertEquals("Schedule exercise 11 was not found", error.message)
            }
        }

    @Test
    fun moveScheduleExerciseUpSwapsWithPreviousAndNormalizesSortOrder() =
        runTest {
            val first = scheduleExercise(id = 1, sortOrder = 0)
            val second = scheduleExercise(id = 2, sortOrder = 1)
            val third = scheduleExercise(id = 3, sortOrder = 5)
            val scheduleDao =
                FakeScheduleDao(scheduleExercises = listOf(first, second, third))
            val repository = ScheduleRepository(scheduleDao, FakeScheduleExerciseDao())

            repository.moveScheduleExercise(
                scheduleId = 7,
                scheduleExerciseId = 3,
                direction = MoveDirection.Up,
            )

            assertEquals(
                listOf(
                    first.copy(sortOrder = 0),
                    third.copy(sortOrder = 1),
                    second.copy(sortOrder = 2),
                ),
                scheduleDao.updatedScheduleExercises,
            )
        }

    @Test
    fun moveScheduleExerciseDownSwapsWithNextAndNormalizesSortOrder() =
        runTest {
            val first = scheduleExercise(id = 1, sortOrder = 0)
            val second = scheduleExercise(id = 2, sortOrder = 1)
            val third = scheduleExercise(id = 3, sortOrder = 2)
            val scheduleDao =
                FakeScheduleDao(scheduleExercises = listOf(first, second, third))
            val repository = ScheduleRepository(scheduleDao, FakeScheduleExerciseDao())

            repository.moveScheduleExercise(
                scheduleId = 7,
                scheduleExerciseId = 1,
                direction = MoveDirection.Down,
            )

            assertEquals(
                listOf(
                    second.copy(sortOrder = 0),
                    first.copy(sortOrder = 1),
                    third.copy(sortOrder = 2),
                ),
                scheduleDao.updatedScheduleExercises,
            )
        }

    @Test
    fun moveScheduleExercisePreservesTargetFieldsAndOnlyChangesSortOrder() =
        runTest {
            val first =
                scheduleExercise(
                    id = 1,
                    sortOrder = 0,
                    targetSets = 3,
                    targetReps = 12,
                    targetWeight = 50.0,
                    targetDurationSeconds = 60L,
                    targetDistance = 1.5,
                )
            val second =
                scheduleExercise(
                    id = 2,
                    sortOrder = 1,
                    targetSets = 4,
                    targetReps = 8,
                    targetWeight = 60.0,
                    targetDurationSeconds = 90L,
                    targetDistance = 2.0,
                )
            val scheduleDao = FakeScheduleDao(scheduleExercises = listOf(first, second))
            val repository = ScheduleRepository(scheduleDao, FakeScheduleExerciseDao())

            repository.moveScheduleExercise(
                scheduleId = 7,
                scheduleExerciseId = 1,
                direction = MoveDirection.Down,
            )

            assertEquals(
                listOf(
                    second.copy(sortOrder = 0),
                    first.copy(sortOrder = 1),
                ),
                scheduleDao.updatedScheduleExercises,
            )
        }

    @Test
    fun moveScheduleExerciseAtEdgeDoesNothing() =
        runTest {
            val scheduleDao =
                FakeScheduleDao(
                    scheduleExercises =
                        listOf(
                            scheduleExercise(id = 1, sortOrder = 0),
                            scheduleExercise(id = 2, sortOrder = 1),
                        ),
                )
            val repository = ScheduleRepository(scheduleDao, FakeScheduleExerciseDao())

            repository.moveScheduleExercise(
                scheduleId = 7,
                scheduleExerciseId = 1,
                direction = MoveDirection.Up,
            )

            assertEquals(emptyList<ScheduleExerciseEntity>(), scheduleDao.updatedScheduleExercises)
        }

    @Test
    fun moveMissingScheduleExerciseThrows() =
        runTest {
            val scheduleDao =
                FakeScheduleDao(
                    scheduleExercises =
                        listOf(
                            scheduleExercise(id = 1, sortOrder = 0),
                        ),
                )
            val repository = ScheduleRepository(scheduleDao, FakeScheduleExerciseDao())

            try {
                repository.moveScheduleExercise(
                    scheduleId = 7,
                    scheduleExerciseId = 99,
                    direction = MoveDirection.Down,
                )
                fail("Expected missing schedule exercise move to fail")
            } catch (error: IllegalArgumentException) {
                assertEquals("Schedule exercise 99 was not found in schedule 7", error.message)
            }
        }

    @Test
    fun updateScheduleExerciseTargetsPreservesIdentityAndOrder() =
        runTest {
            val existing =
                scheduleExercise(
                    id = 11,
                    sortOrder = 3,
                    targetSets = 2,
                    targetReps = 6,
                    targetWeight = 40.0,
                )
            val scheduleDao = FakeScheduleDao(scheduleExercise = existing)
            val repository = ScheduleRepository(scheduleDao, FakeScheduleExerciseDao())

            repository.updateScheduleExerciseTargets(
                scheduleExerciseId = 11,
                targets =
                    ScheduleExerciseTargets(
                        targetSets = 4,
                        targetReps = 8,
                        targetWeight = 60.0,
                        targetDurationSeconds = null,
                        targetDistance = null,
                    ),
            )

            assertEquals(
                existing.copy(
                    targetSets = 4,
                    targetReps = 8,
                    targetWeight = 60.0,
                    targetDurationSeconds = null,
                    targetDistance = null,
                ),
                scheduleDao.updatedScheduleExercises.single(),
            )
        }

    @Test
    fun updateMissingScheduleExerciseTargetsThrows() =
        runTest {
            val repository = ScheduleRepository(FakeScheduleDao(scheduleExercise = null), FakeScheduleExerciseDao())

            try {
                repository.updateScheduleExerciseTargets(
                    scheduleExerciseId = 11,
                    targets =
                        ScheduleExerciseTargets(
                            targetSets = 4,
                            targetReps = 8,
                            targetWeight = 60.0,
                            targetDurationSeconds = null,
                            targetDistance = null,
                        ),
                )
                fail("Expected missing target update to fail")
            } catch (error: IllegalArgumentException) {
                assertEquals("Schedule exercise 11 was not found", error.message)
            }
        }
}

private fun scheduleExercise(
    id: Long = 0,
    sortOrder: Int,
    targetSets: Int? = null,
    targetReps: Int? = null,
    targetWeight: Double? = null,
    targetDurationSeconds: Long? = null,
    targetDistance: Double? = null,
): ScheduleExerciseEntity =
    ScheduleExerciseEntity(
        id = id,
        scheduleId = 7,
        exerciseId = 100 + sortOrder.toLong(),
        sortOrder = sortOrder,
        targetSets = targetSets,
        targetReps = targetReps,
        targetWeight = targetWeight,
        targetDurationSeconds = targetDurationSeconds,
        targetDistance = targetDistance,
    )

private class FakeScheduleDao(
    private val schedulesFlow: Flow<List<ScheduleEntity>> = MutableStateFlow(emptyList()),
    private val scheduleExercisesFlow: Flow<List<ScheduleExerciseEntity>> = MutableStateFlow(emptyList()),
    private val schedule: ScheduleEntity? = null,
    private val scheduleExercise: ScheduleExerciseEntity? = scheduleExercise(id = 11, sortOrder = 0),
    private val scheduleExercises: List<ScheduleExerciseEntity> = emptyList(),
    private val insertScheduleId: Long = 1,
    private val insertScheduleExerciseId: Long = 1,
) : ScheduleDao {
    var insertedSchedule: ScheduleEntity? = null
    var updatedSchedule: ScheduleEntity? = null
    var deletedScheduleId: Long? = null
    var deletedScheduleExercisesForScheduleId: Long? = null
    var insertedScheduleExercise: ScheduleExerciseEntity? = null
    val updatedScheduleExercises = mutableListOf<ScheduleExerciseEntity>()
    var deletedScheduleExerciseId: Long? = null

    override fun observeSchedules(): Flow<List<ScheduleEntity>> = schedulesFlow

    override fun observeSchedule(id: Long): Flow<ScheduleEntity?> = MutableStateFlow(null)

    override suspend fun getSchedule(id: Long): ScheduleEntity? = schedule?.takeIf { it.id == id }

    override suspend fun getAllSchedules(): List<ScheduleEntity> = listOfNotNull(schedule)

    override fun observeScheduleExercises(scheduleId: Long): Flow<List<ScheduleExerciseEntity>> = scheduleExercisesFlow

    override suspend fun getScheduleExercises(scheduleId: Long): List<ScheduleExerciseEntity> = scheduleExercises

    override suspend fun getAllScheduleExercises(): List<ScheduleExerciseEntity> = scheduleExercises

    override suspend fun insertSchedule(schedule: ScheduleEntity): Long {
        insertedSchedule = schedule
        return insertScheduleId
    }

    override suspend fun insertSchedules(schedules: List<ScheduleEntity>) = Unit

    override suspend fun updateSchedule(schedule: ScheduleEntity) {
        updatedSchedule = schedule
    }

    override suspend fun deleteSchedule(id: Long): Int {
        deletedScheduleId = id
        return 1
    }

    override suspend fun deleteAllSchedules() = Unit

    override suspend fun getScheduleExercise(id: Long) = scheduleExercise?.takeIf { it.id == id }

    override suspend fun deleteScheduleExercisesForSchedule(scheduleId: Long) {
        deletedScheduleExercisesForScheduleId = scheduleId
    }

    override suspend fun insertScheduleExercise(scheduleExercise: ScheduleExerciseEntity): Long {
        insertedScheduleExercise = scheduleExercise
        return insertScheduleExerciseId
    }

    override suspend fun insertScheduleExercises(scheduleExercises: List<ScheduleExerciseEntity>) = Unit

    override suspend fun updateScheduleExercise(scheduleExercise: ScheduleExerciseEntity) {
        updatedScheduleExercises += scheduleExercise
    }

    override suspend fun deleteScheduleExercise(id: Long) {
        deletedScheduleExerciseId = id
    }

    override suspend fun deleteAllScheduleExercises() = Unit
}

private class FakeScheduleExerciseDao(
    private val exerciseFlow: Flow<List<ExerciseEntity>> = MutableStateFlow(emptyList()),
) : ExerciseDao {
    override fun observeExercises(): Flow<List<ExerciseEntity>> = exerciseFlow

    override suspend fun getExercise(id: Long): ExerciseEntity? = null

    override suspend fun getAllExercises(): List<ExerciseEntity> = emptyList()

    override suspend fun insertExercise(exercise: ExerciseEntity): Long = 1

    override suspend fun insertExercises(exercises: List<ExerciseEntity>) = Unit

    override suspend fun updateExercise(exercise: ExerciseEntity) = Unit

    override suspend fun deleteExercise(id: Long): Int = 1

    override suspend fun deleteScheduleExercisesForExercise(exerciseId: Long) = Unit

    override suspend fun deleteAllExercises() = Unit
}
