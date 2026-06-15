import Foundation

enum WakeupPreset: String, CaseIterable, Identifiable {
    case comfortable
    case onTime
    case forced

    var id: String { rawValue }

    var label: String {
        switch self {
        case .comfortable: return "편안한 기상"
        case .onTime: return "지각 방지"
        case .forced: return "강제 기상"
        }
    }
}

enum WakeupProfileLevel {
    case strong
    case balanced
    case weak

    var label: String {
        switch self {
        case .strong: return "강함"
        case .balanced: return "보통"
        case .weak: return "약함"
        }
    }
}

struct WakeupProfile {
    let level: WakeupProfileLevel
    let title: String
    let recommendation: String
}

enum WakeupCoaching {
    static func apply(_ preset: WakeupPreset, to alarm: Alarm) -> Alarm {
        var copy = alarm
        switch preset {
        case .forced:
            if copy.challengeType == .none { copy.challengeType = .mathMedium }
            copy.maxSnoozes = copy.maxSnoozes == 0 ? 1 : min(copy.maxSnoozes, 2)
            if copy.routineItems.isEmpty {
                copy.routineItems = ["물 한 잔 마시기", "불 켜기"]
            }
            copy.gradualWakeup = true
            copy.alarmVolumePercent = max(copy.alarmVolumePercent, 85)
            copy.soundEnabled = true
            copy.vibrate = true
        case .comfortable:
            copy.gradualWakeup = true
            copy.maxSnoozes = copy.maxSnoozes == 0 ? 3 : min(copy.maxSnoozes, 3)
            copy.alarmVolumePercent = max(copy.alarmVolumePercent, 70)
            copy.soundEnabled = true
            copy.vibrate = true
        case .onTime:
            if copy.challengeType == .none { copy.challengeType = .typing }
            copy.maxSnoozes = 1
            if copy.routineItems.isEmpty {
                copy.routineItems = ["자리에서 일어나기", "오늘 첫 일정 확인"]
            }
            copy.gradualWakeup = true
            copy.alarmVolumePercent = max(copy.alarmVolumePercent, 90)
            copy.soundEnabled = true
            copy.vibrate = true
        }
        return copy
    }

    static func profile(for alarm: Alarm) -> WakeupProfile {
        let hasGate = alarm.challengeType != .none || !alarm.routineItems.isEmpty
        let cappedSnooze = (1...3).contains(alarm.maxSnoozes)
        let strongSound = alarm.soundEnabled && alarm.alarmVolumePercent >= 70
        let score = [hasGate, cappedSnooze, strongSound, alarm.gradualWakeup]
            .filter { $0 }
            .count

        switch score {
        case 3...:
            return WakeupProfile(
                level: .strong,
                title: "실사용 강도 좋음",
                recommendation: "지금 설정은 실제 아침 알람으로 쓰기 좋아요."
            )
        case 2:
            return WakeupProfile(
                level: .balanced,
                title: "조금만 보강하면 좋아요",
                recommendation: "스누즈 제한이나 챌린지 중 하나를 더 켜보세요."
            )
        default:
            return WakeupProfile(
                level: .weak,
                title: "쉽게 꺼질 수 있어요",
                recommendation: "강제 기상 모드로 끄기 장치를 추가해보세요."
            )
        }
    }

    static func readinessLabel(for alarm: Alarm) -> String {
        let risky = (!alarm.soundEnabled && !alarm.vibrate)
            || alarm.alarmVolumePercent < 40
            || (alarm.challengeType == .none && alarm.routineItems.isEmpty)
        return risky ? "주의" : "좋음"
    }

    static func watchAccess(for alarm: Alarm) -> WatchControlAccess {
        let requiresPhone = alarm.challengeType != .none || !alarm.routineItems.isEmpty
        return WatchControlAccess(
            canSnooze: true,
            canDismiss: !requiresPhone,
            requiresPhone: requiresPhone
        )
    }
}

struct WatchControlAccess {
    let canSnooze: Bool
    let canDismiss: Bool
    let requiresPhone: Bool
}

struct WakeupInsight: Identifiable {
    let id: String
    let title: String
    let detail: String
    let action: WakeupInsightAction?

    init(id: String, title: String, detail: String, action: WakeupInsightAction? = nil) {
        self.id = id
        self.title = title
        self.detail = detail
        self.action = action
    }
}

struct WakeupInsightAction {
    let label: String
    let preset: WakeupPreset
}

struct WakeupInsightInput {
    let fired: Int
    let dismissed: Int
    let snoozed: Int
    let avgResponseSec: Int
    let challengesCompleted: Int
    let streakDays: Int
}

enum ReportInsightEngine {
    static func insights(for input: WakeupInsightInput) -> [WakeupInsight] {
        var result: [WakeupInsight] = []
        let completionRate = input.fired == 0 ? 100 : input.dismissed * 100 / input.fired
        let snoozeRate = input.fired == 0 ? 0 : input.snoozed * 100 / input.fired

        if completionRate >= 90 && input.snoozed == 0 && input.streakDays >= 7 {
            result.append(WakeupInsight(
                id: "strong_week",
                title: "이번 주 흐름이 좋아요",
                detail: "연속 기상과 스누즈 없는 패턴이 안정적으로 유지되고 있어요."
            ))
        }
        if input.fired >= 3 && completionRate < 70 {
            result.append(WakeupInsight(
                id: "low_completion",
                title: "알람 신뢰도부터 점검하세요",
                detail: "울린 알람 대비 끈 알람이 적어요. 권한, 배터리, 테스트 알람을 먼저 확인하는 게 좋아요."
            ))
        }
        if input.fired >= 3 && snoozeRate >= 50 {
            result.append(WakeupInsight(
                id: "high_snooze",
                title: "스누즈가 많은 편이에요",
                detail: "스누즈를 1-2회로 제한하고 챌린지 난이도를 한 단계 올려보세요.",
                action: WakeupInsightAction(label: "지각 방지 모드 적용", preset: .onTime)
            ))
        }
        if input.dismissed >= 3 && input.challengesCompleted == 0 {
            result.append(WakeupInsight(
                id: "no_challenge",
                title: "챌린지를 하나 켜보세요",
                detail: "무의식적으로 끄는 일이 있다면 수학 쉬움이나 타이핑 챌린지가 부담이 적어요.",
                action: WakeupInsightAction(label: "강제 기상 모드 적용", preset: .forced)
            ))
        }
        if input.avgResponseSec >= 180 {
            result.append(WakeupInsight(
                id: "slow_response",
                title: "끄기까지 시간이 오래 걸려요",
                detail: "단계적 알람을 켜면 갑작스러운 기상 부담을 줄일 수 있어요.",
                action: WakeupInsightAction(label: "편안한 기상 모드 적용", preset: .comfortable)
            ))
        }
        if result.isEmpty {
            result.append(WakeupInsight(
                id: "not_enough_data",
                title: "데이터를 조금 더 모아볼게요",
                detail: "일주일 정도 사용하면 스누즈, 챌린지, 기상 성공률 기반 추천이 더 정확해져요."
            ))
        }
        return result
    }
}
