@file:Suppress("TooManyFunctions")

package com.brokenpip3.gymbro.backup

import androidx.room.withTransaction
import com.brokenpip3.gymbro.data.GymbroDatabase
import com.brokenpip3.gymbro.data.entities.ExerciseEntity
import com.brokenpip3.gymbro.data.entities.ExerciseResultEntity
import com.brokenpip3.gymbro.data.entities.ScheduleEntity
import com.brokenpip3.gymbro.data.entities.ScheduleExerciseEntity
import com.brokenpip3.gymbro.data.entities.SetResultEntity
import com.brokenpip3.gymbro.data.entities.WorkoutRunEntity
import com.brokenpip3.gymbro.domain.TrackingMode

enum class BackupScope(
    val wireValue: String,
) {
    EXERCISES("exercises"),
    EXERCISES_SCHEDULES("exercises_schedules"),
    FULL("full"),
    ;

    companion object {
        fun fromWireValueOrNull(value: String?): BackupScope? = entries.firstOrNull { it.wireValue == value }
    }
}

class GymbroBackupStore(
    private val dataSource: GymbroBackupDataSource,
) {
    suspend fun exportText(scope: BackupScope = BackupScope.FULL): String =
        GymbroBackupCodec.encode(
            dataSource
                .readBackupData()
                .toBackup(scope),
        )

    suspend fun importText(text: String) {
        val backup = GymbroBackupCodec.decode(text)
        val scope =
            BackupScope.fromWireValueOrNull(backup.scope)
                ?: throw InvalidBackupException("Unknown backup scope: ${backup.scope}")
        backup.validate(scope)
        when (scope) {
            BackupScope.FULL -> dataSource.replaceAll(backup.toBackupData())
            BackupScope.EXERCISES, BackupScope.EXERCISES_SCHEDULES -> dataSource.merge(backup.toPartialData())
        }
    }
}

data class GymbroBackupData(
    val exercises: List<ExerciseEntity>,
    val schedules: List<ScheduleEntity>,
    val scheduleExercises: List<ScheduleExerciseEntity>,
    val workoutRuns: List<WorkoutRunEntity>,
    val exerciseResults: List<ExerciseResultEntity>,
    val setResults: List<SetResultEntity>,
)

interface GymbroBackupDataSource {
    suspend fun readBackupData(): GymbroBackupData

    suspend fun replaceAll(data: GymbroBackupData)

    suspend fun merge(data: GymbroBackupData)
}

class RoomGymbroBackupDataSource(
    private val database: GymbroDatabase,
) : GymbroBackupDataSource {
    override suspend fun readBackupData(): GymbroBackupData {
        val exerciseDao = database.exerciseDao()
        val scheduleDao = database.scheduleDao()
        val workoutRunDao = database.workoutRunDao()
        return GymbroBackupData(
            exercises = exerciseDao.getAllExercises(),
            schedules = scheduleDao.getAllSchedules(),
            scheduleExercises = scheduleDao.getAllScheduleExercises(),
            workoutRuns = workoutRunDao.getAllWorkoutRuns(),
            exerciseResults = workoutRunDao.getAllExerciseResults(),
            setResults = workoutRunDao.getAllSetResults(),
        )
    }

    override suspend fun merge(data: GymbroBackupData) {
        database.withTransaction {
            database.mergePartial(data)
        }
    }

    override suspend fun replaceAll(data: GymbroBackupData) {
        database.withTransaction {
            val exerciseDao = database.exerciseDao()
            val scheduleDao = database.scheduleDao()
            val workoutRunDao = database.workoutRunDao()

            workoutRunDao.deleteAllSetResults()
            workoutRunDao.deleteAllExerciseResults()
            workoutRunDao.deleteAllWorkoutRuns()
            scheduleDao.deleteAllScheduleExercises()
            scheduleDao.deleteAllSchedules()
            exerciseDao.deleteAllExercises()

            exerciseDao.insertExercises(data.exercises)
            scheduleDao.insertSchedules(data.schedules)
            scheduleDao.insertScheduleExercises(data.scheduleExercises)
            workoutRunDao.insertWorkoutRuns(data.workoutRuns)
            workoutRunDao.insertExerciseResults(data.exerciseResults)
            workoutRunDao.insertSetResults(data.setResults)
        }
    }
}

