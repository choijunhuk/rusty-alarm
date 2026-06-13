import Foundation
import UserNotifications

/// Schedules iOS local notifications that approximate Android's exact alarms.
///
/// Caveat: iOS does not expose a reliable "wake the device + override silent"
/// API outside of Apple's Clock app. The closest path is `.criticalAlert`
/// authorisation, which Apple grants only on entitlement approval.
///
/// We schedule one `UNCalendarNotificationTrigger` per alarm (or one per
/// active weekday for repeating alarms) and rely on the system delivery.
final class AlarmScheduler {
    static let shared = AlarmScheduler()
    private init() {}

    func requestAuthorization() async {
        let center = UNUserNotificationCenter.current()
        _ = try? await center.requestAuthorization(options: [.alert, .sound, .badge])
    }

    func reschedule(_ alarm: Alarm) {
        cancel(alarm.id)
        guard alarm.enabled else { return }

        let content = UNMutableNotificationContent()
        content.title = alarm.title
        content.body  = alarm.message.isEmpty
            ? "알람이 울리고 있어요"
            : alarm.message
        content.sound = .defaultCritical

        if alarm.repeatDays.isEmpty {
            schedule(once: alarm, content: content)
        } else {
            schedule(repeating: alarm, content: content)
        }
    }

    private func schedule(once alarm: Alarm, content: UNNotificationContent) {
        var components = DateComponents()
        components.hour   = alarm.hour
        components.minute = alarm.minute
        if let specific = alarm.specificDate {
            let cal = Calendar.current
            components.year  = cal.component(.year,  from: specific)
            components.month = cal.component(.month, from: specific)
            components.day   = cal.component(.day,   from: specific)
        }
        let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
        let req = UNNotificationRequest(
            identifier: alarm.id.uuidString,
            content: content,
            trigger: trigger,
        )
        UNUserNotificationCenter.current().add(req)
    }

    private func schedule(repeating alarm: Alarm, content: UNNotificationContent) {
        // iOS weekday: 1=Sunday … 7=Saturday. Android: 0=Sunday … 6=Saturday.
        for day in alarm.repeatDays {
            var components = DateComponents()
            components.hour    = alarm.hour
            components.minute  = alarm.minute
            components.weekday = day + 1
            let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: true)
            let id = "\(alarm.id.uuidString)#\(day)"
            let req = UNNotificationRequest(identifier: id, content: content, trigger: trigger)
            UNUserNotificationCenter.current().add(req)
        }
    }

    func cancel(_ alarmId: UUID) {
        let center = UNUserNotificationCenter.current()
        center.getPendingNotificationRequests { reqs in
            let prefix = alarmId.uuidString
            let ids = reqs.map(\.identifier).filter { $0.hasPrefix(prefix) }
            center.removePendingNotificationRequests(withIdentifiers: ids)
        }
    }
}
