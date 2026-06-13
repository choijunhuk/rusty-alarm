import Foundation
import Combine

/// Simple JSON-backed store in `UserDefaults`. Production code would move
/// this to a Core Data / GRDB layer, but for v1 this matches Android JSON
/// import/export 1:1 and keeps deps to zero.
final class AlarmStore: ObservableObject {
    @Published private(set) var alarms: [Alarm] = []

    private let key = "rusty.alarms.v1"

    init() { load() }

    func load() {
        guard let data = UserDefaults.standard.data(forKey: key) else { return }
        alarms = (try? JSONDecoder().decode([Alarm].self, from: data)) ?? []
    }

    func save() {
        if let data = try? JSONEncoder().encode(alarms) {
            UserDefaults.standard.set(data, forKey: key)
        }
    }

    func upsert(_ alarm: Alarm) {
        var copy = alarm
        copy.updatedAt = Date()
        if let idx = alarms.firstIndex(where: { $0.id == alarm.id }) {
            alarms[idx] = copy
        } else {
            alarms.append(copy)
        }
        save()
        AlarmScheduler.shared.reschedule(copy)
    }

    func delete(_ alarm: Alarm) {
        alarms.removeAll { $0.id == alarm.id }
        save()
        AlarmScheduler.shared.cancel(alarm.id)
    }

    func toggle(_ alarm: Alarm, enabled: Bool) {
        guard let idx = alarms.firstIndex(where: { $0.id == alarm.id }) else { return }
        alarms[idx].enabled = enabled
        save()
        if enabled { AlarmScheduler.shared.reschedule(alarms[idx]) }
        else       { AlarmScheduler.shared.cancel(alarms[idx].id) }
    }
}
