package com.example.rustyalarm.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerSection(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // State lives here; initialised once when the composable first enters composition.
    // AlarmEditScreen gates display behind `isLoaded`, so this is always correct.
    val state = rememberTimePickerState(
        initialHour = hour,
        initialMinute = minute,
        is24Hour = true,
    )

    // Push user changes back to ViewModel
    LaunchedEffect(state.hour, state.minute) {
        onHourChange(state.hour)
        onMinuteChange(state.minute)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Live time preview
        Text(
            text = "%02d:%02d".format(state.hour, state.minute),
            fontSize = 60.sp,
            fontWeight = FontWeight.Thin,
            color = MaterialTheme.colorScheme.primary,
        )

        // TimeInput = HH:MM text boxes (diagram style, no clock dial)
        TimeInput(
            state = state,
            colors = TimePickerDefaults.colors(
                timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                timeSelectorSelectedContentColor   = MaterialTheme.colorScheme.onPrimaryContainer,
                timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
    }
}
