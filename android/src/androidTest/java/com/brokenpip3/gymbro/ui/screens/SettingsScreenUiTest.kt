package com.brokenpip3.gymbro.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.brokenpip3.gymbro.ui.theme.GymbroTheme
import com.brokenpip3.gymbro.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun resetHistoryShowsShortConfirmationAndSupportsCancel() {
        var resets = 0
        composeRule.setContent {
            GymbroTheme {
                var confirming by remember { mutableStateOf(false) }
                SettingsScreen(
                    state = SettingsUiState(isHistoryResetConfirmationVisible = confirming),
                    onRequestHistoryReset = { confirming = true },
                    onDismissHistoryReset = { confirming = false },
                    onConfirmHistoryReset = {
                        resets++
                        confirming = false
                    },
                )
            }
        }

        composeRule.onNodeWithText("Reset workout history and statistics").performScrollTo().performClick()
        composeRule.onNodeWithText("This permanently deletes all completed workouts and their statistics.").assertIsDisplayed()
        composeRule.onNodeWithText("Reset").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Reset workout history?").assertDoesNotExist()
        assertEquals(0, resets)
        composeRule.onNodeWithText("Reset workout history and statistics").performScrollTo().performClick()
        composeRule.onNodeWithText("Reset").performClick()
        assertEquals(1, resets)
        composeRule.onNodeWithText("Reset workout history?").assertDoesNotExist()
    }

    @Test
    fun backupFileSectionShowsScopeButtonsAndImport() {
        composeRule.setContent {
            GymbroTheme {
                SettingsScreen(
                    state = SettingsUiState(),
                )
            }
        }

        composeRule.onNodeWithText("Backup file").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Export exercises").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Export exercises + schedules").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Export everything").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Import from file").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun selectingThemeModeUpdatesTheSelectedLabel() {
        composeRule.setContent {
            GymbroTheme {
                var themeMode by remember { mutableStateOf(ThemeMode.System) }
                Column(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(
                        state = SettingsUiState(),
                        themeMode = themeMode,
                        onThemeModeSelected = { themeMode = it },
                        modifier = Modifier.weight(1f),
                    )
                    Text(text = "Selected theme: ${themeMode.label}")
                }
            }
        }

        composeRule.onNodeWithText("Selected theme: System").assertIsDisplayed()
        composeRule.onNodeWithText("Dark").performClick()

        composeRule.onNodeWithText("Selected theme: Dark").assertIsDisplayed()
    }

    @Test
    fun keepScreenOnSettingCanBeEnabled() {
        composeRule.setContent {
            GymbroTheme {
                var keepScreenOn by remember { mutableStateOf(false) }
                SettingsScreen(
                    state = SettingsUiState(),
                    keepScreenOnDuringWorkouts = keepScreenOn,
                    onKeepScreenOnDuringWorkoutsChange = { keepScreenOn = it },
                )
            }
        }

        composeRule.onNodeWithText("Keep screen on during workouts").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("keep-screen-on-workouts").performClick().assertIsOn()
    }

    @Test
    fun backupOperationMessageIsShownInSnackbarHost() {
        composeRule.setContent {
            GymbroTheme {
                SettingsScreen(
                    state = SettingsUiState(message = "Export complete"),
                )
            }
        }

        composeRule
            .onNodeWithText("Export complete")
            .assertIsDisplayed()
            .assert(hasAnyAncestor(hasTestTag("settings-snackbar")))
    }
}
