package com.brokenpip3.gymbro.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.brokenpip3.gymbro.data.entities.ScheduleEntity
import com.brokenpip3.gymbro.data.entities.ScheduleExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
@Suppress("TooManyFunctions")
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY name COLLATE NOCASE")
    fun observeSchedules(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE id = :id")
    fun observeSchedule(id: Long): Flow<ScheduleEntity?>

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getSchedule(id: Long): ScheduleEntity?

    @Query("SELECT * FROM schedules ORDER BY id")
    suspend fun getAllSchedules(): List<ScheduleEntity>

    @Query("SELECT * FROM schedule_exercises WHERE scheduleId = :scheduleId ORDER BY sortOrder")
    fun observeScheduleExercises(scheduleId: Long): Flow<List<ScheduleExerciseEntity>>

    @Query("SELECT * FROM schedule_exercises WHERE scheduleId = :scheduleId ORDER BY sortOrder")
    suspend fun getScheduleExercises(scheduleId: Long): List<ScheduleExerciseEntity>

    @Query("SELECT * FROM schedule_exercises ORDER BY id")
    suspend fun getAllScheduleExercises(): List<ScheduleExerciseEntity>

    @Query("SELECT * FROM schedule_exercises WHERE id = :id")
    suspend fun getScheduleExercise(id: Long): ScheduleExerciseEntity?

    @Insert
    suspend fun insertSchedule(schedule: ScheduleEntity): Long

    @Insert
    suspend fun insertSchedules(schedules: List<ScheduleEntity>)

    @Update
    suspend fun updateSchedule(schedule: ScheduleEntity)

    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun deleteSchedule(id: Long): Int

    @Transaction
    suspend fun deleteScheduleAndAssignments(id: Long): Boolean {
        if (getSchedule(id) == null) return false

        deleteScheduleExercisesForSchedule(id)
        return deleteSchedule(id) > 0
    }

    @Query("DELETE FROM schedules")
    suspend fun deleteAllSchedules()

    @Query("DELETE FROM schedule_exercises WHERE scheduleId = :scheduleId")
    suspend fun deleteScheduleExercisesForSchedule(scheduleId: Long)

    @Insert
    suspend fun insertScheduleExercise(scheduleExercise: ScheduleExerciseEntity): Long

    @Transaction
    suspend fun insertScheduleExerciseAtEnd(
        scheduleId: Long,
        exerciseId: Long,
    ): Long {
        val nextSortOrder =
            getScheduleExercises(scheduleId)
                .maxOfOrNull { scheduleExercise -> scheduleExercise.sortOrder }
                ?.plus(1)
                ?: 0

        return insertScheduleExercise(
            ScheduleExerciseEntity(
                scheduleId = scheduleId,
                exerciseId = exerciseId,
                sortOrder = nextSortOrder,
                targetSets = null,
                targetReps = null,
                targetWeight = null,
                targetDurationSeconds = null,
                targetDistance = null,
            ),
        )
    }

    @Insert
    suspend fun insertScheduleExercises(scheduleExercises: List<ScheduleExerciseEntity>)

    @Update
    suspend fun updateScheduleExercise(scheduleExercise: ScheduleExerciseEntity)

    @Query("DELETE FROM schedule_exercises WHERE id = :id")
    suspend fun deleteScheduleExercise(id: Long)

    @Query("DELETE FROM schedule_exercises")
    suspend fun deleteAllScheduleExercises()
}
