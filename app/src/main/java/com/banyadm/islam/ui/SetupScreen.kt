package com.banyadm.islam.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.android.gms.location.LocationServices

@Composable
fun SetupScreen(onSetupComplete: () -> Unit) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(0) }
    var statusText by remember { mutableStateOf("") }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            step = 2
        } else {
            statusText = "Location permission is needed to calculate prayer times."
        }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        locationLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1B2A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "بسم الله",
                color = Color(0xFFD4AF37),
                fontSize = 36.sp,
                fontWeight = FontWeight.Light
            )
            Text(
                text = "Salah Times",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            when (step) {
                0 -> {
                    Text(
                        text = "This app needs a few permissions to deliver prayer alarms reliably.",
                        color = Color(0xFFB0BEC5),
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp
                    )
                    SetupButton("Get Started") { step = 1 }
                }
                1 -> {
                    Text(
                        text = "Step 1 of 3\n\nAllow notifications so prayer alarms can appear on your screen.",
                        color = Color(0xFFB0BEC5),
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        SetupButton("Allow Notifications") {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    } else {
                        step = 2
                    }
                }
                2 -> {
                    Text(
                        text = "Step 2 of 3\n\nAllow location access to calculate accurate prayer times for your area.",
                        color = Color(0xFFB0BEC5),
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp
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
                        text = "Step 3 of 3\n\nDisable battery optimization so alarms are never missed — even in sleep mode.",
                        color = Color(0xFFB0BEC5),
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp
                    )
                    SetupButton("Disable Battery Optimization") {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                        step = 4
                    }
                }
                4 -> {
                    Text(
                        text = "All set! Fetching your prayer times now...",
                        color = Color(0xFFB0BEC5),
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp
                    )
                    LaunchedEffect(Unit) {
                        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                        fusedClient.lastLocation.addOnSuccessListener { loc ->
                            if (loc != null) {
                                val prefs = com.banyadm.islam.data.SalahPreferences(context)
                                kotlinx.coroutines.MainScope().launch {
                                    prefs.setLocation(loc.latitude, loc.longitude)
                                    prefs.setSetupDone(true)
                                    com.banyadm.islam.worker.PrayerSyncWorker.scheduleTomorrow(context)
                                    val repo = com.banyadm.islam.data.PrayerRepository()
                                    val method = prefs.calcMethod.first()
                                    val result = repo.fetchTimes(loc.latitude, loc.longitude, method)
                                    if (result.isSuccess) {
                                        val times = result.getOrThrow()
                                        prefs.cacheTimes(times)
                                        val toggles = prefs.prayerToggles.first()
                                        com.banyadm.islam.alarm.AlarmScheduler.scheduleAll(context, times, toggles)
                                    }
                                    onSetupComplete()
                                }
                            }
                        }
                    }
                    CircularProgressIndicator(color = Color(0xFFD4AF37))
                }
            }

            if (statusText.isNotEmpty()) {
                Text(text = statusText, color = Color(0xFFEF9A9A), textAlign = TextAlign.Center)
            }

            if (step == 2) {
                LaunchedEffect(step) { step = 3 }
            }
        }
    }
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
