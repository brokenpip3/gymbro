package com.brokenpip3.gymbro.data.entities

data class ExerciseUsage(
    val exerciseId: Long,
    val sessionCount: Int,
    val lastCompletedAt: Long?,
)
