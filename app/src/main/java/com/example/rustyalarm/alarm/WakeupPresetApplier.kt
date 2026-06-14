package com.example.rustyalarm.alarm

enum class WakeupPreset(
    val label: String,
    val description: String,
) {
    STRICT(
        label = "확실히 일어나기",
        description = "챌린지, 스누즈 제한, 루틴을 함께 켜요.",
    ),
    GENTLE_SAFE(
        label = "부드럽지만 안전하게",
        description = "미리알림과 단계적 알람으로 부담을 줄여요.",
    ),
    FOCUS_DAY(
        label = "중요한 날",
        description = "회의나 수업 전에 놓치지 않도록 강하게 보강해요.",
    ),
}

enum class WakeupProfileLevel(val label: String) {
    STRONG("강함"),
    BALANCED("보통"),
    WEAK("약함"),
}

data class WakeupProfile(
    val level: WakeupProfileLevel,
    val title: String,
    val recommendation: String,
)

object WakeupPresetApplier {

    fun apply(alarm: Alarm, preset: WakeupPreset): Alarm = when (preset) {
        WakeupPreset.STRICT -> alarm.copy(
            challengeType = if (alarm.challengeType == ChallengeType.NONE) {
                ChallengeType.MATH_MEDIUM
            } else {
                alarm.challengeType
            },
            maxSnoozes = when (alarm.maxSnoozes) {
                0 -> 1
                else -> alarm.maxSnoozes.coerceAtMost(2)
            },
            routineItems = alarm.routineItems.ifEmpty {
                listOf("물 한 잔 마시기", "불 켜기")
            },
            gradualWakeup = true,
            volumeRampSeconds = alarm.volumeRampSeconds.coerceAtLeast(10),
            alarmVolumePercent = alarm.alarmVolumePercent.coerceAtLeast(85),
            preAlarmMinutes = if (alarm.preAlarmMinutes == 0) 10 else alarm.preAlarmMinutes,
            soundEnabled = true,
            vibrate = true,
        )

        WakeupPreset.GENTLE_SAFE -> alarm.copy(
            gradualWakeup = true,
            volumeRampSeconds = alarm.volumeRampSeconds.coerceAtLeast(20),
            preAlarmMinutes = if (alarm.preAlarmMinutes == 0) 15 else alarm.preAlarmMinutes,
            alarmVolumePercent = alarm.alarmVolumePercent.coerceAtLeast(70),
            soundEnabled = true,
            vibrate = true,
        )

        WakeupPreset.FOCUS_DAY -> alarm.copy(
            challengeType = if (alarm.challengeType == ChallengeType.NONE) {
                ChallengeType.TYPING
            } else {
                alarm.challengeType
            },
            maxSnoozes = 1,
            routineItems = alarm.routineItems.ifEmpty {
                listOf("자리에서 일어나기", "오늘 첫 일정 확인")
            },
            gradualWakeup = true,
            volumeRampSeconds = alarm.volumeRampSeconds.coerceAtLeast(5),
            alarmVolumePercent = alarm.alarmVolumePercent.coerceAtLeast(90),
            preAlarmMinutes = if (alarm.preAlarmMinutes == 0) 10 else alarm.preAlarmMinutes,
            soundEnabled = true,
            vibrate = true,
        )
    }

    fun profile(alarm: Alarm): WakeupProfile {
        val hasGate = alarm.challengeType != ChallengeType.NONE || alarm.routineItems.isNotEmpty()
        val cappedSnooze = alarm.maxSnoozes in 1..3
        val strongSound = alarm.soundEnabled && alarm.alarmVolumePercent >= 70
        val score = listOf(hasGate, cappedSnooze, strongSound, alarm.gradualWakeup).count { it }

        return when {
            score >= 3 -> WakeupProfile(
                level = WakeupProfileLevel.STRONG,
                title = "실사용 강도 좋음",
                recommendation = "지금 설정은 실제 아침 알람으로 쓰기 좋아요.",
            )
            score == 2 -> WakeupProfile(
                level = WakeupProfileLevel.BALANCED,
                title = "조금만 보강하면 좋아요",
                recommendation = "스누즈 제한이나 챌린지 중 하나를 더 켜보세요.",
            )
            else -> WakeupProfile(
                level = WakeupProfileLevel.WEAK,
                title = "쉽게 꺼질 수 있어요",
                recommendation = "확실히 일어나기 프리셋으로 끄기 장치를 추가해보세요.",
            )
        }
    }
}
