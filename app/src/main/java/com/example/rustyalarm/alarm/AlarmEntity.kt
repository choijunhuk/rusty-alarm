package com.example.rustyalarm.alarm

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "알람",
    val hour: Int = 8,
    val minute: Int = 0,
    val repeatDays: String = "",
    val specificDate: Long? = null,
    val enabled: Boolean = true,
    val vibrate: Boolean = true,
    val soundEnabled: Boolean = true,
    val ringtoneUri: String? = null,
    val challengeType: String = ChallengeType.NONE.name,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    fun toAlarm(): Alarm = Alarm(
        id = id,
        title = title,
        hour = hour,
        minute = minute,
        repeatDays = if (repeatDays.isBlank()) emptyList()
                     else repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() },
        specificDate = specificDate,
        enabled = enabled,
        vibrate = vibrate,
        soundEnabled = soundEnabled,
        ringtoneUri = ringtoneUri,
        challengeType = runCatching { ChallengeType.valueOf(challengeType) }
            .getOrDefault(ChallengeType.NONE),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        fun fromAlarm(alarm: Alarm): AlarmEntity = AlarmEntity(
            id = alarm.id,
            title = alarm.title,
            hour = alarm.hour,
            minute = alarm.minute,
            repeatDays = alarm.repeatDays.joinToString(","),
            specificDate = alarm.specificDate,
            enabled = alarm.enabled,
            vibrate = alarm.vibrate,
            soundEnabled = alarm.soundEnabled,
            ringtoneUri = alarm.ringtoneUri,
            challengeType = alarm.challengeType.name,
            createdAt = alarm.createdAt,
            updatedAt = alarm.updatedAt,
        )
    }
}
