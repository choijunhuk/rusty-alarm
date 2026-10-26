package com.example.rustyalarm.alarm

enum class WakeupPreset(
    val label: String,
    val description: String,
) {
    STANDARD(
        label = "일반 알람",
        description = "버튼으로 끄기 · 스누즈 무제한 · 음량 100%",
    ),
    COMFORTABLE(
        label = "편안한 기상",
        description = "버튼으로 끄기 · 스누즈 3회 · 음량 70%, 20초 페이드인",
    ),
    ON_TIME(
        label = "지각 방지",
        description = "타이핑으로 끄기 · 스누즈 1회 · 음량 90%, 5초 페이드인",
    ),
    FORCED(
        label = "강제 기상",
        description = "수학 보통 1문제와 루틴 2개 · 스누즈 1회 · 음량 100%, 10초 페이드인",
    ),
}

object WakeupPresetApplier {

    // Each preset replaces the previous mode's settings, so choosing a gentler
    // mode also removes the previous challenge and routine. Pre-alarms are opt-in.
    fun apply(alarm: Alarm, preset: WakeupPreset): Alarm {
        val base = alarm.copy(
            challengeType = ChallengeType.NONE,
            maxSnoozes = 0,
            routineItems = emptyList(),
            mathProblemCount = 1,
            gradualWakeup = false,
            volumeRampSeconds = 0,
            alarmVolumePercent = 100,
            preAlarmMinutes = 0,
            isSmartAlarm = false,
            soundEnabled = true,
            vibrate = true,
        )
        return when (preset) {
            WakeupPreset.STANDARD -> base
            WakeupPreset.COMFORTABLE -> base.copy(
                maxSnoozes = 3,
                gradualWakeup = true,
                volumeRampSeconds = 20,
                alarmVolumePercent = 70,
            )
            WakeupPreset.ON_TIME -> base.copy(
                challengeType = ChallengeType.TYPING,
                maxSnoozes = 1,
                gradualWakeup = true,
                volumeRampSeconds = 5,
                alarmVolumePercent = 90,
            )
            WakeupPreset.FORCED -> base.copy(
                challengeType = ChallengeType.MATH_MEDIUM,
                maxSnoozes = 1,
                routineItems = listOf("물 한 잔 마시기", "불 켜기"),
                gradualWakeup = true,
                volumeRampSeconds = 10,
            )
        }
    }
}
