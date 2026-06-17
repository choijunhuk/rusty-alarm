package com.example.rustyalarm.alarm

enum class ReliabilityLevel(val label: String) {
    READY("좋음"),
    CAUTION("주의"),
    BLOCKED("위험"),
}

enum class ReliabilityActionKind {
    NONE,
    NOTIFICATION_SETTINGS,
    EXACT_ALARM_SETTINGS,
    BATTERY_SETTINGS,
    CREATE_TEST_ALARM,
    EDIT_ALARM,
}

data class ReliabilityIssue(
    val id: String,
    val title: String,
    val detail: String,
    val actionLabel: String? = null,
    val blocking: Boolean = false,
    val actionKind: ReliabilityActionKind = ReliabilityActionKind.NONE,
)

data class ReliabilityDiagnostic(
    val score: Int,
    val level: ReliabilityLevel,
    val headline: String,
    val issues: List<ReliabilityIssue>,
    val strengths: List<String>,
)

object AlarmReliability {

    fun diagnose(
        permissions: PermissionsStatus,
        enabledAlarmCount: Int,
        nextAlarm: Alarm?,
    ): ReliabilityDiagnostic {
        val issues = mutableListOf<ReliabilityIssue>()
        val strengths = mutableListOf<String>()
        var score = 100

        if (!permissions.notifications) {
            issues += ReliabilityIssue(
                id = "notifications",
                title = "알림 권한이 꺼져 있어요",
                detail = "전체 화면 알람과 알림 버튼이 제한될 수 있어요.",
                actionLabel = "알림 설정",
                blocking = true,
                actionKind = ReliabilityActionKind.NOTIFICATION_SETTINGS,
            )
            score -= 25
        } else {
            strengths += "알림 권한 OK"
        }

        if (!permissions.exactAlarm) {
            issues += ReliabilityIssue(
                id = "exact_alarm",
                title = "정확한 알람 권한이 필요해요",
                detail = "Android가 알람을 몇 분 늦게 전달할 수 있어요.",
                actionLabel = "정확한 알람 허용",
                blocking = true,
                actionKind = ReliabilityActionKind.EXACT_ALARM_SETTINGS,
            )
            score -= 30
        } else {
            strengths += "정확한 알람 OK"
        }

        if (!permissions.ignoringBatteryOpts) {
            issues += ReliabilityIssue(
                id = "battery_optimization",
                title = "배터리 최적화가 알람을 막을 수 있어요",
                detail = "일부 제조사는 백그라운드 알람을 강하게 제한해요.",
                actionLabel = "배터리 예외",
                actionKind = ReliabilityActionKind.BATTERY_SETTINGS,
            )
            score -= 15
        } else {
            strengths += "배터리 예외 OK"
        }

        if (enabledAlarmCount == 0) {
            issues += ReliabilityIssue(
                id = "no_enabled_alarm",
                title = "켜진 알람이 없어요",
                detail = "실사용 검증을 하려면 최소 하나의 알람이 필요해요.",
                actionLabel = "5분 테스트 알람",
                actionKind = ReliabilityActionKind.CREATE_TEST_ALARM,
            )
            score -= 20
        }

        nextAlarm?.let { alarm ->
            val alarmIssues = alarmIssues(alarm)
            issues += alarmIssues
            score -= alarmIssues.sumOf { issuePenalty(it.id) }
            strengths += alarmStrengths(alarm)
        }

        val boundedScore = score.coerceIn(0, 100)
        val level = when {
            issues.any { it.blocking } || boundedScore < 60 -> ReliabilityLevel.BLOCKED
            boundedScore < 90 -> ReliabilityLevel.CAUTION
            else -> ReliabilityLevel.READY
        }
        val headline = when (level) {
            ReliabilityLevel.READY -> "오늘 알람 환경은 안정적이에요"
            ReliabilityLevel.CAUTION -> "알람은 울리지만 몇 가지 보강이 좋아요"
            ReliabilityLevel.BLOCKED -> "먼저 권한과 시스템 설정을 확인하세요"
        }

        return ReliabilityDiagnostic(
            score = boundedScore,
            level = level,
            headline = headline,
            issues = issues.distinctBy { it.id },
            strengths = strengths.distinct(),
        )
    }

