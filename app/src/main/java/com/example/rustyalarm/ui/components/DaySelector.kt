package com.example.rustyalarm.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
            val containerColor = if (selected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant

            val contentColor = if (selected)
                MaterialTheme.colorScheme.onPrimary
            else
                MaterialTheme.colorScheme.onSurfaceVariant

            Surface(
                onClick = { onDayToggle(index) },
                shape = CircleShape,
                color = containerColor,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        color = contentColor,
                    )
                }
            }
        }
    }
}
