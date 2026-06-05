package com.banyadm.islam.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.banyadm.islam.alarm.AlarmReceiver
import com.banyadm.islam.alarm.AlarmScheduler
import com.banyadm.islam.data.PrayerRepository
import com.banyadm.islam.data.SalahPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { SalahPreferences(context) }
    val scope = rememberCoroutineScope()

    val snoozeCount by prefs.snoozeCount.collectAsState(initial = 3)
    val snoozeDuration by prefs.snoozeDuration.collectAsState(initial = 5)
    val calcMethod by prefs.calcMethod.collectAsState(initial = 3)

    var testAlarmActive by remember { mutableStateOf(false) }
    var testCountdown by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1B2A))
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Animated back button
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(
                onClick = onBack,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color(0xFF1A2E40),
                    contentColor = Color(0xFFD4AF37)
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("Settings", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Snooze settings
        SettingsSection("Snooze") {
            SettingsRow("Times allowed") {
                StepCounter(value = snoozeCount, min = 0, max = 10,
                    onValueChange = { scope.launch { prefs.setSnooze(it, snoozeDuration) } })
            }
            SettingsRow("Duration (minutes)") {
                StepCounter(value = snoozeDuration, min = 1, max = 30,
                    onValueChange = { scope.launch { prefs.setSnooze(snoozeCount, it) } })
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Calculation method
        SettingsSection("Calculation Method") {
            val methods = listOf(
                2 to "ISNA (North America)",
                3 to "Muslim World League",
                4 to "Umm Al-Qura (Saudi)",
                5 to "Egyptian Authority",
                16 to "Dubai"
            )
            methods.forEach { (id, name) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = calcMethod == id,
                        onClick = {
                            scope.launch {
                                prefs.setCalcMethod(id)
                                // Refetch with new method
                                val loc = prefs.location.first() ?: return@launch
                                val result = PrayerRepository().fetchTimes(loc.first, loc.second, id)
                                if (result.isSuccess) {
                                    val times = result.getOrThrow()
                                    prefs.cacheTimes(times)
                                    val toggles = prefs.prayerToggles.first()
                                    AlarmScheduler.scheduleAll(context, times, toggles)
                                }
                            }
                        },
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFD4AF37))
                    )
                    Text(name, color = Color.White, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Test alarm
        SettingsSection("Test Alarm") {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (testAlarmActive) {
                    Text(
                        "⏳ Test alarm will ring in $testCountdown seconds...",
                        color = Color(0xFFD4AF37),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else {
                    Text(
                        "Triggers a test alarm after 10 seconds so you can hear how it sounds.",
                        color = Color(0xFFB0BEC5),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Button(
                    onClick = {
                        if (!testAlarmActive) {
                            testAlarmActive = true
                            testCountdown = 10
                            // Schedule alarm in 10 seconds
                            val alarmManager = context.getSystemService(AlarmManager::class.java)
                            val intent = Intent(context, AlarmReceiver::class.java).apply {
                                putExtra("prayer_name", "Test Alarm")
                                putExtra("prayer_arabic", "اختبار")
                                putExtra("prayer_id", 99)
                            }
                            val pending = PendingIntent.getBroadcast(
                                context, 999, intent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                System.currentTimeMillis() + 10_000,
                                pending
                            )
                            scope.launch {
                                while (testCountdown > 0) {
                                    delay(1000)
                                    testCountdown--
                                }
                                testAlarmActive = false
                            }
                        }
                    },
                    enabled = !testAlarmActive,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1F3347),
                        disabledContainerColor = Color(0xFF0F1E2A)
                    )
                ) {
                    Text(
                        if (testAlarmActive) "Waiting..." else "Trigger Test Alarm",
                        color = if (testAlarmActive) Color(0xFF546E7A) else Color(0xFFD4AF37)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, color = Color(0xFFD4AF37), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2E40))
    ) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
fun SettingsRow(label: String, control: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 15.sp)
        control()
    }
}

@Composable
fun StepCounter(value: Int, min: Int, max: Int, onValueChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(
            onClick = { if (value > min) onValueChange(value - 1) },
            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD4AF37))
        ) { Text("−", fontSize = 20.sp) }
        Text("$value", color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 8.dp))
        TextButton(
            onClick = { if (value < max) onValueChange(value + 1) },
            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD4AF37))
        ) { Text("+", fontSize = 20.sp) }
    }
}
