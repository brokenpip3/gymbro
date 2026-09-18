package com.brokenpip3.gymbro.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "set_results",
    indices = [
        Index(value = ["exerciseResultId", "setOrder"], unique = true),
    ],
)
data class SetResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val exerciseResultId: Long,
    val setOrder: Int,
    val reps: Int?,
    val weight: Double?,
    val durationSeconds: Long?,
    val distance: Double?,
    val notes: String?,
    val isCompleted: Boolean = true,
)
