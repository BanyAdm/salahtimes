package com.banyadm.islam.ui

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
fun MainScreen(onSettingsClick: () -> Unit) {
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
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = SimpleDateFormat("EEE, d MMM", Locale.US).format(Date()),
                color = Color(0xFFB0BEC5),
                fontSize = 14.sp
            )
            TextButton(onClick = onSettingsClick) {
                Text("Settings", color = Color(0xFFD4AF37))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "صلاة",
            color = Color(0xFFD4AF37),
            fontSize = 40.sp,
            fontWeight = FontWeight.Light
        )
        Text(
            text = "Salah Times",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (cachedTimes == null) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFD4AF37))
            }
        } else {
            val times = cachedTimes!!
            val prayerList = listOf(
                Prayer.FAJR to times.fajr,
                Prayer.DHUHR to times.dhuhr,
                Prayer.ASR to times.asr,
                Prayer.MAGHRIB to times.maghrib,
                Prayer.ISHA to times.isha
            )
            prayerList.forEach { (prayer, time) ->
                PrayerCard(
                    prayer = prayer,
                    time = to12h(time),
                    enabled = toggles[prayer] ?: true,
                    isNext = prayer == nextPrayer,
                    onToggle = { enabled ->
                        scope.launch {
                            prefs.setToggle(prayer, enabled)
                            val allToggles = prefs.prayerToggles.first()
                            AlarmScheduler.scheduleAll(context, times, allToggles)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun PrayerCard(
    prayer: Prayer,
    time: String,
    enabled: Boolean,
    isNext: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val borderColor = if (isNext) Color(0xFFD4AF37) else Color.Transparent
    val bgColor = if (isNext) Color(0xFF1F3347) else Color(0xFF1A2E40)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(
            if (isNext) 1.5.dp else 0.dp, borderColor
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = prayer.arabic,
                    color = Color(0xFFD4AF37),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Light
                )
                Text(
                    text = prayer.displayName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                if (isNext) {
                    Text(
                        text = "Next prayer",
                        color = Color(0xFFD4AF37),
                        fontSize = 11.sp
                    )
                }
            }
            Text(
                text = time,
                color = if (enabled) Color.White else Color(0xFF546E7A),
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(end = 12.dp)
            )
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFD4AF37),
                    checkedTrackColor = Color(0xFF3D5A47)
                )
            )
        }
    }
}

fun getNextPrayer(times: PrayerTimes?): Prayer? {
    if (times == null) return null
    val now = Calendar.getInstance()
    val sdf = SimpleDateFormat("HH:mm", Locale.US)
    val list = listOf(
        Prayer.FAJR to times.fajr,
        Prayer.DHUHR to times.dhuhr,
        Prayer.ASR to times.asr,
        Prayer.MAGHRIB to times.maghrib,
        Prayer.ISHA to times.isha
    )
    return list.firstOrNull { (_, time) ->
        val parsed = sdf.parse(time) ?: return@firstOrNull false
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, parsed.hours)
            set(Calendar.MINUTE, parsed.minutes)
            set(Calendar.SECOND, 0)
        }
        cal.after(now)
    }?.first
}

fun to12h(time: String): String {
    return try {
        val sdf = SimpleDateFormat("HH:mm", Locale.US)
        val t = sdf.parse(time) ?: return time
        SimpleDateFormat("hh:mm a", Locale.US).format(t)
    } catch (e: Exception) { time }
}
