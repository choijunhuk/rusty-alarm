package com.example.rustyalarm.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rustyalarm.rust.RustAlarmCore

@Composable
fun AlarmRingScreen(
    alarmId: Long,
    title: String,
    hour: Int,
    minute: Int,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
) {
    val timeText = RustAlarmCore.formatTime(hour, minute)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = timeText,
            fontSize = 72.sp,
            fontWeight = FontWeight.Thin,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(64.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier.weight(1f).height(56.dp),
            ) {
                Text("5분 뒤")
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text("끄기", fontWeight = FontWeight.Bold)
            }
        }
    }
}
