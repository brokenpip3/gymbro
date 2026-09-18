package com.brokenpip3.gymbro.ui.screens.exercises

import com.brokenpip3.gymbro.domain.TrackingMode

data class ExerciseFormState(
    val name: String = "",
    val notes: String = "",
    val trackingMode: TrackingMode = TrackingMode.Strength,
    val nameError: String? = null,
    val saveError: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
)
