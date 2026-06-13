package com.example.rustyalarm.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DAY_LABELS = listOf("일", "월", "화", "수", "목", "금", "토")

@Composable
fun DaySelector(
    selectedDays: List<Int>,
    onDayToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        DAY_LABELS.forEachIndexed { index, label ->
            val selected = selectedDays.contains(index)
            val isSunday = index == 0
            val isSaturday = index == 6

            val baseTint = when {
                isSunday   -> Color(0xFFE53935) // 일요일 — red
                isSaturday -> Color(0xFF1E88E5) // 토요일 — blue
                else       -> MaterialTheme.colorScheme.primary
            }

            val containerColor = if (selected) baseTint
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            val contentColor = if (selected) Color.White
                else baseTint.copy(alpha = 0.8f)

            Surface(
                onClick = { onDayToggle(index) },
                shape = CircleShape,
                color = containerColor,
                tonalElevation = if (selected) 4.dp else 0.dp,
                shadowElevation = if (selected) 2.dp else 0.dp,
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor,
                    )
                }
            }
        }
    }
}
