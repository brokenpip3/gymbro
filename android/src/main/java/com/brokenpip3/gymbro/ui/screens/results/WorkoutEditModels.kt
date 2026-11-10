package com.brokenpip3.gymbro.ui.screens.results

import com.brokenpip3.gymbro.data.repositories.CompletedSetEdit
import com.brokenpip3.gymbro.data.repositories.CompletedWorkoutEdit
import com.brokenpip3.gymbro.domain.TrackingMode
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

private val WORKOUT_EDIT_TIME_FORMAT =
    DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT)

data class WorkoutEditorState(
    val draft: WorkoutEditDraft,
    val isSaving: Boolean = false,
    val error: String? = null,
)

data class WorkoutEditDraft(
    val detail: WorkoutDetailUiModel,
    val start: String,
    val end: String,
    val sets: List<WorkoutSetEditDraft>,
    val zone: ZoneId,
) {
    fun toEdit(zone: ZoneId = this.zone): CompletedWorkoutEdit =
        CompletedWorkoutEdit(
            workoutRunId = detail.runId,
            startedAt = parseTime(start, detail.startedAt, zone),
            finishedAt = parseTime(end, detail.finishedAt, zone),
            sets = sets.map { it.toEdit() },
        )

    fun validationError(
        zone: ZoneId = this.zone,
        nowMillis: Long = System.currentTimeMillis(),
    ): String? =
        try {
            toEdit(zone).validateTimes(nowMillis)
            sets.forEach { set -> set.toEdit().validate(set.mode) }
            null
        } catch (error: IllegalArgumentException) {
            error.message ?: "Check workout times and set values"
        }

    companion object {
        fun from(
            detail: WorkoutDetailUiModel,
            zone: ZoneId = ZoneId.systemDefault(),
        ): WorkoutEditDraft =
            WorkoutEditDraft(
                detail = detail,
                start = formatTime(detail.startedAt, zone),
                end = formatTime(detail.finishedAt, zone),
                zone = zone,
                sets =
                    detail.exercises.flatMap { exercise ->
                        exercise.sets.map { set ->
                            WorkoutSetEditDraft(
                                setId = set.setId,
                                exerciseName = exercise.exerciseName,
                                setNumber = set.setNumber,
                                mode = exercise.trackingMode,
                                reps = set.reps?.toString().orEmpty(),
                                weight = set.weight?.toString().orEmpty(),
                                duration = set.durationSeconds?.toString().orEmpty(),
                                distance = set.distance?.toString().orEmpty(),
                                isCompleted = set.isCompleted,
                            )
                        }
                    },
            )
    }
}

@Suppress("LongParameterList")
data class WorkoutSetEditDraft(
    val setId: Long,
    val exerciseName: String,
    val setNumber: Int,
    val mode: TrackingMode,
    val reps: String,
    val weight: String,
    val duration: String,
    val distance: String,
    val isCompleted: Boolean,
) {
    fun toEdit(): CompletedSetEdit =
        CompletedSetEdit(
            setId = setId,
            reps = parseMetric(reps, "Reps", String::toIntOrNull),
            weight = parseMetric(weight, "Weight", String::toDoubleOrNull),
            durationSeconds = parseMetric(duration, "Duration", String::toLongOrNull),
            distance = parseMetric(distance, "Distance", String::toDoubleOrNull),
            isCompleted = isCompleted,
        )

    fun validationError(): String? =
        try {
            toEdit().validate(mode)
            null
        } catch (error: IllegalArgumentException) {
            error.message
        }
}

private fun <T> parseMetric(
    text: String,
    label: String,
    parse: (String) -> T?,
): T? = if (text.isBlank()) null else requireNotNull(parse(text.trim())) { "$label: enter a valid number" }

private fun formatTime(
    millis: Long,
    zone: ZoneId,
): String = Instant.ofEpochMilli(millis).atZone(zone).format(WORKOUT_EDIT_TIME_FORMAT)

private fun parseTime(
    text: String,
    original: Long,
    zone: ZoneId,
): Long {
    if (text == formatTime(original, zone)) return original
    val local =
        try {
            LocalDateTime.parse(text, WORKOUT_EDIT_TIME_FORMAT)
        } catch (_: java.time.DateTimeException) {
            throw IllegalArgumentException("Use a valid date and time: YYYY-MM-DD HH:MM")
        }
    val offsets = zone.rules.getValidOffsets(local)
    require(offsets.isNotEmpty()) { "This local time does not exist because of daylight saving time" }
    require(offsets.size == 1) { "This local time occurs twice; choose a time outside the daylight saving change" }
    return try {
        local.toInstant(offsets.single()).toEpochMilli()
    } catch (_: ArithmeticException) {
        throw IllegalArgumentException("Date is out of range")
    }
}
