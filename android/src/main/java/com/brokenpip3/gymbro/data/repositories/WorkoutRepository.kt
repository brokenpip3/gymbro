package com.brokenpip3.gymbro.data.repositories

import androidx.room.withTransaction
import com.brokenpip3.gymbro.data.GymbroDatabase
import com.brokenpip3.gymbro.data.dao.ExerciseDao
import com.brokenpip3.gymbro.data.dao.ScheduleDao
import com.brokenpip3.gymbro.data.dao.WorkoutRunDao
import com.brokenpip3.gymbro.data.entities.ExerciseResultEntity
import com.brokenpip3.gymbro.data.entities.ScheduleExerciseEntity
import com.brokenpip3.gymbro.data.entities.SetResultEntity
import com.brokenpip3.gymbro.data.entities.WorkoutRunEntity
import com.brokenpip3.gymbro.domain.TrackingMode
import com.brokenpip3.gymbro.ui.screens.schedules.WorkoutStarter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

@Suppress("TooManyFunctions")
class WorkoutRepository internal constructor(
    private val workoutRunDao: WorkoutRunDao,
    private val scheduleDao: ScheduleDao,
    private val exerciseDao: ExerciseDao,
    private val transactionRunner: TransactionRunner = ImmediateTransactionRunner,
) : WorkoutStarter {
    constructor(database: GymbroDatabase) : this(
        workoutRunDao = database.workoutRunDao(),
        scheduleDao = database.scheduleDao(),
        exerciseDao = database.exerciseDao(),
        transactionRunner = RoomTransactionRunner(database),
    )

    override fun observeActiveWorkoutRun(): Flow<WorkoutRunEntity?> = workoutRunDao.observeActiveWorkoutRun()

    suspend fun hasActiveWorkoutRun(): Boolean = observeActiveWorkoutRun().first() != null

    fun observeAvailableExercises() = exerciseDao.observeExercises()

    override suspend fun startWorkout(
        scheduleId: Long,
        nowMillis: Long,
    ): Long =
        transactionRunner.run {
            workoutRunDao.getActiveWorkoutRun()?.let { return@run it.id }

            val schedule =
                scheduleDao.getSchedule(scheduleId)
                    ?: throw IllegalArgumentException("Cannot start workout: schedule $scheduleId was not found")

            val scheduleExercises = scheduleDao.getScheduleExercises(scheduleId)
            require(scheduleExercises.isNotEmpty()) {
                "Cannot start workout: schedule $scheduleId has no assigned exercises"
            }

            val previousExerciseResults =
                workoutRunDao
                    .getLatestCompletedWorkoutRunForSchedule(scheduleId)
                    ?.let { workoutRunDao.getExerciseResults(it.id) }
                    .orEmpty()

            val workoutRunId =
                workoutRunDao.insertWorkoutRun(
                    WorkoutRunEntity(
                        scheduleId = scheduleId,
                        scheduleNameSnapshot = schedule.name,
                        startedAt = nowMillis,
                        finishedAt = null,
                        notes = null,
                    ),
                )

            scheduleExercises.forEach { scheduleExercise ->
                val exercise = exerciseDao.getExercise(scheduleExercise.exerciseId)
                val previousSnapshot =
                    previousExerciseResults.firstOrNull { previous ->
                        previous.sortOrder == scheduleExercise.sortOrder &&
                            previous.exerciseId == scheduleExercise.exerciseId
                    }
                val exerciseResult =
                    ExerciseResultEntity(
                        workoutRunId = workoutRunId,
                        exerciseId = exercise?.id ?: scheduleExercise.exerciseId,
                        exerciseNameSnapshot =
                            exercise?.name
                                ?: previousSnapshot?.exerciseNameSnapshot
                                ?: "Deleted exercise",
                        trackingModeSnapshot =
                            exercise?.trackingMode
                                ?: previousSnapshot?.trackingModeSnapshot
                                ?: TrackingMode.Bodyweight.databaseValue,
                        sortOrder = scheduleExercise.sortOrder,
                        notes = null,
                    )
                val exerciseResultId = workoutRunDao.insertExerciseResult(exerciseResult)
                val previousExerciseResult = previousExerciseResults.findPreviousMatch(exerciseResult)
                insertInitialSetRows(
                    exerciseResultId = exerciseResultId,
                    exerciseResult = exerciseResult,
                    scheduleExercise = scheduleExercise,
                    previousExerciseResult = previousExerciseResult,
                )
            }

            workoutRunId
        }

    suspend fun addExerciseToActiveWorkout(
        workoutRunId: Long,
        exerciseId: Long,
    ): Long =
        transactionRunner.run {
            val workoutRun =
                workoutRunDao.getWorkoutRun(workoutRunId)
                    ?: throw IllegalArgumentException("Cannot add exercise: active workout $workoutRunId was not found")
            require(workoutRun.finishedAt == null) {
                "Cannot add exercise: workout $workoutRunId is already finished"
            }
            val exercise =
                exerciseDao.getExercise(exerciseId)
                    ?: throw IllegalArgumentException("Cannot add exercise: exercise $exerciseId was not found")
            val nextSortOrder =
                workoutRunDao
                    .getExerciseResults(workoutRunId)
                    .maxOfOrNull { exerciseResult -> exerciseResult.sortOrder }
                    ?.plus(1)
                    ?: 0

            workoutRunDao.insertExerciseResult(
                ExerciseResultEntity(
                    workoutRunId = workoutRunId,
                    exerciseId = exercise.id,
                    exerciseNameSnapshot = exercise.name,
                    trackingModeSnapshot = exercise.trackingMode,
                    sortOrder = nextSortOrder,
                    notes = null,
                ),
            )
        }

    suspend fun finishWorkout(
        workoutRunId: Long,
        nowMillis: Long,
    ) {
        transactionRunner.run {
            val workoutRun = workoutRunDao.getWorkoutRun(workoutRunId) ?: return@run
            if (workoutRun.finishedAt != null) return@run

            workoutRunDao.updateWorkoutRun(workoutRun.copy(finishedAt = nowMillis))
        }
    }

    suspend fun updateWorkoutNotes(
        workoutRunId: Long,
        notes: String?,
    ) {
        workoutRunDao.updateWorkoutRunNotes(
            workoutRunId = workoutRunId,
            notes = notes,
        )
    }

    suspend fun discardWorkout(workoutRunId: Long) {
        transactionRunner.run {
            val workoutRun = workoutRunDao.getWorkoutRun(workoutRunId) ?: return@run
            require(workoutRun.finishedAt == null) {
                "Cannot discard workout: workout $workoutRunId is already finished"
            }

            workoutRunDao.getExerciseResults(workoutRunId).forEach { exerciseResult ->
                workoutRunDao.deleteSetResultsForExercise(exerciseResult.id)
                workoutRunDao.deleteExerciseResult(exerciseResult.id)
            }
            check(workoutRunDao.deleteWorkoutRun(workoutRunId) == 1) {
                "Unable to discard workout $workoutRunId"
            }
        }
    }

    suspend fun addSet(
        exerciseResultId: Long,
        reps: Int?,
        weight: Double?,
        durationSeconds: Long?,
        distance: Double?,
        notes: String?,
    ): Long =
        transactionRunner.run {
            val nextSetOrder =
                workoutRunDao
                    .getSetResults(exerciseResultId)
                    .maxOfOrNull { it.setOrder }
                    ?.plus(1)
                    ?: 0

            workoutRunDao.insertSetResult(
                SetResultEntity(
                    exerciseResultId = exerciseResultId,
                    setOrder = nextSetOrder,
                    reps = reps,
                    weight = weight,
                    durationSeconds = durationSeconds,
                    distance = distance,
                    notes = notes,
                ),
            )
        }

    suspend fun addEmptySet(exerciseResultId: Long): Long =
        transactionRunner.run {
            insertEmptySet(exerciseResultId)
        }

    suspend fun addSetFromPrevious(exerciseResultId: Long): Long =
        transactionRunner.run {
            val previousSets = workoutRunDao.getSetResults(exerciseResultId)
            val latestSet = previousSets.maxByOrNull { it.setOrder }

            if (latestSet == null) {
                insertEmptySet(exerciseResultId)
            } else {
                workoutRunDao.insertSetResult(
                    SetResultEntity(
                        exerciseResultId = exerciseResultId,
                        setOrder = latestSet.setOrder + 1,
                        reps = latestSet.reps,
                        weight = latestSet.weight,
                        durationSeconds = latestSet.durationSeconds,
                        distance = latestSet.distance,
                        notes = null,
                        isCompleted = false,
                    ),
                )
            }
        }

    suspend fun updateSetMetrics(
        setId: Long,
        reps: Int?,
        weight: Double?,
        durationSeconds: Long?,
        distance: Double?,
    ) {
        workoutRunDao.updateSetMetrics(
            setId = setId,
            reps = reps,
            weight = weight,
            durationSeconds = durationSeconds,
            distance = distance,
        )
    }

    suspend fun updateSetCompletion(
        setId: Long,
        isCompleted: Boolean,
    ) {
        workoutRunDao.updateSetCompletion(setId = setId, isCompleted = isCompleted)
    }

    suspend fun updateSetNotes(
        setId: Long,
        notes: String?,
    ) {
        workoutRunDao.updateSetNotes(
            setId = setId,
            notes = notes,
        )
    }

    suspend fun updateExerciseResultNotes(
        exerciseResultId: Long,
        notes: String?,
    ) {
        workoutRunDao.updateExerciseResultNotes(
            exerciseResultId = exerciseResultId,
            notes = notes,
        )
    }

    suspend fun updateSet(setResult: SetResultEntity) {
        workoutRunDao.updateSetResult(setResult)
    }

    suspend fun deleteSet(setResultId: Long) {
        workoutRunDao.deleteSetResult(setResultId)
    }

    suspend fun deleteExerciseFromWorkout(exerciseResultId: Long) {
        transactionRunner.run {
            workoutRunDao.deleteSetResultsForExercise(exerciseResultId)
            workoutRunDao.deleteExerciseResult(exerciseResultId)
        }
    }

    fun observeCompletedWorkoutRuns(): Flow<List<WorkoutRunEntity>> = workoutRunDao.observeCompletedWorkoutRuns()

    fun observeExerciseResultChanges(): Flow<Int> = workoutRunDao.observeExerciseResultChanges()

    fun observeSetResultChanges(): Flow<Int> = workoutRunDao.observeSetResultChanges()

    @Suppress("ktlint:standard:function-expression-body")
    suspend fun getExerciseResults(workoutRunId: Long): List<ExerciseResultEntity> {
        return workoutRunDao.getExerciseResults(workoutRunId)
    }

    @Suppress("ktlint:standard:function-expression-body")
    suspend fun getExerciseResult(id: Long): ExerciseResultEntity? {
        return workoutRunDao.getExerciseResult(id)
    }

    @Suppress("ktlint:standard:function-expression-body")
    suspend fun getSetResults(exerciseResultId: Long): List<SetResultEntity> {
        return workoutRunDao.getSetResults(exerciseResultId)
    }

    private suspend fun nextSetOrder(exerciseResultId: Long): Int =
        workoutRunDao
            .getSetResults(exerciseResultId)
            .maxOfOrNull { it.setOrder }
            ?.plus(1)
            ?: 0

    private suspend fun insertEmptySet(exerciseResultId: Long): Long =
        workoutRunDao.insertSetResult(
            SetResultEntity(
                exerciseResultId = exerciseResultId,
                setOrder = nextSetOrder(exerciseResultId),
                reps = null,
                weight = null,
                durationSeconds = null,
                distance = null,
                notes = null,
                isCompleted = false,
            ),
        )

    private suspend fun insertInitialSetRows(
        exerciseResultId: Long,
        exerciseResult: ExerciseResultEntity,
        scheduleExercise: ScheduleExerciseEntity,
        previousExerciseResult: ExerciseResultEntity?,
    ) {
        val setResults =
            previousExerciseResult
                ?.let { previous -> workoutRunDao.getSetResults(previous.id).toStarterSetResults(exerciseResultId) }
                ?: scheduleExercise.toPlannedSetResults(
                    exerciseResultId = exerciseResultId,
                    trackingMode = exerciseResult.trackingModeSnapshot.toTrackingMode(),
                )

        setResults.forEach { setResult ->
            workoutRunDao.insertSetResult(setResult)
        }
    }
}

