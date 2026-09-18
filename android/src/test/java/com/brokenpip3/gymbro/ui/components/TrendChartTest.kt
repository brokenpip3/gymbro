package com.brokenpip3.gymbro.ui.components

import com.brokenpip3.gymbro.ui.screens.results.ChartPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class TrendChartTest {
    @Test
    fun chartScaleLabelsShowFormattedMaximumAndMinimum() {
        val points =
            listOf(
                ChartPoint(label = "Jan 1", value = 800.0),
                ChartPoint(label = "Jan 8", value = 1_200.0),
            )

        assertEquals(
            listOf("1,200 kg", "800 kg"),
            chartScaleLabels(points) { value ->
                "${value.toInt().toString().reversed().chunked(3).joinToString(",").reversed()} kg"
            },
        )
    }

    @Test
    fun chartScaleLabelsCollapseFlatSeriesToOneLabel() {
        val points = listOf(ChartPoint(label = "Jan 1", value = 10.0), ChartPoint(label = "Jan 8", value = 10.0))

        assertEquals(listOf("10"), chartScaleLabels(points) { value -> value.toInt().toString() })
    }

    @Test
    fun chartBoundaryLabelsExposeFirstAndLastDates() {
        val points =
            listOf(
                ChartPoint(label = "Jan 1", value = 10.0),
                ChartPoint(label = "Jan 2", value = 12.0),
                ChartPoint(label = "Jan 4", value = 11.0),
            )

        assertEquals(listOf("Jan 1", "Jan 4"), chartBoundaryLabels(points))
    }

    @Test
    fun chartBoundaryLabelsAvoidDuplicateSingleDate() {
        val points = listOf(ChartPoint(label = "Jan 1", value = 10.0))

        assertEquals(listOf("Jan 1"), chartBoundaryLabels(points))
        assertEquals(emptyList<String>(), chartBoundaryLabels(emptyList()))
    }

    @Test
    fun nearestChartPointUsesTheClosestHorizontalSession() {
        val points =
            listOf(
                ChartPoint(label = "Jan 1", value = 10.0),
                ChartPoint(label = "Jan 8", value = 14.0),
                ChartPoint(label = "Jan 15", value = 12.0),
            )

        assertEquals(0, nearestChartPointIndex(points, x = 20f, width = 300f))
        assertEquals(1, nearestChartPointIndex(points, x = 160f, width = 300f))
        assertEquals(2, nearestChartPointIndex(points, x = 290f, width = 300f))
        assertEquals(-1, nearestChartPointIndex(emptyList(), x = 100f, width = 300f))
    }

    @Test
    fun trendChartContentDescriptionSummarizesItsRangeAndScale() {
        val points =
            listOf(
                ChartPoint(label = "Jan 1", value = 10.0),
                ChartPoint(label = "Jan 8", value = 14.0),
            )

        assertEquals(
            "Trend chart from Jan 1 to Jan 8. Minimum 10 reps. Maximum 14 reps.",
            trendChartContentDescription(points) { value -> "${value.toInt()} reps" },
        )
        assertEquals("Trend chart with no data.", trendChartContentDescription(emptyList()) { it.toString() })
    }
}
