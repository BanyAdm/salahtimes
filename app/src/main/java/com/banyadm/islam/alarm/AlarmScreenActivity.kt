package com.banyadm.islam.alarm

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.banyadm.islam.ui.theme.SalahTheme
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

        // Auto dismiss if triggered from notification button
        if (intent.getBooleanExtra("auto_dismiss", false)) {
            finish()
            return
        }

        val prayerName = intent.getStringExtra("prayer_name") ?: "Prayer"
        val prayerArabic = intent.getStringExtra("prayer_arabic") ?: ""

        setContent {
            SalahTheme {
                val currentTime = remember { SimpleDateFormat("hh:mm a", Locale.US).format(Date()) }
                var offsetY by remember { mutableStateOf(0f) }
                val animatedOffset by animateFloatAsState(
                    targetValue = offsetY,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "swipe"
                )

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
                                        dismiss()
                                    } else {
                                        offsetY = 0f
                                    }
                                },
                                onVerticalDrag = { _, dragAmount ->
                                    offsetY = (offsetY + dragAmount).coerceIn(-400f, 0f)
                                }
                            )
                        }
                ) {
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

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 60.dp, start = 32.dp, end = 32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowUp,
                            contentDescription = null,
                            tint = Color(0xFFD4AF37).copy(alpha = glintAlpha * 0.3f),
                            modifier = Modifier.size(40.dp).offset(y = (glintY + 20f).dp)
                        )
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowUp,
                            contentDescription = null,
                            tint = Color(0xFFD4AF37).copy(alpha = glintAlpha * 0.6f),
                            modifier = Modifier.size(48.dp).offset(y = glintY.dp)
                        )
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowUp,
                            contentDescription = null,
                            tint = Color(0xFFD4AF37).copy(alpha = glintAlpha),
                            modifier = Modifier.size(56.dp).offset(y = (glintY - 20f).dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Swipe up to dismiss", color = Color(0xFF546E7A), fontSize = 13.sp)
                    }
                }
            }
        }
    }

    private fun dismiss() {
        AlarmReceiver.stopAlarm()
        stopService(Intent(this, AlarmService::class.java))
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("auto_dismiss", false)) {
            dismiss()
        }
    }
}
