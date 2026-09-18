@file:Suppress("TooManyFunctions")

package com.brokenpip3.gymbro.ui.screens.results

import com.brokenpip3.gymbro.data.entities.ExerciseResultEntity
import com.brokenpip3.gymbro.data.entities.SetResultEntity
import com.brokenpip3.gymbro.data.entities.WorkoutRunEntity
import com.brokenpip3.gymbro.domain.TrackingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal data class ResultRunDetail(
    val workoutRun: WorkoutRunEntity,
    val exerciseDetails: List<ResultExerciseDetail>,
)

internal data class ResultExerciseDetail(
    val exerciseResult: ExerciseResultEntity,
    val setResults: List<SetResultEntity>,
)

internal data class ResultExerciseKey(
    val exerciseId: Long?,
    val exerciseName: String,
    val trackingMode: TrackingMode,
)

internal fun ResultRunDetail.toWorkoutDetailUiModel(): WorkoutDetailUiModel =
    WorkoutDetailUiModel(
        runId = workoutRun.id,
        scheduleName = workoutRun.scheduleNameSnapshot,
        startedAt = workoutRun.startedAt,
        durationSeconds = ((workoutRun.finishedAt ?: workoutRun.startedAt) - workoutRun.startedAt) / 1_000L,
        notes = workoutRun.notes,
        exercises =
            exerciseDetails
                .sortedBy { detail -> detail.exerciseResult.sortOrder }
                .map { detail -> detail.toWorkoutExerciseDetailUiModel() },
    )

private fun ResultExerciseDetail.toWorkoutExerciseDetailUiModel(): WorkoutExerciseDetailUiModel {
    val trackingMode = exerciseResult.trackingModeSnapshot.toTrackingMode()
    return WorkoutExerciseDetailUiModel(
        exerciseResultId = exerciseResult.id,
        exerciseName = exerciseResult.exerciseNameSnapshot,
        trackingMode = trackingMode,
        notes = exerciseResult.notes,
        sets =
            setResults
                .sortedBy { setResult -> setResult.setOrder }
                .mapIndexed { index, setResult ->
                    WorkoutSetDetailUiModel(
                        setId = setResult.id,
                        setNumber = index + 1,
                        value = setResult.formatSet(trackingMode),
                        isCompleted = setResult.isCompleted,
                        notes = setResult.notes,
                    )
                },
    )
}

private data class ExercisePoint(
    val selectionKey: ExerciseStatsSelectionKey,
    val exerciseName: String,
    val trackingMode: TrackingMode,
    val workoutRun: WorkoutRunEntity,
    val value: Double,
    val distance: Double?,
    val setResults: List<SetResultEntity>,
    val allSetResults: List<SetResultEntity>,
    val notes: String?,
)

private data class PersonalRecordCandidate(
    val metric: String,
    val numericValue: Double,
    val displayValue: String,
)

internal fun ExerciseResultEntity.toResultExerciseKey(): ResultExerciseKey =
    ResultExerciseKey(
        exerciseId = exerciseId,
        exerciseName = exerciseNameSnapshot,
        trackingMode = trackingModeSnapshot.toTrackingMode(),
    )

internal fun List<ResultRunDetail>.toExerciseStats(): List<ExerciseStatsUiModel> {
    val points = toExercisePoints()

    return points
        .groupBy { point -> point.selectionKey }
        .map { (_, exercisePoints) ->
            exercisePoints.toExerciseStatsUiModel()
        }.sortedWith(
            compareBy<ExerciseStatsUiModel> { stats -> stats.exerciseName }
                .thenBy { stats -> stats.trackingMode.ordinal }
                .thenBy { stats -> stats.exerciseId ?: Long.MIN_VALUE },
        )
}

internal fun List<ResultRunDetail>.toExerciseStatDetails(): List<ExerciseStatsDetailUiModel> {
    val points = toExercisePoints()

    return points
        .groupBy { point -> point.selectionKey }
        .map { (_, exercisePoints) ->
            exercisePoints.toDetailUiModel()
        }.sortedWith(
            compareBy<ExerciseStatsDetailUiModel> { stats -> stats.exerciseName }
                .thenBy { stats -> stats.trackingMode.ordinal }
                .thenBy { stats -> stats.exerciseId ?: Long.MIN_VALUE },
        )
}

internal fun List<ResultRunDetail>.detailFor(exerciseResult: ExerciseResultEntity): ExerciseStatsDetailUiModel? {
    val key = exerciseResult.toResultExerciseKey()
    val sameIdDetails =
        key.exerciseId
            ?.let { exerciseId ->
                matchingDetails { detail ->
                    detail.exerciseResult.exerciseId == exerciseId &&
                        detail.exerciseResult.trackingModeSnapshot.toTrackingMode() == key.trackingMode
                }
            }.orEmpty()
    val matchingDetails =
        if (sameIdDetails.isNotEmpty()) {
            sameIdDetails
        } else {
            matchingDetails { detail ->
                detail.exerciseResult.exerciseNameSnapshot == key.exerciseName &&
                    detail.exerciseResult.trackingModeSnapshot.toTrackingMode() == key.trackingMode
            }
        }

    return matchingDetails.toExerciseStatDetails().firstOrNull()
}

