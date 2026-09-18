@file:Suppress("TooManyFunctions")

package com.brokenpip3.gymbro.ui.screens.results

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.brokenpip3.gymbro.domain.TrackingMode
import com.brokenpip3.gymbro.ui.components.EmptyState
import com.brokenpip3.gymbro.ui.components.GymbroIcons
import com.brokenpip3.gymbro.ui.components.GymbroListTextRole
import com.brokenpip3.gymbro.ui.components.MetricTile
import com.brokenpip3.gymbro.ui.components.SectionHeader
import com.brokenpip3.gymbro.ui.components.TrendChart
import com.brokenpip3.gymbro.ui.components.color
import com.brokenpip3.gymbro.ui.rememberKeyboardDismissal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ResultsRoute(
    repository: ResultsSource,
    initialExerciseId: Long? = null,
    onDismissExerciseDetail: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val viewModel: ResultsViewModel =
        viewModel(factory = ResultsViewModelFactory(repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(initialExerciseId, uiState.exerciseStats) {
        initialExerciseId
            ?.let { exerciseId ->
                uiState.exerciseStats
                    .firstOrNull { stat -> stat.exerciseId == exerciseId }
                    ?.let(viewModel::selectExerciseStats)
            }
    }

    ResultsScreen(
        uiState = uiState,
        onExerciseSelected = viewModel::selectExerciseStats,
        onDismissExerciseDetail = {
            if (initialExerciseId == null) {
                viewModel.dismissExerciseDetail()
            } else {
                onDismissExerciseDetail()
            }
        },
        onWorkoutSelected = viewModel::selectWorkout,
        onDismissWorkoutDetail = viewModel::dismissWorkoutDetail,
        onUpdateExerciseNotes = viewModel::updateExerciseNotes,
        onUpdateSetNotes = viewModel::updateSetNotes,
        onUpdateWorkoutNotes = viewModel::updateWorkoutNotes,
        onStatsRangeSelected = viewModel::selectStatsRange,
        modifier = modifier,
    )
}

@Composable
@Suppress("LongMethod", "LongParameterList", "CyclomaticComplexMethod")
fun ResultsScreen(
    uiState: ResultsUiState,
    modifier: Modifier = Modifier,
    onExerciseSelected: (ExerciseStatsUiModel) -> Unit = {},
    onDismissExerciseDetail: () -> Unit = {},
    onWorkoutSelected: (WorkoutSummaryUiModel) -> Unit = {},
    onDismissWorkoutDetail: () -> Unit = {},
    onUpdateExerciseNotes: (Long, String) -> Unit = { _, _ -> },
    onUpdateSetNotes: (Long, String) -> Unit = { _, _ -> },
    onUpdateWorkoutNotes: (Long, String) -> Unit = { _, _ -> },
    onStatsRangeSelected: (ResultsStatsRange) -> Unit = {},
) {
    val hasVisibleResults = uiState.recentWorkouts.isNotEmpty() || uiState.exerciseStats.isNotEmpty()
    if (!hasVisibleResults && !uiState.hasHistoricalData) {
        EmptyState(
            icon = GymbroIcons.Results,
            title = "No results yet",
            body = "Completed workouts will appear here with your insights.",
            actionLabel = null,
            onAction = null,
            modifier = modifier.fillMaxSize(),
        )
        return
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilterName by rememberSaveable { mutableStateOf(ExerciseStatsFilter.All.name) }
    var selectedSortName by rememberSaveable { mutableStateOf(ExerciseStatsSort.Recent.name) }
    var noteEditRequest by remember { mutableStateOf<ResultNoteEditRequest?>(null) }
    val selectedFilter =
        ExerciseStatsFilter.entries.firstOrNull { filter -> filter.name == selectedFilterName }
            ?: ExerciseStatsFilter.All
    val selectedSort =
        ExerciseStatsSort.entries.firstOrNull { sort -> sort.name == selectedSortName }
            ?: ExerciseStatsSort.Recent
    val visibleExerciseStats =
        filterAndSortExerciseStats(
            stats = uiState.exerciseStats,
            query = searchQuery,
            filter = selectedFilter,
            sort = selectedSort,
        )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (hasVisibleResults) {
                    ResultsOverview(uiState = uiState)
                }
                if (uiState.insights.isNotEmpty()) {
                    ResultsInsights(insights = uiState.insights)
                }
                if (uiState.hasHistoricalData || hasVisibleResults) {
                    ResultsStatsRangeSelector(
                        selectedRange = uiState.statsRange,
                        onRangeSelected = onStatsRangeSelected,
                    )
                }
                if (!hasVisibleResults && uiState.hasHistoricalData) {
                    ResultsRangeEmptyState(range = uiState.statsRange)
                }
            }
        }

        if (uiState.recentWorkouts.isNotEmpty()) {
            item {
                SectionHeader(text = "Recent workouts")
            }
            items(uiState.recentWorkouts) { workout ->
                WorkoutSummaryRow(
                    workout = workout,
                    onClick = { onWorkoutSelected(workout) },
                )
            }
        }

        if (uiState.exerciseStats.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExerciseStatsSearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                    )
                    ExerciseStatsFilters(
                        selectedFilter = selectedFilter,
                        selectedSort = selectedSort,
                        onFilterSelected = { selectedFilterName = it.name },
                        onSortSelected = { selectedSortName = it.name },
                    )
                    SectionHeader(text = "Exercise stats")
                    if (visibleExerciseStats.isEmpty()) {
                        Text(
                            text = "No exercise stats match your search.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(visibleExerciseStats) { stat ->
                ExerciseStatsRow(
                    stat = stat,
                    onClick = { onExerciseSelected(stat) },
                )
            }
        }
    }

    uiState.selectedExerciseDetail?.let { detail ->
        ExerciseStatsDetailDialog(
            detail = detail,
            onDismiss = onDismissExerciseDetail,
        )
    }

    uiState.selectedWorkoutDetail?.let { detail ->
        WorkoutDetailDialog(
            detail = detail,
            onDismiss = onDismissWorkoutDetail,
            onEditWorkoutNotes = {
                noteEditRequest =
                    ResultNoteEditRequest(
                        target = ResultNoteTarget.Workout,
                        id = detail.runId,
                        title = "Workout notes",
                        initialNotes = detail.notes,
                    )
            },
            onEditExerciseNotes = { exercise ->
                noteEditRequest =
                    ResultNoteEditRequest(
                        target = ResultNoteTarget.Exercise,
                        id = exercise.exerciseResultId,
                        title = "Exercise notes",
                        initialNotes = exercise.notes,
                    )
            },
            onEditSetNotes = { set ->
                noteEditRequest =
                    ResultNoteEditRequest(
                        target = ResultNoteTarget.Set,
                        id = set.setId,
                        title = "Set ${set.setNumber} notes",
                        initialNotes = set.notes,
                    )
            },
        )
    }

    noteEditRequest?.let { request ->
        ResultNoteEditDialog(
            request = request,
            onDismiss = { noteEditRequest = null },
            onSave = { notes ->
                when (request.target) {
                    ResultNoteTarget.Workout -> onUpdateWorkoutNotes(request.id, notes)
                    ResultNoteTarget.Exercise -> onUpdateExerciseNotes(request.id, notes)
                    ResultNoteTarget.Set -> onUpdateSetNotes(request.id, notes)
                }
                noteEditRequest = null
            },
        )
    }
}

