package com.example.rustyalarm.ui.screens

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
import com.example.rustyalarm.auth.AuthViewModel
import com.example.rustyalarm.ui.components.PinKeypad

private const val PIN_LENGTH = 4

@Composable
fun PinSetupScreen(
    vm: AuthViewModel,
    onComplete: () -> Unit,
) {
    var step by remember { mutableStateOf(SetupStep.CREATE) }
    var firstPin by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var mismatchError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0D0D22), Color(0xFF0A0A1A)))
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))
            Icon(
                Icons.Default.Lock, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                when (step) {
                    SetupStep.CREATE  -> "PIN 4자리 만들기"
                    SetupStep.CONFIRM -> "다시 한 번 입력"
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (mismatchError) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "PIN이 일치하지 않아요. 처음부터 다시 시도해 주세요.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.weight(1f))
            PinKeypad(
                pinLength = pin.length,
                maxLength = PIN_LENGTH,
                onDigit = { d ->
                    mismatchError = false
                    if (pin.length < PIN_LENGTH) {
                        pin += d.toString()
                        if (pin.length == PIN_LENGTH) {
                            when (step) {
                                SetupStep.CREATE -> {
                                    firstPin = pin
                                    pin = ""
                                    step = SetupStep.CONFIRM
                                }
                                SetupStep.CONFIRM -> {
                                    if (pin == firstPin) {
                                        vm.setPin(pin)
                                        onComplete()
                                    } else {
                                        mismatchError = true
                                        firstPin = ""
                                        pin = ""
                                        step = SetupStep.CREATE
                                    }
                                }
                            }
                        }
                    }
                },
                onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
            )
            Spacer(Modifier.height(48.dp))
        }
    }
}

private enum class SetupStep { CREATE, CONFIRM }
