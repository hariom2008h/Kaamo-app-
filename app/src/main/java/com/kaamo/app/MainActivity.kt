package com.kaamo.app

import android.graphics.Color as AndroidColor
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.kaamo.app.ui.navigation.KaamoNavHost
import com.kaamo.app.ui.theme.KaamoColors
import com.kaamo.app.ui.theme.KaamoTheme

class MainActivity : ComponentActivity() {
    private var inviteCode by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inviteCode = inviteFrom(intent)
        // Light-only app: always dark status/navigation icons on the white background.
        val lightBars = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = lightBars, navigationBarStyle = lightBars)
        setContent {
            KaamoTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = KaamoColors.White) {
                    KaamoNavHost(inviteCode = inviteCode)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        inviteFrom(intent)?.let { inviteCode = it }
    }

    /** Invite codes are 12 hex characters; anything else in the link is ignored. */
    private fun inviteFrom(intent: Intent?): String? =
        intent?.data?.getQueryParameter("invite")?.trim()?.takeIf { Regex("[0-9a-fA-F]{12}").matches(it) }
}
