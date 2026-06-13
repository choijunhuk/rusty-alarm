package com.example.rustyalarm.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import java.util.Calendar

/**
 * Whole-screen background that smoothly shifts colour with the time of day so
 * the app feels alive instead of looking like a static Material demo.
 *
 * Buckets — the palette is sampled by interpolating between anchors so the
 * background drifts gradually rather than snapping at the hour boundary:
 *  - 04–07  Dawn        (deep indigo → warm coral)
 *  - 07–11  Morning     (sky blue → cream)
 *  - 11–16  Daytime     (washed sky)
 *  - 16–19  Sunset      (peach → magenta)
 *  - 19–04  Night       (cool deep blue → graphite)
 */
@Composable
fun TimeOfDayBackground(
    isDark: Boolean,
    content: @Composable () -> Unit,
) {
    val (top, mid, bot) = currentPalette(isDark)
    val topA  by animateColorAsState(top,  tween(800), label = "top")
    val midA  by animateColorAsState(mid,  tween(800), label = "mid")
    val botA  by animateColorAsState(bot,  tween(800), label = "bot")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(topA, midA, botA))
            )
    ) {
        // Veil the gradient slightly with theme surface so Material widgets
        // sitting on top keep proper contrast against the user's theme.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f)),
        ) { content() }
    }
}

private fun currentPalette(isDark: Boolean): Triple<Color, Color, Color> {
    val cal = Calendar.getInstance()
    val hour = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60f

    return if (isDark) darkPalette(hour) else lightPalette(hour)
}

private fun lightPalette(hour: Float): Triple<Color, Color, Color> {
    val anchors = listOf(
        4f  to Triple(Color(0xFF20174A), Color(0xFF6D3E83), Color(0xFFE49679)),
        7f  to Triple(Color(0xFFFEB07A), Color(0xFFFFDFA8), Color(0xFFFEF9F0)),
        11f to Triple(Color(0xFFB3DBFF), Color(0xFFD8ECFF), Color(0xFFFFFBF5)),
        16f to Triple(Color(0xFFFFCB8B), Color(0xFFFFE0CA), Color(0xFFFFF7EE)),
        19f to Triple(Color(0xFFE07B5C), Color(0xFFB35AA0), Color(0xFF3D2660)),
        23f to Triple(Color(0xFF13123A), Color(0xFF1B1840), Color(0xFF0B0A22)),
    )
    return interpolate(hour, anchors)
}

private fun darkPalette(hour: Float): Triple<Color, Color, Color> {
    val anchors = listOf(
        4f  to Triple(Color(0xFF050513), Color(0xFF15102E), Color(0xFF291B4A)),
        7f  to Triple(Color(0xFF1A1240), Color(0xFF2B1F58), Color(0xFF453166)),
        11f to Triple(Color(0xFF0F1635), Color(0xFF192655), Color(0xFF2F3F77)),
        16f to Triple(Color(0xFF231836), Color(0xFF3A1F4D), Color(0xFF5A2D5A)),
        19f to Triple(Color(0xFF0F0D26), Color(0xFF1B1840), Color(0xFF2A1F5C)),
        23f to Triple(Color(0xFF06061A), Color(0xFF0B0A22), Color(0xFF13123A)),
    )
    return interpolate(hour, anchors)
}

private fun interpolate(
    hour: Float,
    anchors: List<Pair<Float, Triple<Color, Color, Color>>>,
): Triple<Color, Color, Color> {
    // Wrap to 24h palette so the night anchor at 23h flows back to 4h.
    val sorted = anchors.sortedBy { it.first }
    val low = sorted.lastOrNull { it.first <= hour } ?: sorted.last()
    val high = sorted.firstOrNull { it.first > hour } ?: sorted.first()
    val span = ((high.first - low.first) + 24f) % 24f
    val t = if (span == 0f) 0f else (((hour - low.first) + 24f) % 24f) / span
    return Triple(
        lerp(low.second.first,  high.second.first,  t),
        lerp(low.second.second, high.second.second, t),
        lerp(low.second.third,  high.second.third,  t),
    )
}
