package com.example.rustyalarm.ui.screens

import com.example.rustyalarm.ui.theme.screenBackgroundBrush

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rustyalarm.sleep.NoiseColor
import com.example.rustyalarm.sleep.SleepSoundService
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepSoundsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val selected by SleepSoundService.state.collectAsStateWithLifecycle()
    val playing = selected != null

    var timerMin by remember { mutableIntStateOf(0) }
    var remainingSec by remember { mutableIntStateOf(0) }

    LaunchedEffect(playing, timerMin) {
        if (playing && timerMin > 0) {
            remainingSec = timerMin * 60
            while (isActive && remainingSec > 0 && SleepSoundService.state.value != null) {
                delay(1000)
                remainingSec--
            }
            if (SleepSoundService.state.value != null) {
                SleepSoundService.stop(context)
            }
        } else {
            remainingSec = 0
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBackgroundBrush()),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("수면 사운드", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (playing) {
                    Text(
                        "🌙 백그라운드에서 계속 재생돼요. 알림에서 정지할 수 있어요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Text(
                    "사운드 선택",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )

                NoiseColor.entries.forEach { color ->
                    SoundRow(
                        color = color,
                        selected = selected == color,
                        onSelect = {
                            if (selected == color) {
                                SleepSoundService.stop(context)
                            } else {
                                SleepSoundService.start(context, color)
                            }
                        },
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                Text(
                    "타이머",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "끔", 10 to "10분", 30 to "30분", 60 to "60분").forEach { (m, l) ->
                        FilterChip(
                            selected = timerMin == m,
                            onClick = { timerMin = m },
                            label = { Text(l) },
                        )
                    }
                }
                if (timerMin > 0 && playing) {
                    Text(
                        "남은 시간 — ${"%02d:%02d".format(remainingSec / 60, remainingSec % 60)}",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Spacer(Modifier.weight(1f))

                if (playing) {
                    Button(
                        onClick = { SleepSoundService.stop(context) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("정지")
                    }
                }
            }
        }
    }
}

@Composable
private fun SoundRow(
    color: NoiseColor,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                             else MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(16.dp),
        onClick = onSelect,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(color.emoji, fontSize = 28.sp)
            Text(
                color.label,
                fontWeight = FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(
                if (selected) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                       else MaterialTheme.colorScheme.secondary,
            )
        }
    }
}