private fun List<ResultRunDetail>.matchingDetails(predicate: (ResultExerciseDetail) -> Boolean): List<ResultRunDetail> =
    filter { runDetail -> runDetail.workoutRun.finishedAt != null }
        .map { runDetail ->
            val exerciseDetails = runDetail.exerciseDetails.filter(predicate)
            runDetail.copy(exerciseDetails = exerciseDetails)
        }.filter { runDetail -> runDetail.exerciseDetails.isNotEmpty() }

private fun List<ResultRunDetail>.toExercisePoints(): List<ExercisePoint> =
    filter { runDetail -> runDetail.workoutRun.finishedAt != null }
        .sortedBy { runDetail -> runDetail.workoutRun.finishedAt }
        .flatMap { runDetail -> runDetail.toExercisePoints() }

private fun ResultRunDetail.toExercisePoints(): List<ExercisePoint> {
    val groupedExerciseDetails =
        exerciseDetails.groupBy { exerciseDetail ->
            val result = exerciseDetail.exerciseResult
            result.toSelectionKey(
                deletedScope =
                    if (result.exerciseId == null) {
                        DeletedExerciseScope(
                            scheduleId = workoutRun.scheduleId,
                            sortOrder = result.sortOrder,
                        )
                    } else {
                        null
                    },
            )
        }

    return groupedExerciseDetails.mapNotNull { (selectionKey, exerciseDetails) ->
        val displayExercise = exerciseDetails.maxBy { detail -> detail.exerciseResult.sortOrder }
        val trackingMode = displayExercise.exerciseResult.trackingModeSnapshot.toTrackingMode()
        val completedSets = exerciseDetails.flatMap { detail -> detail.completedSets(trackingMode) }
        val value = completedSets.statValue(trackingMode)
        if (value <= 0.0) {
            null
        } else {
            ExercisePoint(
                selectionKey = selectionKey,
                exerciseName = displayExercise.exerciseResult.exerciseNameSnapshot,
                trackingMode = trackingMode,
                workoutRun = workoutRun,
                value = value,
                distance = completedSets.sumDistance().takeIf { distance -> distance > 0.0 },
                setResults = completedSets,
                allSetResults = exerciseDetails.flatMap { detail -> detail.setResults },
                notes = displayExercise.exerciseResult.notes,
            )
        }
    }
}

@Suppress("ktlint:standard:function-expression-body")
private fun ResultExerciseDetail.completedSets(trackingMode: TrackingMode): List<SetResultEntity> {
    return setResults.filter { setResult ->
        setResult.isCompleted && setResult.isStatEligible(trackingMode)
    }
}

private fun SetResultEntity.isStatEligible(trackingMode: TrackingMode): Boolean =
    when (trackingMode) {
        TrackingMode.Strength -> reps != null && weight != null
        TrackingMode.Bodyweight -> reps != null
        TrackingMode.Timed -> durationSeconds != null
    }

private fun List<SetResultEntity>.statValue(trackingMode: TrackingMode): Double =
    when (trackingMode) {
        TrackingMode.Strength ->
            sumOf { setResult ->
                val reps = setResult.reps
                val weight = setResult.weight
                if (reps != null && weight != null) reps * weight else 0.0
            }

        TrackingMode.Bodyweight ->
            sumOf { setResult -> setResult.reps ?: 0 }.toDouble()

        TrackingMode.Timed ->
            sumOf { setResult -> setResult.durationSeconds ?: 0L }.toDouble()
    }

private fun List<SetResultEntity>.sumDistance(): Double = sumOf { setResult -> setResult.distance ?: 0.0 }

private fun List<ExercisePoint>.toExerciseStatsUiModel(): ExerciseStatsUiModel {
    val orderedPoints = sortedBy { point -> point.workoutRun.finishedAt }
    val latestPoint = orderedPoints.last()

    return ExerciseStatsUiModel(
        exerciseId = latestPoint.selectionKey.exerciseId,
        exerciseName = latestPoint.exerciseName,
        trackingMode = latestPoint.trackingMode,
        deletedScope = latestPoint.selectionKey.deletedScope,
        headline = latestPoint.trackingMode.headline(latestPoint),
        progressDelta = latestPoint.trackingMode.progressDelta(orderedPoints),
        lastTrainedAt = latestPoint.workoutRun.finishedAt ?: latestPoint.workoutRun.startedAt,
        progressScore = orderedPoints.progressScore(),
        personalRecordLabel = orderedPoints.personalRecordLabel(),
        trend = orderedPoints.trend(),
        points =
            orderedPoints.mapIndexed { index, point ->
                ChartPoint(
                    label = point.chartLabel(index),
                    value = point.value,
                )
            },
    )
}

