package com.kaamo.app

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.kaamo.app.ui.navigation.KaamoNavHost
import com.kaamo.app.ui.theme.KaamoColors
import com.kaamo.app.ui.theme.KaamoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Light-only app: always dark status/navigation icons on the white background.
        val lightBars = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = lightBars, navigationBarStyle = lightBars)
        setContent {
            KaamoTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = KaamoColors.White) {
                    KaamoNavHost()
                }
            }
        }
    }
}