private suspend fun GymbroDatabase.mergePartial(data: GymbroBackupData) {
    val exerciseDao = exerciseDao()
    val scheduleDao = scheduleDao()

    val existingExercises = exerciseDao.getAllExercises()
    val exerciseIdByFileId = mutableMapOf<Long, Long>()
    val exerciseIdByKey =
        existingExercises.associateBy { it.name.trim().lowercase() to it.trackingMode }

    data.exercises.forEach { exercise ->
        val duplicateId = exerciseIdByKey[exercise.name.trim().lowercase() to exercise.trackingMode]?.id
        exerciseIdByFileId[exercise.id] =
            duplicateId ?: exerciseDao.insertExercise(exercise.copy(id = 0))
    }

    val existingSchedules = scheduleDao.getAllSchedules()
    val scheduleIdByFileId = mutableMapOf<Long, Long>()
    val scheduleIdByKey = existingSchedules.associateBy { it.name.trim().lowercase() }
    val nextSortOrder = mutableMapOf<Long, Int>()
    existingSchedules.forEach { schedule ->
        nextSortOrder[schedule.id] =
            scheduleDao.getScheduleExercises(schedule.id).maxOfOrNull { it.sortOrder }?.plus(1) ?: 0
    }

    data.schedules.forEach { schedule ->
        val duplicateId = scheduleIdByKey[schedule.name.trim().lowercase()]?.id
        scheduleIdByFileId[schedule.id] = duplicateId ?: scheduleDao.insertSchedule(schedule.copy(id = 0))
    }

    data.scheduleExercises.forEach { row ->
        val scheduleId = scheduleIdByFileId.getValue(row.scheduleId)
        val isExistingSchedule = scheduleIdByKey.values.any { it.id == scheduleId }
        val sortOrder =
            if (isExistingSchedule) {
                val order = nextSortOrder.getValue(scheduleId)
                nextSortOrder[scheduleId] = order + 1
                order
            } else {
                row.sortOrder
            }
        scheduleDao.insertScheduleExercise(
            ScheduleExerciseEntity(
                id = 0,
                scheduleId = scheduleId,
                exerciseId = exerciseIdByFileId.getValue(row.exerciseId),
                sortOrder = sortOrder,
                targetSets = row.targetSets,
                targetReps = row.targetReps,
                targetWeight = row.targetWeight,
                targetDurationSeconds = row.targetDurationSeconds,
                targetDistance = row.targetDistance,
            ),
        )
    }
}

private fun GymbroBackupData.toBackup(scope: BackupScope = BackupScope.FULL): GymbroBackup =
    GymbroBackup(
        schemaVersion = GymbroBackupCodec.SUPPORTED_SCHEMA_VERSION,
        scope = scope.wireValue,
        exercises = exercises.map { it.toBackup() },
        schedules =
            if (scope == BackupScope.EXERCISES_SCHEDULES || scope == BackupScope.FULL) {
                schedules.map { it.toBackup() }
            } else {
                emptyList()
            },
        scheduleExercises =
            if (scope == BackupScope.EXERCISES_SCHEDULES || scope == BackupScope.FULL) {
                scheduleExercises.map { it.toBackup() }
            } else {
                emptyList()
            },
        workoutRuns = if (scope == BackupScope.FULL) workoutRuns.map { it.toBackup() } else emptyList(),
        exerciseResults =
            if (scope == BackupScope.FULL) exerciseResults.map { it.toBackup() } else emptyList(),
        setResults = if (scope == BackupScope.FULL) setResults.map { it.toBackup() } else emptyList(),
    )

private fun GymbroBackup.toPartialData(): GymbroBackupData =
    GymbroBackupData(
        exercises = exercises.map { it.toEntity() },
        schedules = schedules.map { it.toEntity() },
        scheduleExercises = scheduleExercises.map { it.toEntity() },
        workoutRuns = emptyList(),
        exerciseResults = emptyList(),
        setResults = emptyList(),
    )

private fun GymbroBackup.toBackupData(): GymbroBackupData =
    GymbroBackupData(
        exercises = exercises.map { it.toEntity() },
        schedules = schedules.map { it.toEntity() },
        scheduleExercises = scheduleExercises.map { it.toEntity() },
        workoutRuns = workoutRuns.map { it.toEntity() },
        exerciseResults = exerciseResults.map { it.toEntity() },
        setResults = setResults.map { it.toEntity() },
    )

