package com.simodev.gpspositionsaver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.simodev.gpspositionsaver.ui.GpsApp
import com.simodev.gpspositionsaver.ui.theme.GpsSaverTheme

/** Single activity hosting the Compose UI. Location is handled by [ui.AppViewModel]. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GpsSaverTheme {
                GpsApp()
            }
        }
    }
}
