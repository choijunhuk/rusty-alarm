package com.example.rustyalarm.viewmodel

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
            )
        }

        if (input.fired >= 3 && snoozeRate >= 50) {
            insights += WakeupInsight(
                id = "high_snooze",
                title = "스누즈가 많은 편이에요",
                detail = "스누즈를 1-2회로 제한하고 챌린지 난이도를 한 단계 올려보세요.",
            )
        }

        if (input.dismissed >= 3 && input.challengesCompleted == 0) {
            insights += WakeupInsight(
                id = "no_challenge",
                title = "챌린지를 하나 켜보세요",
                detail = "무의식적으로 끄는 일이 있다면 수학 쉬움이나 타이핑 챌린지가 부담이 적어요.",
            )
        }

        if (input.avgResponseSec >= 180) {
            insights += WakeupInsight(
                id = "slow_response",
                title = "끄기까지 시간이 오래 걸려요",
                detail = "미리알림이나 단계적 알람을 켜면 갑작스러운 기상 부담을 줄일 수 있어요.",
            )
        }

        if (insights.isEmpty()) {
            insights += WakeupInsight(
                id = "not_enough_data",
                title = "데이터를 조금 더 모아볼게요",
                detail = "일주일 정도 사용하면 스누즈, 챌린지, 기상 성공률 기반 추천이 더 정확해져요.",
            )
        }

        return insights
    }
}