private fun List<SetResultEntity>.toStarterSetResults(exerciseResultId: Long): List<SetResultEntity> =
    mapIndexed { setOrder, previousSet ->
        SetResultEntity(
            exerciseResultId = exerciseResultId,
            setOrder = setOrder,
            reps = previousSet.reps,
            weight = previousSet.weight,
            durationSeconds = previousSet.durationSeconds,
            distance = previousSet.distance,
            notes = null,
            isCompleted = false,
        )
    }

private fun List<ExerciseResultEntity>.findPreviousMatch(exerciseResult: ExerciseResultEntity): ExerciseResultEntity? =
    firstOrNull { previous ->
        previous.sortOrder == exerciseResult.sortOrder &&
            previous.matchesExerciseIdentity(exerciseResult)
    } ?: firstOrNull { previous ->
        previous.matchesExerciseIdentity(exerciseResult)
    }

private fun ExerciseResultEntity.matchesExerciseIdentity(other: ExerciseResultEntity): Boolean =
    if (exerciseId != null && other.exerciseId != null) {
        exerciseId == other.exerciseId && trackingModeSnapshot == other.trackingModeSnapshot
    } else {
        exerciseNameSnapshot == other.exerciseNameSnapshot &&
            trackingModeSnapshot == other.trackingModeSnapshot
    }

