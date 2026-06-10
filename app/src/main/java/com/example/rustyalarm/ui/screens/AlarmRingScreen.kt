package com.example.rustyalarm.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rustyalarm.alarm.*
import com.example.rustyalarm.fortune.Fortune
import com.example.rustyalarm.rust.RustAlarmCore
import com.example.rustyalarm.ui.components.LocationChallengeCard
import com.example.rustyalarm.ui.components.PhotoChallengeCard
import com.example.rustyalarm.ui.components.TetrisChallenge
import kotlin.math.abs

@Composable
fun AlarmRingScreen(
    alarmId: Long,
    title: String,
    hour: Int,
    minute: Int,
    challengeType: ChallengeType = ChallengeType.NONE,
    message: String = "",
    snoozesRemaining: Int = Int.MAX_VALUE,
    geofenceLat: Double? = null,
    geofenceLng: Double? = null,
    geofenceRadius: Int = 100,
    mathProblemCount: Int = 1,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
) {
    val timeText = RustAlarmCore.formatTime(hour, minute)

    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse,
        ),
        label = "scale",
    )

    // ── challenge state ───────────────────────────────
    val isMathChallenge = challengeType in listOf(
        ChallengeType.MATH_EASY, ChallengeType.MATH_MEDIUM, ChallengeType.MATH_HARD,
    )
    var mathProblem by remember {
        mutableStateOf<MathProblem?>(if (isMathChallenge) generateMathProblem(challengeType) else null)
    }
    var mathSolvedCount by remember { mutableIntStateOf(0) }
    val typingPhrase = remember(challengeType) {
        if (challengeType == ChallengeType.TYPING) getTypingPhrase() else null
    }

    var mathAnswer  by remember { mutableStateOf("") }
    var mathError   by remember { mutableStateOf(false) }
    var typedText   by remember { mutableStateOf("") }
    var shakeCount  by remember { mutableIntStateOf(0) }
    var stepCount   by remember { mutableIntStateOf(0) }
    var solved      by remember { mutableStateOf(challengeType == ChallengeType.NONE) }

    val stepTarget = 20

    // ── shake sensor (3 difficulty levels) ──────────
    val isShakeChallenge = challengeType == ChallengeType.SHAKE_EASY ||
        challengeType == ChallengeType.SHAKE ||
        challengeType == ChallengeType.SHAKE_HARD
    val shakeTarget = remember(challengeType) { shakeTargetCount(challengeType) }
    val threshold = remember(challengeType) { shakeThreshold(challengeType) }

    if (isShakeChallenge) {
        val ctx = LocalContext.current
        DisposableEffect(Unit) {
            val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            var lastUpdate = 0L
            var lx = 0f; var ly = 0f; var lz = 0f

            val listener = object : SensorEventListener {
                override fun onSensorChanged(ev: SensorEvent) {
                    val now = System.currentTimeMillis()
                    if (now - lastUpdate < 100) return
                    lastUpdate = now
                    val dx = abs(ev.values[0] - lx)
                    val dy = abs(ev.values[1] - ly)
                    val dz = abs(ev.values[2] - lz)
                    if (dx + dy + dz > threshold) {
                        shakeCount++
                        if (shakeCount >= shakeTarget) { solved = true; onDismiss() }
                    }
                    lx = ev.values[0]; ly = ev.values[1]; lz = ev.values[2]
                }
                override fun onAccuracyChanged(s: Sensor, a: Int) {}
            }
            sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
            onDispose { sm.unregisterListener(listener) }
        }
    }

    // ── step counter sensor ──────────────────────────
    if (challengeType == ChallengeType.STEP_COUNT) {
        val ctx = LocalContext.current
        DisposableEffect(Unit) {
            val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            var initial: Float? = null
            val listener = object : SensorEventListener {
                override fun onSensorChanged(ev: SensorEvent) {
                    val total = ev.values.getOrNull(0) ?: return
                    if (initial == null) initial = total
                    val taken = (total - (initial ?: total)).toInt().coerceAtLeast(0)
                    stepCount = taken
                    if (stepCount >= stepTarget) {
                        solved = true
                        onDismiss()
                    }
                }
                override fun onAccuracyChanged(s: Sensor, a: Int) {}
            }
            if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            onDispose { sm.unregisterListener(listener) }
        }
    }

    fun checkMath() {
        val input = mathAnswer.trim().toIntOrNull()
        val current = mathProblem
        if (input != null && current != null && input == current.answer) {
            mathSolvedCount += 1
            mathAnswer = ""
            mathError = false
            if (mathSolvedCount >= mathProblemCount) {
                solved = true
                onDismiss()
            } else {
                mathProblem = generateMathProblem(challengeType)
            }
        } else { mathError = true; mathAnswer = "" }
    }

    // ── UI ───────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(
                listOf(Color(0xFF0A0A1A), Color(0xFF1A0A2E), Color(0xFF0A0A1A))
            )),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                Icons.Default.AlarmOff, null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(48.dp),
            )

            Text(
                text = timeText, fontSize = 80.sp, fontWeight = FontWeight.Thin,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.scale(scale),
            )

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )

            if (message.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    ),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        text = "💬 $message",
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // ── today's fortune ──────────────────────
            val fortune = remember { Fortune.forToday() }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "${fortune.emoji}  오늘의 운세",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        fortune.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // ── math challenge ────────────────────────
            val currentMath = mathProblem
            if (currentMath != null && !solved) {
                ChallengeCard {
                    Text("알람을 끄려면 풀어야 해요!", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary)
                    if (mathProblemCount > 1) {
                        Text(
                            "${mathSolvedCount + 1} / $mathProblemCount",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(currentMath.expression, fontSize = 36.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface)
                    OutlinedTextField(
                        value = mathAnswer,
                        onValueChange = { mathAnswer = it; mathError = false },
                        label = { Text("정답") },
                        isError = mathError,
                        supportingText = if (mathError) {{ Text("틀렸어요! 다시 시도해보세요.") }} else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { checkMath() }),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(onClick = ::checkMath, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Text("확인", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── typing challenge ──────────────────────
            if (typingPhrase != null && !solved) {
                ChallengeCard {
                    Text("아래 문장을 입력하면 꺼져요!", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary)
                    Text(
                        "\"$typingPhrase\"", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center,
                    )
                    OutlinedTextField(
                        value = typedText,
                        onValueChange = { txt ->
                            typedText = txt
                            if (txt == typingPhrase) { solved = true; onDismiss() }
                        },
                        label = { Text("입력하세요") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // ── step counter challenge ────────────────
            if (challengeType == ChallengeType.STEP_COUNT && !solved) {
                ChallengeCard {
                    Text("$stepTarget 걸음 걸으면 알람이 꺼져요!",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center)
                    Text(
                        "$stepCount / $stepTarget",
                        fontSize = 48.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    LinearProgressIndicator(
                        progress = { (stepCount.toFloat() / stepTarget).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // ── photo challenge ───────────────────────
            if (challengeType == ChallengeType.PHOTO && !solved) {
                ChallengeCard {
                    PhotoChallengeCard(onSuccess = {
                        solved = true
                        onDismiss()
                    })
                }
            }

            // ── location challenge ────────────────────
            if (challengeType == ChallengeType.LOCATION && !solved) {
                ChallengeCard {
                    if (geofenceLat != null && geofenceLng != null) {
                        LocationChallengeCard(
                            targetLat = geofenceLat,
                            targetLng = geofenceLng,
                            radiusMeters = geofenceRadius,
                            onSuccess = { solved = true; onDismiss() },
                        )
                    } else {
                        Text("위치가 설정되지 않은 알람이에요.",
                            color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // ── tetris challenge ──────────────────────
            if (challengeType == ChallengeType.TETRIS && !solved) {
                ChallengeCard {
                    TetrisChallenge(onLineCleared = {
                        solved = true
                        onDismiss()
                    })
                }
            }

            // ── shake challenge ───────────────────────
            if (isShakeChallenge && !solved) {
                ChallengeCard {
                    Text("휴대폰을 흔들어서 알람을 끄세요!", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
                    Text(
                        "$shakeCount / $shakeTarget",
                        fontSize = 48.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    LinearProgressIndicator(
                        progress = { shakeCount.toFloat() / shakeTarget },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier.weight(1f).height(56.dp),
                    enabled = snoozesRemaining > 0,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                ) {
                    Text(
                        when {
                            snoozesRemaining == Int.MAX_VALUE -> "5분 뒤"
                            snoozesRemaining > 0 -> "5분 뒤 ($snoozesRemaining 회 남음)"
                            else -> "스누즈 소진"
                        }
                    )
                }

                Button(
                    onClick = { if (solved) onDismiss() },
                    modifier = Modifier.weight(1f).height(56.dp),
                    enabled = solved,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (solved) MaterialTheme.colorScheme.primary
                                         else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Text(
                        if (solved) "끄기" else "🔒 먼저 챌린지를",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChallengeCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}