private fun List<ExercisePoint>.toDetailUiModel(): ExerciseStatsDetailUiModel {
    val orderedPoints = sortedBy { point -> point.workoutRun.finishedAt }
    val latestPoint = orderedPoints.last()
    val trackingMode = latestPoint.trackingMode
    val recentPoints = orderedPoints.asReversed()
    val recentSets =
        recentPoints
            .flatMap { point ->
                point.setResults
                    .sortedBy { setResult -> setResult.setOrder }
                    .mapIndexedNotNull { index, setResult ->
                        setResult.formatSet(trackingMode)?.let { value ->
                            RecentSetUiModel(
                                label = "${point.workoutRun.scheduleNameSnapshot} Set ${index + 1}",
                                value = value,
                            )
                        }
                    }
            }.take(MAX_RECENT_SETS)
    val noteDetails =
        recentPoints
            .flatMap { point ->
                val dateLabel = point.workoutRun.finishedAt?.formatChartDate() ?: "Unknown date"
                val scheduleName = point.workoutRun.scheduleNameSnapshot
                buildList {
                    point.notes
                        ?.takeIf { note -> note.isNotBlank() }
                        ?.let { note ->
                            add(
                                ExerciseNoteUiModel(
                                    dateLabel = dateLabel,
                                    contextLabel = "$scheduleName · Exercise",
                                    text = note,
                                ),
                            )
                        }
                    point.allSetResults
                        .sortedBy { setResult -> setResult.setOrder }
                        .mapIndexedNotNull { index, setResult ->
                            setResult.notes
                                ?.takeIf { note -> note.isNotBlank() }
                                ?.let { note ->
                                    ExerciseNoteUiModel(
                                        dateLabel = dateLabel,
                                        contextLabel = "$scheduleName · Set ${index + 1}",
                                        text = note,
                                    )
                                }
                        }.forEach(::add)
                }
            }.distinctBy { note -> note.dateLabel to note.contextLabel to note.text }
            .take(MAX_RECENT_NOTES)

    return ExerciseStatsDetailUiModel(
        exerciseId = latestPoint.selectionKey.exerciseId,
        exerciseName = latestPoint.exerciseName,
        trackingMode = latestPoint.trackingMode,
        deletedScope = latestPoint.selectionKey.deletedScope,
        latest = trackingMode.latest(latestPoint),
        best = trackingMode.best(orderedPoints),
        bestSession = trackingMode.bestSession(orderedPoints),
        bestEstimatedOneRepMax = trackingMode.bestEstimatedOneRepMax(orderedPoints),
        average = trackingMode.average(orderedPoints),
        progressDelta = trackingMode.progressDelta(orderedPoints),
        progressPercent = orderedPoints.progressPercent(),
        personalRecordLabel = orderedPoints.personalRecordLabel(),
        totalSessions = orderedPoints.size,
        totalSets = orderedPoints.sumOf { point -> point.setResults.size },
        summaryMetrics = trackingMode.summaryMetrics(orderedPoints),
        chartSummary = orderedPoints.toChartSummary(),
        personalRecords = orderedPoints.personalRecords(),
        points =
            orderedPoints.mapIndexed { index, point ->
                ChartPoint(
                    label = point.chartLabel(index),
                    value = point.value,
                )
            },
        recentSessions = orderedPoints.toRecentSessions(trackingMode),
        recentSets = recentSets,
        notes = noteDetails.map { note -> note.text }.distinct(),
        noteDetails = noteDetails,
        secondaryChart = trackingMode.secondaryChart(orderedPoints),
        additionalCharts = trackingMode.additionalCharts(orderedPoints),
    )
}

private fun List<ExercisePoint>.toRecentSessions(trackingMode: TrackingMode): List<RecentSessionUiModel> =
    asReversed()
        .take(MAX_RECENT_SESSIONS)
        .mapIndexed { index, point ->
            val previousPoint = getOrNull(lastIndex - index - 1)
            RecentSessionUiModel(
                dateLabel = point.workoutRun.finishedAt?.formatChartDate() ?: "Unknown date",
                scheduleName = point.workoutRun.scheduleNameSnapshot,
                value = trackingMode.latest(point),
                setCount = point.setResults.size,
                comparison =
                    previousPoint?.let { previous ->
                        trackingMode.sessionComparison(
                            current = point,
                            previous = previous,
                        )
                    },
            )
        }

private fun TrackingMode.headline(latestPoint: ExercisePoint): String =
    when (this) {
        TrackingMode.Strength -> "${latestPoint.value.formatStat()} volume"
        TrackingMode.Bodyweight -> "${latestPoint.value.formatStat()} reps"
        TrackingMode.Timed ->
            buildString {
                append("${latestPoint.value.formatStat()} sec")
                latestPoint.distance?.let { distance ->
                    append(" / ${distance.formatStat()} distance")
                }
            }
    }

private fun TrackingMode.latest(latestPoint: ExercisePoint): String =
    when (this) {
        TrackingMode.Strength -> "${latestPoint.value.formatStat()} volume"
        TrackingMode.Bodyweight -> "${latestPoint.value.formatStat()} reps"
        TrackingMode.Timed ->
            buildString {
                append(formatDuration(latestPoint.value.toLong()))
                latestPoint.distance?.let { distance ->
                    append(" / ${distance.formatStat()} distance")
                }
            }
    }

