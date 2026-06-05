package com.banyadm.islam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.banyadm.islam.data.SalahPreferences
import com.banyadm.islam.ui.MainScreen
import com.banyadm.islam.ui.SetupScreen
import com.banyadm.islam.ui.SettingsScreen
import com.banyadm.islam.ui.theme.SalahTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = SalahPreferences(this)
        setContent {
            SalahTheme {
                val setupDone by prefs.isSetupDone.collectAsState(initial = null)
                var showSettings by remember { mutableStateOf(false) }

                when {
                    setupDone == null -> {}
                    !setupDone!! -> SetupScreen(onSetupComplete = {})
                    showSettings -> SettingsScreen(onBack = { showSettings = false })
                    else -> MainScreen(onSettingsClick = { showSettings = true })
                }
            }
        }
    }
}
