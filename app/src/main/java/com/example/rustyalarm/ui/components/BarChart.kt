package com.example.rustyalarm.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

data class BarDatum(val label: String, val value: Float)

/**
 * Compose Canvas bar chart with rounded gradient bars and value labels.
 * Library-free (no MPAndroidChart/Vico) — pure drawscope primitives.
 */
@Composable
fun BarChart(
    data: List<BarDatum>,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
    barColor: Color = MaterialTheme.colorScheme.primary,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    if (data.isEmpty()) return
    val maxValue = max(1f, data.maxOf { it.value })
    val accent = MaterialTheme.colorScheme.secondary

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        val w = size.width
        val h = size.height
        val labelArea = 36f
        val chartH = h - labelArea
        val n = data.size
        val barSlot = w / n
        val barWidth = barSlot * 0.55f
        val gap = (barSlot - barWidth) / 2f

        // Baseline
        drawLine(
            color = labelColor.copy(alpha = 0.25f),
            start = Offset(0f, chartH),
            end = Offset(w, chartH),
            strokeWidth = 1.5f,
        )

        data.forEachIndexed { i, datum ->
            val barH = (datum.value / maxValue) * (chartH - 20f)
            val x = i * barSlot + gap
            val y = chartH - barH

            // Rounded bar with vertical gradient
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(barColor, accent.copy(alpha = 0.7f)),
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
            )

            // Value above bar
            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    datum.value.toInt().toString(),
                    x + barWidth / 2f,
                    y - 6f,
                    android.graphics.Paint().apply {
                        color = labelColor.copy(alpha = 0.8f).toArgb()
                        textSize = 28f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                )
                drawText(
                    datum.label,
                    x + barWidth / 2f,
                    h - 8f,
                    android.graphics.Paint().apply {
                        color = labelColor.copy(alpha = 0.55f).toArgb()
                        textSize = 24f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                )
            }
        }
    }
}

private fun Color.toArgb(): Int =
    android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red   * 255).toInt(),
        (green * 255).toInt(),
        (blue  * 255).toInt(),
    )