private fun TrackingMode.best(points: List<ExercisePoint>): String =
    when (this) {
        TrackingMode.Strength ->
            points
                .flatMap { point -> point.setResults }
                .maxByOrNull { setResult -> (setResult.reps ?: 0) * (setResult.weight ?: 0.0) }
                ?.formatSet(this)
                ?: "No sets"

        TrackingMode.Bodyweight ->
            points
                .flatMap { point -> point.setResults }
                .maxByOrNull { setResult -> setResult.reps ?: 0 }
                ?.formatSet(this)
                ?: "No sets"

        TrackingMode.Timed ->
            if (points.hasCompleteSetPaceData()) {
                points
                    .flatMap { point -> point.setResults }
                    .minByOrNull { setResult -> setResult.setPaceValue() }
                    ?.let { setResult ->
                        "${formatDuration(kotlin.math.round(setResult.setPaceValue()).toLong())} / distance"
                    }
                    ?: "No sets"
            } else {
                points
                    .flatMap { point -> point.setResults }
                    .maxByOrNull { setResult -> setResult.durationSeconds ?: 0L }
                    ?.formatSet(this)
                    ?: "No sets"
            }
    }

private fun TrackingMode.bestSession(points: List<ExercisePoint>): String {
    val bestPoint =
        if (this == TrackingMode.Timed && points.hasPaceData()) {
            points.minByOrNull { point -> point.paceValue() }
        } else {
            points.maxByOrNull { point -> point.value }
        } ?: return "No sessions"

    return when (this) {
        TrackingMode.Strength -> "${bestPoint.value.formatStat()} best volume"
        TrackingMode.Bodyweight -> "${bestPoint.value.formatStat()} best reps"
        TrackingMode.Timed ->
            if (points.hasPaceData()) {
                "${formatDuration(bestPoint.paceValue().toLong())} best pace"
            } else {
                "${formatDuration(bestPoint.value.toLong())} best"
            }
    }
}

private fun TrackingMode.bestEstimatedOneRepMax(points: List<ExercisePoint>): String? =
    if (this == TrackingMode.Strength) {
        points
            .flatMap { point -> point.setResults }
            .mapNotNull { setResult -> setResult.estimatedOneRepMax() }
            .maxOrNull()
            ?.let { bestEstimate ->
                val roundedEstimate = kotlin.math.round(bestEstimate * 10.0) / 10.0
                "${roundedEstimate.formatStat()} est. 1RM"
            }
    } else {
        null
    }

private fun TrackingMode.summaryMetrics(points: List<ExercisePoint>): List<SummaryMetricUiModel> =
    when (this) {
        TrackingMode.Strength ->
            listOfNotNull(
                points.maxWeightMetric(),
                SummaryMetricUiModel(label = "Total volume", value = points.sumOf { it.value }.formatStat()),
                points.totalRepsMetric(),
                SummaryMetricUiModel(label = "Frequency", value = points.frequencyLabel()),
            )

        TrackingMode.Bodyweight ->
            listOf(
                SummaryMetricUiModel(label = "Total reps", value = points.totalReps().toString()),
                SummaryMetricUiModel(label = "Best set", value = best(points)),
                SummaryMetricUiModel(label = "Average", value = average(points)),
                SummaryMetricUiModel(label = "Frequency", value = points.frequencyLabel()),
            )

        TrackingMode.Timed ->
            listOfNotNull(
                SummaryMetricUiModel(
                    label = "Total time",
                    value = formatDuration(points.sumOf { point -> point.value }.toLong()),
                ),
                points.totalDistanceMetric(),
                points.averagePaceMetric(),
                SummaryMetricUiModel(
                    label = if (points.hasCompleteSetPaceData()) "Best pace" else "Best set",
                    value = best(points),
                ),
                SummaryMetricUiModel(label = "Frequency", value = points.frequencyLabel()),
            )
    }

private fun List<ExercisePoint>.personalRecords(): List<PersonalRecordUiModel> {
    val bestValues = mutableMapOf<String, Double>()

    return flatMap { point ->
        point.trackingMode.personalRecordCandidates(point).mapNotNull { candidate ->
            val previousBest = bestValues[candidate.metric]
            val isRecord = previousBest == null || candidate.numericValue > previousBest
            bestValues[candidate.metric] = maxOf(previousBest ?: Double.NEGATIVE_INFINITY, candidate.numericValue)
            if (isRecord) {
                PersonalRecordUiModel(
                    dateLabel = point.workoutRun.finishedAt?.formatChartDate() ?: "Unknown date",
                    metric = candidate.metric,
                    value = candidate.displayValue,
                )
            } else {
                null
            }
        }
    }
}

