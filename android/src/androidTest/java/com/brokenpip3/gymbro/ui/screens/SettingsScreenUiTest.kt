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
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.brokenpip3.gymbro.ui.theme.GymbroTheme
import com.brokenpip3.gymbro.ui.theme.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun backupFileSectionShowsScopeButtonsAndImport() {
        composeRule.setContent {
            GymbroTheme {
                SettingsScreen(
                    state = SettingsUiState(),
                )
            }
        }

        composeRule.onNodeWithText("Backup file").assertIsDisplayed()
        composeRule.onNodeWithText("Export exercises").assertIsDisplayed()
        composeRule.onNodeWithText("Export exercises + schedules").assertIsDisplayed()
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