@Composable
private fun ResultsStatsRangeSelector(
    selectedRange: ResultsStatsRange,
    onRangeSelected: (ResultsStatsRange) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Stats range",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ResultsStatsRange.entries.forEach { range ->
                FilterChip(
                    selected = range == selectedRange,
                    onClick = { onRangeSelected(range) },
                    label = { Text(text = range.label) },
                    modifier =
                        Modifier
                            .weight(1f)
                            .testTag("results-range-${range.name}"),
                )
            }
        }
    }
}

@Composable
private fun ResultsRangeEmptyState(range: ResultsStatsRange) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
    ) {
        Text(
            text = "No completed workouts in ${range.label.lowercase()}.",
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExerciseStatsSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val keyboardDismissal = rememberKeyboardDismissal()

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().testTag("exercise-stats-search"),
        label = { Text(text = "Search exercise stats") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { keyboardDismissal.dismiss() }),
        trailingIcon = {
            if (query.isNotEmpty()) {
                TextButton(onClick = { onQueryChange("") }) {
                    Text(text = "Clear")
                }
            }
        },
    )
}

@Composable
private fun ExerciseStatsFilters(
    selectedFilter: ExerciseStatsFilter,
    selectedSort: ExerciseStatsSort,
    onFilterSelected: (ExerciseStatsFilter) -> Unit,
    onSortSelected: (ExerciseStatsSort) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Track",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(ExerciseStatsFilter.entries) { filter ->
                FilterChip(
                    selected = filter == selectedFilter,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(text = filter.label) },
                    modifier = Modifier.testTag("exercise-stats-filter-${filter.name}"),
                )
            }
        }
        Text(
            text = "Sort",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(ExerciseStatsSort.entries) { sort ->
                FilterChip(
                    selected = sort == selectedSort,
                    onClick = { onSortSelected(sort) },
                    label = { Text(text = sort.label) },
                    modifier = Modifier.testTag("exercise-stats-sort-${sort.name}"),
                )
            }
        }
    }
}

