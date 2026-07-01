package com.gonzalocamera.padelcounter.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gonzalocamera.padelcounter.mobile.ui.ViewModelFactory
import com.gonzalocamera.padelcounter.mobile.ui.navigation.NavGraph
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelMobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as MobileApp
        val factory = ViewModelFactory(app.repository)

        setContent {
            // Dark-only premium theme — ignores the system light/dark setting.
            PadelMobileTheme {
                NavGraph(factory = factory)
            }
        }
    }
}
