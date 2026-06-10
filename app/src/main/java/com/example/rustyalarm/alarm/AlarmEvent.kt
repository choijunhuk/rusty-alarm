package com.example.rustyalarm.alarm

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AlarmEventType { FIRED, DISMISSED, SNOOZED }

@Entity(tableName = "alarm_events")
data class AlarmEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val alarmId: Long,
    val eventType: String,                              // AlarmEventType.name
    val timestamp: Long = System.currentTimeMillis(),
    val challengeType: String = ChallengeType.NONE.name,
    val responseSeconds: Long? = null,                  // time between FIRED and DISMISSED
) {
    fun type(): AlarmEventType =
        runCatching { AlarmEventType.valueOf(eventType) }.getOrDefault(AlarmEventType.FIRED)
}
