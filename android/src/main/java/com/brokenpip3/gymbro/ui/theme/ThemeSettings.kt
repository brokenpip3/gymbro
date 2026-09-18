package com.brokenpip3.gymbro.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ThemeSettings {
    val themeMode: StateFlow<ThemeMode>

    fun setThemeMode(themeMode: ThemeMode)
}

class SharedPreferencesThemeSettings(
    context: Context,
) : ThemeSettings {
    private val preferences =
        context.applicationContext.getSharedPreferences(
            THEME_PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )
    private val _themeMode = MutableStateFlow(preferences.getThemeMode())

    override val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    override fun setThemeMode(themeMode: ThemeMode) {
        preferences
            .edit()
            .putString(THEME_PREFERENCE_KEY, themeMode.name)
            .apply()
        _themeMode.value = themeMode
    }
}

internal const val THEME_PREFERENCES_NAME = "gymbro.theme"
internal const val THEME_PREFERENCE_KEY = "theme_mode"

private fun android.content.SharedPreferences.getThemeMode(): ThemeMode =
    getString(THEME_PREFERENCE_KEY, null)
        ?.let { storedValue ->
            ThemeMode.entries.firstOrNull { themeMode -> themeMode.name == storedValue }
        } ?: ThemeMode.System
