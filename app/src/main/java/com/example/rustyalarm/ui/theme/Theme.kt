package com.example.rustyalarm.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.rustyalarm.prefs.ThemeMode

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

private val LightColors = lightColorScheme(
    primary            = Color(0xFF5E2DD9),
    onPrimary          = Color.White,
    primaryContainer   = Color(0xFFEDE2FF),
    onPrimaryContainer = Color(0xFF2A0080),
    secondary          = Color(0xFF008CA0),
    onSecondary        = Color.White,
    secondaryContainer = Color(0xFFD4F5FB),
    background         = Color(0xFFFAFAFC),
    onBackground       = Color(0xFF111122),
    surface            = Color.White,
    onSurface          = Color(0xFF111122),
    surfaceVariant     = Color(0xFFEEEEF6),
    onSurfaceVariant   = Color(0xFF55556B),
    error              = Color(0xFFD32F2F),
    onError            = Color.White,
    outline            = Color(0xFFB8B8C8),
)

@Composable
fun RustyAlarmTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val useDark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK   -> true
        ThemeMode.LIGHT  -> false
    }
    val ctx = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (useDark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        useDark -> DarkColors
        else    -> LightColors
    }
    MaterialTheme(
        colorScheme = scheme,
        content = content,
    )
}
