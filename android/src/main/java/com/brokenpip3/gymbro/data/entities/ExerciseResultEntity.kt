package com.brokenpip3.gymbro.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_results")
data class ExerciseResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workoutRunId: Long,
    val exerciseId: Long?,
    val exerciseNameSnapshot: String,
    val trackingModeSnapshot: String,
    val sortOrder: Int,
    val notes: String?,
)
