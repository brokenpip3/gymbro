package com.brokenpip3.gymbro.ui.theme

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate

internal fun ThemeMode.toAppCompatNightMode(): Int =
    when (this) {
        ThemeMode.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        ThemeMode.Light -> AppCompatDelegate.MODE_NIGHT_NO
        ThemeMode.Dark -> AppCompatDelegate.MODE_NIGHT_YES
    }

internal fun ThemeMode.toApplicationNightMode(): Int =
    when (this) {
        ThemeMode.System -> UiModeManager.MODE_NIGHT_AUTO
        ThemeMode.Light -> UiModeManager.MODE_NIGHT_NO
        ThemeMode.Dark -> UiModeManager.MODE_NIGHT_YES
    }

internal fun applyAppNightMode(
    context: Context,
    themeMode: ThemeMode,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context
            .getSystemService(UiModeManager::class.java)
            .setApplicationNightMode(themeMode.toApplicationNightMode())
    } else {
        AppCompatDelegate.setDefaultNightMode(themeMode.toAppCompatNightMode())
    }
}
