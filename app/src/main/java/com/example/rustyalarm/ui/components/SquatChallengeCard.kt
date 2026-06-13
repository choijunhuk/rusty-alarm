package com.example.rustyalarm.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * Detects squats by watching the gravity component on the Y axis from the
 * accelerometer. When the phone is held vertically (chest pocket / hand),
 * a squat rep cycles the Y reading from ~9.8 (standing) down toward a
 * smaller value (squat) and back. We count one rep per up-down-up cycle.
 *
 * Lenient — works with the phone in a pocket, in hand, or strapped on.
 */
@Composable
fun SquatChallengeCard(
    targetCount: Int = 10,
    onSuccess: () -> Unit,
) {
    val context = LocalContext.current
    var count by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf<String>("준비") }   // "up" / "down"

    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor: Sensor? = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        var smoothedY = 9.8f
        var state = "up"          // up = standing, down = squatting
        val downThreshold = 7.0f
        val upThreshold   = 9.0f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(ev: SensorEvent) {
                val y = ev.values[1]
                val z = ev.values[2]
                // Use magnitude of the vertical-ish component to be robust
                // to phone orientation (vertical or near-horizontal).
                val v = if (abs(z) > abs(y)) abs(z) else abs(y)
                smoothedY = 0.85f * smoothedY + 0.15f * v
                when (state) {
                    "up" -> if (smoothedY < downThreshold) {
                        state = "down"
                        phase = "내려가는 중"
                    }
                    "down" -> if (smoothedY > upThreshold) {
                        state = "up"
                        count += 1
                        phase = "일어남 ✓"
                        if (count >= targetCount) {
                            onSuccess()
                        }
                    }
                }
            }
            override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {}
        }
        if (sensor != null) {
            sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { sm.unregisterListener(listener) }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            "스쿼트 ${targetCount}회로 알람 끄기",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "폰을 가슴 주머니나 손에 들고 똑바로 서서 시작하세요.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
        )
        Text(
            "$count / $targetCount",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        LinearProgressIndicator(
            progress = { (count.toFloat() / targetCount).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            phase,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}
