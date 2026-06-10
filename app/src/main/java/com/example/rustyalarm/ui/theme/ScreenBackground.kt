package com.example.rustyalarm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Theme-aware vertical background gradient used by all top-level screens.
 *
 * Dark theme: deep navy → near-black for the existing premium feel.
 * Light theme: warm off-white → soft lavender so text using
 *              MaterialTheme.colorScheme.onSurface stays readable.
 */
@Composable
fun screenBackgroundBrush(): Brush {
    val dark = isSystemInDarkTheme()
    val cs = MaterialTheme.colorScheme
    return if (dark) {
        Brush.verticalGradient(
            listOf(Color(0xFF0D0D22), Color(0xFF0A0A1A)),
        )
    } else {
        Brush.verticalGradient(
            listOf(cs.background, cs.surfaceVariant.copy(alpha = 0.5f)),
        )
    }
}
