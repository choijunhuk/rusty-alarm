import Foundation

enum AlarmEventType: String, Codable {
    case fired
    case dismissed
    case snoozed
}

struct AlarmEvent: Codable, Identifiable {
    let id: UUID
    let alarmId: UUID?
    let type: AlarmEventType
    let timestamp: Date
    let challengeType: ChallengeType?
    let responseSec: Int?

    init(
        id: UUID = UUID(),
        alarmId: UUID?,
        type: AlarmEventType,
        timestamp: Date = Date(),
        challengeType: ChallengeType? = nil,
        responseSec: Int? = nil,
    ) {
        self.id = id
        self.alarmId = alarmId
        self.type = type
        self.timestamp = timestamp
        self.challengeType = challengeType
        self.responseSec = responseSec
    }
}