private fun TrackingMode.personalRecordCandidates(point: ExercisePoint): List<PersonalRecordCandidate> =
    when (this) {
        TrackingMode.Strength ->
            listOfNotNull(
                PersonalRecordCandidate(
                    metric = "Volume",
                    numericValue = point.value,
                    displayValue = "${point.value.formatStat()} volume",
                ),
                point.setResults
                    .mapNotNull { setResult -> setResult.weight }
                    .maxOrNull()
                    ?.let { maxWeight ->
                        PersonalRecordCandidate(
                            metric = "Max weight",
                            numericValue = maxWeight,
                            displayValue = maxWeight.formatStat(),
                        )
                    },
            )

        TrackingMode.Bodyweight ->
            listOfNotNull(
                PersonalRecordCandidate(
                    metric = "Total reps",
                    numericValue = point.value,
                    displayValue = "${point.value.formatStat()} reps",
                ),
                point.setResults
                    .mapNotNull { setResult -> setResult.reps }
                    .maxOrNull()
                    ?.let { bestSet ->
                        PersonalRecordCandidate(
                            metric = "Best set",
                            numericValue = bestSet.toDouble(),
                            displayValue = "$bestSet reps",
                        )
                    },
            )

        TrackingMode.Timed ->
            buildList {
                if (point.distance != null && point.distance > 0.0) {
                    add(
                        PersonalRecordCandidate(
                            metric = "Distance",
                            numericValue = point.distance,
                            displayValue = "${point.distance.formatStat()} distance",
                        ),
                    )
                    add(
                        PersonalRecordCandidate(
                            metric = "Pace",
                            numericValue = -point.paceValue(),
                            displayValue = "${formatDuration(point.paceValue().toLong())} / distance",
                        ),
                    )
                } else {
                    add(
                        PersonalRecordCandidate(
                            metric = "Total time",
                            numericValue = point.value,
                            displayValue = formatDuration(point.value.toLong()),
                        ),
                    )
                    point.setResults
                        .mapNotNull { setResult -> setResult.durationSeconds }
                        .maxOrNull()
                        ?.let { bestSet ->
                            add(
                                PersonalRecordCandidate(
                                    metric = "Best set",
                                    numericValue = bestSet.toDouble(),
                                    displayValue = formatDuration(bestSet),
                                ),
                            )
                        }
                }
            }
    }

private fun List<ExercisePoint>.toChartSummary(): ChartSummaryUiModel? =
    takeIf { points -> points.isNotEmpty() }
        ?.let { points ->
            val values = points.map { point -> point.value }
            val firstFinishedAt = points.first().workoutRun.finishedAt
            val lastFinishedAt = points.last().workoutRun.finishedAt
            if (firstFinishedAt != null && lastFinishedAt != null) {
                val trackingMode = points.last().trackingMode
                ChartSummaryUiModel(
                    rangeLabel = "${firstFinishedAt.formatChartDate()} - ${lastFinishedAt.formatChartDate()}",
                    minLabel = "Min ${values.minOrNull()?.let(trackingMode::formatChartValue).orEmpty()}",
                    maxLabel = "Max ${values.maxOrNull()?.let(trackingMode::formatChartValue).orEmpty()}",
                )
            } else {
                null
            }
        }

internal fun TrackingMode.formatChartValue(value: Double): String =
    when (this) {
        TrackingMode.Strength -> "${value.formatStat()} volume"
        TrackingMode.Bodyweight -> "${value.formatStat()} reps"
        TrackingMode.Timed -> formatDuration(value.toLong())
    }

internal fun TrackingMode.formatChartScaleValue(value: Double): String =
    when (this) {
        TrackingMode.Strength -> "${value.formatChartNumber()} volume"
        TrackingMode.Bodyweight -> "${value.formatChartNumber()} reps"
        TrackingMode.Timed -> formatDuration(value.toLong())
    }

private fun TrackingMode.secondaryChart(points: List<ExercisePoint>): ExerciseChartUiModel? {
    val title =
        when (this) {
            TrackingMode.Strength -> "Max weight"
            TrackingMode.Bodyweight -> "Best set"
            TrackingMode.Timed -> "Distance"
        }
    val chartPoints =
        points.mapIndexedNotNull { index, point ->
            val value =
                when (this) {
                    TrackingMode.Strength -> point.setResults.mapNotNull { it.weight }.maxOrNull()
                    TrackingMode.Bodyweight ->
                        point.setResults
                            .mapNotNull { it.reps }
                            .maxOrNull()
                            ?.toDouble()
                    TrackingMode.Timed -> point.distance
                }
            value?.let { ChartPoint(label = point.chartLabel(index), value = it) }
        }

    return chartPoints
        .takeIf { it.isNotEmpty() }
        ?.let { ExerciseChartUiModel(title = title, points = it) }
}

