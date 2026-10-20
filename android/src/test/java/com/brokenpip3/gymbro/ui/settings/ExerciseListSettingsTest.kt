package com.brokenpip3.gymbro.ui.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExerciseListSettingsTest {
    @Before
    fun clearPreferences() {
        ApplicationProvider
            .getApplicationContext<Context>()
            .getSharedPreferences(SETTINGS_PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun defaultsToGroupingDisabled() {
        val settings = SharedPreferencesExerciseListSettings(ApplicationProvider.getApplicationContext<Context>())

        assertFalse(settings.groupExercisesByCategory.value)
    }

    @Test
    fun persistsGroupingChoice() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = SharedPreferencesExerciseListSettings(context)

        settings.setGroupExercisesByCategory(true)

        assertTrue(settings.groupExercisesByCategory.value)
        assertTrue(
            SharedPreferencesExerciseListSettings(context).groupExercisesByCategory.value,
        )
    }

    @Test
    fun defaultsToKeepingScreenOnDisabled() {
        val settings = SharedPreferencesExerciseListSettings(ApplicationProvider.getApplicationContext<Context>())

        assertFalse(settings.keepScreenOnDuringWorkouts.value)
    }

    @Test
    fun persistsKeepScreenOnChoice() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = SharedPreferencesExerciseListSettings(context)

        settings.setKeepScreenOnDuringWorkouts(true)

        assertTrue(settings.keepScreenOnDuringWorkouts.value)
        assertTrue(
            SharedPreferencesExerciseListSettings(context).keepScreenOnDuringWorkouts.value,
        )
    }
}
