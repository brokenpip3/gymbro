package com.brokenpip3.gymbro.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_exercises")
data class ScheduleExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val scheduleId: Long,
    val exerciseId: Long,
    val sortOrder: Int,
    val targetSets: Int?,
    val targetReps: Int?,
    val targetWeight: Double?,
    val targetDurationSeconds: Long?,
    val targetDistance: Double?,
)
