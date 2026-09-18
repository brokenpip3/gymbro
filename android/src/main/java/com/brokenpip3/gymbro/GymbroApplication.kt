package com.brokenpip3.gymbro

import android.app.Application
import com.brokenpip3.gymbro.backup.GymbroBackupStore
import com.brokenpip3.gymbro.backup.RoomGymbroBackupDataSource
import com.brokenpip3.gymbro.data.GymbroDatabase
import com.brokenpip3.gymbro.data.repositories.ExerciseRepository
import com.brokenpip3.gymbro.data.repositories.ScheduleRepository
import com.brokenpip3.gymbro.data.repositories.WorkoutRepository
import com.brokenpip3.gymbro.ui.theme.SharedPreferencesThemeSettings
import com.brokenpip3.gymbro.ui.theme.ThemeSettings

class GymbroApplication : Application() {
    val themeSettings: ThemeSettings by lazy {
        SharedPreferencesThemeSettings(this)
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
