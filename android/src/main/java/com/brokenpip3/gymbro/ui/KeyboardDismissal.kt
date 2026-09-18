package com.brokenpip3.gymbro.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

internal class KeyboardDismissal(
    private val clearFocus: () -> Unit,
    private val hideKeyboard: () -> Unit,
) {
    fun dismiss() {
        clearFocus()
        hideKeyboard()
    }

    fun runBefore(action: () -> Unit) {
        dismiss()
        action()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun rememberKeyboardDismissal(): KeyboardDismissal {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    return remember(focusManager, keyboardController) {
        KeyboardDismissal(
            clearFocus = { focusManager.clearFocus() },
            hideKeyboard = { keyboardController?.hide() },
        )
    }
}
