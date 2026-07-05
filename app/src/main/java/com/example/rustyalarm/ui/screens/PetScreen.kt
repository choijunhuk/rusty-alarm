package com.example.rustyalarm.ui.screens
import com.example.rustyalarm.ui.theme.screenBackgroundBrush

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.rustyalarm.pet.PetSkin
import com.example.rustyalarm.viewmodel.PetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetScreen(
    onBack: () -> Unit,
) {
    val vm: PetViewModel = hiltViewModel()
    val pet by vm.pet.collectAsStateWithLifecycle()
    var renameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var skinDialog by remember { mutableStateOf(false) }

    val bounce = rememberInfiniteTransition(label = "pet")
    val s by bounce.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "scale",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBackgroundBrush()),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("나의 펫") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            newName = pet?.name.orEmpty()
                            renameDialog = true
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "이름 변경")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            },
        ) { padding ->
            val p = pet
            if (p == null) {
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
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(24.dp))

                val displayEmoji = when (p.skinEnum) {
                    PetSkin.GOLDEN  -> "✨${p.stage.emoji}✨"
                    PetSkin.RAINBOW -> "🌈${p.stage.emoji}🌈"
                    else -> p.stage.emoji
                }
                androidx.compose.animation.Crossfade(
                    targetState = displayEmoji,
                    label = "petEvolve",
                    animationSpec = androidx.compose.animation.core.tween(600),
                ) { e ->
                    Text(
                        e,
                        fontSize = 110.sp,
                        modifier = Modifier.scale(s),
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    p.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Text(
                    "${p.stage.label} · Lv ${p.level}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )

                Spacer(Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("다음 레벨까지", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall)
                            Text("${p.exp % 100} / 100",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { p.progressToNextLevel },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )

                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(p.happinessLabel,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium)
                            Text("${p.happiness}%",
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { p.happiness / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("성장 방법",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(8.dp))
                        Text("• 알람을 끄면 +10 경험치",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Text("• 챌린지를 완수하면 +5 경험치 보너스",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Text("• 스누즈는 경험치가 쌓이지 않아요",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }

                Spacer(Modifier.height(8.dp))
                val feedScope = rememberCoroutineScope()
                val ctx = androidx.compose.ui.platform.LocalContext.current
                Button(
                    onClick = {
                        feedScope.launch {
                            val reason = vm.feed()
                            val msg = reason ?: "${p.name}이(가) 기뻐해요. +8 경험치"
                            android.widget.Toast.makeText(
                                ctx, msg, android.widget.Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("먹이주기", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { skinDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("스킨 변경 · 현재 ${p.skinEnum.label}")
                }
            }
        }
    }

    if (skinDialog) {
        val current = pet?.skinEnum ?: PetSkin.DEFAULT
        val level = pet?.level ?: 0
        AlertDialog(
            onDismissRequest = { skinDialog = false },
            title = { Text("펫 스킨") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PetSkin.entries.forEach { skin ->
                        val unlocked = level >= skin.unlockLevel
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column {
                                Text(skin.label, fontWeight = FontWeight.Medium)
                                Text(
                                    if (unlocked) "사용 가능"
                                    else "Lv ${skin.unlockLevel} 부터",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                )
                            }
                            FilledTonalButton(
                                onClick = {
                                    vm.setSkin(skin.name)
                                    skinDialog = false
                                },
                                enabled = unlocked && current != skin,
                            ) {
                                Text(if (current == skin) "사용 중" else "선택")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { skinDialog = false }) { Text("닫기") }
            },
        )
    }

    if (renameDialog) {
        AlertDialog(
            onDismissRequest = { renameDialog = false },
            title = { Text("펫 이름") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { if (it.length <= 10) newName = it },
                    singleLine = true,
                    label = { Text("새 이름") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.rename(newName)
                    renameDialog = false
                }) { Text("저장") }
            },
            dismissButton = {
                TextButton(onClick = { renameDialog = false }) { Text("취소") }
            },
        )
    }
}
