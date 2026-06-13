import Foundation
import Combine

struct WeeklyReport {
    var fired: Int = 0
    var dismissed: Int = 0
    var snoozed: Int = 0
    var avgWakeupHHMM: String = "—"
    var streakDays: Int = 0
    var challengesCompleted: Int = 0
    /// (yyyy-MM-dd, count) for last 30 days, oldest first.
    var heatmap: [(String, Int)] = []
}

final class EventStore: ObservableObject {
    @Published private(set) var events: [AlarmEvent] = []
    private let key = "rusty.events.v1"

    init() { load() }

    func load() {
        guard let data = UserDefaults.standard.data(forKey: key) else { return }
        events = (try? JSONDecoder().decode([AlarmEvent].self, from: data)) ?? []
    }

    func save() {
        if let data = try? JSONEncoder().encode(events) {
            UserDefaults.standard.set(data, forKey: key)
        }
    }

    func record(_ event: AlarmEvent) {
        events.append(event)
        save()
    }

    var lastDismissedAt: Date? {
        events.filter { $0.type == .dismissed }
            .map(\.timestamp).max()
    }

    func dismissedCount(inLast days: Int) -> Int {
        let cutoff = Date().addingTimeInterval(-Double(days) * 86400)
        return events.filter { $0.type == .dismissed && $0.timestamp >= cutoff }.count
    }

    func weeklyReport() -> WeeklyReport {
        let cal = Calendar.current
        let since = cal.date(byAdding: .day, value: -7, to: Date())!
        let week = events.filter { $0.timestamp >= since }
        let fired = week.filter { $0.type == .fired }.count
        let dismissed = week.filter { $0.type == .dismissed }.count
        let snoozed = week.filter { $0.type == .snoozed }.count
        let challengesCompleted = week.filter {
            $0.type == .dismissed && $0.challengeType != nil && $0.challengeType != ChallengeType.none
        }.count

        let dismissEvents = week.filter { $0.type == .dismissed }
        let avg: String = {
            guard !dismissEvents.isEmpty else { return "—" }
            let mins = dismissEvents.map {
                let c = cal.dateComponents([.hour, .minute], from: $0.timestamp)
                return (c.hour ?? 0) * 60 + (c.minute ?? 0)
            }
            let average = mins.reduce(0, +) / mins.count
            return String(format: "%02d:%02d", average / 60, average % 60)
        }()

        let streak = computeStreak()

        // Heatmap last 30 days
        let fmt = DateFormatter()
        fmt.dateFormat = "yyyy-MM-dd"
        let bucket = Dictionary(
            grouping: events.filter { $0.type == .dismissed },
            by: { fmt.string(from: $0.timestamp) },
        )
        let heat: [(String, Int)] = (0..<30).reversed().map { offset in
            let day = cal.date(byAdding: .day, value: -offset, to: Date())!
            let key = fmt.string(from: day)
            return (key, bucket[key]?.count ?? 0)
        }

        return WeeklyReport(
            fired: fired,
            dismissed: dismissed,
            snoozed: snoozed,
            avgWakeupHHMM: avg,
            streakDays: streak,
            challengesCompleted: challengesCompleted,
            heatmap: heat,
        )
    }

    private func computeStreak() -> Int {
        let fmt = DateFormatter()
        fmt.dateFormat = "yyyy-MM-dd"
        let days = Set(
            events.filter { $0.type == .dismissed }
                .map { fmt.string(from: $0.timestamp) }
        )
        if days.isEmpty { return 0 }
        let today = fmt.string(from: Date())
        let yest  = fmt.string(from: Date().addingTimeInterval(-86400))
        guard let start = (days.contains(today) ? today
            : days.contains(yest) ? yest : nil) else { return 0 }
        var streak = 0
        var cur = fmt.date(from: start) ?? Date()
        let cal = Calendar.current
        while days.contains(fmt.string(from: cur)) {
            streak += 1
            cur = cal.date(byAdding: .day, value: -1, to: cur) ?? cur
        }
        return streak
    }
}
