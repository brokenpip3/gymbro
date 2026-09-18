package com.brokenpip3.gymbro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.brokenpip3.gymbro.ui.screens.results.ChartPoint
import kotlin.math.roundToInt

@Composable
fun TrendChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    height: Dp = 88.dp,
    valueFormatter: (Double) -> String = ::formatDefaultChartValue,
    interactive: Boolean = true,
) {
    val labels = chartBoundaryLabels(points)
    val scaleLabels = chartScaleLabels(points, valueFormatter)
    var selectedPointIndex by remember(points) { mutableStateOf<Int?>(null) }
    val selectedPoint = selectedPointIndex?.let(points::getOrNull)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = trendChartContentDescription(points, valueFormatter)
                },
    ) {
        Row {
            if (scaleLabels.isNotEmpty()) {
                Column(
                    modifier = Modifier.width(CHART_SCALE_WIDTH).height(height),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    scaleLabels.forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = GymbroListTextRole.Metadata.color(MaterialTheme.colorScheme),
                            maxLines = 1,
                        )
                    }
                }
            }
            TrendChartCanvas(
                points = points,
                height = height,
                modifier = Modifier.weight(1f),
                interactive = interactive,
                onPointSelected = { index -> selectedPointIndex = index },
            )
        }

        selectedPoint?.let { point ->
            Text(
                text = "${point.label} · ${valueFormatter(point.value)}",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = CHART_SCALE_WIDTH, top = 4.dp, end = 8.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = GymbroListTextRole.Value.color(MaterialTheme.colorScheme),
            )
        }

        when (labels.size) {
            1 ->
                Text(
                    text = labels.single(),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(start = CHART_SCALE_WIDTH, end = 8.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = GymbroListTextRole.Metadata.color(MaterialTheme.colorScheme),
                )

            2 ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(start = CHART_SCALE_WIDTH, end = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    labels.forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = GymbroListTextRole.Metadata.color(MaterialTheme.colorScheme),
                        )
                    }
                }
        }
    }
}

@Composable
private fun TrendChartCanvas(
    points: List<ChartPoint>,
    height: Dp,
    modifier: Modifier,
    interactive: Boolean,
    onPointSelected: (Int) -> Unit,
) {
    val lineColor = MaterialTheme.colorScheme.secondary
    val areaColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    val markerColor = MaterialTheme.colorScheme.onSurface
    val interactionModifier =
        if (interactive) {
            Modifier.pointerInput(points) {
                detectTapGestures { position ->
                    onPointSelected(
                        nearestChartPointIndex(
                            points = points,
                            x = position.x,
                            width = size.width.toFloat(),
                        ),
                    )
                }
            }
        } else {
            Modifier
        }

    Canvas(
        modifier =
            modifier
                .height(height)
                .then(interactionModifier),
    ) {
        if (points.isEmpty()) return@Canvas

        val horizontalPadding = 8.dp.toPx()
        val topPadding = 8.dp.toPx()
        val bottomPadding = 10.dp.toPx()
        val chartWidth = (size.width - horizontalPadding * 2f).coerceAtLeast(1f)
        val chartHeight = (size.height - topPadding - bottomPadding).coerceAtLeast(1f)
        val values = points.map { point -> point.value }
        val min = values.minOrNull() ?: 0.0
        val max = values.maxOrNull() ?: min
        val range = (max - min).takeIf { it > 0.0 } ?: 1.0
        val stepX = if (points.size == 1) 0f else chartWidth / (points.size - 1)
        val offsets =
            points.mapIndexed { index, point ->
                val normalized = ((point.value - min) / range).toFloat()
                Offset(
                    x = if (points.size == 1) size.width / 2f else horizontalPadding + stepX * index,
                    y = topPadding + chartHeight - (normalized * chartHeight),
                )
            }

        repeat(CHART_GRID_LINES) { index ->
            val y = topPadding + chartHeight * index / (CHART_GRID_LINES - 1)
            drawLine(
                color = gridColor,
                start = Offset(horizontalPadding, y),
                end = Offset(size.width - horizontalPadding, y),
                strokeWidth = 1f,
            )
        }

        if (offsets.size > 1) {
            val areaPath =
                Path().apply {
                    moveTo(offsets.first().x, size.height - bottomPadding)
                    offsets.forEach { offset -> lineTo(offset.x, offset.y) }
                    lineTo(offsets.last().x, size.height - bottomPadding)
                    close()
                }
            drawPath(path = areaPath, color = areaColor)
        }

        offsets.zipWithNext().forEach { (start, end) ->
            drawLine(
                color = lineColor,
                start = start,
                end = end,
                strokeWidth = 4f,
                cap = StrokeCap.Round,
            )
        }

        offsets.forEach { offset ->
            drawCircle(
                color = lineColor,
                radius = 5f,
                center = offset,
            )
            drawCircle(
                color = markerColor,
                radius = 5f,
                center = offset,
                style = Stroke(width = 2f),
            )
        }
    }
}

internal fun chartBoundaryLabels(points: List<ChartPoint>): List<String> {
    if (points.isEmpty()) return emptyList()

    val firstLabel = points.first().label
    val lastLabel = points.last().label
    return if (firstLabel == lastLabel) listOf(firstLabel) else listOf(firstLabel, lastLabel)
}

internal fun chartScaleLabels(
    points: List<ChartPoint>,
    valueFormatter: (Double) -> String,
): List<String> =
    points
        .takeIf { it.isNotEmpty() }
        ?.let { nonEmptyPoints ->
            val values = nonEmptyPoints.map { point -> point.value }
            val minimum = values.minOrNull()
            val maximum = values.maxOrNull()
            if (minimum == null || maximum == null) {
                emptyList()
            } else if (minimum == maximum) {
                listOf(valueFormatter(maximum))
            } else {
                listOf(valueFormatter(maximum), valueFormatter(minimum))
            }
        }.orEmpty()

internal fun nearestChartPointIndex(
    points: List<ChartPoint>,
    x: Float,
    width: Float,
): Int {
    val selectedIndex =
        when {
            points.isEmpty() || width <= 0f -> -1
            points.size == 1 -> 0
            else -> {
                val normalizedX = (x / width).coerceIn(0f, 1f)
                (normalizedX * points.lastIndex).roundToInt().coerceIn(0, points.lastIndex)
            }
        }
    return selectedIndex
}

internal fun trendChartContentDescription(
    points: List<ChartPoint>,
    valueFormatter: (Double) -> String,
): String {
    val values = points.map { point -> point.value }
    val minimum = values.minOrNull()
    val maximum = values.maxOrNull()
    return if (minimum == null || maximum == null) {
        "Trend chart with no data."
    } else {
        val firstLabel = points.first().label
        val lastLabel = points.last().label
        if (firstLabel == lastLabel) {
            "Trend chart for $firstLabel. Minimum ${valueFormatter(minimum)}. " +
                "Maximum ${valueFormatter(maximum)}."
        } else {
            "Trend chart from $firstLabel to $lastLabel. Minimum ${valueFormatter(minimum)}. " +
                "Maximum ${valueFormatter(maximum)}."
        }
    }
}

private const val CHART_GRID_LINES = 3
private val CHART_SCALE_WIDTH = 96.dp

private fun formatDefaultChartValue(value: Double): String =
    java.text
        .DecimalFormat(
            "#,##0.##",
            java.text.DecimalFormatSymbols(java.util.Locale.US),
        ).format(value)
