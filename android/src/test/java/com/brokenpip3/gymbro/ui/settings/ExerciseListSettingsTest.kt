package com.brokenpip3.gymbro.ui.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExerciseListSettingsTest {
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
}
