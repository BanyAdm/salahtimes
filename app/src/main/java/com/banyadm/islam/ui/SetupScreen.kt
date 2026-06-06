package com.banyadm.islam.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.banyadm.islam.alarm.AlarmScheduler
import com.banyadm.islam.data.PrayerRepository
import com.banyadm.islam.data.SalahPreferences
import com.banyadm.islam.worker.PrayerSyncWorker
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun SetupScreen(onSetupComplete: () -> Unit) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(0) }
    var statusText by remember { mutableStateOf("") }
    var isFetching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun doFetch() {
        isFetching = true
        statusText = ""
        scope.launch {
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                var lat: Double? = null
                var lon: Double? = null
                val last = fusedClient.lastLocation.await()
                if (last != null) {
                    lat = last.latitude
                    lon = last.longitude
                } else {
                    val loc = withTimeoutOrNull(10_000) {
                        fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
                    }
                    lat = loc?.latitude
                    lon = loc?.longitude
                }
                if (lat != null && lon != null) {
                    val prefs = SalahPreferences(context)
                    prefs.setLocation(lat, lon)
                    val method = prefs.calcMethod.first()
                    val result = PrayerRepository().fetchTimes(lat, lon, method)
                    if (result.isSuccess) {
                        val times = result.getOrThrow()
                        prefs.cacheTimes(times)
                        val toggles = prefs.prayerToggles.first()
                        AlarmScheduler.scheduleAll(context, times, toggles)
                        prefs.setSetupDone(true)
                        PrayerSyncWorker.scheduleTomorrow(context)
                        onSetupComplete()
                    } else {
                        statusText = "Could not fetch prayer times: ${result.exceptionOrNull()?.message}"
                        isFetching = false
                        step = 3
                    }
                } else {
                    statusText = "Could not get location. Make sure GPS is enabled."
                    isFetching = false
                    step = 2
                }
            } catch (e: Exception) {
                statusText = "Error: ${e.message}"
                isFetching = false
                step = 3
            }
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            step = 3
            statusText = ""
        } else {
            statusText = "Location permission is needed."
        }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { step = 2 }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1B2A))
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text("بسم الله", color = Color(0xFFD4AF37), fontSize = 36.sp, fontWeight = FontWeight.Light)
            Text("Salah Times", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            if (isFetching) {
                Text("Fetching prayer times...", color = Color(0xFFB0BEC5), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                repeat(5) {
                    ShimmerSetupCard()
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                when (step) {
                    0 -> {
                        Text(
                            "This app needs a few permissions to deliver prayer alarms reliably.",
                            color = Color(0xFFB0BEC5), textAlign = TextAlign.Center, fontSize = 15.sp
                        )
                        SetupButton("Get Started") { step = 1 }
                    }
                    1 -> {
                        Text(
                            "Step 1 of 4\n\nAllow notifications so prayer alarms can appear on your screen.",
                            color = Color(0xFFB0BEC5), textAlign = TextAlign.Center, fontSize = 15.sp
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            SetupButton("Allow Notifications") {
                                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            SetupButton("Next") { step = 2 }
                        }
                    }
                    2 -> {
                        Text(
                            "Step 2 of 4\n\nAllow location access to calculate accurate prayer times.",
                            color = Color(0xFFB0BEC5), textAlign = TextAlign.Center, fontSize = 15.sp
                        )
                        SetupButton("Allow Location") {
                            locationLauncher.launch(arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ))
                        }
                    }
                    3 -> {
                        Text(
                            "Step 3 of 4\n\nAllow fullscreen alarms so prayer times wake your screen properly — just like a regular alarm clock.",
                            color = Color(0xFFB0BEC5), textAlign = TextAlign.Center, fontSize = 15.sp
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            SetupButton("Allow Fullscreen Alarms") {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                                step = 4
                            }
                            TextButton(onClick = { step = 4 }) {
                                Text("Skip", color = Color(0xFF546E7A), fontSize = 13.sp)
                            }
                        } else {
                            LaunchedEffect(Unit) { step = 4 }
                        }
                    }
                    4 -> {
                        Text(
                            "Step 4 of 4\n\nDisable battery optimization so alarms are never missed.",
                            color = Color(0xFFB0BEC5), textAlign = TextAlign.Center, fontSize = 15.sp
                        )
                        SetupButton("Disable Battery Optimization") {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }
                        SetupButton("Done, Continue") { doFetch() }
                        TextButton(onClick = { doFetch() }) {
                            Text("Skip for now", color = Color(0xFF546E7A), fontSize = 13.sp)
                        }
                    }
                }

                if (statusText.isNotEmpty()) {
                    Text(statusText, color = Color(0xFFEF9A9A), textAlign = TextAlign.Center, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun ShimmerSetupCard() {
    val transition = rememberInfiniteTransition(label = "shimmer")
        val translateAnim by transition.animateFloat(
            initialValue = 0f, targetValue = 1000f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(1200, easing = androidx.compose.animation.core.LinearEasing),
                repeatMode = androidx.compose.animation.core.RepeatMode.Restart
            ), label = "shimmer"
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(Color(0xFF1A2E40), Color(0xFF2A4560), Color(0xFF1A2E40)),
                        start = androidx.compose.ui.geometry.Offset(translateAnim - 200f, 0f),
                        end = androidx.compose.ui.geometry.Offset(translateAnim, 0f)
                    ),
                    RoundedCornerShape(12.dp)
                )
        )
}

@Composable
fun SetupButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
    ) {
        Text(text, color = Color(0xFF0D1B2A), fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}
