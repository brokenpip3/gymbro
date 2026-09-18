package com.brokenpip3.gymbro.data

import android.content.Context
import androidx.room.Room
import com.brokenpip3.gymbro.data.demo.DemoDataSeeder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DemoDataSeederTest {
    @Test
    fun seedCreatesExercisesSchedulesHistoryAndAnUnfinishedWorkout() =
        runTest {
            val database = database()

            val result = DemoDataSeeder(database).seed(nowMillis = 1_000_000L)

            assertTrue(result.inserted)
            assertEquals(8, database.exerciseDao().getAllExercises().size)
            assertEquals(3, database.scheduleDao().getAllSchedules().size)
            assertEquals(9, database.workoutRunDao().getAllWorkoutRuns().count { it.finishedAt != null })
            assertNotNull(database.workoutRunDao().getActiveWorkoutRun())
            assertTrue(database.workoutRunDao().getAllExerciseResults().isNotEmpty())
            assertTrue(database.workoutRunDao().getAllSetResults().isNotEmpty())

            database.close()
        }

    @Test
    fun seedIsIdempotent() =
        runTest {
            val database = database()
            val seeder = DemoDataSeeder(database)

            seeder.seed(nowMillis = 1_000_000L)
            val secondResult = seeder.seed(nowMillis = 2_000_000L)

            assertTrue(!secondResult.inserted)
            assertEquals(8, database.exerciseDao().getAllExercises().size)
            assertEquals(10, database.workoutRunDao().getAllWorkoutRuns().size)

            database.close()
        }
}

private fun database(): GymbroDatabase =
    Room
        .inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication() as Context,
            GymbroDatabase::class.java,
        ).allowMainThreadQueries()
        .build()
