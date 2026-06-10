package com.example.rustyalarm.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary          = Color(0xFFBB86FC),
    onPrimary        = Color(0xFF000000),
    primaryContainer = Color(0xFF3700B3),
    secondary        = Color(0xFF03DAC6),
    onSecondary      = Color(0xFF000000),
    background       = Color(0xFF121212),
    onBackground     = Color(0xFFFFFFFF),
    surface          = Color(0xFF1E1E1E),
    onSurface        = Color(0xFFFFFFFF),
    error            = Color(0xFFCF6679),
)

@Composable
fun RustyAlarmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}
