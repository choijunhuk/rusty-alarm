package com.example.rustyalarm.ui.screens

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rustyalarm.alarm.AlarmRepository
import com.example.rustyalarm.alarm.ChallengeType
import com.example.rustyalarm.ui.components.DaySelector
import com.example.rustyalarm.ui.components.TimePickerSection
import com.example.rustyalarm.viewmodel.AlarmEditViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditScreen(
    alarmId: Long,
    repository: AlarmRepository,
    onBack: () -> Unit,
) {
    val vm: AlarmEditViewModel = viewModel(factory = AlarmEditViewModel.Factory(repository))
    val alarm    by vm.alarm.collectAsStateWithLifecycle()
    val saved    by vm.saved.collectAsStateWithLifecycle()
    val isLoaded by vm.isLoaded.collectAsStateWithLifecycle()

    LaunchedEffect(alarmId) { vm.load(alarmId) }
    LaunchedEffect(saved)   { if (saved) onBack() }

    val isEdit = alarmId != -1L
    var showDeleteDialog       by remember { mutableStateOf(false) }
    var challengeMenuExpanded  by remember { mutableStateOf(false) }
    var showDatePicker         by remember { mutableStateOf(false) }

    // Ringtone picker launcher
    val context = LocalContext.current
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
                title = { Text(if (isEdit) "알람 수정" else "알람 추가") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    if (isEdit) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "삭제")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Time picker
            key("loaded") {
                TimePickerSection(
                    hour = alarm.hour, minute = alarm.minute,
                    onHourChange = vm::updateHour, onMinuteChange = vm::updateMinute,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // Title
            OutlinedTextField(
                value = alarm.title, onValueChange = vm::updateTitle,
                label = { Text("알람 이름") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // ── Schedule section ──────────────────────
            SectionLabel("반복 / 날짜")

            // Specific date toggle
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
                // Show selected date + edit button
                val dateLabel = remember(specificDateMillis) {
                    SimpleDateFormat("yyyy년 M월 d일 (E)", Locale.KOREAN)
                        .format(Date(specificDateMillis))
                }
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(dateLabel)
                }
            } else {
                // Weekday repeat selector
                DaySelector(
                    selectedDays = alarm.repeatDays,
                    onDayToggle = vm::toggleRepeatDay,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // ── Sound section ─────────────────────────
            SectionLabel("소리 / 진동")

            ToggleRow("알람 소리", alarm.soundEnabled, vm::updateSoundEnabled)

            if (alarm.soundEnabled) {
                // Ringtone picker button
                OutlinedButton(
                    onClick = ::launchRingtonePicker,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(ringtoneLabel)
                }
            }

            ToggleRow("진동", alarm.vibrate, vm::updateVibrate)

            if (alarm.soundEnabled) {
                Text(
                    text = if (alarm.volumeRampSeconds == 0) "볼륨 페이드인 — 끔"
                           else "볼륨 페이드인 ${alarm.volumeRampSeconds}초",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                Slider(
                    value = alarm.volumeRampSeconds.toFloat(),
                    onValueChange = { vm.updateVolumeRamp(it.toInt()) },
                    valueRange = 0f..30f,
                    steps = 5,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // ── Group tag ──────────────────────────────
            SectionLabel("그룹 (선택)")
            OutlinedTextField(
                value = alarm.groupTag ?: "",
                onValueChange = { vm.updateGroupTag(it) },
                label = { Text("예: 평일 출근, 주말") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // ── Challenge section ─────────────────────
            SectionLabel("알람 끄기 챌린지")

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

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // ── Smart alarm section ──────────────────
            SectionLabel("스마트 알람")
            ToggleRow(
                "얕은 잠 자동 감지",
                alarm.isSmartAlarm,
                vm::updateSmartAlarm,
            )
            if (alarm.isSmartAlarm) {
                Text(
                    text = "기상 ${alarm.smartWindowMinutes}분 전부터 가속도계로 잠 깊이를 분석해, " +
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

            if (isEdit) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ToggleRow("활성화", alarm.enabled, vm::updateEnabled)
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = vm::save,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text("저장", style = MaterialTheme.typography.titleMedium)
            }
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
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.secondary,
    )
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
