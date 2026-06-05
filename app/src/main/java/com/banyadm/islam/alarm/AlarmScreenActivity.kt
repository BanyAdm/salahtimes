package com.banyadm.islam.alarm

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.banyadm.islam.data.SalahPreferences
import com.banyadm.islam.ui.theme.SalahTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class AlarmScreenActivity : ComponentActivity() {
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
                val scope = rememberCoroutineScope()
                var maxSnooze by remember { mutableStateOf(3) }
                var snoozeMins by remember { mutableStateOf(5) }
                var snoozeUsed by remember { mutableStateOf(0) }
                val currentTime = remember {
                    SimpleDateFormat("hh:mm a", Locale.US).format(Date())
                }

                LaunchedEffect(Unit) {
                    maxSnooze = prefs.snoozeCount.first()
                    snoozeMins = prefs.snoozeDuration.first()
                }

                // Swipe state
                var offsetX by remember { mutableStateOf(0f) }
                val animatedOffset by animateFloatAsState(
                    targetValue = offsetX,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "swipe"
                )

                // Glint animation on the swipe arrow
                val glintTransition = rememberInfiniteTransition(label = "glint")
                val glintX by glintTransition.animateFloat(
                    initialValue = -100f,
                    targetValue = 400f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1800, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "glint"
                )

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
                        Text(currentTime, color = Color(0xFFB0BEC5), fontSize = 18.sp)

                        Text(
                            text = prayerArabic,
                            color = Color(0xFFD4AF37),
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Light
                        )

                        Text(
                            text = "Time for $prayerName",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Swipe to dismiss
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .background(Color(0xFF1A2E40), RoundedCornerShape(32.dp)),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            // Glint sweep
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        androidx.compose.ui.graphics.Brush.linearGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color(0x22D4AF37),
                                                Color.Transparent
                                            ),
                                            start = androidx.compose.ui.geometry.Offset(glintX, 0f),
                                            end = androidx.compose.ui.geometry.Offset(glintX + 100f, 64f)
                                        ),
                                        RoundedCornerShape(32.dp)
                                    )
                            )
                            Text(
                                "Swipe to dismiss →",
                                color = Color(0xFF546E7A),
                                fontSize = 14.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            // Draggable thumb
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                                    .padding(6.dp)
                                    .size(52.dp)
                                    .background(Color(0xFF1B5E20), RoundedCornerShape(26.dp))
                                    .pointerInput(Unit) {
                                        detectHorizontalDragGestures(
                                            onDragEnd = {
                                                if (offsetX > 220f) {
                                                    finish()
                                                } else {
                                                    offsetX = 0f
                                                }
                                            },
                                            onHorizontalDrag = { _, dragAmount ->
                                                offsetX = (offsetX + dragAmount).coerceIn(0f, 280f)
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("→", color = Color.White, fontSize = 20.sp)
                            }
                        }

                        // Snooze
                        if (snoozeUsed < maxSnooze) {
                            OutlinedButton(
                                onClick = {
                                    snoozeUsed++
                                    scheduleSnooze(prayerName, prayerArabic, prayerId, snoozeMins)
                                    finish()
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFD4AF37)
                                )
                            ) {
                                Text(
                                    "Snooze ${snoozeMins}min (${maxSnooze - snoozeUsed} left)",
                                    fontSize = 15.sp
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
        alarmManager.setExactAndAllowWhileIdle(
            android.app.AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + (minutes * 60 * 1000L),
            pending
        )
    }
}
