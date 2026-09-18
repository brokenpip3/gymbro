package com.brokenpip3.gymbro.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_runs")
data class WorkoutRunEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val scheduleId: Long,
    val scheduleNameSnapshot: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val notes: String?,
)