private fun TrackingMode.additionalCharts(points: List<ExercisePoint>): List<ExerciseChartUiModel> =
    buildList {
        when (this@additionalCharts) {
            TrackingMode.Strength -> {
                val repChartPoints =
                    points.mapIndexed { index, point ->
                        ChartPoint(
                            label = point.chartLabel(index),
                            value = point.setResults.sumOf { setResult -> setResult.reps ?: 0 }.toDouble(),
                        )
                    }
                if (repChartPoints.isNotEmpty()) {
                    add(
                        ExerciseChartUiModel(
                            title = "Total reps",
                            points = repChartPoints,
                        ),
                    )
                }

                val chartPoints =
                    points.mapIndexedNotNull { index, point ->
                        point.setResults
                            .mapNotNull { setResult -> setResult.estimatedOneRepMax() }
                            .maxOrNull()
                            ?.let { estimate ->
                                ChartPoint(
                                    label = point.chartLabel(index),
                                    value = estimate,
                                )
                            }
                    }
                if (chartPoints.isNotEmpty()) {
                    add(
                        ExerciseChartUiModel(
                            title = "Estimated 1RM",
                            points = chartPoints,
                        ),
                    )
                }
            }

            TrackingMode.Timed -> {
                val chartPoints =
                    points.mapIndexedNotNull { index, point ->
                        point.distance
                            ?.takeIf { distance -> distance > 0.0 }
                            ?.let { distance ->
                                ChartPoint(
                                    label = point.chartLabel(index),
                                    value = point.value / distance,
                                )
                            }
                    }
                if (chartPoints.isNotEmpty()) {
                    add(
                        ExerciseChartUiModel(
                            title = "Average pace",
                            points = chartPoints,
                            valueFormat = ChartValueFormat.Duration,
                        ),
                    )
                }
            }

            TrackingMode.Bodyweight -> Unit
        }
    }

private fun ExercisePoint.chartLabel(index: Int): String {
    val finishedAt = workoutRun.finishedAt
    return finishedAt?.formatChartDate() ?: "Workout ${index + 1}"
}

internal fun formatResultChartDate(
    epochMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): String =
    CHART_DATE_FORMATTER.format(
        Instant.ofEpochMilli(epochMillis).atZone(zoneId),
    )

private fun Long.formatChartDate(): String = formatResultChartDate(this)

private fun List<ExercisePoint>.maxWeightMetric(): SummaryMetricUiModel? =
    flatMap { point -> point.setResults }
        .mapNotNull { setResult -> setResult.weight }
        .maxOrNull()
        ?.let { maxWeight -> SummaryMetricUiModel(label = "Max weight", value = maxWeight.formatStat()) }

private fun List<ExercisePoint>.totalRepsMetric(): SummaryMetricUiModel? =
    totalReps()
        .takeIf { totalReps -> totalReps > 0 }
        ?.let { totalReps -> SummaryMetricUiModel(label = "Total reps", value = totalReps.toString()) }

private fun List<ExercisePoint>.totalReps(): Int =
    flatMap { point -> point.setResults }
        .sumOf { setResult -> setResult.reps ?: 0 }

private fun List<ExercisePoint>.totalDistanceMetric(): SummaryMetricUiModel? =
    sumOf { point -> point.distance ?: 0.0 }
        .takeIf { distance -> distance > 0.0 }
        ?.let { distance -> SummaryMetricUiModel(label = "Total distance", value = distance.formatStat()) }

private fun List<ExercisePoint>.averagePaceMetric(): SummaryMetricUiModel? {
    val totalDistance = sumOf { point -> point.distance ?: 0.0 }
    if (totalDistance <= 0.0) return null

    val secondsPerDistance = sumOf { point -> point.value } / totalDistance
    return SummaryMetricUiModel(
        label = "Avg pace",
        value = "${formatDuration(kotlin.math.round(secondsPerDistance).toLong())} / distance",
    )
}

private fun List<ExercisePoint>.frequencyLabel(): String =
    when (size) {
        1 -> "1 session"
        else -> "$size sessions"
    }

private fun SetResultEntity.estimatedOneRepMax(): Double? =
    reps
        ?.takeIf { reps -> reps > 0 }
        ?.let { reps ->
            weight
                ?.takeIf { weight -> weight > 0.0 }
                ?.let { weight -> weight * (1.0 + reps / 30.0) }
        }

private fun TrackingMode.average(points: List<ExercisePoint>): String {
    val recent = points.takeLast(MAX_AVERAGE_WORKOUTS)
    val average = recent.map { point -> point.value }.average()

    return when (this) {
        TrackingMode.Strength -> "${average.formatStat()} avg volume"
        TrackingMode.Bodyweight -> "${average.formatStat()} avg reps"
        TrackingMode.Timed ->
            if (recent.hasPaceData()) {
                val averagePace = recent.map { point -> point.paceValue() }.average()
                "${formatDuration(kotlin.math.round(averagePace).toLong())} avg pace / distance"
            } else {
                "${formatDuration(average.toLong())} avg"
            }
    }
}

