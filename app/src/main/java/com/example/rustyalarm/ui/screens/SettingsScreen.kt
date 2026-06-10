package com.example.rustyalarm.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rustyalarm.alarm.AlarmRepository
import com.example.rustyalarm.auth.AuthViewModel
import com.example.rustyalarm.auth.UnlockResult
import com.example.rustyalarm.prefs.ThemeMode
import com.example.rustyalarm.prefs.ThemePreferences
import com.example.rustyalarm.prefs.UserPreferences
import com.example.rustyalarm.prefs.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: AuthViewModel,
    canUseBiometric: Boolean,
    themePrefs: ThemePreferences,
    userPrefs: UserPreferences,
    userProfile: UserProfile,
    repository: AlarmRepository,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = remember { CoroutineScope(Dispatchers.Main) }

    var biometricOn by remember { mutableStateOf(vm.biometricEnabled()) }
    var importMessage by remember { mutableStateOf<String?>(null) }
    var nicknameDialog by remember { mutableStateOf(false) }
    var nicknameDraft by remember { mutableStateOf(userProfile.nickname) }
    var disableLockDialog by remember { mutableStateOf(false) }
    var reauthPin by remember { mutableStateOf("") }
    var reauthError by remember { mutableStateOf(false) }

    val hasPin by vm.hasPin.collectAsStateWithLifecycle()
    val throttleSec by vm.throttleSeconds.collectAsStateWithLifecycle()
    val themeMode by themePrefs.mode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)

    // Tick down throttle while the disable dialog is open
    LaunchedEffect(disableLockDialog, throttleSec) {
        while (disableLockDialog && vm.throttleSeconds.value > 0) {
            kotlinx.coroutines.delay(1000)
            vm.refreshThrottle()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val json = withContext(Dispatchers.IO) { repository.exportToJson() }
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    }
                    importMessage = "백업 저장 완료"
                }.onFailure { importMessage = "백업 실패: ${it.message}" }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val text = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)
                            ?.bufferedReader()?.use { it.readText() } ?: ""
                    }
                    val n = withContext(Dispatchers.IO) { repository.importFromJson(text) }
                    importMessage = "$n 개 알람 가져옴"
                }.onFailure { importMessage = "복원 실패: ${it.message}" }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0D0D22), Color(0xFF0A0A1A)))),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("설정") },
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // ── Profile ──────────────────────────
                SectionTitle("프로필")
                SettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.secondary)
                            Column {
                                Text("닉네임", fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface)
                                Text(userProfile.nickname,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                        TextButton(onClick = {
                            nicknameDraft = userProfile.nickname
                            nicknameDialog = true
                        }) { Text("변경") }
                    }
                }

                // ── Theme ────────────────────────────
                SectionTitle("화면")
                SettingsCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(bottom = 12.dp),
                        ) {
                            Icon(Icons.Default.Palette, null,
                                tint = MaterialTheme.colorScheme.secondary)
                            Text("테마", fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface)
                        }
                        ThemeMode.entries.forEach { m ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = themeMode == m,
                                    onClick = { scope.launch { themePrefs.setMode(m) } },
                                )
                                Text(
                                    when (m) {
                                        ThemeMode.SYSTEM -> "시스템 설정 따름"
                                        ThemeMode.LIGHT  -> "라이트"
                                        ThemeMode.DARK   -> "다크"
                                    },
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }

                // ── Security ─────────────────────────
                SectionTitle("보안")

                // App lock master toggle
                SettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Default.Lock, null,
                                tint = MaterialTheme.colorScheme.secondary)
                            Column {
                                Text("앱 잠금 사용", fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    if (userProfile.appLockEnabled)
                                        "다음 실행 시 PIN으로 잠금"
                                    else "켜면 PIN을 새로 설정하게 됩니다",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                )
                            }
                        }
                        Switch(
                            checked = userProfile.appLockEnabled,
                            onCheckedChange = { on ->
                                if (!on && hasPin) {
                                    // Step-up: require current PIN before disabling the lock
                                    reauthPin = ""
                                    reauthError = false
                                    disableLockDialog = true
                                } else {
                                    scope.launch {
                                        userPrefs.setAppLockEnabled(on)
                                        if (!on) {
                                            biometricOn = false
                                            vm.setBiometricEnabled(false)
                                        }
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }

                if (userProfile.appLockEnabled) {
                    SettingsCard {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(Icons.Default.Fingerprint, null,
                                    tint = MaterialTheme.colorScheme.secondary)
                                Column {
                                    Text("생체 인증", fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        if (canUseBiometric) "지문으로 빠르게 해제"
                                        else "이 기기에서 지원하지 않음",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    )
                                }
                            }
                            Switch(
                                checked = biometricOn && hasPin,
                                enabled = canUseBiometric && hasPin,
                                onCheckedChange = {
                                    biometricOn = it
                                    vm.setBiometricEnabled(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                        }
                    }
                }

                // ── Backup ───────────────────────────
                SectionTitle("백업")
                SettingsCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Backup, null,
                                tint = MaterialTheme.colorScheme.secondary)
                            Text("알람 백업/복원 (JSON)", fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { exportLauncher.launch("rusty_alarm_backup.json") },
                                modifier = Modifier.weight(1f),
                            ) { Text("내보내기") }
                            OutlinedButton(
                                onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Default.Restore, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("가져오기")
                            }
                        }
                        importMessage?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    "Rusty Alarm v1.0",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }

    if (disableLockDialog) {
        val isThrottled = throttleSec > 0
        val supporting: (@Composable () -> Unit)? = when {
            isThrottled -> {{ Text("잠시 후 다시 시도 — $throttleSec 초") }}
            reauthError -> {{ Text("PIN이 일치하지 않아요.") }}
            else        -> null
        }
        AlertDialog(
            onDismissRequest = {
                disableLockDialog = false
                reauthPin = ""
                reauthError = false
            },
            title = { Text("앱 잠금 해제") },
            text = {
                Column {
                    Text("앱 잠금을 끄려면 현재 PIN을 입력하세요.")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = reauthPin,
                        onValueChange = {
                            if (it.length <= 8 && it.all { c -> c.isDigit() }) {
                                reauthPin = it
                                reauthError = false
                            }
                        },
                        label = { Text("현재 PIN") },
                        isError = reauthError || isThrottled,
                        supportingText = supporting,
                        singleLine = true,
                        enabled = !isThrottled,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isThrottled,
                    onClick = {
                        when (vm.verifyForStepUp(reauthPin)) {
                            is UnlockResult.Success -> {
                                scope.launch {
                                    userPrefs.setAppLockEnabled(false)
                                    vm.resetPin()
                                    biometricOn = false
                                    vm.setBiometricEnabled(false)
                                }
                                disableLockDialog = false
                                reauthPin = ""
                                reauthError = false
                            }
                            is UnlockResult.Failed -> {
                                reauthError = true
                                reauthPin = ""
                            }
                            is UnlockResult.Throttled -> {
                                reauthPin = ""
                            }
                        }
                    },
                ) {
                    Text("해제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    disableLockDialog = false
                    reauthPin = ""
                    reauthError = false
                }) { Text("취소") }
            },
        )
    }

    if (nicknameDialog) {
        AlertDialog(
            onDismissRequest = { nicknameDialog = false },
            title = { Text("닉네임 변경") },
            text = {
                OutlinedTextField(
                    value = nicknameDraft,
                    onValueChange = { if (it.length <= 12) nicknameDraft = it },
                    singleLine = true,
                    label = { Text("새 닉네임") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { userPrefs.setNickname(nicknameDraft) }
                    nicknameDialog = false
                }) { Text("저장") }
            },
            dismissButton = {
                TextButton(onClick = { nicknameDialog = false }) { Text("취소") }
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.secondary,
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        content = { content() },
    )
}
