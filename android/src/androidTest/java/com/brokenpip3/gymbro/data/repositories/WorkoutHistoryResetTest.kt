package com.brokenpip3.gymbro.data.repositories

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.brokenpip3.gymbro.backup.RoomGymbroBackupDataSource
import com.brokenpip3.gymbro.data.GymbroDatabase
import com.brokenpip3.gymbro.data.demo.DemoDataSeeder
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutHistoryResetTest {
    @Test
    fun resetClearsHistoryAndRetainsDefinitions() =
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val database = Room.inMemoryDatabaseBuilder(context, GymbroDatabase::class.java).build()
            try {
                val repository = WorkoutRepository(database)
                val backup = RoomGymbroBackupDataSource(database)
                val dao = database.workoutRunDao()
                val now = System.currentTimeMillis()
                DemoDataSeeder(database).seed(now)
                val original = backup.readBackupData()
                assertTrue(original.setResults.isNotEmpty())
                assertFalse(repository.resetWorkoutHistory())
                assertEquals(original, backup.readBackupData())

                repository.finishWorkout(checkNotNull(dao.getActiveWorkoutRun()).id, now)
                val history = repository.observeCompletedWorkoutRuns()
                assertTrue(history.first().isNotEmpty())
                val cleared =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        withTimeout(5_000) { history.first { it.isEmpty() } }
                    }

                assertTrue(repository.resetWorkoutHistory())
                assertTrue(cleared.await().isEmpty())
                val remaining = backup.readBackupData()
                assertEquals(original.exercises, remaining.exercises)
                assertEquals(original.schedules, remaining.schedules)
                assertEquals(original.scheduleExercises, remaining.scheduleExercises)
                assertTrue(remaining.workoutRuns.isEmpty())
                assertTrue(remaining.exerciseResults.isEmpty())
                assertTrue(remaining.setResults.isEmpty())
            } finally {
                database.close()
            }
        }
}