private fun TrackingMode.progressDelta(points: List<ExercisePoint>): String =
    if (points.size < MIN_POINTS_FOR_TREND) {
        "New"
    } else {
        val comparisonValues = points.comparisonValues()
        val previous = comparisonValues[comparisonValues.lastIndex - 1]
        val latest = comparisonValues.last()
        val delta = latest - previous

        when {
            delta == 0.0 -> "No change"
            this == TrackingMode.Strength -> "${if (delta > 0.0) "+" else ""}${delta.formatStat()} volume"
            this == TrackingMode.Bodyweight -> "${if (delta > 0.0) "+" else ""}${delta.formatStat()} reps"
            points.hasPaceData() -> {
                val prefix = if (delta > 0.0) "+" else "-"
                "$prefix${formatDuration(kotlin.math.abs(delta).toLong())} / distance"
            }
            else -> {
                val prefix = if (delta > 0.0) "+" else "-"
                "$prefix${formatDuration(kotlin.math.abs(delta).toLong())}"
            }
        }
    }

private fun TrackingMode.sessionComparison(
    current: ExercisePoint,
    previous: ExercisePoint,
): SessionComparisonUiModel {
    val currentValue = comparisonValue(current)
    val previousValue = comparisonValue(previous)
    val delta = currentValue - previousValue

    if (delta == 0.0) {
        return SessionComparisonUiModel(
            label = "No change",
            trend = TrendDirection.Flat,
        )
    }

    return when (this) {
        TrackingMode.Strength ->
            SessionComparisonUiModel(
                label = "${delta.signedStat()} volume",
                trend = delta.trendDirection(),
            )

        TrackingMode.Bodyweight ->
            SessionComparisonUiModel(
                label = "${delta.signedStat()} reps",
                trend = delta.trendDirection(),
            )

        TrackingMode.Timed -> {
            val hasPaceComparison = current.distance.isPositive() && previous.distance.isPositive()
            if (hasPaceComparison) {
                val faster = delta < 0.0
                SessionComparisonUiModel(
                    label =
                        "${if (faster) "Faster" else "Slower"} " +
                            "${formatDuration(kotlin.math.abs(delta).toLong())} / distance",
                    trend = if (faster) TrendDirection.Up else TrendDirection.Down,
                )
            } else {
                SessionComparisonUiModel(
                    label =
                        "${if (delta > 0.0) "Longer" else "Shorter"} " +
                            formatDuration(kotlin.math.abs(delta).toLong()),
                    trend = delta.trendDirection(),
                )
            }
        }
    }
}

private fun TrackingMode.comparisonValue(point: ExercisePoint): Double =
    if (this == TrackingMode.Timed && point.distance.isPositive()) point.paceValue() else point.value

private fun Double?.isPositive(): Boolean = this != null && this > 0.0

private fun Double.signedStat(): String = if (this > 0.0) "+${formatStat()}" else formatStat()

private fun Double.trendDirection(): TrendDirection =
    when {
        this > 0.0 -> TrendDirection.Up
        this < 0.0 -> TrendDirection.Down
        else -> TrendDirection.Flat
    }

private fun List<ExercisePoint>.progressPercentValue(): Double? =
    takeIf { size >= MIN_POINTS_FOR_TREND }
        ?.let { points ->
            val comparisonValues = points.comparisonValues()
            val previous = comparisonValues[comparisonValues.size - 2]
            previous
                .takeIf { value -> value != 0.0 }
                ?.let {
                    ((comparisonValues.last() - previous) / kotlin.math.abs(previous)) * 100.0
                }
        }

private fun List<ExercisePoint>.progressPercent(): String? =
    progressPercentValue()
        ?.let { percentage ->
            val rounded = kotlin.math.round(percentage * 10.0) / 10.0
            val prefix = if (rounded > 0.0) "+" else ""
            "$prefix${rounded.formatStat()}%"
        }

private fun List<ExercisePoint>.progressScore(): Double? =
    progressPercentValue()?.let { percentage ->
        if (last().trackingMode == TrackingMode.Timed && hasPaceData()) {
            -percentage
        } else {
            percentage
        }
    }

private fun List<ExercisePoint>.personalRecordLabel(): String? {
    if (size < MIN_POINTS_FOR_TREND) return null

    val previousBestByMetric =
        dropLast(1)
            .flatMap { point -> point.trackingMode.personalRecordCandidates(point) }
            .groupBy { candidate -> candidate.metric }
            .mapValues { (_, candidates) -> candidates.maxOf { candidate -> candidate.numericValue } }
    val latestPoint = last()
    return latestPoint.trackingMode
        .personalRecordCandidates(latestPoint)
        .any { candidate ->
            val previousBest = previousBestByMetric[candidate.metric]
            previousBest == null || candidate.numericValue > previousBest
        }.takeIf { isRecord -> isRecord }
        ?.let { "PR" }
}

internal fun SetResultEntity.formatSet(trackingMode: TrackingMode): String? =
    when (trackingMode) {
        TrackingMode.Strength -> {
            val reps = reps
            val weight = weight
            if (reps != null && weight != null) "$reps x ${weight.formatStat()}" else null
        }

        TrackingMode.Bodyweight ->
            reps?.let { reps -> "$reps reps" }

        TrackingMode.Timed ->
            durationSeconds?.let { duration ->
                distance?.let { distanceValue ->
                    "${formatDuration(duration)} / ${distanceValue.formatStat()} distance"
                } ?: formatDuration(duration)
            }
    }