    fun alarmIssues(alarm: Alarm): List<ReliabilityIssue> {
        val issues = mutableListOf<ReliabilityIssue>()
        if (!alarm.soundEnabled && !alarm.vibrate) {
            issues += ReliabilityIssue(
                id = "silent_alarm",
                title = "소리와 진동이 모두 꺼져 있어요",
                detail = "화면을 보고 있지 않으면 알람을 놓칠 가능성이 커요.",
                actionLabel = "알람 수정",
                actionKind = ReliabilityActionKind.EDIT_ALARM,
            )
        }
        if (alarm.soundEnabled && alarm.alarmVolumePercent < 40) {
            issues += ReliabilityIssue(
                id = "low_volume",
                title = "알람 음량이 낮아요",
                detail = "실사용 알람은 70% 이상이 안전해요.",
                actionLabel = "음량 보강",
                actionKind = ReliabilityActionKind.EDIT_ALARM,
            )
        }
        if (alarm.maxSnoozes == 0) {
            issues += ReliabilityIssue(
                id = "unlimited_snooze",
                title = "스누즈가 무제한이에요",
                detail = "다시 잠드는 습관을 막으려면 1-3회 제한이 좋아요.",
                actionLabel = "스누즈 제한",
                actionKind = ReliabilityActionKind.EDIT_ALARM,
            )
        }
        if (alarm.challengeType == ChallengeType.NONE && alarm.routineItems.isEmpty()) {
            issues += ReliabilityIssue(
                id = "no_dismiss_gate",
                title = "끄기 장치가 없어요",
                detail = "챌린지나 루틴이 없으면 무의식적으로 끄기 쉬워요.",
                actionLabel = "끄기 장치 추가",
                actionKind = ReliabilityActionKind.EDIT_ALARM,
            )
        }
        return issues
    }

    fun alarmStrengths(alarm: Alarm): List<String> = buildList {
        if (alarm.challengeType != ChallengeType.NONE) add("챌린지로 무의식 끄기 방지")
        if (alarm.maxSnoozes in 1..3) add("스누즈 ${alarm.maxSnoozes}회 제한")
        if (alarm.routineItems.isNotEmpty()) add("기상 루틴 ${alarm.routineItems.size}개")
        if (alarm.gradualWakeup) add("단계적 알람 사용")
        if (alarm.preAlarmMinutes > 0) add("${alarm.preAlarmMinutes}분 전 미리알림")
    }

    fun primaryIssue(issues: List<ReliabilityIssue>): ReliabilityIssue? =
        issues.minWithOrNull(
            compareBy<ReliabilityIssue> { if (it.blocking) 0 else 1 }
                .thenBy { issuePriority(it.id) },
        )

    fun recommendedPreset(alarm: Alarm): WakeupPreset? {
        val issues = alarmIssues(alarm).map { it.id }.toSet()
        return when {
            "silent_alarm" in issues -> WakeupPreset.FORCED
            "no_dismiss_gate" in issues && "unlimited_snooze" in issues -> WakeupPreset.ON_TIME
            "low_volume" in issues || "unlimited_snooze" in issues -> WakeupPreset.ON_TIME
            "no_dismiss_gate" in issues -> WakeupPreset.FORCED
            else -> null
        }
    }

    private fun issuePriority(id: String): Int = when (id) {
        "notifications" -> 0
        "exact_alarm" -> 1
        "battery_optimization" -> 2
        "no_enabled_alarm" -> 3
        "silent_alarm" -> 4
        "low_volume" -> 5
        "unlimited_snooze" -> 6
        "no_dismiss_gate" -> 7
        else -> 100
    }

    private fun issuePenalty(id: String): Int = when (id) {
        "silent_alarm" -> 15
        "low_volume" -> 8
        "unlimited_snooze" -> 10
        "no_dismiss_gate" -> 10
        else -> 5
    }
}
