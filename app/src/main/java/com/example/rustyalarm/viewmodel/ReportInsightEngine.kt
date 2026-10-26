package com.example.rustyalarm.viewmodel

import com.example.rustyalarm.alarm.WakeupPreset

data class WakeupInsightInput(
    val fired: Int,
    val dismissed: Int,
    val snoozed: Int,
    val avgResponseSec: Long,
    val challengesCompleted: Int,
    val streakDays: Int,
)

data class WakeupInsight(
    val id: String,
    val title: String,
    val detail: String,
    val cause: String? = null,
    val action: WakeupInsightAction? = null,
)

data class WakeupInsightAction(
    val label: String,
    val preset: WakeupPreset,
    val expectedResult: String,
)

object ReportInsightEngine {

    fun insights(input: WakeupInsightInput): List<WakeupInsight> {
        val insights = mutableListOf<WakeupInsight>()
        val completionRate = if (input.fired == 0) 100 else input.dismissed * 100 / input.fired
        val snoozeRate = if (input.fired == 0) 0 else input.snoozed * 100 / input.fired

        if (completionRate >= 90 && input.snoozed == 0 && input.streakDays >= 7) {
            insights += WakeupInsight(
                id = "strong_week",
                title = "이번 주 흐름이 좋아요",
                detail = "연속 기상과 스누즈 없는 패턴이 안정적으로 유지되고 있어요.",
            )
        }

        if (input.fired >= 3 && completionRate < 70) {
            insights += WakeupInsight(
                id = "low_completion",
                title = "알람 신뢰도부터 점검하세요",
                detail = "울린 알람 대비 끈 알람이 적어요. 권한, 배터리 예외, 테스트 알람을 먼저 확인하는 게 좋아요.",
                cause = "기상 완료율 $completionRate%",
            )
        }

        if (input.fired >= 3 && snoozeRate >= 50) {
            insights += WakeupInsight(
                id = "high_snooze",
                title = "스누즈가 많은 편이에요",
                detail = "스누즈를 줄이고 싶다면 타이핑 챌린지와 스누즈 1회를 선택할 수 있어요.",
                cause = "이번 주 스누즈 비율 $snoozeRate%",
                action = WakeupInsightAction(
                    label = "지각 방지 모드 적용",
                    preset = WakeupPreset.ON_TIME,
                    expectedResult = "다음 알람을 타이핑, 스누즈 1회, 음량 90%, 5초 페이드인으로 바꿔요. 소리·진동을 켜고 루틴·미리알림·스마트 알람을 꺼요.",
                ),
            )
        }

        if (input.avgResponseSec >= 180) {
            insights += WakeupInsight(
                id = "slow_response",
                title = "끄기까지 시간이 오래 걸려요",
                detail = "갑작스러운 알람이 부담스럽다면 음량을 서서히 높이는 모드를 선택할 수 있어요.",
                cause = "평균 끄기 시간 ${input.avgResponseSec / 60}분 이상",
                action = WakeupInsightAction(
                    label = "편안한 기상 모드 적용",
                    preset = WakeupPreset.COMFORTABLE,
                    expectedResult = "다음 알람의 챌린지와 루틴, 미리알림, 스마트 알람을 끄고 소리·진동을 켜요. 스누즈 3회, 음량 70%, 20초 페이드인을 적용해요.",
                ),
            )
        }

        if (insights.isEmpty()) {
            insights += WakeupInsight(
                id = "not_enough_data",
                title = "데이터를 조금 더 모아볼게요",
                detail = "알람 해제와 스누즈 기록이 쌓이면 사용 패턴을 확인할 수 있어요.",
            )
        }

        return insights
    }
}
