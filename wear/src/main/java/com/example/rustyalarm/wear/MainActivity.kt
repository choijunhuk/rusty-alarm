package com.example.rustyalarm.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                NextAlarmFace()
            }
        }
    }
}

@Composable
private fun NextAlarmFace() {
    val state by NextAlarmStore.state.collectAsState()
    val brush = Brush.radialGradient(
        listOf(Color(0xFF2A1F5C), Color(0xFF0F0D26)),
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(8.dp),
        ) {
            Text(
                text = "⏰",
                fontSize = 18.sp,
                color = Color(0xFF9B8AFF),
            )
            Text(
                text = "다음 알람",
                fontSize = 11.sp,
                color = Color(0xFFCAC2E8),
            )
            Text(
                text = state.timeText ?: "없음",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            state.title?.let {
                Text(
                    text = it,
                    fontSize = 10.sp,
                    color = Color(0xFFCAC2E8),
                )
            }
            state.untilText?.let {
                Box(
                    modifier = Modifier
                        .background(
                            Color(0xFF9B8AFF).copy(alpha = 0.18f),
                            RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = it,
                        fontSize = 10.sp,
                        color = Color(0xFFFFB088),
                    )
                }
            }
            state.readinessLabel?.let {
                Text(
                    text = "준비 상태 · $it",
                    fontSize = 10.sp,
                    color = if (it == "좋음") Color(0xFF8FE3B0) else Color(0xFFFFB088),
                )
            }
            if (state.requiresPhone) {
                Text(
                    text = "해제는 폰 챌린지에서",
                    fontSize = 9.sp,
                    color = Color(0xFFFFB088),
                )
            } else if (state.canSnooze || state.canDismiss) {
                Text(
                    text = listOfNotNull(
                        "스누즈".takeIf { state.canSnooze },
                        "해제".takeIf { state.canDismiss },
                    ).joinToString(" · ") + " 가능",
                    fontSize = 9.sp,
                    color = Color(0xFFCAC2E8),
                )
            }
        }
    }
}

internal fun formatTime(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

internal fun formatUntil(deltaMs: Long): String {
    if (deltaMs <= 0) return "곧"
    val mins = deltaMs / 60_000L
    val days = mins / (60 * 24)
    val hours = (mins / 60) % 24
    val rem = mins % 60
    return when {
        days > 0 -> "${days}일 후"
        hours > 0 -> "${hours}시간 후"
        else -> "${rem}분 후"
    }
}
