package com.brokenpip3.gymbro.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ThemeSettings {
    val themeMode: StateFlow<ThemeMode>
    val themePalette: StateFlow<ThemePalette>

    fun setThemeMode(themeMode: ThemeMode)
    fun setThemePalette(themePalette: ThemePalette)
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
    private val _themePalette = MutableStateFlow(preferences.getThemePalette())

    override val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()
    override val themePalette: StateFlow<ThemePalette> = _themePalette.asStateFlow()

    override fun setThemeMode(themeMode: ThemeMode) {
        preferences
            .edit()
            .putString(THEME_PREFERENCE_KEY, themeMode.name)
            .apply()
        _themeMode.value = themeMode
    }

    override fun setThemePalette(themePalette: ThemePalette) {
        preferences
            .edit()
            .putString(THEME_PREFERENCE_PALETTE_KEY, themePalette.name)
            .apply()
        _themePalette.value = themePalette
    }
}

internal const val THEME_PREFERENCES_NAME = "gymbro.theme"
internal const val THEME_PREFERENCE_KEY = "theme_mode"
internal const val THEME_PREFERENCE_PALETTE_KEY = "theme_palette"

private fun android.content.SharedPreferences.getThemeMode(): ThemeMode =
    getString(THEME_PREFERENCE_KEY, null)
        ?.let { storedValue ->
            ThemeMode.entries.firstOrNull { themeMode -> themeMode.name == storedValue }
        } ?: ThemeMode.System

private fun android.content.SharedPreferences.getThemePalette(): ThemePalette =
    getString(THEME_PREFERENCE_PALETTE_KEY, null)
        ?.let { storedValue ->
            ThemePalette.entries.firstOrNull { palette -> palette.name == storedValue }
        } ?: ThemePalette.Forest
