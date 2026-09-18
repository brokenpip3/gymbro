package com.brokenpip3.gymbro.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.brokenpip3.gymbro.ui.screens.results.ChartPoint
import com.brokenpip3.gymbro.ui.theme.GymbroTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrendChartUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun singlePointChartShowsOneBoundaryLabelAndHasPixels() {
        composeRule.setContent {
            GymbroTheme {
                TrendChart(
                    points = listOf(ChartPoint(label = "Jan 1", value = 100.0)),
                    modifier = Modifier.testTag(CHART_TAG),
                )
            }
        }

        composeRule.onNodeWithTag(CHART_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Jan 1").assertIsDisplayed()
        assertTrue(composeRule.onNodeWithTag(CHART_TAG).captureToImage().width > 0)
    }

    @Test
    fun multiPointChartShowsBothBoundaryLabels() {
        composeRule.setContent {
            GymbroTheme {
                TrendChart(
                    points =
                        listOf(
                            ChartPoint(label = "Jan 1", value = 10.0),
                            ChartPoint(label = "Jan 8", value = 18.0),
                            ChartPoint(label = "Jan 15", value = 12.0),
                        ),
                    modifier = Modifier.testTag(CHART_TAG),
                )
            }
        }

        composeRule.onNodeWithText("Jan 1").assertIsDisplayed()
        composeRule.onNodeWithText("Jan 15").assertIsDisplayed()
    }

    @Test
    fun emptyChartHasNoBoundaryLabels() {
        composeRule.setContent {
            GymbroTheme {
                TrendChart(points = emptyList(), modifier = Modifier.testTag(CHART_TAG))
            }
        }

        composeRule.onNodeWithTag(CHART_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Jan 1").assertDoesNotExist()
    }

    @Test
    fun flatChartStillShowsItsDateRange() {
        composeRule.setContent {
            GymbroTheme {
                TrendChart(
                    points =
                        listOf(
                            ChartPoint(label = "Jan 1", value = 12.0),
                            ChartPoint(label = "Jan 8", value = 12.0),
                        ),
                    modifier = Modifier.testTag(CHART_TAG),
                )
            }
        }

        composeRule.onNodeWithText("Jan 1").assertIsDisplayed()
        composeRule.onNodeWithText("Jan 8").assertIsDisplayed()
    }

    @Test
    fun chartExposesAnAccessibleRangeSummary() {
        composeRule.setContent {
            GymbroTheme {
                TrendChart(
                    points =
                        listOf(
                            ChartPoint(label = "Jan 1", value = 10.0),
                            ChartPoint(label = "Jan 8", value = 14.0),
                        ),
                    valueFormatter = { value -> "${value.toInt()} reps" },
                )
            }
        }

        composeRule
            .onNodeWithContentDescription(
                "Trend chart from Jan 1 to Jan 8. Minimum 10 reps. Maximum 14 reps.",
            ).assertIsDisplayed()
    }

    @Test
    fun tappingChartRevealsTheNearestSessionValue() {
        composeRule.setContent {
            GymbroTheme {
                TrendChart(
                    points =
                        listOf(
                            ChartPoint(label = "Jan 1", value = 10.0),
                            ChartPoint(label = "Jan 8", value = 14.0),
                            ChartPoint(label = "Jan 15", value = 12.0),
                        ),
                    modifier = Modifier.testTag(CHART_TAG),
                )
            }
        }

        composeRule.onNodeWithTag(CHART_TAG).performTouchInput { click(center) }

        composeRule.onNodeWithText("Jan 8 · 14").assertIsDisplayed()
    }
}

private const val CHART_TAG = "exercise-trend-chart"
