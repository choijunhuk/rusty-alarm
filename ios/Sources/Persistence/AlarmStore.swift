import Foundation
import Combine

/// Simple JSON-backed store in `UserDefaults`. Production code would move
/// this to a Core Data / GRDB layer, but for v1 this matches Android JSON
/// import/export 1:1 and keeps deps to zero.
final class AlarmStore: ObservableObject {
    @Published private(set) var alarms: [Alarm] = []

    private let key = "rusty.alarms.v1"

    init() {
        load()
        syncWatch()
    }

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
        syncWatch()
    }

    func delete(_ alarm: Alarm) {
        alarms.removeAll { $0.id == alarm.id }
        save()
        AlarmScheduler.shared.cancel(alarm.id)
        syncWatch()
    }

    func toggle(_ alarm: Alarm, enabled: Bool) {
        guard let idx = alarms.firstIndex(where: { $0.id == alarm.id }) else { return }
        alarms[idx].enabled = enabled
        save()
        if enabled { AlarmScheduler.shared.reschedule(alarms[idx]) }
        else       { AlarmScheduler.shared.cancel(alarms[idx].id) }
        syncWatch()
    }

    @discardableResult
    func apply(_ preset: WakeupPreset, toNextEnabledAlarm _: Void = ()) -> Alarm? {
        guard let next = nextEnabledAlarm() else { return nil }
        let updated = WakeupCoaching.apply(preset, to: next)
        upsert(updated)
        return updated
    }

    private func nextEnabledAlarm() -> Alarm? {
        alarms.filter(\.enabled).min { lhs, rhs in
            nextDate(for: lhs) < nextDate(for: rhs)
        }
    }

    private func nextDate(for alarm: Alarm) -> Date {
        if let specific = alarm.specificDate { return specific }
        return Calendar.current.nextDate(
            after: Date(),
            matching: DateComponents(hour: alarm.hour, minute: alarm.minute),
            matchingPolicy: .nextTime,
        ) ?? .distantFuture
    }

    private func syncWatch() {
        let next = nextEnabledAlarm()
        let access = next.map(WakeupCoaching.watchAccess)
        WatchSync.shared.pushNext(
            triggerAt: next.map(nextDate),
            title: next?.title,
            streak: 0,
            readiness: next.map(WakeupCoaching.readinessLabel),
            canSnooze: access?.canSnooze ?? false,
            canDismiss: access?.canDismiss ?? false,
            requiresPhone: access?.requiresPhone ?? false
        )
    }
}
