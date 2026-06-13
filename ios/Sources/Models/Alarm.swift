import Foundation

/// Mirrors the Android `Alarm` model. iOS persists these via [`AlarmStore`]
/// and schedules them through `UNUserNotificationCenter`.
struct Alarm: Identifiable, Codable, Equatable {
    var id: UUID = UUID()
    var title: String = "알람"
    var hour: Int = 8
    var minute: Int = 0
    /// 0=Sun … 6=Sat to match Android's Calendar convention.
    var repeatDays: [Int] = []
    var specificDate: Date? = nil
    var enabled: Bool = true
    var vibrate: Bool = true
    var soundEnabled: Bool = true
    var ringtoneName: String? = nil
    var challengeType: ChallengeType = .none
    var maxSnoozes: Int = 0
    var message: String = ""
    var gradualWakeup: Bool = false
    var mathProblemCount: Int = 1
    var routineItems: [String] = []
    var youtubeUrl: String? = nil
    var alarmVolumePercent: Int = 100
    var geofenceLat: Double? = nil
    var geofenceLng: Double? = nil
    var geofenceRadius: Int = 100
    var qrRequiredValue: String? = nil
    var createdAt: Date = Date()
    var updatedAt: Date = Date()
}
