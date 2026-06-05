package com.banyadm.islam.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD4AF37),
    onPrimary = Color(0xFF0D1B2A),
    background = Color(0xFF0D1B2A),
    surface = Color(0xFF1A2E40),
    onBackground = Color.White,
    onSurface = Color.White,
    secondary = Color(0xFF4A90A4)
)

@Composable
fun SalahTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
