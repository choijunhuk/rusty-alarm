import XCTest
@testable import RustyAlarm

final class WakeupCoachingTests: XCTestCase {
    func testComfortableModeAllowsSeveralSnoozesAndGradualWakeup() {
        let alarm = WakeupCoaching.apply(.comfortable, to: Alarm(maxSnoozes: 0))

        XCTAssertEqual(alarm.maxSnoozes, 3)
        XCTAssertTrue(alarm.gradualWakeup)
        XCTAssertGreaterThanOrEqual(alarm.alarmVolumePercent, 70)
    }

    func testOnTimeModeUsesOneSnoozeAndLowFrictionChallenge() {
        let alarm = WakeupCoaching.apply(.onTime, to: Alarm())

        XCTAssertEqual(alarm.maxSnoozes, 1)
        XCTAssertEqual(alarm.challengeType, .typing)
        XCTAssertGreaterThanOrEqual(alarm.alarmVolumePercent, 90)
    }

    func testForcedModeCannotDismissFromWatchWhenChallengeExists() {
        let alarm = WakeupCoaching.apply(.forced, to: Alarm())
        let access = WakeupCoaching.watchAccess(for: alarm)

        XCTAssertFalse(access.canDismiss)
        XCTAssertTrue(access.requiresPhone)
    }

    func testHighSnoozeRecommendationAppliesOnTimeMode() {
        let insight = ReportInsightEngine.insights(
            for: WakeupInsightInput(
                fired: 8,
                dismissed: 8,
                snoozed: 6,
                avgResponseSec: 120,
                challengesCompleted: 1,
                streakDays: 2
            )
        ).first { $0.id == "high_snooze" }

        XCTAssertEqual(insight?.action?.preset, .onTime)
    }
}
