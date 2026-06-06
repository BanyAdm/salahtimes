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
import androidx.compose.foundation.layout.FlowRow
import kotlinx.coroutines.flow.first
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

    val calcMethod by prefs.calcMethod.collectAsState(initial = 3)
    val reminderToggles by prefs.reminderToggles.collectAsState(initial = emptyMap())
    val reminderMinutes by prefs.reminderMinutes.collectAsState(initial = 15)
    val cachedTimes by prefs.cachedTimes.collectAsState(initial = null)

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

        // Reminder settings
        SettingsSection("Reminder Alarms") {
            Text(
                "Get a notification before each prayer. Time is calculated based on prayer gaps.",
                color = Color(0xFFB0BEC5), fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (cachedTimes != null) {
                val times = cachedTimes!!
                val prayerTimes = listOf(
                    com.banyadm.islam.data.Prayer.FAJR to times.fajr,
                    com.banyadm.islam.data.Prayer.DHUHR to times.dhuhr,
                    com.banyadm.islam.data.Prayer.ASR to times.asr,
                    com.banyadm.islam.data.Prayer.MAGHRIB to times.maghrib,
                    com.banyadm.islam.data.Prayer.ISHA to times.isha
                )
                // Reminder minutes selector
                val allTimes = prayerTimes.map { (_, t) -> t }
                val gaps = allTimes.zipWithNext { a, b ->
                    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US)
                    val ta = sdf.parse(a)!!
                    val tb = sdf.parse(b)!!
                    ((tb.time - ta.time) / 60000).toInt().let { if (it < 0) it + 1440 else it }
                }
                val minGap = gaps.minOrNull() ?: 60
                val options = com.banyadm.islam.data.calculateReminderOptions(minGap)
                if (options.isNotEmpty()) {
                    Text("Reminder time", color = Color.White, fontSize = 15.sp)
                    FlowRow(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        options.forEach { mins ->
                            FilterChip(
                                selected = reminderMinutes == mins,
                                onClick = { scope.launch { prefs.setReminderMinutes(mins) } },
                                label = { Text("${mins}m", fontSize = 12.sp) },
                                modifier = Modifier.padding(end = 4.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFD4AF37),
                                    selectedLabelColor = Color(0xFF0D1B2A)
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                prayerTimes.forEach { (prayer, _) ->
                    SettingsRow(prayer.displayName) {
                        Switch(
                            checked = reminderToggles[prayer] ?: false,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    prefs.setReminderToggle(prayer, enabled)
                                    val toggles = prefs.prayerToggles.first()
                                    val remToggles = prefs.reminderToggles.first()
                                    val remMins = prefs.reminderMinutes.first()
                                    com.banyadm.islam.alarm.AlarmScheduler.scheduleAll(
                                        context, times, toggles, remToggles, remMins
                                    )
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFD4AF37),
                                checkedTrackColor = Color(0xFF3D5A47)
                            )
                        )
                    }
                }
            } else {
                Text("Prayer times not loaded yet.", color = Color(0xFF546E7A), fontSize = 13.sp)
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