internal fun filterExerciseStats(
    stats: List<ExerciseStatsUiModel>,
    query: String,
): List<ExerciseStatsUiModel> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isBlank()) return stats

    return stats.filter { stat ->
        stat.exerciseName.contains(normalizedQuery, ignoreCase = true)
    }
}

@Composable
private fun ResultsOverview(uiState: ResultsUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricTile(
                    label = "Workouts",
                    value = uiState.recentWorkouts.size.toString(),
                    modifier = Modifier.weight(1f),
                )
                MetricTile(
                    label = "Sets",
                    value = uiState.recentWorkouts.sumOf { workout -> workout.completedSetCount }.toString(),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricTile(
                    label = "Time",
                    value =
                        formatWorkoutDuration(
                            uiState.recentWorkouts.sumOf { workout -> workout.durationSeconds },
                        ),
                    modifier = Modifier.weight(1f),
                )
                MetricTile(
                    label = "Records",
                    value = uiState.exerciseStats.count { stat -> stat.personalRecordLabel != null }.toString(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ResultsInsights(insights: List<ResultsInsightUiModel>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(text = "Insights")
        insights.forEachIndexed { index, insight ->
            ResultsInsightRow(
                insight = insight,
                modifier = Modifier.testTag("results-insight-$index"),
            )
        }
    }
}

@Composable
private fun ResultsInsightRow(
    insight: ResultsInsightUiModel,
    modifier: Modifier = Modifier,
) {
    val isPositive = insight.tone == ResultsInsightTone.Positive
    val contentColor =
        if (isPositive) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    val containerColor =
        if (isPositive) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
        }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = if (isPositive) GymbroIcons.ArrowUp else GymbroIcons.Results,
                contentDescription = null,
                modifier = Modifier.padding(top = 2.dp),
                tint = contentColor,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = insight.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor,
                )
            }
        }
    }
}

@Composable
private fun WorkoutSummaryRow(
    workout: WorkoutSummaryUiModel,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .testTag("workout-summary-${workout.runId}"),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workout.scheduleName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "${workout.startedAt.formatResultDate()} · ${workout.completedSetCount} sets",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = formatWorkoutDuration(workout.durationSeconds),
                style = MaterialTheme.typography.bodyMedium,
                color = GymbroListTextRole.Value.color(MaterialTheme.colorScheme),
            )
        }
    }
}

