package com.brokenpip3.gymbro.ui.screens

import androidx.compose.runtime.Composable
import com.brokenpip3.gymbro.ui.theme.ThemeMode
import com.brokenpip3.gymbro.ui.theme.ThemePalette
import org.junit.Test

class SettingsScreenContractTest {
    @Test
    fun signaturesCompile() = Unit
}

@Composable
private fun SettingsColorsSectionContract() {
    SettingsScreen(
        state = SettingsUiState(),
        themeMode = ThemeMode.Dark,
        onThemeModeSelected = {},
        themePalette = ThemePalette.Ocean,
        onThemePaletteSelected = {},
    )
}

@Composable
private fun SettingsScreenSignatureContract() {
    SettingsScreen(
        state = SettingsUiState(message = "Import complete"),
    )
}

@Composable
private fun SettingsExercisesSectionContract() {
    SettingsScreen(
        state = SettingsUiState(),
        groupByCategory = true,
        onGroupByCategoryChange = {},
    )
}
