package com.example.rustyalarm.ui.screens
import com.example.rustyalarm.ui.theme.screenBackgroundBrush

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.rustyalarm.alarm.AlarmRepository
import com.example.rustyalarm.viewmodel.ReportViewModel
import com.example.rustyalarm.viewmodel.WakeupInsight
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    repository: AlarmRepository,
    onBack: () -> Unit,
) {
    val vm: ReportViewModel = hiltViewModel()
    val report by vm.report.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var pendingInsight by remember { mutableStateOf<WakeupInsight?>(null) }
    var appliedMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBackgroundBrush()),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("주간 리포트") },
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
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Empty state — no wakeup events recorded yet
                if (report.ready && report.fired == 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("🌅", fontSize = 40.sp)
                            Text(
                                "아직 기상 기록이 없어요",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "알람을 설정하고 첫 기상을 하면\n주간 리포트가 여기에 쌓여요.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                }

                // Streak hero
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Icon(
                            Icons.Default.LocalFireDepartment, null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(48.dp),
                        )
                        Column {
                            Text("연속 기상 ${report.streakDays}일",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                when {
                                    report.streakDays >= 7 -> "일주일 연속 잘 일어나고 있어요"
                                    report.streakDays > 0  -> "오늘도 잘 일어났어요"
                                    else                   -> "오늘 첫 기상으로 시작해보세요"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            )
                        }
                    }
                }

                // Metric cards
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricBox(
                        Icons.Default.Schedule,
                        "평균 기상 시각",
                        report.avgWakeupHHMM,
                        Modifier.weight(1f),
                    )
                    MetricBox(
                        Icons.Default.TrendingUp,
                        "기상 완료율",
                        if (report.fired == 0) "—" else "${report.completionRatePercent}%",
                        Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricBox(
                        Icons.Default.Schedule,
                        "평균 끄기 시간",
                        report.avgResponseLabel,
                        Modifier.weight(1f),
                    )
                    MetricBox(
                        Icons.Default.TrendingUp,
                        "스누즈 비율",
                        if (report.fired == 0) "—" else "${report.snoozeRatePercent}%",
                        Modifier.weight(1f),
                    )
                }

                if (report.insights.isNotEmpty()) Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("다음 개선 액션",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary)
                        report.insights.take(3).forEach { insight ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    insight.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    insight.detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                )
                                insight.cause?.let { cause ->
                                    Text(
                                        cause,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                insight.action?.let { action ->
                                    Text(
                                        action.expectedResult,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                    )
                                    TextButton(onClick = { pendingInsight = insight }) {
                                        Text(action.label)
                                    }
                                }
                            }
                        }
                        appliedMessage?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("이번 주 요약",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.height(12.dp))
                        SummaryRow("울린 알람", "${report.fired}회")
                        SummaryRow("끈 알람", "${report.dismissed}회")
                        SummaryRow("스누즈", "${report.snoozed}회")
                        if (report.challengesCompleted > 0) {
                            SummaryRow("챌린지 완수", "${report.challengesCompleted}회")
                        }
                    }
                }

                // ── Monthly heatmap ─────────────────────
                if (report.heatmap.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("최근 30일 기상 히트맵",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(12.dp))
                            HeatmapGrid(data = report.heatmap)
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("적음",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                listOf(0.15f, 0.35f, 0.6f, 0.9f).forEach { level ->
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = level),
                                                RoundedCornerShape(2.dp),
                                            )
                                    )
                                }
                                Text("많음",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }
                    }
                }

                if (!report.ready) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    pendingInsight?.action?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingInsight = null },
            title = { Text("${action.preset.label} 모드를 적용할까요?") },
            text = { Text("가장 가까운 활성 알람의 주요 설정이 바뀝니다. 적용 후에도 알람 편집에서 조정할 수 있어요.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingInsight = null
                        scope.launch {
                            val updated = repository.applyPresetToNextEnabled(action.preset)
                            appliedMessage = if (updated == null) {
                                "먼저 활성 알람을 하나 만들어주세요."
                            } else {
                                "'${updated.title}'에 ${action.preset.label} 모드를 적용했어요."
                            }
                        }
                    },
                ) { Text("적용") }
            },
            dismissButton = {
                TextButton(onClick = { pendingInsight = null }) { Text("취소") }
            },
        )
    }
}

@Composable
private fun HeatmapGrid(data: List<Pair<String, Int>>) {
    val maxCount = data.maxOf { it.second }.coerceAtLeast(1)
    // 5 rows x 6 columns = 30 cells
    val rows = 5
    val cols = 6
    val cellSize = 28.dp
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(rows) { r ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(cols) { c ->
                    val idx = r * cols + c
                    if (idx < data.size) {
                        val (_, count) = data[idx]
                        val intensity = if (count == 0) 0.08f
                                        else 0.2f + 0.8f * (count.toFloat() / maxCount)
                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = intensity),
                                    RoundedCornerShape(4.dp),
                                )
                        )
                    } else {
                        Box(modifier = Modifier.size(cellSize))
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface)
        Text(value, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
    }
}