private fun ExerciseEntity.toBackup(): BackupExercise =
    BackupExercise(
        id = id,
        name = name,
        notes = notes,
        trackingMode = trackingMode,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun BackupExercise.toEntity(): ExerciseEntity =
    ExerciseEntity(
        id = id,
        name = name,
        notes = notes,
        trackingMode = trackingMode,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun ScheduleEntity.toBackup(): BackupSchedule =
    BackupSchedule(
        id = id,
        name = name,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun BackupSchedule.toEntity(): ScheduleEntity =
    ScheduleEntity(
        id = id,
        name = name,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun ScheduleExerciseEntity.toBackup(): BackupScheduleExercise =
    BackupScheduleExercise(
        id = id,
        scheduleId = scheduleId,
        exerciseId = exerciseId,
        sortOrder = sortOrder,
        targetSets = targetSets,
        targetReps = targetReps,
        targetWeight = targetWeight,
        targetDurationSeconds = targetDurationSeconds,
        targetDistance = targetDistance,
    )

private fun BackupScheduleExercise.toEntity(): ScheduleExerciseEntity =
    ScheduleExerciseEntity(
        id = id,
        scheduleId = scheduleId,
        exerciseId = exerciseId,
        sortOrder = sortOrder,
        targetSets = targetSets,
        targetReps = targetReps,
        targetWeight = targetWeight,
        targetDurationSeconds = targetDurationSeconds,
        targetDistance = targetDistance,
    )

private fun WorkoutRunEntity.toBackup(): BackupWorkoutRun =
    BackupWorkoutRun(
        id = id,
        scheduleId = scheduleId,
        scheduleNameSnapshot = scheduleNameSnapshot,
        startedAt = startedAt,
        finishedAt = finishedAt,
        notes = notes,
    )

private fun BackupWorkoutRun.toEntity(): WorkoutRunEntity =
    WorkoutRunEntity(
        id = id,
        scheduleId = scheduleId,
        scheduleNameSnapshot = scheduleNameSnapshot,
        startedAt = startedAt,
        finishedAt = finishedAt,
        notes = notes,
    )

private fun ExerciseResultEntity.toBackup(): BackupExerciseResult =
    BackupExerciseResult(
        id = id,
        workoutRunId = workoutRunId,
        exerciseId = exerciseId,
        exerciseNameSnapshot = exerciseNameSnapshot,
        trackingModeSnapshot = trackingModeSnapshot,
        sortOrder = sortOrder,
        notes = notes,
    )

private fun BackupExerciseResult.toEntity(): ExerciseResultEntity =
    ExerciseResultEntity(
        id = id,
        workoutRunId = workoutRunId,
        exerciseId = exerciseId,
        exerciseNameSnapshot = exerciseNameSnapshot,
        trackingModeSnapshot = trackingModeSnapshot,
        sortOrder = sortOrder,
        notes = notes,
    )

private fun SetResultEntity.toBackup(): BackupSetResult =
    BackupSetResult(
        id = id,
        exerciseResultId = exerciseResultId,
        setOrder = setOrder,
        reps = reps,
        weight = weight,
        durationSeconds = durationSeconds,
        distance = distance,
        notes = notes,
        isCompleted = isCompleted,
    )

private fun BackupSetResult.toEntity(): SetResultEntity =
    SetResultEntity(
        id = id,
        exerciseResultId = exerciseResultId,
        setOrder = setOrder,
        reps = reps,
        weight = weight,
        durationSeconds = durationSeconds,
        distance = distance,
        notes = notes,
        isCompleted = isCompleted,
    )

private fun GymbroBackup.validate(scope: BackupScope = BackupScope.FULL) {
    validateScopeContents(scope)
    val exerciseIds = exercises.requireUniqueIds(tableName = "exercises") { it.id }
    val scheduleIds = schedules.requireUniqueIds(tableName = "schedules") { it.id }
    scheduleExercises.requireUniqueIds(tableName = "schedule_exercises") { it.id }
    val workoutRunIds = workoutRuns.requireUniqueIds(tableName = "workout_runs") { it.id }
    val exerciseResultIds = exerciseResults.requireUniqueIds(tableName = "exercise_results") { it.id }
    setResults.requireUniqueIds(tableName = "set_results") { it.id }

    exercises.requirePositiveIds(tableName = "exercises") { it.id }
    schedules.requirePositiveIds(tableName = "schedules") { it.id }
    scheduleExercises.requirePositiveIds(tableName = "schedule_exercises") { it.id }
    workoutRuns.requirePositiveIds(tableName = "workout_runs") { it.id }
    exerciseResults.requirePositiveIds(tableName = "exercise_results") { it.id }
    setResults.requirePositiveIds(tableName = "set_results") { it.id }

    exercises.forEach { exercise ->
        exercise.name.requireNotBlank(fieldName = "exercises.name")
        exercise.trackingMode.requireKnownTrackingMode()
    }
    schedules.forEach { schedule ->
        schedule.name.requireNotBlank(fieldName = "schedules.name")
    }
    exerciseResults.forEach { exerciseResult ->
        exerciseResult.exerciseNameSnapshot.requireNotBlank(fieldName = "exerciseResults.exerciseNameSnapshot")
        exerciseResult.trackingModeSnapshot.requireKnownTrackingMode()
    }

    scheduleExercises.requireNonNegativeOrder(tableName = "schedule_exercises") { it.sortOrder }
    exerciseResults.requireNonNegativeOrder(tableName = "exercise_results") { it.sortOrder }
    setResults.requireNonNegativeOrder(tableName = "set_results") { it.setOrder }
    scheduleExercises.requireUniqueOrder(
        tableName = "schedule_exercises",
        parentId = { it.scheduleId },
        order = { it.sortOrder },
    )
    exerciseResults.requireUniqueOrder(
        tableName = "exercise_results",
        parentId = { it.workoutRunId },
        order = { it.sortOrder },
    )

    scheduleExercises.forEach { scheduleExercise ->
        scheduleIds.requireContains(
            value = scheduleExercise.scheduleId,
            fieldName = "scheduleExercises.scheduleId",
        )
        exerciseIds.requireContains(
            value = scheduleExercise.exerciseId,
            fieldName = "scheduleExercises.exerciseId",
        )
    }

    exerciseResults.forEach { exerciseResult ->
        workoutRunIds.requireContains(
            value = exerciseResult.workoutRunId,
            fieldName = "exerciseResults.workoutRunId",
        )
    }

    setResults.forEach { setResult ->
        exerciseResultIds.requireContains(
            value = setResult.exerciseResultId,
            fieldName = "setResults.exerciseResultId",
        )
    }

    setResults
        .groupBy { it.exerciseResultId to it.setOrder }
        .filterValues { it.size > 1 }
        .keys
        .firstOrNull()
        ?.let { key ->
            throw InvalidBackupException(
                "Duplicate set_results exerciseResultId/setOrder: ${key.first}/${key.second}",
            )
        }
}

private fun GymbroBackup.validateScopeContents(scope: BackupScope) {
    when (scope) {
        BackupScope.FULL -> Unit
        BackupScope.EXERCISES -> {
            requireNotEmpty(exercises, "Exercises backup contains no exercises")
            requireEmpty(
                schedules + scheduleExercises + workoutRuns + exerciseResults + setResults,
                "Exercises backup must not contain schedules or workout history",
            )
        }
        BackupScope.EXERCISES_SCHEDULES -> {
            requireNotEmpty(exercises, "Schedules backup contains no exercises")
            requireNotEmpty(schedules, "Schedules backup contains no schedules")
            requireNotEmpty(scheduleExercises, "Schedules backup contains no schedule exercises")
            requireEmpty(
                workoutRuns + exerciseResults + setResults,
                "Schedules backup must not contain workout history",
            )
        }
    }
}

private fun requireEmpty(
    items: List<Any>,
    message: String,
) {
    if (items.isNotEmpty()) {
        throw InvalidBackupException(message)
    }
}

private fun requireNotEmpty(
    items: List<Any>,
    message: String,
) {
    if (items.isEmpty()) {
        throw InvalidBackupException(message)
    }
}

private fun <T> List<T>.requireUniqueIds(
    tableName: String,
    id: (T) -> Long,
): Set<Long> {
    val ids = map(id)
    ids
        .groupingBy { it }
        .eachCount()
        .filterValues { it > 1 }
        .keys
        .firstOrNull()
        ?.let { duplicateId ->
            throw InvalidBackupException("Duplicate $tableName id: $duplicateId")
        }
    return ids.toSet()
}

private fun <T> List<T>.requirePositiveIds(
    tableName: String,
    id: (T) -> Long,
) {
    firstOrNull { item -> id(item) <= 0L }?.let { item ->
        throw InvalidBackupException("$tableName id must be positive: ${id(item)}")
    }
}

private fun <T> List<T>.requireNonNegativeOrder(
    tableName: String,
    order: (T) -> Int,
) {
    firstOrNull { item -> order(item) < 0 }?.let { item ->
        throw InvalidBackupException("$tableName order must not be negative: ${order(item)}")
    }
}

private fun <T> List<T>.requireUniqueOrder(
    tableName: String,
    parentId: (T) -> Long,
    order: (T) -> Int,
) {
    groupBy { item -> parentId(item) to order(item) }
        .filterValues { items -> items.size > 1 }
        .keys
        .firstOrNull()
        ?.let { key ->
            throw InvalidBackupException(
                "Duplicate $tableName parent/order: ${key.first}/${key.second}",
            )
        }
}

private fun String.requireNotBlank(fieldName: String) {
    if (isBlank()) {
        throw InvalidBackupException("$fieldName must not be blank")
    }
}

private fun String.requireKnownTrackingMode() {
    if (TrackingMode.entries.none { trackingMode -> trackingMode.databaseValue == this }) {
        throw InvalidBackupException("Unknown tracking mode: $this")
    }
}

private fun Set<Long>.requireContains(
    value: Long,
    fieldName: String,
) {
    if (value !in this) {
        throw InvalidBackupException("$fieldName $value was not found in backup")
    }
}