private fun List<ExercisePoint>.trend(): TrendDirection {
    if (size < MIN_POINTS_FOR_TREND) return TrendDirection.Unknown

    val comparisonValues = comparisonValues()
    val previous = comparisonValues[comparisonValues.size - 2]
    val latest = comparisonValues.last()

    return if (last().trackingMode == TrackingMode.Timed && hasPaceData()) {
        when {
            latest < previous -> TrendDirection.Up
            latest > previous -> TrendDirection.Down
            else -> TrendDirection.Flat
        }
    } else {
        when {
            latest > previous -> TrendDirection.Up
            latest < previous -> TrendDirection.Down
            else -> TrendDirection.Flat
        }
    }
}

private fun List<ExercisePoint>.hasPaceData(): Boolean =
    all { point -> point.trackingMode == TrackingMode.Timed && point.distance?.let { it > 0.0 } == true }

private fun List<ExercisePoint>.hasCompleteSetPaceData(): Boolean =
    all { point ->
        point.trackingMode == TrackingMode.Timed &&
            point.setResults.isNotEmpty() &&
            point.setResults.all { setResult -> setResult.hasPaceData() }
    }

@Suppress("ktlint:standard:function-expression-body")
private fun SetResultEntity.hasPaceData(): Boolean {
    return durationSeconds?.let { it > 0L } == true &&
        distance?.let { it > 0.0 } == true
}

private fun SetResultEntity.setPaceValue(): Double = durationSeconds!!.toDouble() / distance!!

private fun List<ExercisePoint>.comparisonValues(): List<Double> =
    if (hasPaceData()) {
        map { point -> point.paceValue() }
    } else {
        map { point -> point.value }
    }

private fun ExercisePoint.paceValue(): Double = value / (distance ?: error("Pace requires distance"))

@Suppress("MaxLineLength")
internal fun ExerciseResultEntity.toSelectionKey(deletedScope: DeletedExerciseScope? = null): ExerciseStatsSelectionKey =
    exerciseId
        ?.let { exerciseId ->
            ExerciseStatsSelectionKey(
                exerciseId = exerciseId,
                exerciseName = null,
                trackingMode = trackingModeSnapshot.toTrackingMode(),
                deletedScope = null,
            )
        } ?: ExerciseStatsSelectionKey(
        exerciseId = null,
        exerciseName = exerciseNameSnapshot,
        trackingMode = trackingModeSnapshot.toTrackingMode(),
        deletedScope = deletedScope,
    )

internal fun ExerciseStatsUiModel.toSelectionKey(): ExerciseStatsSelectionKey =
    exerciseId
        ?.let { exerciseId ->
            ExerciseStatsSelectionKey(
                exerciseId = exerciseId,
                exerciseName = null,
                trackingMode = trackingMode,
                deletedScope = deletedScope,
            )
        } ?: ExerciseStatsSelectionKey(
        exerciseId = null,
        exerciseName = exerciseName,
        trackingMode = trackingMode,
        deletedScope = deletedScope,
    )

internal fun ExerciseStatsDetailUiModel.matchesKey(key: ExerciseStatsSelectionKey): Boolean = toSelectionKey() == key

private fun ExerciseStatsDetailUiModel.toSelectionKey(): ExerciseStatsSelectionKey =
    exerciseId
        ?.let { exerciseId ->
            ExerciseStatsSelectionKey(
                exerciseId = exerciseId,
                exerciseName = null,
                trackingMode = trackingMode,
                deletedScope = deletedScope,
            )
        } ?: ExerciseStatsSelectionKey(
        exerciseId = null,
        exerciseName = exerciseName,
        trackingMode = trackingMode,
        deletedScope = deletedScope,
    )

internal fun String.toTrackingMode(): TrackingMode =
    TrackingMode.entries.firstOrNull { trackingMode ->
        trackingMode.databaseValue == this
    } ?: TrackingMode.Bodyweight

internal fun Double.formatStat(): String =
    java.text
        .DecimalFormat(
            "0.##",
            java.text.DecimalFormatSymbols(Locale.US),
        ).format(this)

internal fun ChartValueFormat.format(value: Double): String =
    when (this) {
        ChartValueFormat.Number -> value.formatStat()
        ChartValueFormat.Duration -> formatDuration(kotlin.math.round(value).toLong())
    }

private fun Double.formatChartNumber(): String =
    java.text
        .DecimalFormat(
            "#,##0.##",
            java.text.DecimalFormatSymbols(Locale.US),
        ).format(this)

internal fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "${hours}h ${minutes}m ${seconds}s"
    } else {
        "${minutes}m ${seconds}s"
    }
}

private const val MIN_POINTS_FOR_TREND = 2
private const val MAX_AVERAGE_WORKOUTS = 5
private const val MAX_RECENT_SETS = 6
private const val MAX_RECENT_SESSIONS = 5
private const val MAX_RECENT_NOTES = 5
private val CHART_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d", Locale.US)
