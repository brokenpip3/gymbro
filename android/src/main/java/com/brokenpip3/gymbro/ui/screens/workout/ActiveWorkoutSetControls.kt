package com.brokenpip3.gymbro.ui.screens.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.brokenpip3.gymbro.domain.TrackingMode
import com.brokenpip3.gymbro.ui.rememberKeyboardDismissal

@Composable
internal fun AddSetControls(
    exercise: WorkoutExerciseUiModel,
    onAddSet: (Long, Int?, Double?, Long?, Double?) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stackAction = shouldStackSetAction(maxWidth.value.toInt())
        when (exercise.trackingMode) {
            TrackingMode.Strength -> StrengthSetControls(exercise.exerciseResultId, stackAction, onAddSet)
            TrackingMode.Bodyweight -> BodyweightSetControls(exercise.exerciseResultId, stackAction, onAddSet)
            TrackingMode.Timed -> TimedSetControls(exercise.exerciseResultId, stackAction, onAddSet)
        }
    }
}

@Composable
private fun StrengthSetControls(
    exerciseResultId: Long,
    stackAction: Boolean,
    onAddSet: (Long, Int?, Double?, Long?, Double?) -> Unit,
) {
    var reps by rememberSaveable(exerciseResultId) { mutableStateOf("") }
    var weight by rememberSaveable(exerciseResultId) { mutableStateOf("") }
    var parseResult by rememberSaveable(exerciseResultId, stateSaver = SetInputParseResultSaver) {
        mutableStateOf(SetInputParseResult())
    }
    val keyboardDismissal = rememberKeyboardDismissal()

    val addSet = {
        val result = parseStrengthSetInput(reps, weight)
        parseResult = result
        result.metrics?.let { metrics ->
            onAddSet(exerciseResultId, metrics.reps, metrics.weight, metrics.durationSeconds, metrics.distance)
            reps = ""
            weight = ""
            parseResult = SetInputParseResult()
        }
        keyboardDismissal.dismiss()
        Unit
    }
    val fields: @Composable RowScope.() -> Unit = {
        NumericField(reps, { reps = it }, "Reps", KeyboardType.Number, parseResult.repsError, Modifier.weight(1f))
        NumericField(
            weight,
            { weight = it },
            "Weight",
            KeyboardType.Decimal,
            parseResult.weightError,
            Modifier.weight(1f),
        )
    }

    if (stackAction) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = fields,
            )
            Button(
                onClick = addSet,
                modifier = Modifier.fillMaxWidth().testTag("workout-done-$exerciseResultId"),
            ) {
                Text(text = "Add")
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            fields()
            Button(
                onClick = addSet,
                modifier = Modifier.width(88.dp).testTag("workout-done-$exerciseResultId"),
            ) {
                Text(text = "Add")
            }
        }
    }
}

@Composable
private fun BodyweightSetControls(
    exerciseResultId: Long,
    stackAction: Boolean,
    onAddSet: (Long, Int?, Double?, Long?, Double?) -> Unit,
) {
    var reps by rememberSaveable(exerciseResultId) { mutableStateOf("") }
    var parseResult by rememberSaveable(exerciseResultId, stateSaver = SetInputParseResultSaver) {
        mutableStateOf(SetInputParseResult())
    }
    val keyboardDismissal = rememberKeyboardDismissal()

    val addSet = {
        val result = parseBodyweightSetInput(reps)
        parseResult = result
        result.metrics?.let { metrics ->
            onAddSet(exerciseResultId, metrics.reps, metrics.weight, metrics.durationSeconds, metrics.distance)
            reps = ""
            parseResult = SetInputParseResult()
        }
        keyboardDismissal.dismiss()
        Unit
    }
    if (stackAction) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NumericField(reps, { reps = it }, "Reps", KeyboardType.Number, parseResult.repsError)
            Button(
                onClick = addSet,
                modifier = Modifier.fillMaxWidth().testTag("workout-done-$exerciseResultId"),
            ) {
                Text(text = "Add")
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NumericField(reps, { reps = it }, "Reps", KeyboardType.Number, parseResult.repsError, Modifier.weight(1f))
            Button(
                onClick = addSet,
                modifier = Modifier.width(88.dp).testTag("workout-done-$exerciseResultId"),
            ) {
                Text(text = "Add")
            }
        }
    }
}

@Composable
private fun TimedSetControls(
    exerciseResultId: Long,
    stackAction: Boolean,
    onAddSet: (Long, Int?, Double?, Long?, Double?) -> Unit,
) {
    var minutes by rememberSaveable(exerciseResultId) { mutableStateOf("") }
    var seconds by rememberSaveable(exerciseResultId) { mutableStateOf("") }
    var distance by rememberSaveable(exerciseResultId) { mutableStateOf("") }
    var parseResult by rememberSaveable(exerciseResultId, stateSaver = SetInputParseResultSaver) {
        mutableStateOf(SetInputParseResult())
    }
    val keyboardDismissal = rememberKeyboardDismissal()

    val addSet = {
        val result = parseTimedSetInput(minutes, seconds, distance)
        parseResult = result
        result.metrics?.let { metrics ->
            onAddSet(
                exerciseResultId,
                metrics.reps,
                metrics.weight,
                metrics.durationSeconds,
                metrics.distance,
            )
            minutes = ""
            seconds = ""
            distance = ""
            parseResult = SetInputParseResult()
        }
        keyboardDismissal.dismiss()
        Unit
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NumericField(
                minutes,
                { minutes = it },
                "Min",
                KeyboardType.Number,
                parseResult.minutesError,
                Modifier.weight(1f),
            )
            NumericField(
                seconds,
                { seconds = it },
                "Sec",
                KeyboardType.Number,
                parseResult.secondsError,
                Modifier.weight(1f),
            )
        }
        if (stackAction) {
            NumericField(
                distance,
                { distance = it },
                "Distance",
                KeyboardType.Decimal,
                parseResult.distanceError,
                Modifier.fillMaxWidth(),
            )
            Button(
                onClick = addSet,
                modifier = Modifier.fillMaxWidth().testTag("workout-done-$exerciseResultId"),
            ) {
                Text(text = "Add")
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NumericField(
                    distance,
                    { distance = it },
                    "Distance",
                    KeyboardType.Decimal,
                    parseResult.distanceError,
                    Modifier.weight(1f),
                )
                Button(
                    onClick = addSet,
                    modifier = Modifier.width(88.dp).testTag("workout-done-$exerciseResultId"),
                ) {
                    Text(text = "Add")
                }
            }
        }
    }
}

internal fun shouldStackSetAction(availableWidthDp: Int): Boolean = availableWidthDp < 336

private val SetInputParseResultSaver: Saver<SetInputParseResult, Any> =
    listSaver(
        save = { result ->
            listOf(
                result.repsError,
                result.weightError,
                result.minutesError,
                result.secondsError,
                result.distanceError,
            )
        },
        restore = { values ->
            SetInputParseResult(
                repsError = values.getOrNull(0),
                weightError = values.getOrNull(1),
                minutesError = values.getOrNull(2),
                secondsError = values.getOrNull(3),
                distanceError = values.getOrNull(4),
            )
        },
    )

@Composable
private fun NumericField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    error: String?,
    modifier: Modifier = Modifier,
) {
    val keyboardDismissal = rememberKeyboardDismissal()

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        isError = error != null,
        supportingText = error?.let { message -> { Text(text = message) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { keyboardDismissal.dismiss() }),
        modifier = modifier.testTag("workout-input-$label"),
    )
}
