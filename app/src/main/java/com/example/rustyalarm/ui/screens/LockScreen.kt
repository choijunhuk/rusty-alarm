package com.example.rustyalarm.ui.screens
import com.example.rustyalarm.ui.theme.screenBackgroundBrush

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rustyalarm.auth.AuthViewModel
import com.example.rustyalarm.auth.UnlockResult
import com.example.rustyalarm.ui.components.PinKeypad
import kotlinx.coroutines.delay

private const val PIN_LENGTH = 4

@Composable
fun LockScreen(
    vm: AuthViewModel,
    onBiometric: () -> Unit,
    biometricEnabled: Boolean,
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val throttle by vm.throttleSeconds.collectAsStateWithLifecycle()

    // Tick down throttle counter
    LaunchedEffect(throttle) {
        if (throttle > 0) {
            while (vm.throttleSeconds.value > 0) {
                delay(1000)
                vm.refreshThrottle()
            }
        }
    }

    val isThrottled = throttle > 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                screenBackgroundBrush()
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(64.dp))
            Icon(
                Icons.Default.Lock, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Rusty Alarm",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Light,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                when {
                    isThrottled -> "잠시 후 다시 시도하세요 — $throttle 초"
                    error -> "PIN이 일치하지 않아요"
                    else -> "PIN을 입력하세요"
                },
                fontSize = 14.sp,
                color = if (error || isThrottled) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )

            Spacer(Modifier.weight(1f))

            PinKeypad(
                pinLength = pin.length,
                maxLength = PIN_LENGTH,
                onDigit = { d ->
                    if (isThrottled) return@PinKeypad
                    error = false
                    if (pin.length < PIN_LENGTH) {
                        pin += d.toString()
                        if (pin.length == PIN_LENGTH) {
                            when (val r = vm.tryUnlock(pin)) {
                                is UnlockResult.Success      -> Unit
                                is UnlockResult.Failed       -> { error = true; pin = "" }
                                is UnlockResult.Throttled    -> { pin = "" }
                            }
                        }
                    }
                },
                onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
                onBiometric = if (biometricEnabled && !isThrottled) onBiometric else null,
            )
            Spacer(Modifier.height(48.dp))
        }
    }
}
