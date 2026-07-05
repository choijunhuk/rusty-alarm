package com.example.rustyalarm.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.TextStyle
import com.example.rustyalarm.ui.theme.TimeOfDayBackground
import com.example.rustyalarm.ui.theme.isAppInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Insights
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.rustyalarm.alarm.Alarm
import com.example.rustyalarm.alarm.AlarmReliability
import com.example.rustyalarm.alarm.Permissions
import com.example.rustyalarm.alarm.ReliabilityActionKind
import com.example.rustyalarm.alarm.ReliabilityDiagnostic
import com.example.rustyalarm.alarm.ReliabilityLevel
import com.example.rustyalarm.alarm.ReliabilityIssue
import com.example.rustyalarm.ui.components.AlarmCard
import com.example.rustyalarm.viewmodel.AlarmListViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AlarmListScreen(
    nickname: String = "사용자",
    onAddAlarm: () -> Unit,
    onEditAlarm: (Alarm) -> Unit,
    onOpenStats: () -> Unit = {},
    onOpenReport: () -> Unit = {},
    onOpenPet: () -> Unit = {},
    onOpenSleep: () -> Unit = {},
    onOpenSettings: (() -> Unit)? = null,
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val vm: AlarmListViewModel = hiltViewModel()
    val alarms by vm.alarms.collectAsStateWithLifecycle()
    val groups by vm.groups.collectAsStateWithLifecycle()

    // Live tick — refreshes every 30s for countdown
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            nowMillis = System.currentTimeMillis()
        }
    }

    // Recompute trigger map only when alarms change OR an hour rolls over —
    // not every 30 s tick. Repeating alarms' next-trigger ms doesn't shift
    // minute-to-minute, so memoising on the hour avoids recomposing every
    // AlarmCard each ticker fire.
    val tickHour = nowMillis / 3_600_000L
    val nextTriggerByAlarmId = remember(alarms, tickHour) {
        alarms.associate { it.id to (nextTriggerMillis(it, nowMillis) ?: 0L) }
    }
    val nextAlarm = remember(alarms, tickHour, nextTriggerByAlarmId) {
        alarms.filter { it.enabled }
            .mapNotNull { a ->
                val t = nextTriggerByAlarmId[a.id] ?: 0L
                if (t > 0L) a to t else null
            }
            .minByOrNull { it.second }
    }
    val nextAlarmIssues = remember(nextAlarm?.first) {
        nextAlarm?.first?.let { AlarmReliability.alarmIssues(it) }.orEmpty()
    }
    val permissionsStatus = remember(nowMillis) { Permissions.status(ctx) }
    val readinessDiagnostic = remember(permissionsStatus, alarms, nextAlarm?.first) {
        AlarmReliability.diagnose(
            permissions = permissionsStatus,
            enabledAlarmCount = alarms.count { it.enabled },
            nextAlarm = nextAlarm?.first,
        )
    }
    val lastDismissedAt by vm.lastDismissedAt.collectAsStateWithLifecycle()
    val weather by vm.weather.collectAsStateWithLifecycle()
    val weeklyDismissed by vm.weeklyDismissed.collectAsStateWithLifecycle()
    var quickMenuOpen by remember { mutableStateOf(false) }

    fun openReliabilityAction(issue: ReliabilityIssue) {
        runCatching {
            when (issue.actionKind) {
                ReliabilityActionKind.NOTIFICATION_SETTINGS ->
                    ctx.startActivity(Permissions.appNotificationSettings(ctx))
                ReliabilityActionKind.EXACT_ALARM_SETTINGS ->
                    Permissions.exactAlarmSettings(ctx)?.let(ctx::startActivity)
                ReliabilityActionKind.BATTERY_SETTINGS ->
                    ctx.startActivity(Permissions.batteryOptimizationSettings(ctx))
                ReliabilityActionKind.CREATE_TEST_ALARM -> vm.quickAlarm(5)
                ReliabilityActionKind.EDIT_ALARM -> nextAlarm?.first?.let(onEditAlarm)
                ReliabilityActionKind.NONE -> Unit
            }
        }.onFailure {
            android.widget.Toast.makeText(
                ctx,
                "설정을 열 수 없어요. 시스템 설정에서 Rusty Alarm을 확인해주세요.",
                android.widget.Toast.LENGTH_SHORT,
            ).show()
        }
    }

    TimeOfDayBackground(isDark = isAppInDarkTheme()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Rusty Alarm",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    actions = {
                        IconButton(onClick = onOpenStats) {
                            Icon(
                                Icons.Default.Insights,
                                contentDescription = "전체 통계",
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            },
            floatingActionButton = {
                if (alarms.isNotEmpty()) {
                    Box {
                        ExtendedFloatingActionButton(
                            onClick = onAddAlarm,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            icon = { Icon(Icons.Default.Add, contentDescription = null) },
                            text = { Text("알람 추가", fontWeight = FontWeight.SemiBold) },
                            modifier = Modifier.combinedClickable(
                                onClick = onAddAlarm,
                                onLongClick = { quickMenuOpen = true },
                            ),
                        )
                        DropdownMenu(
                            expanded = quickMenuOpen,
                            onDismissRequest = { quickMenuOpen = false },
                        ) {
                            Text(
                                "빠른 알람",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                            listOf(5, 10, 15, 30, 60).forEach { mins ->
                                DropdownMenuItem(
                                    text = { Text("${mins}분 후 알람") },
                                    onClick = {
                                        quickMenuOpen = false
                                        vm.quickAlarm(mins)
                                    },
                                )
                            }
                        }
                    }
                }
            },
        ) { padding ->

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // ── Hero next-alarm card ──────────────
                item {
                    NextAlarmHero(
                        nickname = nickname,
                        nextAlarm = nextAlarm?.first,
                        nextMillis = nextAlarm?.second,
                        nowMillis = nowMillis,
                        lastDismissedAt = lastDismissedAt,
                        weather = weather,
                        weeklyDismissed = weeklyDismissed,
                        nextAlarmIssues = nextAlarmIssues,
                        onRecordBedtime = { vm.recordBedtime() },
                    )
                }

                item {
                    ReadinessActionPanel(
                        diagnostic = readinessDiagnostic,
                        onIssueAction = ::openReliabilityAction,
                    )
                }

                if (groups.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            groups.forEach { tag ->
                                val anyEnabled = alarms.any { it.groupTag == tag && it.enabled }
                                AssistChip(
                                    onClick = { vm.toggleGroup(tag, !anyEnabled) },
                                    label = { Text(tag) },
                                    leadingIcon = {
                                        Icon(
                                            if (anyEnabled) Icons.Default.ToggleOn
                                            else Icons.Default.ToggleOff,
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
                    item { EmptyAlarmState(onAdd = onAddAlarm) }
                } else {
                    items(alarms, key = { it.id }) { alarm ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { v ->
                                when (v) {
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        vm.deleteAlarm(alarm); true
                                    }
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        vm.toggleAlarm(alarm, !alarm.enabled); false
                                    }
                                    else -> false
                                }
                            },
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val isToggle = dismissState.targetValue ==
                                    SwipeToDismissBoxValue.StartToEnd
                                val align = if (isToggle) Alignment.CenterStart
                                else Alignment.CenterEnd
                                val tint = if (isToggle) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                                val ic = if (isToggle) {
                                    if (alarm.enabled) Icons.Default.ToggleOff
                                    else Icons.Default.ToggleOn
                                } else Icons.Default.Delete
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 24.dp),
                                    contentAlignment = align,
                                ) {
                                    Icon(
                                        ic,
                                        contentDescription = if (isToggle) "토글" else "삭제",
                                        tint = tint,
                                    )
                                }
                            },
                        ) {
                            val ctx = androidx.compose.ui.platform.LocalContext.current
                            AlarmCard(
                                alarm = alarm,
                                onToggle = { enabled -> vm.toggleAlarm(alarm, enabled) },
                                onClick = { onEditAlarm(alarm) },
                                onDelete = { vm.deleteAlarm(alarm) },
                                onDuplicate = { vm.duplicateAlarm(alarm) },
                                nextTriggerMillis = nextTriggerByAlarmId[alarm.id]
                                    ?.takeIf { it > 0L },
                                onShare = {
                                    val msg = buildString {
                                        append("⏰ ${alarm.title}\n")
                                        append("%02d:%02d".format(alarm.hour, alarm.minute))
                                        if (alarm.message.isNotBlank()) append("\n💬 ${alarm.message}")
                                    }
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_SEND
                                    ).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_TEXT, msg)
                                    }
                                    ctx.startActivity(
                                        android.content.Intent.createChooser(intent, "알람 공유")
                                    )
                                },
                            )
                        }
                    }
                }

                item {
                    Text(
                        "made by 최준혁",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        textAlign = TextAlign.Center,
                    )
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun ReadinessActionPanel(
    diagnostic: ReliabilityDiagnostic,
    onIssueAction: (ReliabilityIssue) -> Unit,
) {
    val primaryIssue = AlarmReliability.primaryIssue(diagnostic.issues)
    if (primaryIssue == null && diagnostic.level == ReliabilityLevel.READY) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        "다음 알람 준비도",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        diagnostic.headline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                    )
                }
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text("${diagnostic.score} · ${diagnostic.level.label}") },
                )
            }

            primaryIssue?.let { issue ->
                Text(
                    issue.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    issue.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                )
                issue.actionLabel?.let { label ->
                    FilledTonalButton(
                        onClick = { onIssueAction(issue) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(label)
                    }
                }
            }

            val extraCount = diagnostic.issues.size - 1
            if (extraCount > 0) {
                Text(
                    "추가 보강 ${extraCount}개는 설정과 알람 편집에서 이어서 조정할 수 있어요.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}

private val TabularDigits = TextStyle(fontFeatureSettings = "tnum")

@Composable
private fun NextAlarmHero(
    nickname: String,
    nextAlarm: Alarm?,
    nextMillis: Long?,
    nowMillis: Long,
    lastDismissedAt: Long?,
    weather: com.example.rustyalarm.weather.Weather?,
    weeklyDismissed: Int = 0,
    nextAlarmIssues: List<ReliabilityIssue> = emptyList(),
    onRecordBedtime: () -> Unit = {},
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "$nickname 님 · ${greetingText()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f),
                )
                weather?.let { w ->
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${w.emoji} ${"%.0f".format(w.tempCelsius)}°",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                            fontWeight = FontWeight.SemiBold,
                        )
                        val parts = buildList {
                            if (!w.maxC.isNaN() && !w.minC.isNaN())
                                add("↑%.0f° ↓%.0f°".format(w.maxC, w.minC))
                            if (w.precipProb in 1..100)
                                add("💧${w.precipProb}%")
                        }
                        if (parts.isNotEmpty()) {
                            Text(
                                parts.joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            if (nextAlarm == null || nextMillis == null) {
                Text(
                    "예정된 알람이 없어요",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "알람을 추가해서 시작해보세요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
            } else {
                Text(
                    text = "다음 알람",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                )
                val pulse = rememberInfiniteTransition(label = "heroPulse")
                val breath by pulse.animateFloat(
                    initialValue = 0.995f, targetValue = 1.012f,
                    animationSpec = infiniteRepeatable(
                        tween(2400, easing = FastOutSlowInEasing),
                        RepeatMode.Reverse,
                    ),
                    label = "breath",
                )
                Text(
                    text = "%02d:%02d".format(nextAlarm.hour, nextAlarm.minute),
                    fontSize = 60.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    letterSpacing = (-1).sp,
                    style = TabularDigits,
                    modifier = Modifier.scale(breath),
                )
                Text(
                    text = "${nextAlarm.title} · ${countdownText(nextMillis - nowMillis)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                )

                weather?.forecastAt(nextMillis)?.let { (temp, emo) ->
                    Text(
                        text = "$emo 알람 시각 ${"%.0f".format(temp)}°",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                    )
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val bedtimeMs = nextMillis - 7L * 60 * 60 * 1000L - 30 * 60 * 1000L
                    if (bedtimeMs > nowMillis) {
                        val bedtimeStr = SimpleDateFormat("a h:mm", Locale.KOREAN)
                            .format(Date(bedtimeMs))
                        HeroPill(
                            icon = Icons.Default.Bedtime,
                            text = bedtimeStr,
                            onClick = { onRecordBedtime() },
                        )
                    }
                    if (weeklyDismissed > 0) {
                        HeroPill(
                            icon = Icons.Default.LocalFireDepartment,
                            text = "이번 주 ${weeklyDismissed}",
                        )
                    }
                    HeroPill(
                        icon = Icons.Default.Alarm,
                        text = if (nextAlarmIssues.isEmpty()) "실사용 안정"
                               else "보강 ${nextAlarmIssues.size}개",
                    )
                }
                nextAlarmIssues.firstOrNull()?.let { issue ->
                    Text(
                        text = issue.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.62f),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Icon(
            icon, contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
        )
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun EmptyAlarmState(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            Icons.Default.Alarm,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
            modifier = Modifier.size(96.dp),
        )
        Text(
            "조용한 아침이네요",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "첫 알람을 등록해서\n내일 아침을 시작해보세요",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
        )
        FilledTonalButton(
            onClick = onAdd,
            shape = RoundedCornerShape(14.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null,
                modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("알람 추가하기")
        }
    }
}

private fun countdownText(deltaMs: Long): String {
    if (deltaMs <= 0) return "곧 울려요"
    val totalMin = max(1L, deltaMs / 60_000)
    val days = totalMin / (60 * 24)
    val hours = (totalMin / 60) % 24
    val mins = totalMin % 60
    return when {
        days > 0 -> "${days}일 ${hours}시간 후"
        hours > 0 -> "${hours}시간 ${mins}분 후"
        else -> "${mins}분 후"
    }
}

private fun nextTriggerMillis(alarm: Alarm, now: Long): Long? {
    if (!alarm.enabled) return null

    // Specific-date alarm
    alarm.specificDate?.let { dateMs ->
        val cal = Calendar.getInstance().apply {
            timeInMillis = dateMs
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis.takeIf { it > now }
    }

    val base = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, alarm.hour)
        set(Calendar.MINUTE, alarm.minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    // No repeat days — once, next occurrence today or tomorrow
    if (alarm.repeatDays.isEmpty()) {
        if (base.timeInMillis > now) return base.timeInMillis
        base.add(Calendar.DAY_OF_YEAR, 1)
        return base.timeInMillis
    }

    // Repeat — find next matching weekday (Calendar.SUNDAY=1..SATURDAY=7;
    // Alarm.repeatDays use 0=Mon..6=Sun? Adapt: try both, take min in 7 days)
    for (i in 0..7) {
        val candidate = (base.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, i) }
        if (candidate.timeInMillis <= now) continue
        val dow = candidate.get(Calendar.DAY_OF_WEEK) // SUNDAY=1..SATURDAY=7
        // Map both common conventions:
        // Convention A (Mon=0..Sun=6): mon=0,tue=1,wed=2,thu=3,fri=4,sat=5,sun=6
        val convA = when (dow) {
            Calendar.MONDAY -> 0; Calendar.TUESDAY -> 1; Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3; Calendar.FRIDAY -> 4; Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6; else -> -1
        }
        // Convention B (Sun=1..Sat=7) — Calendar's own
        val convB = dow
        if (convA in alarm.repeatDays || convB in alarm.repeatDays) {
            return candidate.timeInMillis
        }
    }
    return null
}

private fun greetingText(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 6  -> "밤이 깊었네요 🌙"
        hour < 12 -> "좋은 아침이에요 ☀️"
        hour < 18 -> "좋은 오후예요 🌤"
        else      -> "좋은 저녁이에요 🌆"
    }
}
