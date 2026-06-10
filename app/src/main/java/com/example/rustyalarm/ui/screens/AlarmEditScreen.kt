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
    val alarm by vm.alarm.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    val isLoaded by vm.isLoaded.collectAsStateWithLifecycle()

    LaunchedEffect(alarmId) { vm.load(alarmId) }
    LaunchedEffect(saved) { if (saved) onBack() }

    val isEdit = alarmId != -1L
    var showDeleteDialog by remember { mutableStateOf(false) }

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
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
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
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // key("loaded") ensures TimePickerState is initialised exactly once,
            // after the alarm data is loaded from the DB.
            key("loaded") {
                TimePickerSection(
                    hour = alarm.hour,
                    minute = alarm.minute,
                    onHourChange = vm::updateHour,
                    onMinuteChange = vm::updateMinute,
                )
            }

            // Title
            OutlinedTextField(
                value = alarm.title,
                onValueChange = vm::updateTitle,
                label = { Text("알람 이름") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Repeat days
            Text("반복 요일", style = MaterialTheme.typography.titleMedium)
            DaySelector(
                selectedDays = alarm.repeatDays,
                onDayToggle = vm::toggleRepeatDay,
            )

            // Vibrate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("진동", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = alarm.vibrate,
                    onCheckedChange = vm::updateVibrate,
                )
            }

            // Enabled (edit mode only)
            if (isEdit) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("활성화", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = alarm.enabled,
                        onCheckedChange = vm::updateEnabled,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Save button
            Button(
                onClick = vm::save,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text("저장")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("알람 삭제") },
            text = { Text("이 알람을 삭제하시겠습니까?") },
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
