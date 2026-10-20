package com.brokenpip3.gymbro.ui.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ExerciseListSettings {
    val groupExercisesByCategory: StateFlow<Boolean>
    val keepScreenOnDuringWorkouts: StateFlow<Boolean>

    fun setGroupExercisesByCategory(enabled: Boolean)

    fun setKeepScreenOnDuringWorkouts(enabled: Boolean)
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
    private val _keepScreenOnDuringWorkouts =
        MutableStateFlow(preferences.getBoolean(KEEP_SCREEN_ON_DURING_WORKOUTS_KEY, false))

    override val groupExercisesByCategory: StateFlow<Boolean> =
        _groupExercisesByCategory.asStateFlow()
    override val keepScreenOnDuringWorkouts: StateFlow<Boolean> =
        _keepScreenOnDuringWorkouts.asStateFlow()

    override fun setGroupExercisesByCategory(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(GROUP_EXERCISES_KEY, enabled)
            .apply()
        _groupExercisesByCategory.value = enabled
    }

    override fun setKeepScreenOnDuringWorkouts(enabled: Boolean) {
        preferences
            .edit()
            .putBoolean(KEEP_SCREEN_ON_DURING_WORKOUTS_KEY, enabled)
            .apply()
        _keepScreenOnDuringWorkouts.value = enabled
    }
}

internal const val SETTINGS_PREFERENCES_NAME = "gymbro.settings"
internal const val GROUP_EXERCISES_KEY = "group_exercises_by_category"
internal const val KEEP_SCREEN_ON_DURING_WORKOUTS_KEY = "keep_screen_on_during_workouts"
