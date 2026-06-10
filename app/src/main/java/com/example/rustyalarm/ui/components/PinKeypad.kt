package com.example.rustyalarm.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PinKeypad(
    pinLength: Int,
    maxLength: Int = 4,
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    onBiometric: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Dot indicators
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            repeat(maxLength) { i ->
                val filled = i < pinLength
                Surface(
                    shape = CircleShape,
                    color = if (filled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(16.dp),
                ) {}
            }
        }

        Spacer(Modifier.height(8.dp))

        // 1 2 3
        // 4 5 6
        // 7 8 9
        // ⓘ 0 ⌫
        val rows = listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6),
            listOf(7, 8, 9),
        )
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { d ->
                    KeyButton(label = d.toString(), onClick = { onDigit(d) })
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBiometric != null) {
                IconKeyButton(
                    icon = Icons.Default.Fingerprint,
                    contentDescription = "지문",
                    onClick = onBiometric,
                )
            } else {
                Spacer(Modifier.size(72.dp))
            }
            KeyButton(label = "0", onClick = { onDigit(0) })
            IconKeyButton(
                icon = Icons.Default.Backspace,
                contentDescription = "지우기",
                onClick = onBackspace,
            )
        }
    }
}

@Composable
private fun KeyButton(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.size(72.dp).clip(CircleShape),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun IconKeyButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        modifier = Modifier.size(72.dp).clip(CircleShape),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
