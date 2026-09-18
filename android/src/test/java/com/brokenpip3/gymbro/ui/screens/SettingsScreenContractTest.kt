package com.brokenpip3.gymbro.ui.screens

import androidx.compose.runtime.Composable
import org.junit.Test

class SettingsScreenContractTest {
    @Test
    fun signaturesCompile() = Unit
}

@Composable
private fun SettingsScreenSignatureContract() {
    SettingsScreen(
        state = SettingsUiState(message = "Import complete"),
    )
}
