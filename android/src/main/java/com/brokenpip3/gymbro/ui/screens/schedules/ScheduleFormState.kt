package com.brokenpip3.gymbro.ui.screens.schedules

data class ScheduleFormState(
    val name: String = "",
    val notes: String = "",
    val editingScheduleId: Long? = null,
    val nameError: String? = null,
    val saveError: String? = null,
    val isSaving: Boolean = false,
)
