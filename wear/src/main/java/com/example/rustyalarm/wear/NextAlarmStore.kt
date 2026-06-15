package com.example.rustyalarm.wear

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * In-process snapshot of the most recently received "next alarm" payload from
 * the phone. The wear face observes this and re-renders on change.
 */
data class NextAlarmState(
    val triggerAtMillis: Long? = null,
    val title: String? = null,
    val readinessLabel: String? = null,
    val canSnooze: Boolean = false,
    val canDismiss: Boolean = false,
    val requiresPhone: Boolean = false,
) {
    val timeText: String? = triggerAtMillis?.let { formatTime(it) }
    val untilText: String? = triggerAtMillis?.let {
        val delta = it - System.currentTimeMillis()
        if (delta > 0) formatUntil(delta) else null
    }
}

object NextAlarmStore {
    private val _state = MutableStateFlow(NextAlarmState())
    val state: StateFlow<NextAlarmState> = _state

    fun update(
        triggerAtMillis: Long?,
        title: String?,
        readinessLabel: String?,
        canSnooze: Boolean,
        canDismiss: Boolean,
        requiresPhone: Boolean,
    ) {
        _state.value = NextAlarmState(
            triggerAtMillis = triggerAtMillis,
            title = title,
            readinessLabel = readinessLabel,
            canSnooze = canSnooze,
            canDismiss = canDismiss,
            requiresPhone = requiresPhone,
        )
    }
}
