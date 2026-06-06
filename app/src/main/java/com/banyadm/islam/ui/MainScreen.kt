package com.banyadm.islam.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.banyadm.islam.alarm.AlarmScheduler
import com.banyadm.islam.data.Prayer
import com.banyadm.islam.data.PrayerTimes
import com.banyadm.islam.data.SalahPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainScreen(
    onSettingsClick: () -> Unit,
    cityName: String,
    quranVerse: Pair<String, String>?
) {
    val context = LocalContext.current
    val prefs = remember { SalahPreferences(context) }
    val scope = rememberCoroutineScope()

    val cachedTimes by prefs.cachedTimes.collectAsState(initial = null)
    val toggles by prefs.prayerToggles.collectAsState(initial = emptyMap())
    val nextPrayer = remember(cachedTimes) { getNextPrayer(cachedTimes) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1B2A))
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFF1A2E40), Color(0xFFD4AF37), Color(0xFF1A2E40))
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        SimpleDateFormat("EEE, d MMM", Locale.US).format(Date()),
                        color = Color(0xFFB0BEC5), fontSize = 13.sp
                    )
                    if (cityName.isNotEmpty()) {
                        Text("📍 $cityName", color = Color(0xFFD4AF37), fontSize = 12.sp)
                    }
                }
                TextButton(onClick = onSettingsClick) {
                    Text("Settings", color = Color(0xFFD4AF37), fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("صلاة", color = Color(0xFFD4AF37), fontSize = 40.sp, fontWeight = FontWeight.Light)
        Text("Salah Times", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)

        quranVerse?.let { (arabic, translation) ->
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A2E40), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        arabic, color = Color(0xFFD4AF37), fontSize = 16.sp,
                        fontWeight = FontWeight.Light, textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        translation, color = Color(0xFFB0BEC5), fontSize = 11.sp,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (cachedTimes == null) {
            repeat(5) {
                ShimmerPrayerCard()
                Spacer(modifier = Modifier.height(10.dp))
            }
        } else {
            val times = cachedTimes!!
            listOf(
                Prayer.FAJR to times.fajr,
                Prayer.DHUHR to times.dhuhr,
                Prayer.ASR to times.asr,
                Prayer.MAGHRIB to times.maghrib,
                Prayer.ISHA to times.isha
            ).forEach { (prayer, time) ->
                PrayerCard(
                    prayer = prayer,
                    time = to12h(time),
                    enabled = toggles[prayer] ?: true,
                    isNext = prayer == nextPrayer,
                    isRefreshing = false,
                    onToggle = { enabled ->
                        scope.launch {
                            prefs.setToggle(prayer, enabled)
                            val allToggles = prefs.prayerToggles.first()
                            val remToggles = prefs.reminderToggles.first()
                            val remMins = prefs.reminderMinutes.first()
                            AlarmScheduler.scheduleAll(context, times, allToggles, remToggles, remMins)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun ShimmerPrayerCard() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "shimmer"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1A2E40), Color(0xFF2A4560), Color(0xFF1A2E40)),
                    start = Offset(translateAnim - 200f, 0f),
                    end = Offset(translateAnim, 0f)
                ),
                RoundedCornerShape(14.dp)
            )
    )
}

@Composable
fun PrayerCard(
    prayer: Prayer,
    time: String,
    enabled: Boolean,
    isNext: Boolean,
    isRefreshing: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val bgColor = if (isNext) Color(0xFF1F3347) else Color(0xFF1A2E40)
    val borderColor = if (isNext) Color(0xFFD4AF37) else Color.Transparent

    val glintTransition = rememberInfiniteTransition(label = "glint")
    val glintX by glintTransition.animateFloat(
        initialValue = -200f, targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "glint"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(if (isNext) 1.5.dp else 0.dp, borderColor)
    ) {
        Box {
            if (isRefreshing) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color.Transparent, Color(0x22D4AF37), Color.Transparent),
                                start = Offset(glintX, 0f),
                                end = Offset(glintX + 200f, 100f)
                            ),
                            RoundedCornerShape(14.dp)
                        )
                )
            }
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(prayer.arabic, color = Color(0xFFD4AF37), fontSize = 18.sp, fontWeight = FontWeight.Light)
                    Text(prayer.displayName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    if (isNext) Text("Next prayer", color = Color(0xFFD4AF37), fontSize = 11.sp)
                }
                Text(
                    time,
                    color = if (enabled) Color.White else Color(0xFF546E7A),
                    fontSize = 20.sp, fontWeight = FontWeight.Light,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Switch(
                    checked = enabled, onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFD4AF37),
                        checkedTrackColor = Color(0xFF3D5A47)
                    )
                )
            }
        }
    }
}

fun getNextPrayer(times: PrayerTimes?): Prayer? {
    if (times == null) return null
    val now = Calendar.getInstance()
    val sdf = SimpleDateFormat("HH:mm", Locale.US)
    return listOf(
        Prayer.FAJR to times.fajr, Prayer.DHUHR to times.dhuhr,
        Prayer.ASR to times.asr, Prayer.MAGHRIB to times.maghrib, Prayer.ISHA to times.isha
    ).firstOrNull { (_, time) ->
        val parsed = sdf.parse(time) ?: return@firstOrNull false
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, parsed.hours)
            set(Calendar.MINUTE, parsed.minutes)
            set(Calendar.SECOND, 0)
        }.after(now)
    }?.first
}

fun to12h(time: String): String {
    return try {
        val t = SimpleDateFormat("HH:mm", Locale.US).parse(time) ?: return time
        SimpleDateFormat("hh:mm a", Locale.US).format(t)
    } catch (e: Exception) { time }
}
