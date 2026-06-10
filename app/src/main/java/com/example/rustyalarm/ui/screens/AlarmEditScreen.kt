package com.example.rustyalarm.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rustyalarm.alarm.AlarmRepository
import com.example.rustyalarm.alarm.ChallengeType
import com.example.rustyalarm.ui.components.DaySelector
import com.example.rustyalarm.ui.components.TimePickerSection
import com.example.rustyalarm.viewmodel.AlarmEditViewModel

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
    var showDeleteDialog by remember { mutableStateOf(false) }
    var challengeMenuExpanded by remember { mutableStateOf(false) }

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
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
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
            // Time picker (TimeInput — diagram style)
            key("loaded") {
                TimePickerSection(
                    hour = alarm.hour,
                    minute = alarm.minute,
                    onHourChange = vm::updateHour,
                    onMinuteChange = vm::updateMinute,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // Title
            OutlinedTextField(
                value = alarm.title,
                onValueChange = vm::updateTitle,
                label = { Text("알람 이름") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Repeat days
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("반복 요일", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                DaySelector(
                    selectedDays = alarm.repeatDays,
                    onDayToggle = vm::toggleRepeatDay,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // Vibrate row
            ToggleRow("진동", alarm.vibrate, vm::updateVibrate)

            // Sound row
            ToggleRow("알람 소리", alarm.soundEnabled, vm::updateSoundEnabled)

            // Challenge type dropdown
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("알람 끄기 챌린지", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                ExposedDropdownMenuBox(
                    expanded = challengeMenuExpanded,
                    onExpandedChange = { challengeMenuExpanded = it },
                ) {
                    OutlinedTextField(
                        value = alarm.challengeType.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("챌린지 유형") },
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
                                onClick = {
                                    vm.updateChallengeType(type)
                                    challengeMenuExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }
            }

            // Enabled toggle (edit only)
            if (isEdit) {
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
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
        )
    }
}
