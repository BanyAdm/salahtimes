package com.banyadm.islam.alarm

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
                val currentTime = remember { SimpleDateFormat("hh:mm a", Locale.US).format(Date()) }

                LaunchedEffect(Unit) {
                    maxSnooze = prefs.snoozeCount.first()
                    snoozeMins = prefs.snoozeDuration.first()
                }

                // Swipe up state - tracks drag from bottom half
                var offsetY by remember { mutableStateOf(0f) }
                val animatedOffset by animateFloatAsState(
                    targetValue = offsetY,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "swipe"
                )

                // Glint on arrow
                val glintTransition = rememberInfiniteTransition(label = "glint")
                val glintAlpha by glintTransition.animateFloat(
                    initialValue = 0.2f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(900, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ), label = "glint"
                )
                val glintY by glintTransition.animateFloat(
                    initialValue = 20f, targetValue = -20f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(900, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ), label = "glintY"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0D1B2A))
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (offsetY < -180f) {
                                        finish()
                                    } else {
                                        offsetY = 0f
                                    }
                                },
                                onVerticalDrag = { _, dragAmount ->
                                    // Only allow upward swipe (negative = up)
                                    offsetY = (offsetY + dragAmount).coerceIn(-400f, 0f)
                                }
                            )
                        }
                ) {
                    // Top half — prayer info (moves up with swipe)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .offset { IntOffset(0, animatedOffset.roundToInt()) }
                            .padding(top = 80.dp, start = 32.dp, end = 32.dp)
                    ) {
                        Text(currentTime, color = Color(0xFFB0BEC5), fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(prayerArabic, color = Color(0xFFD4AF37), fontSize = 64.sp, fontWeight = FontWeight.Light)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Time for $prayerName",
                            color = Color.White, fontSize = 28.sp,
                            fontWeight = FontWeight.Medium, textAlign = TextAlign.Center
                        )
                    }

                    // Bottom half — snooze + swipe up indicator
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 60.dp, start = 32.dp, end = 32.dp)
                    ) {
                        // Snooze button
                        if (snoozeUsed < maxSnooze) {
                            OutlinedButton(
                                onClick = {
                                    snoozeUsed++
                                    scheduleSnooze(prayerName, prayerArabic, prayerId, snoozeMins)
                                    finish()
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD4AF37))
                            ) {
                                Text("Snooze ${snoozeMins}min (${maxSnooze - snoozeUsed} left)", fontSize = 15.sp)
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }

                        // Swipe up arrow glint
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Three stacked arrows with decreasing alpha = motion trail
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowUp,
                                contentDescription = null,
                                tint = Color(0xFFD4AF37).copy(alpha = glintAlpha * 0.3f),
                                modifier = Modifier
                                    .size(40.dp)
                                    .offset(y = (glintY + 20f).dp)
                            )
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowUp,
                                contentDescription = null,
                                tint = Color(0xFFD4AF37).copy(alpha = glintAlpha * 0.6f),
                                modifier = Modifier
                                    .size(48.dp)
                                    .offset(y = glintY.dp)
                            )
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowUp,
                                contentDescription = null,
                                tint = Color(0xFFD4AF37).copy(alpha = glintAlpha),
                                modifier = Modifier
                                    .size(56.dp)
                                    .offset(y = (glintY - 20f).dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Swipe up to dismiss",
                                color = Color(0xFF546E7A),
                                fontSize = 13.sp
                            )
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
