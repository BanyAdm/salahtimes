package com.banyadm.islam

import android.location.Geocoder
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = SalahPreferences(this)

        setContent {
            SalahTheme {
                val setupDone by prefs.isSetupDone.collectAsState(initial = null)
                var showSettings by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                // Lifted state — survives settings navigation
                var cityName by remember { mutableStateOf("") }
                var quranVerse by remember { mutableStateOf<Pair<String, String>?>(null) }

                LaunchedEffect(setupDone) {
                    if (setupDone == true) {
                        // City name
                        scope.launch {
                            val loc = prefs.location.collect { loc ->
                                if (loc != null && cityName.isEmpty()) {
                                    withContext(Dispatchers.IO) {
                                        try {
                                            val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
                                            val addresses = geocoder.getFromLocation(loc.first, loc.second, 1)
                                            if (!addresses.isNullOrEmpty()) {
                                                cityName = addresses[0].locality
                                                    ?: addresses[0].subAdminArea
                                                    ?: addresses[0].adminArea
                                                    ?: ""
                                            }
                                        } catch (e: Exception) {}
                                    }
                                }
                            }
                        }
                        // Quran verse — only once
                        scope.launch {
                            try {
                                val randomSurah = (1..114).random()
                                val url = URL("https://quranapi.pages.dev/api/$randomSurah.json")
                                val conn = url.openConnection() as HttpURLConnection
                                conn.connectTimeout = 5000
                                conn.readTimeout = 5000
                                val response = withContext(Dispatchers.IO) {
                                    conn.inputStream.bufferedReader().readText()
                                }
                                conn.disconnect()
                                val json = JSONObject(response)
                                val totalVerses = json.getInt("totalAyah")
                                val randomAyah = (1..totalVerses).random()
                                val arabic = json.getJSONArray("arabic1").getString(randomAyah - 1)
                                val english = json.getJSONArray("english").getString(randomAyah - 1)
                                val surahName = json.getString("surahName")
                                quranVerse = Pair(arabic, "$english\n— $surahName $randomAyah")
                            } catch (e: Exception) {}
                        }
                    }
                }

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
                                MainScreen(
                                    onSettingsClick = { showSettings = true },
                                    cityName = cityName,
                                    quranVerse = quranVerse
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
