package com.example.rustyalarm.alarm

enum class ChallengeType(val label: String) {
    NONE("없음"),
    MATH_EASY("수학 — 쉬움"),
    MATH_MEDIUM("수학 — 보통"),
    MATH_HARD("수학 — 어려움"),
    SHAKE("흔들기 10회"),
    TYPING("타이핑 챌린지"),
}

data class Alarm(
    val id: Long = 0,
    val title: String = "알람",
    val hour: Int = 8,
    val minute: Int = 0,
    val repeatDays: List<Int> = emptyList(),
    val specificDate: Long? = null,        // non-null → one-time on this epoch-millis date
    val enabled: Boolean = true,
    val vibrate: Boolean = true,
    val soundEnabled: Boolean = true,
    val ringtoneUri: String? = null,       // null → system default alarm sound
    val challengeType: ChallengeType = ChallengeType.NONE,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
