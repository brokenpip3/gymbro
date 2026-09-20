package com.brokenpip3.gymbro.ui.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ExerciseListSettings {
    val groupExercisesByCategory: StateFlow<Boolean>

    fun setGroupExercisesByCategory(enabled: Boolean)
}

class SharedPreferencesExerciseListSettings(
    context: Context,
) : ExerciseListSettings {
    private val preferences =
        context.applicationContext.getSharedPreferences(
            SETTINGS_PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )
    private val _groupExercisesByCategory =
        MutableStateFlow(preferences.getBoolean(GROUP_EXERCISES_KEY, false))

    override val groupExercisesByCategory: StateFlow<Boolean> =
        _groupExercisesByCategory.asStateFlow()

    override fun setGroupExercisesByCategory(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(GROUP_EXERCISES_KEY, enabled)
            .apply()
        _groupExercisesByCategory.value = enabled
    }
}

internal const val SETTINGS_PREFERENCES_NAME = "gymbro.settings"
internal const val GROUP_EXERCISES_KEY = "group_exercises_by_category"
