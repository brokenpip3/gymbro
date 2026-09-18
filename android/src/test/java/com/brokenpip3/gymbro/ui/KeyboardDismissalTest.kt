package com.brokenpip3.gymbro.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardDismissalTest {
    @Test
    fun dismissRunsFocusAndKeyboardCallbacksBeforeSubmitAction() {
        val calls = mutableListOf<String>()
        val dismissal =
            KeyboardDismissal(
                clearFocus = { calls += "focus" },
                hideKeyboard = { calls += "keyboard" },
            )

        dismissal.runBefore {
            calls += "action"
        }

        assertEquals(listOf("focus", "keyboard", "action"), calls)
    }

    @Test
    fun dismissOnlyRunsFocusAndKeyboardCallbacks() {
        val calls = mutableListOf<String>()
        val dismissal =
            KeyboardDismissal(
                clearFocus = { calls += "focus" },
                hideKeyboard = { calls += "keyboard" },
            )

        dismissal.dismiss()

        assertEquals(listOf("focus", "keyboard"), calls)
    }
}
