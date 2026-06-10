package com.example.rustyalarm.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rustyalarm.alarm.ChallengeType
import com.example.rustyalarm.alarm.MathProblem
import com.example.rustyalarm.alarm.generateMathProblem
import com.example.rustyalarm.rust.RustAlarmCore

@Composable
fun AlarmRingScreen(
    alarmId: Long,
    title: String,
    hour: Int,
    minute: Int,
    challengeType: ChallengeType = ChallengeType.NONE,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
) {
    val timeText = RustAlarmCore.formatTime(hour, minute)

    // Pulse animation for time display
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )

    // Math challenge state
    val mathProblem: MathProblem? = remember(challengeType) {
        if (challengeType != ChallengeType.NONE) generateMathProblem(challengeType) else null
    }
    var userAnswer by remember { mutableStateOf("") }
    var answerError by remember { mutableStateOf(false) }
    var solved by remember { mutableStateOf(challengeType == ChallengeType.NONE) }

    fun checkAnswer() {
        val input = userAnswer.trim().toIntOrNull()
        if (input != null && mathProblem != null && input == mathProblem.answer) {
            solved = true
            onDismiss()
        } else {
            answerError = true
            userAnswer = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A0A1A), Color(0xFF1A0A2E), Color(0xFF0A0A1A))
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp),
        ) {

            // Alarm icon
            Icon(
                Icons.Default.AlarmOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(48.dp),
            )

            // Time — pulsing
            Text(
                text = timeText,
                fontSize = 80.sp,
                fontWeight = FontWeight.Thin,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.scale(scale),
            )

            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            // Math challenge card
            if (mathProblem != null && !solved) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = "알람을 끄려면 풀어야 해요!",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Text(
                            text = mathProblem.expression,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        OutlinedTextField(
                            value = userAnswer,
                            onValueChange = { userAnswer = it; answerError = false },
                            label = { Text("정답") },
                            isError = answerError,
                            supportingText = if (answerError) {{ Text("틀렸어요! 다시 시도해보세요.") }} else null,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = { checkAnswer() }),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = ::checkAnswer,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                        ) {
                            Text("확인", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Snooze — always available
                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary,
                    ),
                ) {
                    Text("5분 뒤")
                }

                // Dismiss — only if no challenge or challenge solved
                Button(
                    onClick = { if (solved) onDismiss() },
                    modifier = Modifier.weight(1f).height(56.dp),
                    enabled = solved,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (solved)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Text(
                        text = if (solved) "끄기" else "🔒 풀어야 꺼요",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
