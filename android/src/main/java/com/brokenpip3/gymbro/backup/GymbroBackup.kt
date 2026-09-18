package com.brokenpip3.gymbro.backup

import kotlinx.serialization.Serializable

@Serializable
data class GymbroBackup(
    val schemaVersion: Int,
    val scope: String = "full",
    val exercises: List<BackupExercise> = emptyList(),
    val schedules: List<BackupSchedule> = emptyList(),
    val scheduleExercises: List<BackupScheduleExercise> = emptyList(),
    val workoutRuns: List<BackupWorkoutRun> = emptyList(),
    val exerciseResults: List<BackupExerciseResult> = emptyList(),
    val setResults: List<BackupSetResult> = emptyList(),
)

@Serializable
data class BackupExercise(
    val id: Long,
    val name: String,
    val notes: String?,
    val trackingMode: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class BackupSchedule(
    val id: Long,
    val name: String,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class BackupScheduleExercise(
    val id: Long,
    val scheduleId: Long,
    val exerciseId: Long,
    val sortOrder: Int,
    val targetSets: Int?,
    val targetReps: Int?,
    val targetWeight: Double?,
    val targetDurationSeconds: Long?,
    val targetDistance: Double?,
)

@Serializable
data class BackupWorkoutRun(
    val id: Long,
    val scheduleId: Long,
    val scheduleNameSnapshot: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val notes: String?,
)

@Serializable
data class BackupExerciseResult(
    val id: Long,
    val workoutRunId: Long,
    val exerciseId: Long?,
    val exerciseNameSnapshot: String,
    val trackingModeSnapshot: String,
    val sortOrder: Int,
    val notes: String?,
)

@Serializable
data class BackupSetResult(
    val id: Long,
    val exerciseResultId: Long,
    val setOrder: Int,
    val reps: Int?,
    val weight: Double?,
    val durationSeconds: Long?,
    val distance: Double?,
    val notes: String?,
    val isCompleted: Boolean = true,
)
