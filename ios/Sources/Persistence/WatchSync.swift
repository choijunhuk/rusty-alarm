import Foundation
import WatchConnectivity

/// Pushes the next-alarm payload to the paired Apple Watch via
/// WatchConnectivity. Also writes to App Group defaults so the
/// home-screen widget timeline can pick it up.
final class WatchSync: NSObject, WCSessionDelegate {
    static let shared = WatchSync()

    private override init() { super.init() }

    func start() {
        guard WCSession.isSupported() else { return }
        let session = WCSession.default
        session.delegate = self
        session.activate()
    }

    func pushNext(triggerAt: Date?, title: String?, streak: Int) {
        // App Group defaults (widget consumes these)
        let defaults = UserDefaults(suiteName: "group.com.example.rustyalarm")
        defaults?.set(triggerAt?.timeIntervalSince1970 ?? 0.0, forKey: "next.alarm.ts")
        defaults?.set(title, forKey: "next.alarm.title")
        defaults?.set(streak, forKey: "next.alarm.streak")

        // Watch — via applicationContext (latest-wins, low-priority)
        guard WCSession.isSupported(), WCSession.default.isPaired else { return }
        let context: [String: Any] = [
            "trigger_at": triggerAt?.timeIntervalSince1970 ?? 0,
            "title": title ?? "",
            "streak": streak,
        ]
        try? WCSession.default.updateApplicationContext(context)
    }

    // MARK: WCSessionDelegate (required stubs)

    func session(_ session: WCSession,
                 activationDidCompleteWith activationState: WCSessionActivationState,
                 error: Error?) {}
    #if os(iOS)
    func sessionDidBecomeInactive(_ session: WCSession) {}
    func sessionDidDeactivate(_ session: WCSession) {
        WCSession.default.activate()
    }
    #endif
}
