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
import com.banyadm.islam.data.SalahPreferences
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { SalahPreferences(context) }
    val scope = rememberCoroutineScope()

    val snoozeCount by prefs.snoozeCount.collectAsState(initial = 3)
    val snoozeDuration by prefs.snoozeDuration.collectAsState(initial = 5)
    val calcMethod by prefs.calcMethod.collectAsState(initial = 3)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1B2A))
            .statusBarsPadding().padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) {
                Text("← Back", color = Color(0xFFD4AF37))
            }
            Text(
                text = "Settings",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection("Snooze") {
            SettingsRow("Times allowed") {
                StepCounter(
                    value = snoozeCount,
                    min = 0, max = 10,
                    onValueChange = { scope.launch { prefs.setSnooze(it, snoozeDuration) } }
                )
            }
            SettingsRow("Duration (minutes)") {
                StepCounter(
                    value = snoozeDuration,
                    min = 1, max = 30,
                    onValueChange = { scope.launch { prefs.setSnooze(snoozeCount, it) } }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = calcMethod == id,
                        onClick = { scope.launch { prefs.setCalcMethod(id) } },
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFD4AF37))
                    )
                    Text(name, color = Color.White, fontSize = 14.sp)
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
        Text(
            text = "$value",
            color = Color.White,
            fontSize = 18.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        TextButton(
            onClick = { if (value < max) onValueChange(value + 1) },
            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD4AF37))
        ) { Text("+", fontSize = 20.sp) }
    }
}
