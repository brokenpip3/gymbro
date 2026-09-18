package com.brokenpip3.gymbro.ui.theme

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThemeModeTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun clearPreferences() {
        context
            .getSharedPreferences(THEME_PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun systemModeFollowsTheDeviceAndExplicitModesIgnoreIt() {
        assertTrue(ThemeMode.System.isDark(systemIsDark = true))
        assertFalse(ThemeMode.System.isDark(systemIsDark = false))
        assertTrue(ThemeMode.Dark.isDark(systemIsDark = false))
        assertFalse(ThemeMode.Light.isDark(systemIsDark = true))
    }

    @Test
    fun themeChoiceIsLoadedAndPersistedLocally() {
        val firstSettings = SharedPreferencesThemeSettings(context)
        assertEquals(ThemeMode.System, firstSettings.themeMode.value)

        firstSettings.setThemeMode(ThemeMode.Dark)

        assertEquals(ThemeMode.Dark, firstSettings.themeMode.value)
        assertEquals(ThemeMode.Dark, SharedPreferencesThemeSettings(context).themeMode.value)
    }

    @Test
    fun unknownStoredThemeFallsBackToSystem() {
        context
            .getSharedPreferences(THEME_PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(THEME_PREFERENCE_KEY, "solarized")
            .commit()

        assertEquals(ThemeMode.System, SharedPreferencesThemeSettings(context).themeMode.value)
    }
}
