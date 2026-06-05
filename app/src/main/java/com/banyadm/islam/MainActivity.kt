package com.banyadm.islam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
                    else -> {
                        AnimatedContent(
                            targetState = showSettings,
                            transitionSpec = {
                                if (targetState) {
                                    slideInHorizontally(
                                        initialOffsetX = { it },
                                        animationSpec = tween(350)
                                    ) togetherWith slideOutHorizontally(
                                        targetOffsetX = { -it / 3 },
                                        animationSpec = tween(350)
                                    )
                                } else {
                                    slideInHorizontally(
                                        initialOffsetX = { -it / 3 },
                                        animationSpec = tween(350)
                                    ) togetherWith slideOutHorizontally(
                                        targetOffsetX = { it },
                                        animationSpec = tween(350)
                                    )
                                }
                            },
                            label = "nav"
                        ) { inSettings ->
                            if (inSettings) {
                                SettingsScreen(onBack = { showSettings = false })
                            } else {
                                MainScreen(onSettingsClick = { showSettings = true })
                            }
                        }
                    }
                }
            }
        }
    }
}
