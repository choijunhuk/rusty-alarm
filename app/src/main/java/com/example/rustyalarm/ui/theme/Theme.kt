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

// ── Brand palette ──────────────────────────────────
// Inspired by sunrise → calm-violet, warm peach, golden glow.

// LIGHT
private val LightColors = lightColorScheme(
    primary              = Color(0xFF5B3FE4),
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFEDE6FF),
    onPrimaryContainer   = Color(0xFF24006B),
    secondary            = Color(0xFFFF7A45),
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFFFD9C7),
    onSecondaryContainer = Color(0xFF3D1100),
    tertiary             = Color(0xFFFFB200),
    onTertiary           = Color(0xFF402B00),
    tertiaryContainer    = Color(0xFFFFE5A8),
    onTertiaryContainer  = Color(0xFF2A1B00),
    background           = Color(0xFFFFFBF5),
    onBackground         = Color(0xFF1A1625),
    surface              = Color(0xFFFFFDF8),
    onSurface            = Color(0xFF1A1625),
    surfaceVariant       = Color(0xFFF1E8E2),
    onSurfaceVariant     = Color(0xFF5A4F4A),
    outline              = Color(0xFFB8AFA7),
    error                = Color(0xFFD32F2F),
    onError              = Color.White,
)

// DARK
private val DarkColors = darkColorScheme(
    primary              = Color(0xFF9B8AFF),
    onPrimary            = Color(0xFF1B0B5C),
    primaryContainer     = Color(0xFF3A2A78),
    onPrimaryContainer   = Color(0xFFE8DFFF),
    secondary            = Color(0xFFFFB088),
    onSecondary          = Color(0xFF3D1100),
    secondaryContainer   = Color(0xFF5A2300),
    onSecondaryContainer = Color(0xFFFFD9C7),
    tertiary             = Color(0xFFFFD278),
    onTertiary           = Color(0xFF422B00),
    tertiaryContainer    = Color(0xFF604200),
    onTertiaryContainer  = Color(0xFFFFE5A8),
    background           = Color(0xFF0F0D26),
    onBackground         = Color(0xFFE8E4F0),
    surface              = Color(0xFF1B1840),
    onSurface            = Color(0xFFE8E4F0),
    surfaceVariant       = Color(0xFF2A2654),
    onSurfaceVariant     = Color(0xFFCAC2E8),
    outline              = Color(0xFF54508A),
    error                = Color(0xFFFF6B6B),
    onError              = Color.White,
)

// Back-compat exports (used elsewhere in the project)
val Purple80  = Color(0xFF9B8AFF)
val Cyan80    = Color(0xFFFFB088)
val Navy20    = Color(0xFF0F0D26)
val Navy30    = Color(0xFF1B1840)
val Navy40    = Color(0xFF2A2654)
val GrayMuted = Color(0xFF888899)

@Composable
fun RustyAlarmTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,    // Material You — feels like the user's phone
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
