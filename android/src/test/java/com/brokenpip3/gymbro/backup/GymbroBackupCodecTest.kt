package com.brokenpip3.gymbro.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GymbroBackupCodecTest {
    @Test
    fun encodesPrettyPrintedVersionedJson() {
        val encoded =
            GymbroBackupCodec.encode(
                GymbroBackup(schemaVersion = GymbroBackupCodec.SUPPORTED_SCHEMA_VERSION),
            )

        assertTrue(encoded.contains("\"schemaVersion\": 1"))
        assertTrue(encoded.contains("\n"))
    }

    @Test
    fun decodesSupportedBackup() {
        val decoded = GymbroBackupCodec.decode("""{ "schemaVersion": 1 }""")

        assertEquals(1, decoded.schemaVersion)
    }

    @Test
    fun roundTripsAllBackupFields() {
        val backup =
            GymbroBackup(
                schemaVersion = GymbroBackupCodec.SUPPORTED_SCHEMA_VERSION,
                exercises =
                    listOf(
                        BackupExercise(
                            id = 1,
                            name = "Squat",
                            notes = "Low bar",
                            trackingMode = "strength",
                            createdAt = 10,
                            updatedAt = 20,
                        ),
                    ),
                schedules =
                    listOf(
                        BackupSchedule(
                            id = 2,
                            name = "Leg day",
                            notes = "Heavy",
                            createdAt = 30,
                            updatedAt = 40,
                        ),
                    ),
                scheduleExercises =
                    listOf(
                        BackupScheduleExercise(
                            id = 3,
                            scheduleId = 2,
                            exerciseId = 1,
                            sortOrder = 0,
                            targetSets = 4,
                            targetReps = 8,
                            targetWeight = 100.0,
                            targetDurationSeconds = null,
                            targetDistance = null,
                        ),
                    ),
                workoutRuns =
                    listOf(
                        BackupWorkoutRun(
                            id = 4,
                            scheduleId = 2,
                            scheduleNameSnapshot = "Leg day",
                            startedAt = 50,
                            finishedAt = 80,
                            notes = "Good",
                        ),
                    ),
                exerciseResults =
                    listOf(
                        BackupExerciseResult(
                            id = 5,
                            workoutRunId = 4,
                            exerciseId = 1,
                            exerciseNameSnapshot = "Squat",
                            trackingModeSnapshot = "strength",
                            sortOrder = 0,
                            notes = "Stable",
                        ),
                    ),
                setResults =
                    listOf(
                        BackupSetResult(
                            id = 6,
                            exerciseResultId = 5,
                            setOrder = 1,
                            reps = 8,
                            weight = 100.0,
                            durationSeconds = null,
                            distance = null,
                            notes = "RPE 8",
                            isCompleted = false,
                        ),
                    ),
            )

        val decoded = GymbroBackupCodec.decode(GymbroBackupCodec.encode(backup))

        assertEquals(backup, decoded)
    }

    @Test
    fun rejectsInvalidJson() {
        val exception =
            assertThrows(InvalidBackupException::class.java) {
                GymbroBackupCodec.decode("""{ "schemaVersion": """)
            }

        assertTrue(exception.message.orEmpty().contains("Invalid Gymbro backup JSON"))
    }

    @Test
    fun preservesSetCompletion() {
        val decoded =
            GymbroBackupCodec.decode(
                """
                {
                  "schemaVersion": 1,
                  "setResults": [
                    {
                      "id": 7,
                      "exerciseResultId": 6,
                      "setOrder": 2,
                      "reps": 10,
                      "weight": 40.0,
                      "durationSeconds": null,
                      "distance": null,
                      "notes": null,
                      "isCompleted": false
                    }
                  ]
                }
                """.trimIndent(),
            )

        assertEquals(false, decoded.setResults.single().isCompleted)
    }

    @Test
    fun rejectsUnsupportedSchemaVersion() {
        val exception =
            assertThrows(InvalidBackupException::class.java) {
                GymbroBackupCodec.decode("""{ "schemaVersion": 99 }""")
            }

        assertTrue(exception.message.orEmpty().contains("Unsupported Gymbro backup schema version"))
    }
}
