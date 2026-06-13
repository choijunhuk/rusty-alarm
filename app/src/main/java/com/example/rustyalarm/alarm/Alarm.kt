package com.example.rustyalarm.alarm

enum class ChallengeType(val label: String) {
    NONE("없음"),
    MATH_EASY("수학 — 쉬움"),
    MATH_MEDIUM("수학 — 보통"),
    MATH_HARD("수학 — 어려움"),
    SHAKE_EASY("흔들기 — 쉬움 5회"),
    SHAKE("흔들기 — 보통 10회"),
    SHAKE_HARD("흔들기 — 어려움 25회"),
    TYPING("타이핑 챌린지"),
    TETRIS("테트리스 한 줄"),
    STEP_COUNT("걷기 20걸음"),
    PHOTO("사진 인증"),
    LOCATION("위치 인증"),
    QR_SCAN("QR 코드 스캔"),
    VOICE("음성 인식 — '일어났다'"),
    SQUAT("스쿼트 10회"),
}

data class Alarm(
    val id: Long = 0,
    val title: String = "알람",
    val hour: Int = 8,
    val minute: Int = 0,
    val repeatDays: List<Int> = emptyList(),
    val specificDate: Long? = null,
    val enabled: Boolean = true,
    val vibrate: Boolean = true,
    val soundEnabled: Boolean = true,
    val ringtoneUri: String? = null,
    val challengeType: ChallengeType = ChallengeType.NONE,
    val isSmartAlarm: Boolean = false,
    val smartWindowMinutes: Int = 30,
    val volumeRampSeconds: Int = 0,
    val groupTag: String? = null,
    val maxSnoozes: Int = 0,          // 0 = unlimited
    val message: String = "",         // shown on ring screen, e.g. "Drink water!"
    val gradualWakeup: Boolean = false,
    val mathProblemCount: Int = 1,       // 1-10 problems for math challenges
    val routineItems: List<String> = emptyList(),  // morning checklist shown after dismiss-gate
    val youtubeUrl: String? = null,       // YouTube video or playlist URL — auto-played on ring
    val alarmVolumePercent: Int = 100,    // 0-100, applied as soft scale on top of system alarm volume
    val preAlarmMinutes: Int = 15,        // 0 = disabled; otherwise gentle pre-buzz N minutes ahead  // vibrate → soft → loud staged ramp
    val geofenceLat: Double? = null,     // location challenge target
    val geofenceLng: Double? = null,
    val geofenceRadius: Int = 100,       // metres for location proximity
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