@Composable
private fun ExerciseStatsRow(
    stat: ExerciseStatsUiModel,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .testTag("exercise-stat-${stat.exerciseId ?: stat.exerciseName}"),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stat.exerciseName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = stat.trackingMode.resultLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = GymbroListTextRole.Stat.color(MaterialTheme.colorScheme),
                    )
                    Text(
                        text = stat.headline,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text =
                            buildString {
                                append("${stat.points.size} ${if (stat.points.size == 1) "session" else "sessions"}")
                                if (stat.lastTrainedAt > 0L) {
                                    append(" · Last ${stat.lastTrainedAt.formatResultDate()}")
                                }
                            },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text =
                            listOfNotNull(stat.progressDelta, stat.personalRecordLabel)
                                .joinToString(separator = " / "),
                        style = MaterialTheme.typography.labelMedium,
                        color = GymbroListTextRole.Stat.color(MaterialTheme.colorScheme),
                    )
                }
                TrendBadge(trend = stat.trend)
            }
            TrendChart(points = stat.points, height = 96.dp, interactive = false)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ExerciseStatsDetailDialog(
    detail: ExerciseStatsDetailUiModel,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        modifier = Modifier.testTag("exercise-stats-detail"),
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        ExerciseStatsDetailContent(detail = detail, onDismiss = onDismiss)
    }
}

@Composable
private fun ExerciseStatsDetailContent(
    detail: ExerciseStatsDetailUiModel,
    onDismiss: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ExerciseStatsDetailHeader(detail = detail)
        ExerciseStatsTrendSection(detail = detail)
        ExerciseStatsOptionalSections(detail = detail)
        ExerciseStatsMetricSection(detail = detail)
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(text = "Close")
        }
    }
}

@Composable
private fun ExerciseStatsDetailHeader(detail: ExerciseStatsDetailUiModel) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = detail.exerciseName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = detail.trackingMode.resultLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExerciseStatsTrendSection(detail: ExerciseStatsDetailUiModel) {
    SectionHeader(text = "Trend")
    ChartSection(
        title = detail.trackingMode.primaryChartLabel,
        points = detail.points,
        valueFormatter = detail.trackingMode::formatChartScaleValue,
    )
    detail.secondaryChart?.let { chart ->
        ChartSection(title = chart.title, points = chart.points)
    }
    detail.additionalCharts.forEach { chart ->
        ChartSection(
            title = chart.title,
            points = chart.points,
            valueFormatter = chart.valueFormat::format,
        )
    }
    detail.chartSummary?.let { summary ->
        ChartSummaryRow(summary = summary)
    }
}

@Composable
private fun ExerciseStatsOptionalSections(detail: ExerciseStatsDetailUiModel) {
    if (detail.summaryMetrics.isNotEmpty()) {
        SectionHeader(text = "Summary")
        SummaryMetricGrid(metrics = detail.summaryMetrics)
    }
    if (detail.recentSessions.isNotEmpty()) {
        SectionHeader(text = "Recent sessions")
        detail.recentSessions.forEach { session ->
            RecentSessionRow(session = session)
        }
    }
    if (detail.noteDetails.isNotEmpty()) {
        SectionHeader(text = "Recent notes")
        detail.noteDetails.forEach { note ->
            ExerciseNoteRow(note = note)
        }
    } else if (detail.notes.isNotEmpty()) {
        SectionHeader(text = "Recent notes")
        detail.notes.forEach { note ->
            Text(text = note, style = MaterialTheme.typography.bodyMedium)
        }
    }
    if (detail.personalRecords.isNotEmpty()) {
        SectionHeader(text = "Personal records")
        detail.personalRecords.asReversed().forEach { record ->
            PersonalRecordRow(record = record)
        }
    }
}

