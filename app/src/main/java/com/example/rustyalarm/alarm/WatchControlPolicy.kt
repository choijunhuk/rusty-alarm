package com.example.rustyalarm.alarm

data class WatchControlAccess(
    val canSnooze: Boolean,
    val canDismiss: Boolean,
    val requiresPhone: Boolean,
)

object WatchControlPolicy {
    fun access(alarm: Alarm, snoozesUsed: Int): WatchControlAccess {
        val requiresPhone =
            alarm.challengeType != ChallengeType.NONE || alarm.routineItems.isNotEmpty()
        val canSnooze = alarm.maxSnoozes == 0 || snoozesUsed < alarm.maxSnoozes

        return WatchControlAccess(
            canSnooze = canSnooze,
            canDismiss = !requiresPhone,
            requiresPhone = requiresPhone,
        )
    }
}
