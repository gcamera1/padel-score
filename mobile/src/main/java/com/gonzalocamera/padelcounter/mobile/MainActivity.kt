package com.gonzalocamera.padelcounter.mobile

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gonzalocamera.padelcounter.mobile.ui.ViewModelFactory
import com.gonzalocamera.padelcounter.mobile.ui.navigation.NavGraph
import com.gonzalocamera.padelcounter.mobile.ui.theme.PadelMobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // La app es dark-only e ignora el tema del sistema, así que fijamos las barras
        // como oscuras en vez de usar `auto`: con el sistema en claro, `auto` pondría
        // iconos oscuros sobre nuestro fondo negro. Va acá y no en un SideEffect del
        // tema para que se aplique antes del primer frame.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

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
