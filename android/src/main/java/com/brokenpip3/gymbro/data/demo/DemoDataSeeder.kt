package com.brokenpip3.gymbro.data.demo

import androidx.room.withTransaction
import com.brokenpip3.gymbro.data.GymbroDatabase
import com.brokenpip3.gymbro.data.entities.ExerciseEntity
import com.brokenpip3.gymbro.data.entities.ExerciseResultEntity
import com.brokenpip3.gymbro.data.entities.ScheduleEntity
import com.brokenpip3.gymbro.data.entities.ScheduleExerciseEntity
import com.brokenpip3.gymbro.data.entities.SetResultEntity
import com.brokenpip3.gymbro.data.entities.WorkoutRunEntity
import com.brokenpip3.gymbro.domain.TrackingMode

data class DemoDataSeedResult(
    val inserted: Boolean,
    val message: String,
)

class DemoDataSeeder(
    private val database: GymbroDatabase,
) {
    suspend fun seed(nowMillis: Long = System.currentTimeMillis()): DemoDataSeedResult =
        database.withTransaction {
            if (database.scheduleDao().getAllSchedules().any { it.name == LEG_DAY }) {
                return@withTransaction DemoDataSeedResult(
                    inserted = false,
                    message = "Demo data is already loaded",
                )
            }

            val exerciseIds =
                EXERCISES.associate { exercise ->
                    exercise.key to
                        database.exerciseDao().insertExercise(
                            ExerciseEntity(
                                name = exercise.name,
                                notes = exercise.notes,
                                trackingMode = exercise.mode.databaseValue,
                                createdAt = nowMillis,
                                updatedAt = nowMillis,
                            ),
                        )
                }
            val scheduleIds =
                SCHEDULES.associate { schedule ->
                    schedule.name to
                        database.scheduleDao().insertSchedule(
                            ScheduleEntity(
                                name = schedule.name,
                                notes = schedule.notes,
                                createdAt = nowMillis,
                                updatedAt = nowMillis,
                            ),
                        )
                }

            SCHEDULES.forEach { schedule ->
                database.scheduleDao().insertScheduleExercises(
                    schedule.exerciseKeys.mapIndexed { index, exerciseKey ->
                        val exercise = EXERCISES.first { it.key == exerciseKey }
                        val target = exercise.sampleSets.first()
                        ScheduleExerciseEntity(
                            scheduleId = scheduleIds.getValue(schedule.name),
                            exerciseId = exerciseIds.getValue(exerciseKey),
                            sortOrder = index,
                            targetSets = exercise.sampleSets.size,
                            targetReps = target.reps,
                            targetWeight = target.weight,
                            targetDurationSeconds = target.durationSeconds,
                            targetDistance = target.distance,
                        )
                    },
                )
            }

            HISTORY.forEach { history ->
                insertRun(
                    schedule = SCHEDULES.first { it.name == history.scheduleName },
                    scheduleId = scheduleIds.getValue(history.scheduleName),
                    exerciseIds = exerciseIds,
                    startedAt = nowMillis - history.daysAgo * DAY_MILLIS,
                    finishedAt = nowMillis - history.daysAgo * DAY_MILLIS + history.durationMillis,
                    notes = "Demo workout for testing history and charts",
                    setsByExercise = history.setsByExercise,
                )
            }

            insertRun(
                schedule = SCHEDULES.first { it.name == LEG_DAY },
                scheduleId = scheduleIds.getValue(LEG_DAY),
                exerciseIds = exerciseIds,
                startedAt = nowMillis - 12 * MINUTE_MILLIS,
                finishedAt = null,
                notes = "Unfinished demo workout - resume this session",
                setsByExercise =
                    mapOf(
                        "back_squat" to
                            listOf(
                                Metric(reps = 8, weight = 80.0),
                                Metric(reps = 8, weight = 80.0),
                                Metric(reps = 8, weight = 80.0, completed = false),
                            ),
                        "romanian_deadlift" to listOf(Metric(completed = false)),
                        "walking_lunge" to listOf(Metric(completed = false)),
                    ),
            )

            DemoDataSeedResult(
                inserted = true,
                message = "Demo data loaded: exercises, schedules, history and a resumable workout",
            )
        }

    private suspend fun insertRun(
        schedule: DemoSchedule,
        scheduleId: Long,
        exerciseIds: Map<String, Long>,
        startedAt: Long,
        finishedAt: Long?,
        notes: String?,
        setsByExercise: Map<String, List<Metric>>,
    ) {
        val runId =
            database.workoutRunDao().insertWorkoutRun(
                WorkoutRunEntity(
                    scheduleId = scheduleId,
                    scheduleNameSnapshot = schedule.name,
                    startedAt = startedAt,
                    finishedAt = finishedAt,
                    notes = notes,
                ),
            )
        schedule.exerciseKeys.forEachIndexed { sortOrder, exerciseKey ->
            val exercise = EXERCISES.first { it.key == exerciseKey }
            val exerciseResultId =
                database.workoutRunDao().insertExerciseResult(
                    ExerciseResultEntity(
                        workoutRunId = runId,
                        exerciseId = exerciseIds.getValue(exerciseKey),
                        exerciseNameSnapshot = exercise.name,
                        trackingModeSnapshot = exercise.mode.databaseValue,
                        sortOrder = sortOrder,
                        notes = if (finishedAt == null) null else exercise.notes,
                    ),
                )
            val sets = setsByExercise[exerciseKey].orEmpty()
            database.workoutRunDao().insertSetResults(
                sets.mapIndexed { setOrder, metric ->
                    SetResultEntity(
                        exerciseResultId = exerciseResultId,
                        setOrder = setOrder,
                        reps = metric.reps,
                        weight = metric.weight,
                        durationSeconds = metric.durationSeconds,
                        distance = metric.distance,
                        notes = metric.notes,
                        isCompleted = metric.completed,
                    )
                },
            )
        }
    }

    private data class DemoExercise(
        val key: String,
        val name: String,
        val mode: TrackingMode,
        val notes: String,
        val sampleSets: List<Metric>,
    )

    private data class DemoSchedule(
        val name: String,
        val notes: String,
        val exerciseKeys: List<String>,
    )

    private data class DemoHistory(
        val scheduleName: String,
        val daysAgo: Long,
        val durationMillis: Long,
        val setsByExercise: Map<String, List<Metric>>,
    )

    private data class Metric(
        val reps: Int? = null,
        val weight: Double? = null,
        val durationSeconds: Long? = null,
        val distance: Double? = null,
        val notes: String? = null,
        val completed: Boolean = true,
    )

    private companion object {
        const val LEG_DAY = "Demo - Leg Day"
        const val UPPER_BODY = "Demo - Upper Body"
        const val CONDITIONING = "Demo - Conditioning"
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
        const val MINUTE_MILLIS = 60 * 1000L

        val EXERCISES =
            listOf(
                DemoExercise(
                    key = "back_squat",
                    name = "Back Squat",
                    mode = TrackingMode.Strength,
                    notes = "Keep the brace steady and track depth.",
                    sampleSets = strengthSets(reps = 8, weight = 80.0),
                ),
                DemoExercise(
                    key = "romanian_deadlift",
                    name = "Romanian Deadlift",
                    mode = TrackingMode.Strength,
                    notes = "Move slowly and keep the bar close.",
                    sampleSets = strengthSets(reps = 10, weight = 60.0),
                ),
                DemoExercise(
                    key = "walking_lunge",
                    name = "Walking Lunge",
                    mode = TrackingMode.Bodyweight,
                    notes = "Keep each step controlled.",
                    sampleSets = bodyweightSets(reps = 12),
                ),
                DemoExercise(
                    key = "bench_press",
                    name = "Bench Press",
                    mode = TrackingMode.Strength,
                    notes = "Pause briefly at the chest.",
                    sampleSets = strengthSets(reps = 8, weight = 55.0),
                ),
                DemoExercise(
                    key = "pull_up",
                    name = "Pull Up",
                    mode = TrackingMode.Bodyweight,
                    notes = "Start each rep from a controlled hang.",
                    sampleSets = bodyweightSets(reps = 8),
                ),
                DemoExercise(
                    key = "shoulder_press",
                    name = "Shoulder Press",
                    mode = TrackingMode.Strength,
                    notes = "Keep the ribs down throughout the press.",
                    sampleSets = strengthSets(reps = 10, weight = 30.0),
                ),
                DemoExercise(
                    key = "running",
                    name = "Running",
                    mode = TrackingMode.Timed,
                    notes = "Build pace gradually.",
                    sampleSets = listOf(Metric(durationSeconds = 570, distance = 1.0)),
                ),
                DemoExercise(
                    key = "plank",
                    name = "Plank",
                    mode = TrackingMode.Timed,
                    notes = "Keep a straight line from shoulders to heels.",
                    sampleSets = listOf(Metric(durationSeconds = 60)),
                ),
            )

        val SCHEDULES =
            listOf(
                DemoSchedule(
                    name = LEG_DAY,
                    notes = "Sample lower-body routine with progressive loads.",
                    exerciseKeys = listOf("back_squat", "romanian_deadlift", "walking_lunge"),
                ),
                DemoSchedule(
                    name = UPPER_BODY,
                    notes = "Sample upper-body routine for trend testing.",
                    exerciseKeys = listOf("bench_press", "pull_up", "shoulder_press"),
                ),
                DemoSchedule(
                    name = CONDITIONING,
                    notes = "Sample timed routine for duration and distance charts.",
                    exerciseKeys = listOf("running", "plank"),
                ),
            )

        val HISTORY =
            listOf(
                DemoHistory(
                    scheduleName = LEG_DAY,
                    daysAgo = 35,
                    durationMillis = 42 * MINUTE_MILLIS,
                    setsByExercise =
                        mapOf(
                            "back_squat" to strengthSets(reps = 8, weight = 70.0),
                            "romanian_deadlift" to strengthSets(reps = 10, weight = 50.0),
                            "walking_lunge" to bodyweightSets(reps = 10),
                        ),
                ),
                DemoHistory(
                    scheduleName = LEG_DAY,
                    daysAgo = 21,
                    durationMillis = 45 * MINUTE_MILLIS,
                    setsByExercise =
                        mapOf(
                            "back_squat" to strengthSets(reps = 8, weight = 75.0),
                            "romanian_deadlift" to strengthSets(reps = 10, weight = 55.0),
                            "walking_lunge" to bodyweightSets(reps = 11),
                        ),
                ),
                DemoHistory(
                    scheduleName = LEG_DAY,
                    daysAgo = 7,
                    durationMillis = 46 * MINUTE_MILLIS,
                    setsByExercise =
                        mapOf(
                            "back_squat" to strengthSets(reps = 8, weight = 80.0),
                            "romanian_deadlift" to strengthSets(reps = 10, weight = 60.0),
                            "walking_lunge" to bodyweightSets(reps = 12),
                        ),
                ),
                DemoHistory(
                    scheduleName = UPPER_BODY,
                    daysAgo = 32,
                    durationMillis = 38 * MINUTE_MILLIS,
                    setsByExercise =
                        mapOf(
                            "bench_press" to strengthSets(reps = 8, weight = 50.0),
                            "pull_up" to bodyweightSets(reps = 6),
                            "shoulder_press" to strengthSets(reps = 10, weight = 25.0),
                        ),
                ),
                DemoHistory(
                    scheduleName = UPPER_BODY,
                    daysAgo = 18,
                    durationMillis = 40 * MINUTE_MILLIS,
                    setsByExercise =
                        mapOf(
                            "bench_press" to strengthSets(reps = 8, weight = 52.5),
                            "pull_up" to bodyweightSets(reps = 7),
                            "shoulder_press" to strengthSets(reps = 10, weight = 27.5),
                        ),
                ),
                DemoHistory(
                    scheduleName = UPPER_BODY,
                    daysAgo = 4,
                    durationMillis = 41 * MINUTE_MILLIS,
                    setsByExercise =
                        mapOf(
                            "bench_press" to strengthSets(reps = 8, weight = 55.0),
                            "pull_up" to bodyweightSets(reps = 8),
                            "shoulder_press" to strengthSets(reps = 10, weight = 30.0),
                        ),
                ),
                DemoHistory(
                    scheduleName = CONDITIONING,
                    daysAgo = 42,
                    durationMillis = 20 * MINUTE_MILLIS,
                    setsByExercise =
                        mapOf(
                            "running" to listOf(Metric(durationSeconds = 660, distance = 1.0)),
                            "plank" to listOf(Metric(durationSeconds = 40)),
                        ),
                ),
                DemoHistory(
                    scheduleName = CONDITIONING,
                    daysAgo = 28,
                    durationMillis = 21 * MINUTE_MILLIS,
                    setsByExercise =
                        mapOf(
                            "running" to listOf(Metric(durationSeconds = 630, distance = 1.0)),
                            "plank" to listOf(Metric(durationSeconds = 45)),
                        ),
                ),
                DemoHistory(
                    scheduleName = CONDITIONING,
                    daysAgo = 14,
                    durationMillis = 22 * MINUTE_MILLIS,
                    setsByExercise =
                        mapOf(
                            "running" to listOf(Metric(durationSeconds = 600, distance = 1.0)),
                            "plank" to listOf(Metric(durationSeconds = 45)),
                        ),
                ),
            )

        fun strengthSets(
            reps: Int,
            weight: Double,
        ): List<Metric> = List(3) { Metric(reps = reps, weight = weight) }

        fun bodyweightSets(reps: Int): List<Metric> = List(3) { Metric(reps = reps) }
    }
}
