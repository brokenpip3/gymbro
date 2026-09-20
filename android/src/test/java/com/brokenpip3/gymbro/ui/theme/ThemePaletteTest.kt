package com.brokenpip3.gymbro.ui.theme

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThemePaletteTest {
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
    fun paletteDefaultsToForest() {
        val settings = SharedPreferencesThemeSettings(context)

        assertEquals(ThemePalette.Forest, settings.themePalette.value)
    }

    @Test
    fun paletteChoiceIsLoadedAndPersistedLocally() {
        val firstSettings = SharedPreferencesThemeSettings(context)

        firstSettings.setThemePalette(ThemePalette.Sunset)

        assertEquals(ThemePalette.Sunset, firstSettings.themePalette.value)
        assertEquals(ThemePalette.Sunset, SharedPreferencesThemeSettings(context).themePalette.value)
    }

    @Test
    fun unknownStoredPaletteFallsBackToForest() {
        context
            .getSharedPreferences(THEME_PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(THEME_PREFERENCE_PALETTE_KEY, "Neon")
            .commit()

        val settings = SharedPreferencesThemeSettings(context)

        assertEquals(ThemePalette.Forest, settings.themePalette.value)
    }
}
