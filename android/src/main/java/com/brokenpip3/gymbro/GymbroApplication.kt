package com.brokenpip3.gymbro

import android.app.Application
import com.brokenpip3.gymbro.backup.GymbroBackupStore
import com.brokenpip3.gymbro.backup.RoomGymbroBackupDataSource
import com.brokenpip3.gymbro.data.GymbroDatabase
import com.brokenpip3.gymbro.data.repositories.ExerciseRepository
import com.brokenpip3.gymbro.data.repositories.ScheduleRepository
import com.brokenpip3.gymbro.data.repositories.WorkoutRepository
import com.brokenpip3.gymbro.ui.settings.ExerciseListSettings
import com.brokenpip3.gymbro.ui.settings.SharedPreferencesExerciseListSettings
import com.brokenpip3.gymbro.ui.theme.SharedPreferencesThemeSettings
import com.brokenpip3.gymbro.ui.theme.ThemeSettings
import com.brokenpip3.gymbro.ui.theme.applyAppNightMode

class GymbroApplication : Application() {
    private lateinit var configuredThemeSettings: ThemeSettings
    val themeSettings: ThemeSettings get() = configuredThemeSettings

    override fun onCreate() {
        super.onCreate()
        configuredThemeSettings =
            SharedPreferencesThemeSettings(this) { themeMode ->
                applyAppNightMode(this, themeMode)
            }
    }

    val exerciseListSettings: ExerciseListSettings by lazy {
        SharedPreferencesExerciseListSettings(this)
    }

    val database: GymbroDatabase by lazy {
        GymbroDatabase.create(this)
    }

    val exerciseRepository: ExerciseRepository by lazy {
        ExerciseRepository(database.exerciseDao(), database.workoutRunDao())
    }

    val scheduleRepository: ScheduleRepository by lazy {
        ScheduleRepository(database.scheduleDao(), database.exerciseDao(), database.workoutRunDao())
    }

    val workoutRepository: WorkoutRepository by lazy {
        WorkoutRepository(database)
    }

    val backupStore: GymbroBackupStore by lazy {
        GymbroBackupStore(RoomGymbroBackupDataSource(database))
    }
}
