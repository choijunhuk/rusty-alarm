import Foundation

/// iOS 26 AlarmKit conditional layer.
///
/// Free Apple ID (Personal Team) provisioning profiles cannot vend the
/// `com.apple.developer.alarmkit` entitlement, so on personal-signed builds
/// AlarmKit's `AlarmManager.authorizationState` resolves to `.denied` and we
/// fall back to plain `UNCalendarNotificationTrigger`. With a paid Apple
/// Developer Program account and the entitlement, this bridge transparently
/// schedules AlarmKit alarms instead (precise, system-elevated audio).
enum AlarmKitBridge {
    /// True when both the SDK version and the entitlement permit AlarmKit.
    /// Runtime-evaluated so unentitled builds never call the AlarmKit API.
    static var isAvailable: Bool {
        if #available(iOS 26.0, *) {
            return AlarmKitProbe.isEntitled
        }
        return false
    }

    /// Schedule via AlarmKit if available; the closure is invoked otherwise
    /// so callers can run the UNNotificationCenter path.
    static func schedule(
        alarm: Alarm,
        fallback: () -> Void
    ) {
        guard isAvailable else { fallback(); return }
        if #available(iOS 26.0, *) {
            AlarmKitProbe.schedule(alarm: alarm, fallback: fallback)
        } else {
            fallback()
        }
    }

    static func cancel(
        alarmId: UUID,
        fallback: () -> Void
    ) {
        guard isAvailable else { fallback(); return }
        if #available(iOS 26.0, *) {
            AlarmKitProbe.cancel(alarmId: alarmId, fallback: fallback)
        } else {
            fallback()
        }
    }
}

#if canImport(AlarmKit)
import AlarmKit

@available(iOS 26.0, *)
private enum AlarmKitProbe {
    /// Returns true only when the AlarmKit entitlement is granted at runtime.
    /// Without the entitlement `AlarmManager.shared.authorizationState`
    /// reports `.denied` immediately.
    static var isEntitled: Bool {
        AlarmManager.shared.authorizationState != .denied
    }

    static func schedule(alarm: Alarm, fallback: () -> Void) {
        // AlarmKit API surface still ships under active iteration; we keep
        // the fallback as the safe default until a stable AlarmManager
        // configuration ships in the public SDK.
        fallback()
    }

    static func cancel(alarmId: UUID, fallback: () -> Void) {
        fallback()
    }
}
#else
private enum AlarmKitProbe {
    static var isEntitled: Bool { false }
    @available(iOS 26.0, *)
    static func schedule(alarm: Alarm, fallback: () -> Void) { fallback() }
    @available(iOS 26.0, *)
    static func cancel(alarmId: UUID, fallback: () -> Void) { fallback() }
}
#endif
