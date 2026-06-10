package com.example.rustyalarm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rustyalarm.alarm.AlarmEventDao
import com.example.rustyalarm.alarm.ChallengeType
import com.example.rustyalarm.ui.components.BarChart
import com.example.rustyalarm.ui.components.BarDatum
import com.example.rustyalarm.viewmodel.StatsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    eventDao: AlarmEventDao,
    onBack: () -> Unit,
) {
    val vm: StatsViewModel = viewModel(factory = StatsViewModel.Factory(eventDao))
    val daily      by vm.dailyCounts.collectAsStateWithLifecycle()
    val challenges by vm.challengeBreakdown.collectAsStateWithLifecycle()
    val summary    by vm.summary.collectAsStateWithLifecycle()

    val chartData = remember(daily) {
        if (daily.isEmpty()) {
            emptyList()
        } else {
            // Last 7 days, fill missing with 0
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val cal = Calendar.getInstance()
            val labelFmt = SimpleDateFormat("E", Locale.KOREAN)
            (6 downTo 0).map { offset ->
                cal.timeInMillis = System.currentTimeMillis()
                cal.add(Calendar.DAY_OF_YEAR, -offset)
                val key = fmt.format(cal.time)
                val labelDay = labelFmt.format(cal.time)
                val cnt = daily.firstOrNull { it.dayKey == key }?.cnt ?: 0
                BarDatum(labelDay, cnt.toFloat())
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0D0D22), Color(0xFF0A0A1A)))
            ),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("알람 통계") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
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
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Metric cards row
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard("총 알람 울림", summary.fired.toString(), Modifier.weight(1f))
                    MetricCard("총 스누즈", summary.snoozed.toString(), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard(
                        "평균 반응 시간",
                        "${summary.avgResponseSec.toInt()}초",
                        Modifier.weight(1f),
                    )
                    MetricCard(
                        "기상 완료율",
                        if (summary.fired == 0) "—"
                        else "${(summary.dismissed * 100 / maxOf(1, summary.fired))}%",
                        Modifier.weight(1f),
                    )
                }

                // Daily bar chart
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "최근 7일 알람 횟수",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(Modifier.height(12.dp))
                        if (chartData.isEmpty()) {
                            Text(
                                "데이터 없음",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            )
                        } else {
                            BarChart(data = chartData, height = 180.dp)
                        }
                    }
                }

                // Challenge breakdown
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "챌린지 사용 현황",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(Modifier.height(12.dp))
                        if (challenges.isEmpty()) {
                            Text(
                                "데이터 없음",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            )
                        } else {
                            challenges.forEach { stat ->
                                val label = runCatching {
                                    ChallengeType.valueOf(stat.challengeType).label
                                }.getOrDefault(stat.challengeType)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(label, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        "${stat.cnt}회",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
