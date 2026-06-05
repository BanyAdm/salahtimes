package com.banyadm.islam.alarm

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.banyadm.islam.data.SalahPreferences
import com.banyadm.islam.ui.theme.SalahTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AlarmScreenActivity : ComponentActivity() {

    private var snoozeJob: kotlinx.coroutines.Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        val prayerName = intent.getStringExtra("prayer_name") ?: "Prayer"
        val prayerArabic = intent.getStringExtra("prayer_arabic") ?: ""
        val prayerId = intent.getIntExtra("prayer_id", 0)
        val prefs = SalahPreferences(this)

        setContent {
            SalahTheme {
                var snoozeUsed by remember { mutableStateOf(0) }
                var maxSnooze by remember { mutableStateOf(3) }
                var snoozeMins by remember { mutableStateOf(5) }
                val currentTime = remember {
                    SimpleDateFormat("hh:mm a", Locale.US).format(Date())
                }

                LaunchedEffect(Unit) {
                    maxSnooze = prefs.snoozeCount.first()
                    snoozeMins = prefs.snoozeDuration.first()
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0D1B2A)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = currentTime,
                            color = Color(0xFFB0BEC5),
                            fontSize = 18.sp
                        )
                        Text(
                            text = prayerArabic,
                            color = Color(0xFFD4AF37),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Light
                        )
                        Text(
                            text = "Time for $prayerName",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { finish() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1B5E20)
                            )
                        ) {
                            Text("Dismiss", fontSize = 18.sp, color = Color.White)
                        }

                        if (snoozeUsed < maxSnooze) {
                            OutlinedButton(
                                onClick = {
                                    snoozeUsed++
                                    scheduleSnooze(prayerName, prayerArabic, prayerId, snoozeMins)
                                    finish()
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFD4AF37)
                                )
                            ) {
                                Text(
                                    "Snooze ${snoozeMins}min (${maxSnooze - snoozeUsed} left)",
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun scheduleSnooze(name: String, arabic: String, id: Int, minutes: Int) {
        val alarmManager = getSystemService(android.app.AlarmManager::class.java)
        val intent = android.content.Intent(this, AlarmReceiver::class.java).apply {
            putExtra("prayer_name", name)
            putExtra("prayer_arabic", arabic)
            putExtra("prayer_id", id)
        }
        val pending = android.app.PendingIntent.getBroadcast(
            this, id + 100, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = System.currentTimeMillis() + (minutes * 60 * 1000L)
        alarmManager.setExactAndAllowWhileIdle(
            android.app.AlarmManager.RTC_WAKEUP, triggerAt, pending
        )
    }
}
