package com.brokenpip3.gymbro.ui.screens.results

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ExerciseStatsCalculatorTest {
    @Test
    fun chartDateUsesTheRequestedTimeZone() {
        val timestamp = Instant.parse("2026-01-01T23:30:00Z").toEpochMilli()

        assertEquals("Jan 1", formatResultChartDate(timestamp, ZoneId.of("UTC")))
        assertEquals("Jan 2", formatResultChartDate(timestamp, ZoneId.of("Europe/Rome")))
    }

    @Test
    fun statNumbersHideFloatingPointArtifacts() {
        assertEquals("0.3", (0.1 + 0.2).formatStat())
        assertEquals("1234.57", 1234.567.formatStat())
    }
}
