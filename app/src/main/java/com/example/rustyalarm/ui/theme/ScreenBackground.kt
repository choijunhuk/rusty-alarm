package com.example.rustyalarm.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Whether the *applied* color scheme is dark — unlike isSystemInDarkTheme(),
 * this respects the manual LIGHT/DARK override in Settings (ThemeMode).
 */
@Composable
fun isAppInDarkTheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/**
 * Theme-aware vertical background gradient used by all top-level screens.
 *
 * Dark theme: deep navy → near-black for the existing premium feel.
 * Light theme: warm off-white → soft lavender so text using
 *              MaterialTheme.colorScheme.onSurface stays readable.
 */
@Composable
fun screenBackgroundBrush(): Brush {
    val dark = isAppInDarkTheme()
    val cs = MaterialTheme.colorScheme
    return if (dark) {
        Brush.verticalGradient(
            listOf(Color(0xFF0F0D26), Color(0xFF1B1840)),
        )
    } else {
        Brush.verticalGradient(
            listOf(cs.background, cs.tertiaryContainer.copy(alpha = 0.25f)),
        )
    }
}
