package com.brokenpip3.gymbro.ui.screens.workout

internal data class ParsedSetMetrics(
    val reps: Int?,
    val weight: Double?,
    val durationSeconds: Long?,
    val distance: Double?,
)

internal data class SetInputParseResult(
    val metrics: ParsedSetMetrics? = null,
    val repsError: String? = null,
    val weightError: String? = null,
    val minutesError: String? = null,
    val secondsError: String? = null,
    val distanceError: String? = null,
)

internal fun parseStrengthSetInput(
    repsText: String,
    weightText: String,
): SetInputParseResult {
    val reps = repsText.parseOptionalInt()
    val weight = weightText.parseOptionalDouble(error = "Enter a decimal weight.")
    val repsError = reps.error ?: reps.value.positiveError("Reps must be positive.")
    val weightError =
        weight.error
            ?: weight.value.positiveFiniteError(
                finiteError = "Weight must be finite.",
                positiveError = "Weight must be positive.",
            )

    return if (repsError != null || weightError != null) {
        SetInputParseResult(repsError = repsError, weightError = weightError)
    } else {
        SetInputParseResult(
            metrics =
                ParsedSetMetrics(
                    reps = reps.value,
                    weight = weight.value,
                    durationSeconds = null,
                    distance = null,
                ),
        )
    }
}

internal fun parseBodyweightSetInput(repsText: String): SetInputParseResult {
    val reps = repsText.parseOptionalInt()
    val repsError = reps.error ?: reps.value.positiveError("Reps must be positive.")
    return if (repsError != null) {
        SetInputParseResult(repsError = repsError)
    } else {
        SetInputParseResult(
            metrics =
                ParsedSetMetrics(
                    reps = reps.value,
                    weight = null,
                    durationSeconds = null,
                    distance = null,
                ),
        )
    }
}

internal fun parseTimedSetInput(
    minutesText: String,
    secondsText: String,
    distanceText: String,
): SetInputParseResult {
    val minutes = minutesText.parseOptionalLong(error = "Enter whole-number minutes.")
    val seconds = secondsText.parseOptionalLong(error = "Enter whole-number seconds.")
    val distance = distanceText.parseOptionalDouble(error = "Enter a decimal distance.")
    val durationSeconds = (minutes.value ?: 0L) * 60L + (seconds.value ?: 0L)
    val minutesError =
        minutes.error
            ?: minutes.value.negativeError("Minutes must not be negative.")
            ?: durationSeconds.durationError()
    val secondsError = seconds.error ?: seconds.value.negativeError("Seconds must not be negative.")
    val distanceError =
        distance.error
            ?: distance.value.positiveFiniteError(
                finiteError = "Distance must be finite.",
                positiveError = "Distance must be positive.",
            )

    return if (minutesError != null || secondsError != null || distanceError != null) {
        SetInputParseResult(
            minutesError = minutesError,
            secondsError = secondsError,
            distanceError = distanceError,
        )
    } else {
        SetInputParseResult(
            metrics =
                ParsedSetMetrics(
                    reps = null,
                    weight = null,
                    durationSeconds = durationSeconds,
                    distance = distance.value,
                ),
        )
    }
}

private data class ParsedInput<T>(
    val value: T?,
    val error: String?,
)

private fun String.parseOptionalInt(): ParsedInput<Int> =
    trim().takeIf { it.isNotEmpty() }?.let { value ->
        value.toIntOrNull()?.let { ParsedInput(value = it, error = null) }
            ?: ParsedInput(value = null, error = "Enter whole-number reps.")
    } ?: ParsedInput(value = null, error = null)

private fun String.parseOptionalLong(error: String): ParsedInput<Long> =
    trim().takeIf { it.isNotEmpty() }?.let { value ->
        value.toLongOrNull()?.let { ParsedInput(value = it, error = null) }
            ?: ParsedInput(value = null, error = error)
    } ?: ParsedInput(value = null, error = null)

private fun String.parseOptionalDouble(error: String): ParsedInput<Double> =
    trim().takeIf { it.isNotEmpty() }?.let { value ->
        value.toDoubleOrNull()?.let { ParsedInput(value = it, error = null) }
            ?: ParsedInput(value = null, error = error)
    } ?: ParsedInput(value = null, error = null)

private fun Int?.positiveError(message: String): String? =
    when {
        this != null && this <= 0 -> message
        else -> null
    }

private fun Long?.negativeError(message: String): String? =
    when {
        this != null && this < 0L -> message
        else -> null
    }

private fun Long.durationError(): String? =
    when {
        this <= 0L -> "Duration must be positive."
        else -> null
    }

private fun Double?.positiveFiniteError(
    finiteError: String,
    positiveError: String,
): String? =
    when {
        this != null && !isFinite() -> finiteError
        this != null && this <= 0.0 -> positiveError
        else -> null
    }
