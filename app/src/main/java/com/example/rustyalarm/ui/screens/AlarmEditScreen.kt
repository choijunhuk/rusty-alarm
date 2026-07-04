package com.example.rustyalarm.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import android.app.NotificationManager
import android.media.AudioManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.rustyalarm.alarm.Alarm
import com.example.rustyalarm.alarm.AlarmReliability
import com.example.rustyalarm.alarm.ChallengeType
import com.example.rustyalarm.alarm.WakeupPreset
import com.example.rustyalarm.alarm.WakeupPresetApplier
import com.example.rustyalarm.ui.components.DaySelector
import com.example.rustyalarm.ui.components.TimePickerSection
import com.example.rustyalarm.viewmodel.AlarmEditViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlarmEditScreen(
    alarmId: Long,
    onBack: () -> Unit,
) {
    val vm: AlarmEditViewModel = hiltViewModel()
    val context = LocalContext.current
    val alarm    by vm.alarm.collectAsStateWithLifecycle()
    val saved    by vm.saved.collectAsStateWithLifecycle()
    val isLoaded by vm.isLoaded.collectAsStateWithLifecycle()

    LaunchedEffect(alarmId) { vm.load(alarmId) }
    val saveToast by vm.saveToast.collectAsStateWithLifecycle()
    LaunchedEffect(saved) {
        if (saved) {
            saveToast?.let {
                android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            }
            onBack()
        }
    }

    val isEdit = alarmId != -1L
    var showDeleteDialog       by remember { mutableStateOf(false) }
    var challengeMenuExpanded  by remember { mutableStateOf(false) }
    var showDatePicker         by remember { mutableStateOf(false) }
    var warning                by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Ringtone picker launcher
    val ringtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val uri = result.data
                ?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            vm.updateRingtoneUri(uri?.toString())
        }
    }

    fun launchRingtonePicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            alarm.ringtoneUri?.let {
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it))
            }
        }
        ringtoneLauncher.launch(intent)
    }

    // Ringtone preview lifecycle — stops on screen leave or after 10 s
    var preview by remember { mutableStateOf<Ringtone?>(null) }
    var previewing by remember { mutableStateOf(false) }
    var previewJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            previewJob?.cancel()
            preview?.runCatching { stop() }; preview = null
        }
    }
    fun togglePreview() {
        if (previewing) {
            previewJob?.cancel(); previewJob = null
            preview?.runCatching { stop() }
            preview = null
            previewing = false
            return
        }
        val uri = alarm.ringtoneUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: return
        runCatching {
            val r = RingtoneManager.getRingtone(context, uri)
            r.play()
            preview = r
            previewing = true
            previewJob = scope.launch {
                kotlinx.coroutines.delay(10_000)
                preview?.runCatching { stop() }
                preview = null
                previewing = false
            }
        }
    }

    val ringtoneLabel = remember(alarm.ringtoneUri) {
        alarm.ringtoneUri?.let { uriStr ->
            runCatching {
                val ringtone = RingtoneManager.getRingtone(context, Uri.parse(uriStr))
                ringtone.getTitle(context)
            }.getOrNull()
        } ?: "기본 알람음"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEdit) "알람 수정" else "알람 추가",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    if (isEdit) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "삭제",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (!isLoaded) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── HERO time picker card ───────────────────
            HeroTimeCard {
                key("loaded") {
                    TimePickerSection(
                        hour = alarm.hour, minute = alarm.minute,
                        onHourChange = vm::updateHour, onMinuteChange = vm::updateMinute,
                    )
                }
            }

            // ── Quick presets — only new alarm ─────────
            if (!isEdit) {
                SectionCard(
                    title = "빠른 설정",
                    icon = Icons.Default.AccessTime,
                    initiallyExpanded = true,
                ) {
                    Text(
                        "지금부터 시간 지나면 울려요",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        listOf(
                            5 to "5분 후",
                            15 to "15분 후",
                            30 to "30분 후",
                            60 to "1시간 후",
                            180 to "3시간 후",
                            480 to "8시간 후",
                        ).forEach { (min, label) ->
                            AssistChip(
                                onClick = { vm.saveQuickFromNow(min, label) },
                                label = { Text(label) },
                            )
                        }
                    }
                }
            }

            // ── Real-use success profile ─────────────
            SectionCard(
                title = "기상 성공률",
                icon = Icons.Default.NotificationsActive,
                initiallyExpanded = true,
            ) {
                val profile = remember(alarm) { WakeupPresetApplier.profile(alarm) }
                Text(
                    "${profile.title} · ${profile.level.label}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    profile.recommendation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    WakeupPreset.entries.forEach { preset ->
                        AssistChip(
                            onClick = { vm.applyWakeupPreset(preset) },
                            label = { Text(preset.label) },
                        )
                    }
                }
                Text(
                    "기상 모드는 현재 알람에 바로 반영되고, 저장을 눌러야 적용돼요.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }

            // ── 기본 정보 ──────────────────────────────
            SectionCard(
                title = "기본 정보",
                icon = Icons.Default.Edit,
                initiallyExpanded = true,
            ) {
                OutlinedTextField(
                    value = alarm.title, onValueChange = vm::updateTitle,
                    label = { Text("알람 이름") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = alarm.message,
                    onValueChange = vm::updateMessage,
                    label = { Text("한 줄 메시지 (선택)") },
                    placeholder = { Text("예: 물 한 잔 마시기") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                ToggleRow(
                    label = "특정 날짜 지정",
                    checked = alarm.specificDate != null,
                    onCheckedChange = { on ->
                        vm.updateSpecificDate(if (on) System.currentTimeMillis() else null)
                        if (on) showDatePicker = true
                    },
                )

                val specificDateMillis = alarm.specificDate
                if (specificDateMillis != null) {
                    val dateLabel = remember(specificDateMillis) {
                        SimpleDateFormat("yyyy년 M월 d일 (E)", Locale.KOREAN)
                            .format(Date(specificDateMillis))
                    }
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(dateLabel)
                    }
                } else {
                    // Repeat presets
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        val presets = listOf(
                            "평일" to listOf(1, 2, 3, 4, 5),
                            "주말" to listOf(0, 6),
                            "매일" to listOf(0, 1, 2, 3, 4, 5, 6),
                            "한 번만" to emptyList(),
                        )
                        presets.forEach { (label, days) ->
                            val active = alarm.repeatDays.sorted() == days.sorted()
                            FilterChip(
                                selected = active,
                                onClick = { vm.setRepeatDays(days) },
                                label = { Text(label) },
                            )
                        }
                    }
                    DaySelector(
                        selectedDays = alarm.repeatDays,
                        onDayToggle = vm::toggleRepeatDay,
                    )
                }
            }

            // ── 소리 & 진동 ─────────────────────────────
            SectionCard(
                title = "소리 & 진동",
                icon = Icons.Default.MusicNote,
                initiallyExpanded = true,
            ) {
                ToggleRow("알람 소리", alarm.soundEnabled, vm::updateSoundEnabled)

                if (alarm.soundEnabled) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = ::launchRingtonePicker,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(
                                Icons.Default.MusicNote, contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(ringtoneLabel, maxLines = 1)
                        }
                        FilledTonalIconButton(
                            onClick = ::togglePreview,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                if (previewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (previewing) "미리듣기 정지" else "미리듣기",
                            )
                        }
                    }

                    LabeledSlider(
                        label = if (alarm.volumeRampSeconds == 0) "볼륨 페이드인 — 끔"
                                else "볼륨 페이드인 ${alarm.volumeRampSeconds}초",
                        value = alarm.volumeRampSeconds.toFloat(),
                        onValueChange = { vm.updateVolumeRamp(it.toInt()) },
                        valueRange = 0f..30f,
                        steps = 5,
                    )
                }

                if (alarm.soundEnabled) {
                    LabeledSlider(
                        label = "알람 음량 ${alarm.alarmVolumePercent}%",
                        value = alarm.alarmVolumePercent.toFloat(),
                        onValueChange = { vm.updateAlarmVolumePercent(it.toInt()) },
                        valueRange = 0f..100f,
                        steps = 9,
                    )
                }

                ToggleRow("진동", alarm.vibrate, vm::updateVibrate)
                ToggleRow(
                    "단계적 알람 (진동→약→강)",
                    alarm.gradualWakeup, vm::updateGradualWakeup,
                )

                // YouTube BGM
                OutlinedTextField(
                    value = alarm.youtubeUrl ?: "",
                    onValueChange = vm::updateYoutubeUrl,
                    label = { Text("YouTube 영상/플레이리스트 URL (선택)") },
                    placeholder = { Text("youtube.com/playlist?list=…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!alarm.youtubeUrl.isNullOrBlank()) {
                    val thumb = remember(alarm.youtubeUrl) {
                        com.example.rustyalarm.ui.components.youtubeThumbnailUrl(alarm.youtubeUrl!!)
                    }
                    if (thumb != null) {
                        coil.compose.AsyncImage(
                            model = thumb,
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(12.dp)),
                        )
                        Text(
                            "▶ 알람 울릴 때 자동 재생돼요. Wi-Fi/데이터 필요.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Text(
                            "⚠️ URL 형식이 올바르지 않아요. youtube.com/watch / youtu.be / shorts / playlist 지원.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            // ── 챌린지 ─────────────────────────────────
            SectionCard(
                title = "알람 끄기 챌린지",
                icon = Icons.Default.Bolt,
                initiallyExpanded = true,
            ) {
                ExposedDropdownMenuBox(
                    expanded = challengeMenuExpanded,
                    onExpandedChange = { challengeMenuExpanded = it },
                ) {
                    OutlinedTextField(
                        value = alarm.challengeType.label, onValueChange = {},
                        readOnly = true, label = { Text("챌린지 유형") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(challengeMenuExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = challengeMenuExpanded,
                        onDismissRequest = { challengeMenuExpanded = false },
                    ) {
                        ChallengeType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.label) },
                                onClick = { vm.updateChallengeType(type); challengeMenuExpanded = false },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }

                // Math problem count
                if (alarm.challengeType == ChallengeType.MATH_EASY ||
                    alarm.challengeType == ChallengeType.MATH_MEDIUM ||
                    alarm.challengeType == ChallengeType.MATH_HARD) {
                    LabeledSlider(
                        label = "수학 문제 ${alarm.mathProblemCount}개",
                        value = alarm.mathProblemCount.toFloat(),
                        onValueChange = { vm.updateMathProblemCount(it.toInt()) },
                        valueRange = 1f..10f,
                        steps = 8,
                    )
                }

                // Location
                if (alarm.challengeType == ChallengeType.LOCATION) {
                    val lat = alarm.geofenceLat
                    val lng = alarm.geofenceLng
                    Text(
                        if (lat != null && lng != null)
                            "위치 — %.5f, %.5f".format(lat, lng)
                        else "위치 미지정",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    LabeledSlider(
                        label = "허용 반경 ${alarm.geofenceRadius}m",
                        value = alarm.geofenceRadius.toFloat(),
                        onValueChange = { vm.updateGeofence(lat, lng, it.toInt()) },
                        valueRange = 20f..500f,
                        steps = 9,
                    )
                    OutlinedButton(
                        onClick = {
                            captureCurrentLocation(context) { la, lo ->
                                vm.updateGeofence(la, lo)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("📍 현재 위치 사용")
                    }
                }
            }

            // ── 기상 루틴 ──────────────────────────────
            SectionCard(
                title = "기상 후 루틴",
                icon = Icons.Default.Checklist,
                initiallyExpanded = false,
            ) {
                Text(
                    "체크리스트로 알람 끄기를 막아요. 한 줄에 하나씩 적어주세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                OutlinedTextField(
                    value = alarm.routineItems.joinToString("\n"),
                    onValueChange = vm::updateRoutineItems,
                    label = { Text("기상 후 할 일") },
                    placeholder = { Text("물 한 잔\n스트레칭\n양치") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (alarm.routineItems.isNotEmpty()) {
                    Text(
                        "✓ ${alarm.routineItems.size}개 항목",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // ── 고급 ──────────────────────────────────
            SectionCard(
                title = "고급 설정",
                icon = Icons.Default.Tune,
                initiallyExpanded = false,
            ) {
                LabeledSlider(
                    label = if (alarm.maxSnoozes == 0) "스누즈 무제한"
                            else "스누즈 최대 ${alarm.maxSnoozes}회",
                    value = alarm.maxSnoozes.toFloat(),
                    onValueChange = { vm.updateMaxSnoozes(it.toInt()) },
                    valueRange = 0f..10f,
                    steps = 9,
                )

                Text(
                    "미리알림 (부드러운 진동) — N분 전",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    listOf(0 to "끔", 5 to "5분", 10 to "10분", 15 to "15분", 30 to "30분")
                        .forEach { (mins, label) ->
                            FilterChip(
                                selected = alarm.preAlarmMinutes == mins,
                                onClick = { vm.updatePreAlarmMinutes(mins) },
                                label = { Text(label) },
                            )
                        }
                }

                ToggleRow(
                    "얕은 잠 자동 감지 (스마트 알람)",
                    alarm.isSmartAlarm,
                    vm::updateSmartAlarm,
                )
                if (alarm.isSmartAlarm) {
                    Text(
                        "기상 ${alarm.smartWindowMinutes}분 전부터 가속도계로 잠 깊이를 분석해, " +
                            "얕은 잠 구간에 미리 깨워 드려요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    Slider(
                        value = alarm.smartWindowMinutes.toFloat(),
                        onValueChange = { vm.updateSmartWindow(it.toInt()) },
                        valueRange = 10f..45f,
                        steps = 6,
                    )
                }

                OutlinedTextField(
                    value = alarm.groupTag ?: "",
                    onValueChange = { vm.updateGroupTag(it) },
                    label = { Text("그룹 (예: 평일 출근, 주말)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (isEdit) {
                    ToggleRow("활성화", alarm.enabled, vm::updateEnabled)
                }
            }

            DraftReliabilityPreview(
                alarm = alarm,
                onApplyPreset = vm::applyWakeupPreset,
            )

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    scope.launch {
                        val conflict = vm.checkConflict()
                        val dnd = if (alarm.soundEnabled) checkDndWarning(context) else null
                        val combined = listOfNotNull(conflict, dnd).joinToString("\n\n")
                        if (combined.isNotEmpty()) warning = combined else vm.save()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null,
                    modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("저장",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // ── Date picker dialog ────────────────────────────
    if (showDatePicker) {
        val dpState = rememberDatePickerState(
            initialSelectedDateMillis = alarm.specificDate ?: System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    vm.updateSpecificDate(dpState.selectedDateMillis)
                    showDatePicker = false
                }) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("취소") }
            },
        ) { DatePicker(state = dpState) }
    }

    // ── Warning dialog (conflict + DND) ──────────────
    val pendingWarning = warning
    if (pendingWarning != null) {
        AlertDialog(
            onDismissRequest = { warning = null },
            title = { Text("저장 전에 확인") },
            text = { Text(pendingWarning) },
            confirmButton = {
                TextButton(onClick = {
                    warning = null
                    vm.save()
                }) { Text("그래도 저장") }
            },
            dismissButton = {
                TextButton(onClick = { warning = null }) { Text("취소") }
            },
        )
    }

    // ── Delete dialog ─────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("알람 삭제") },
            text = { Text("이 알람을 삭제할까요?") },
            confirmButton = {
                TextButton(onClick = { vm.delete(); showDeleteDialog = false }) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("취소") }
            },
        )
    }
}

@Composable
private fun HeroTimeCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 16.dp),
            content = content,
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    initiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    icon, null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
            AnimatedVisibility(expanded) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 18.dp)
                        .padding(bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun DraftReliabilityPreview(
    alarm: Alarm,
    onApplyPreset: (WakeupPreset) -> Unit,
) {
    val profile = remember(alarm) { WakeupPresetApplier.profile(alarm) }
    val issues = remember(alarm) { AlarmReliability.alarmIssues(alarm) }
    val strengths = remember(alarm) { AlarmReliability.alarmStrengths(alarm) }
    val recommendedPreset = remember(alarm) { AlarmReliability.recommendedPreset(alarm) }

    SectionCard(
        title = "저장 전 성공 점검",
        icon = Icons.Default.NotificationsActive,
        initiallyExpanded = true,
    ) {
        Text(
            "${profile.title} · ${profile.level.label}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            profile.recommendation,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
        )

        issues.firstOrNull()?.let { issue ->
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        issue.title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        issue.detail,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.72f),
                    )
                }
            }
        }

        if (strengths.isNotEmpty()) {
            Text(
                strengths.take(3).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            )
        }

        recommendedPreset?.let { preset ->
            Text(
                presetImpactText(preset),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
            )
            FilledTonalButton(
                onClick = { onApplyPreset(preset) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("${preset.label} 적용")
            }
        }
    }
}

private fun presetImpactText(preset: WakeupPreset): String = when (preset) {
    WakeupPreset.COMFORTABLE ->
        "미리알림, 단계적 알람, 최대 3회 스누즈로 부담을 낮춰요."
    WakeupPreset.ON_TIME ->
        "스누즈 1회, 타이핑 챌린지, 높은 음량으로 시간을 지키게 해요."
    WakeupPreset.FORCED ->
        "끄기 장치, 강한 음량, 기상 루틴을 함께 켜서 무의식 끄기를 막아요."
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
        )
    }
}

/**
 * Returns a warning string if DND is on or alarm stream is muted.
 * The alarm subsystem usually bypasses these, but OEMs vary — surface the risk.
 */
private fun checkDndWarning(context: android.content.Context): String? {
    val warnings = mutableListOf<String>()
    runCatching {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val filter = nm?.currentInterruptionFilter
            if (filter == NotificationManager.INTERRUPTION_FILTER_NONE) {
                warnings += "방해 금지가 '전체 차단' 상태예요. 알람이 무음이 될 수 있어요."
            } else if (filter == NotificationManager.INTERRUPTION_FILTER_ALARMS) {
                // OK — alarms allowed
            } else if (filter == NotificationManager.INTERRUPTION_FILTER_PRIORITY) {
                warnings += "방해 금지가 켜져 있어요. 알람 카테고리가 허용돼 있는지 확인하세요."
            }
        }
    }
    runCatching {
        val am = context.getSystemService(AudioManager::class.java)
        val v = am?.getStreamVolume(AudioManager.STREAM_ALARM) ?: 1
        if (v == 0) warnings += "알람 음량이 0이에요. 시스템 음량을 올려주세요."
    }
    return if (warnings.isEmpty()) null else warnings.joinToString("\n")
}

@SuppressLint("MissingPermission")
private fun captureCurrentLocation(
    context: android.content.Context,
    onResult: (Double, Double) -> Unit,
) {
    if (androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_FINE_LOCATION,
        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
    ) {
        android.widget.Toast.makeText(
            context, "위치 권한이 필요해요. 설정에서 허용하세요.",
            android.widget.Toast.LENGTH_LONG,
        ).show()
        return
    }
    com.google.android.gms.location.LocationServices
        .getFusedLocationProviderClient(context)
        .getCurrentLocation(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null,
        )
        .addOnSuccessListener { loc ->
            if (loc != null) onResult(loc.latitude, loc.longitude)
            else android.widget.Toast.makeText(
                context, "위치를 가져올 수 없어요.",
                android.widget.Toast.LENGTH_SHORT,
            ).show()
        }
        .addOnFailureListener {
            android.widget.Toast.makeText(
                context, "위치 가져오기 실패: ${it.localizedMessage}",
                android.widget.Toast.LENGTH_SHORT,
            ).show()
        }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
        )
    }
}
