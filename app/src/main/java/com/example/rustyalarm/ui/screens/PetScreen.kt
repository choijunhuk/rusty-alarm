package com.example.rustyalarm.ui.screens
import com.example.rustyalarm.ui.theme.screenBackgroundBrush

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rustyalarm.pet.PetDao
import com.example.rustyalarm.pet.PetSkin
import com.example.rustyalarm.viewmodel.PetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetScreen(
    petDao: PetDao,
    onBack: () -> Unit,
) {
    val vm: PetViewModel = viewModel(factory = PetViewModel.Factory(petDao))
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
                            Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (p == null) {
                    CircularProgressIndicator()
                    return@Column
                }

                Spacer(Modifier.height(24.dp))

                val displayEmoji = when (p.skinEnum) {
                    PetSkin.GOLDEN  -> "✨${p.stage.emoji}✨"
                    PetSkin.RAINBOW -> "🌈${p.stage.emoji}🌈"
                    else -> p.stage.emoji
                }
                Text(
                    displayEmoji,
                    fontSize = 110.sp,
                    modifier = Modifier.scale(s),
                )

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
                    }
                }

                Spacer(Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🌅 잘 일어날수록 펫이 자라요!",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.height(8.dp))
                        Text("알람을 끄면 +10 EXP — 챌린지 완수 시 +5 보너스. 스누즈는 EXP가 쌓이지 않아요.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { skinDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("🎨 스킨 변경 (현재: ${p.skinEnum.label})")
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
