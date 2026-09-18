package com.brokenpip3.gymbro.data.repositories

import android.content.Context
import androidx.room.Room
import com.brokenpip3.gymbro.data.GymbroDatabase
import com.brokenpip3.gymbro.data.entities.ExerciseEntity
import com.brokenpip3.gymbro.data.entities.ScheduleEntity
import com.brokenpip3.gymbro.domain.TrackingMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ScheduleRepositoryConcurrencyTest {
    @Test
    fun concurrentAssignmentsReceiveUniqueSequentialSortOrders() =
        runTest {
            val context = RuntimeEnvironment.getApplication() as Context
            val database =
                Room
                    .inMemoryDatabaseBuilder(context, GymbroDatabase::class.java)
                    .allowMainThreadQueries()
                    .build()
            val exerciseId =
                database.exerciseDao().insertExercise(
                    ExerciseEntity(
                        name = "Squat",
                        notes = null,
                        trackingMode = TrackingMode.Strength.databaseValue,
                        createdAt = 0L,
                        updatedAt = 0L,
                    ),
                )
            val scheduleId =
                database.scheduleDao().insertSchedule(
                    ScheduleEntity(
                        name = "Leg Day",
                        notes = null,
                        createdAt = 0L,
                        updatedAt = 0L,
                    ),
                )
            val repository = ScheduleRepository(database.scheduleDao(), database.exerciseDao())

            List(2) {
                async(Dispatchers.Default) {
                    repository.assignExercise(scheduleId = scheduleId, exerciseId = exerciseId)
                }
            }.awaitAll()

            assertEquals(
                listOf(0, 1),
                database
                    .scheduleDao()
                    .getScheduleExercises(scheduleId)
                    .map { it.sortOrder }
                    .sorted(),
            )
            database.close()
        }
}
