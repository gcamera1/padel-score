package com.gonzalocamera.padelcounter.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.gonzalocamera.padelcounter.mobile.data.UserPreferences
import com.gonzalocamera.padelcounter.mobile.ui.ViewModelFactory
import com.gonzalocamera.padelcounter.mobile.ui.navigation.NavGraph
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelMobileTheme
import com.gonzalocamera.padelcounter.shared.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as MobileApp
        val factory = ViewModelFactory(app.repository)
        val preferencesFlow = app.preferences.userPreferences

        setContent {
            val prefs by preferencesFlow.collectAsState(initial = UserPreferences())
            val darkTheme = when (prefs.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            PadelMobileTheme(darkTheme = darkTheme) {
                NavGraph(factory = factory)
            }
        }
    }
}
