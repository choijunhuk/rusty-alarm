package com.example.rustyalarm.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Purple80   = Color(0xFF7C4DFF)
val Cyan80     = Color(0xFF00E5FF)
val Navy20     = Color(0xFF0A0A1A)
val Navy30     = Color(0xFF16162A)
val Navy40     = Color(0xFF21213A)
val GrayMuted  = Color(0xFF8888AA)

private val DarkColors = darkColorScheme(
    primary            = Color(0xFF7C4DFF),
    onPrimary          = Color.White,
    primaryContainer   = Color(0xFF4A148C),
    onPrimaryContainer = Color(0xFFE8D5FF),
    secondary          = Color(0xFF00E5FF),
    onSecondary        = Color.Black,
    secondaryContainer = Color(0xFF00838F),
    background         = Color(0xFF0A0A1A),
    onBackground       = Color.White,
    surface            = Color(0xFF16162A),
    onSurface          = Color.White,
    surfaceVariant     = Color(0xFF21213A),
    onSurfaceVariant   = Color(0xFFBBBBDD),
    error              = Color(0xFFFF5252),
    onError            = Color.White,
    outline            = Color(0xFF44446A),
)

@Composable
fun RustyAlarmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}
