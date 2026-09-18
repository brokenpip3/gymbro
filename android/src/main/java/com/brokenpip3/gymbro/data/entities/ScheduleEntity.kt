package com.brokenpip3.gymbro.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