private fun ScheduleExerciseEntity.toPlannedSetResults(
    exerciseResultId: Long,
    trackingMode: TrackingMode,
): List<SetResultEntity> {
    val sets = targetSets?.takeIf { targetSets -> targetSets > 0 } ?: return emptyList()

    return List(sets) { index ->
        SetResultEntity(
            exerciseResultId = exerciseResultId,
            setOrder = index,
            reps =
                when (trackingMode) {
                    TrackingMode.Strength,
                    TrackingMode.Bodyweight,
                    -> targetReps
                    TrackingMode.Timed -> null
                },
            weight = targetWeight.takeIf { trackingMode == TrackingMode.Strength },
            durationSeconds = targetDurationSeconds.takeIf { trackingMode == TrackingMode.Timed },
            distance = targetDistance.takeIf { trackingMode == TrackingMode.Timed },
            notes = null,
            isCompleted = false,
        )
    }
}

private fun String.toTrackingMode(): TrackingMode =
    TrackingMode.entries.firstOrNull { trackingMode ->
        trackingMode.databaseValue == this
    } ?: TrackingMode.Bodyweight

internal interface TransactionRunner {
    suspend fun <T> run(block: suspend () -> T): T
}

private class RoomTransactionRunner(
    private val database: GymbroDatabase,
) : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = database.withTransaction(block)
}

private object ImmediateTransactionRunner : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = block()
}
