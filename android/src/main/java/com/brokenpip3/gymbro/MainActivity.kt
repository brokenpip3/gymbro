package com.brokenpip3.gymbro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.brokenpip3.gymbro.ui.GymbroApp
import com.brokenpip3.gymbro.ui.theme.GymbroTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val gymbroApplication = application as GymbroApplication
        setContent {
            val themeMode by gymbroApplication.themeSettings.themeMode.collectAsState()
            GymbroTheme(darkTheme = themeMode.isDark(isSystemInDarkTheme())) {
                GymbroApp()
            }
        }
    }
}