@Composable
private fun ExerciseNoteRow(note: ExerciseNoteUiModel) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = note.contextLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = GymbroListTextRole.Notes.color(MaterialTheme.colorScheme),
                )
                Text(
                    text = note.dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(text = note.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ExerciseStatsMetricSection(detail: ExerciseStatsDetailUiModel) {
    DetailMetricRow(label = "Latest", value = detail.latest)
    DetailMetricRow(label = "Change", value = detail.progressDelta)
    detail.progressPercent?.let { percentage ->
        DetailMetricRow(label = "Relative change", value = percentage)
    }
    detail.personalRecordLabel?.let { label ->
        DetailMetricRow(label = "Record", value = label)
    }
    DetailMetricRow(label = "Best", value = detail.best)
    DetailMetricRow(label = "Best session", value = detail.bestSession)
    detail.bestEstimatedOneRepMax?.let { oneRepMax ->
        DetailMetricRow(label = "Estimated 1RM", value = oneRepMax)
    }
    DetailMetricRow(label = "Average", value = detail.average)
    Text(
        text = "${detail.totalSessions} sessions / ${detail.totalSets} sets",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (detail.recentSets.isNotEmpty()) {
        SectionHeader(text = "Recent sets")
        detail.recentSets.forEach { set ->
            DetailMetricRow(label = set.label, value = set.value)
        }
    }
}

@Composable
private fun WorkoutDetailDialog(
    detail: WorkoutDetailUiModel,
    onDismiss: () -> Unit,
    onEditWorkoutNotes: () -> Unit,
    onEditExerciseNotes: (WorkoutExerciseDetailUiModel) -> Unit,
    onEditSetNotes: (WorkoutSetDetailUiModel) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(text = detail.scheduleName)
                    Text(
                        text =
                            "${detail.startedAt.formatResultDate()} · " +
                                formatWorkoutDuration(detail.durationSeconds),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = onEditWorkoutNotes,
                    modifier = Modifier.testTag("edit-workout-notes-${detail.runId}"),
                ) {
                    Icon(
                        imageVector = GymbroIcons.Edit,
                        contentDescription =
                            if (detail.notes.isNullOrBlank()) {
                                "Add workout notes"
                            } else {
                                "Edit workout notes"
                            },
                    )
                }
            }
        },
        text = {
            Column(
                modifier =
                    Modifier
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                detail.notes?.takeIf { notes -> notes.isNotBlank() }?.let { notes ->
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                detail.exercises.forEach { exercise ->
                    WorkoutExerciseDetailSection(
                        exercise = exercise,
                        onEditExerciseNotes = { onEditExerciseNotes(exercise) },
                        onEditSetNotes = onEditSetNotes,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close")
            }
        },
    )
}

@Composable
private fun WorkoutExerciseDetailSection(
    exercise: WorkoutExerciseDetailUiModel,
    onEditExerciseNotes: () -> Unit,
    onEditSetNotes: (WorkoutSetDetailUiModel) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = exercise.exerciseName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = exercise.trackingMode.resultLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = onEditExerciseNotes,
                    modifier = Modifier.testTag("edit-exercise-notes-${exercise.exerciseResultId}"),
                ) {
                    Icon(
                        imageVector = GymbroIcons.Edit,
                        contentDescription =
                            if (exercise.notes.isNullOrBlank()) {
                                "Add notes to ${exercise.exerciseName}"
                            } else {
                                "Edit notes for ${exercise.exerciseName}"
                            },
                    )
                }
            }
            exercise.notes?.takeIf { notes -> notes.isNotBlank() }?.let { notes ->
                Text(text = notes, style = MaterialTheme.typography.bodySmall)
            }
            if (exercise.sets.isEmpty()) {
                Text(
                    text = "No sets logged",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                exercise.sets.forEach { set ->
                    WorkoutSetDetailRow(
                        set = set,
                        onEditNotes = { onEditSetNotes(set) },
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutSetDetailRow(
    set: WorkoutSetDetailUiModel,
    onEditNotes: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Set ${set.setNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = set.value ?: "Not logged",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = if (set.isCompleted) "Done" else "Open",
                    style = MaterialTheme.typography.labelSmall,
                    color =
                        if (set.isCompleted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
            set.notes?.takeIf { notes -> notes.isNotBlank() }?.let { notes ->
                Text(
                    text = notes,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(
            onClick = onEditNotes,
            modifier = Modifier.testTag("edit-set-notes-${set.setId}"),
        ) {
            Icon(
                imageVector = GymbroIcons.Edit,
                contentDescription =
                    if (set.notes.isNullOrBlank()) {
                        "Add notes to set ${set.setNumber}"
                    } else {
                        "Edit notes for set ${set.setNumber}"
                    },
            )
        }
    }
}

private enum class ResultNoteTarget {
    Workout,
    Exercise,
    Set,
}

private data class ResultNoteEditRequest(
    val target: ResultNoteTarget,
    val id: Long,
    val title: String,
    val initialNotes: String?,
)

@Composable
private fun ResultNoteEditDialog(
    request: ResultNoteEditRequest,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var notes by remember(request.target, request.id) { mutableStateOf(request.initialNotes.orEmpty()) }
    val keyboardDismissal = rememberKeyboardDismissal()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = request.title) },
        text = {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(text = "Notes") },
                modifier = Modifier.fillMaxWidth().testTag("result-note-input"),
                minLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions =
                    KeyboardActions(
                        onDone = { keyboardDismissal.dismiss() },
                    ),
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(notes) }) {
                Text(text = "Save")
            }
        },
    )
}

@Composable
private fun TrendBadge(trend: TrendDirection) {
    val (label, containerColor, contentColor) = trend.badgeColors
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = containerColor,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ChartSection(
    title: String,
    points: List<ChartPoint>,
    valueFormatter: (Double) -> String = { value -> value.formatStat() },
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
        )
        TrendChart(
            points = points,
            height = 128.dp,
            valueFormatter = valueFormatter,
        )
    }
}

@Composable
private fun PersonalRecordRow(record: PersonalRecordUiModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.metric,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = record.dateLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = record.value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun RecentSessionRow(session: RecentSessionUiModel) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = session.scheduleName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = session.dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = session.value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                session.comparison?.let { comparison ->
                    Text(
                        text = comparison.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = comparison.trend.sessionComparisonColor,
                    )
                }
                Text(
                    text = "${session.setCount} sets",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ChartSummaryRow(summary: ChartSummaryUiModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = summary.rangeLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${summary.minLabel} / ${summary.maxLabel}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SummaryMetricGrid(metrics: List<SummaryMetricUiModel>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        summaryMetricRows(metrics).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { metric ->
                    SummaryMetricTile(
                        metric = metric,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricTile(
    metric: SummaryMetricUiModel,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
    ) {
        MetricTile(
            label = metric.label,
            value = metric.value,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

private typealias SummaryMetricRows = List<List<SummaryMetricUiModel>>

internal fun summaryMetricRows(metrics: List<SummaryMetricUiModel>): SummaryMetricRows = metrics.chunked(2)

@Composable
private fun DetailMetricRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = GymbroListTextRole.Value.color(MaterialTheme.colorScheme),
        )
    }
}

private val TrendDirection.badgeColors: Triple<String, Color, Color>
    @Composable
    get() =
        when (this) {
            TrendDirection.Up ->
                Triple(
                    "Up",
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer,
                )
            TrendDirection.Flat ->
                Triple(
                    "Flat",
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.onSecondaryContainer,
                )
            TrendDirection.Down ->
                Triple(
                    "Down",
                    MaterialTheme.colorScheme.errorContainer,
                    MaterialTheme.colorScheme.onErrorContainer,
                )
            TrendDirection.Unknown ->
                Triple("New", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        }

private val TrendDirection.sessionComparisonColor: Color
    @Composable
    get() =
        when (this) {
            TrendDirection.Up -> MaterialTheme.colorScheme.primary
            TrendDirection.Down -> MaterialTheme.colorScheme.error
            TrendDirection.Flat,
            TrendDirection.Unknown,
            -> MaterialTheme.colorScheme.onSurfaceVariant
        }

private val TrackingMode.primaryChartLabel: String
    get() =
        when (this) {
            TrackingMode.Strength -> "Volume"
            TrackingMode.Bodyweight -> "Total reps"
            TrackingMode.Timed -> "Total time"
        }

private val TrackingMode.resultLabel: String
    get() =
        when (this) {
            TrackingMode.Strength -> "Strength"
            TrackingMode.Bodyweight -> "Bodyweight"
            TrackingMode.Timed -> "Timed"
        }

private fun formatWorkoutDuration(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return if (minutes > 0) {
        "${minutes}m ${remainingSeconds}s"
    } else {
        "${remainingSeconds}s"
    }
}

private val RESULT_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy")

@Suppress("ktlint:standard:function-expression-body")
private fun Long.formatResultDate(): String {
    val instant = Instant.ofEpochMilli(this)
    val zonedDate = instant.atZone(ZoneId.systemDefault())
    return RESULT_DATE_FORMATTER.format(zonedDate)
}

private class ResultsViewModelFactory(
    private val repository: ResultsSource,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ResultsViewModel(repository) as T
}
