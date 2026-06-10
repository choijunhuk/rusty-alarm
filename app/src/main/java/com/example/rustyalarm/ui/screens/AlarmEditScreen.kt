package com.example.rustyalarm.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    var advancedExpanded       by remember { mutableStateOf(false) }

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
            if (!isEdit) {
                // Quick presets — only for new alarms
                SectionLabel("빠른 설정 (지금부터)")
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
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            }

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

            // Message (shown on ring screen)
            OutlinedTextField(
                value = alarm.message,
                onValueChange = vm::updateMessage,
                label = { Text("한 줄 메시지 (선택)") },
                placeholder = { Text("예: 물 한 잔 마시기") },
                singleLine = true,
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

            // Snooze cap
            Text(
                text = if (alarm.maxSnoozes == 0) "스누즈 무제한"
                       else "스누즈 최대 ${alarm.maxSnoozes}회",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Slider(
                value = alarm.maxSnoozes.toFloat(),
                onValueChange = { vm.updateMaxSnoozes(it.toInt()) },
                valueRange = 0f..10f,
                steps = 9,
            )

            // Gradual wakeup
            ToggleRow("단계적 알람 (진동 30초 → 약하게 → 크게)",
                alarm.gradualWakeup, vm::updateGradualWakeup)

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // ── Advanced expander ─────────────────────
            TextButton(
                onClick = { advancedExpanded = !advancedExpanded },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (advancedExpanded) "▲ 고급 설정 접기" else "▼ 고급 설정 펼치기",
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            if (advancedExpanded) {
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

            // Location picker — only when LOCATION challenge selected
            if (alarm.challengeType == ChallengeType.LOCATION) {
                Spacer(Modifier.height(8.dp))
                val lat = alarm.geofenceLat
                val lng = alarm.geofenceLng
                Text(
                    if (lat != null && lng != null)
                        "위치 — %.5f, %.5f".format(lat, lng)
                    else "위치 미지정",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Text(
                    "허용 반경 ${alarm.geofenceRadius}m",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Slider(
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
                ) {
                    Text("📍 현재 위치 사용")
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
            }  // end if (advancedExpanded)

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
