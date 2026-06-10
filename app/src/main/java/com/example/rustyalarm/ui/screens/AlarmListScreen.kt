package com.example.rustyalarm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rustyalarm.alarm.Alarm
import com.example.rustyalarm.alarm.AlarmRepository
import com.example.rustyalarm.ui.components.AlarmCard
import com.example.rustyalarm.viewmodel.AlarmListViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    repository: AlarmRepository,
    nickname: String = "사용자",
    onAddAlarm: () -> Unit,
    onEditAlarm: (Alarm) -> Unit,
    onOpenStats: () -> Unit = {},
    onOpenReport: () -> Unit = {},
    onOpenPet: () -> Unit = {},
    onOpenSleep: () -> Unit = {},
    onOpenSettings: (() -> Unit)? = null,
) {
    val vm: AlarmListViewModel = viewModel(factory = AlarmListViewModel.Factory(repository))
    val alarms by vm.alarms.collectAsStateWithLifecycle()
    val groups by vm.groups.collectAsStateWithLifecycle()

    // Live current time for header
    var currentTime by remember { mutableStateOf(currentTimeString()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1_000)
            currentTime = currentTimeString()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D0D22), Color(0xFF0A0A1A))
                )
            ),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Rusty Alarm",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenSleep) {
                            Icon(
                                Icons.Default.Bedtime,
                                contentDescription = "수면 사운드",
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        IconButton(onClick = onOpenPet) {
                            Icon(
                                Icons.Default.Pets,
                                contentDescription = "펫",
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        IconButton(onClick = onOpenReport) {
                            Icon(
                                Icons.Default.Assessment,
                                contentDescription = "리포트",
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        IconButton(onClick = onOpenStats) {
                            Icon(
                                Icons.Default.Insights,
                                contentDescription = "통계",
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        if (onOpenSettings != null) {
                            IconButton(onClick = onOpenSettings) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "설정",
                                    tint = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onAddAlarm,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Default.Add, contentDescription = "알람 추가")
                }
            },
        ) { padding ->

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 88.dp),
            ) {
                // Live clock header
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = currentTime,
                            fontSize = 56.sp,
                            fontWeight = FontWeight.ExtraLight,
                            color = MaterialTheme.colorScheme.onBackground,
                            letterSpacing = 2.sp,
                        )
                        Text(
                            text = "$nickname 님, ${greetingText()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }

                if (groups.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            groups.forEach { tag ->
                                val anyEnabled = alarms.any { it.groupTag == tag && it.enabled }
                                AssistChip(
                                    onClick = { vm.toggleGroup(tag, !anyEnabled) },
                                    label = { Text(tag) },
                                    leadingIcon = {
                                        Icon(
                                            if (anyEnabled) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                                            contentDescription = null,
                                            tint = if (anyEnabled) MaterialTheme.colorScheme.primary
                                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }

                if (alarms.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Icon(
                                Icons.Default.Alarm,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                                modifier = Modifier.size(80.dp),
                            )
                            Text(
                                text = "등록된 알람이 없습니다",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = "+ 버튼을 눌러 추가하세요",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                            )
                        }
                    }
                } else {
                    items(alarms, key = { it.id }) { alarm ->
                        AlarmCard(
                            alarm = alarm,
                            onToggle = { enabled -> vm.toggleAlarm(alarm, enabled) },
                            onClick = { onEditAlarm(alarm) },
                            onDelete = { vm.deleteAlarm(alarm) },
                        )
                    }
                }
            }
        }
    }
}

private fun currentTimeString(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

private fun greetingText(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 6  -> "밤이 깊었네요 🌙"
        hour < 12 -> "좋은 아침이에요 ☀️"
        hour < 18 -> "좋은 오후예요 🌤"
        else      -> "좋은 저녁이에요 🌆"
    }
}
