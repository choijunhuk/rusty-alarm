import SwiftUI
import WatchKit
import WatchConnectivity

/// watchOS companion. Listens for `applicationContext` updates from the
/// iPhone with the next-alarm payload and renders a minimal face.
@main
struct RustyWatchApp: App {
    var body: some Scene {
        WindowGroup {
            WatchRootView()
        }
    }
}

private final class WatchModel: NSObject, ObservableObject, WCSessionDelegate {
    @Published var triggerAt: Date? = nil
    @Published var title: String? = nil
    @Published var streak: Int = 0
    @Published var readiness: String? = nil
    @Published var canSnooze: Bool = false
    @Published var canDismiss: Bool = false
    @Published var requiresPhone: Bool = false

    override init() {
        super.init()
        if WCSession.isSupported() {
            let s = WCSession.default
            s.delegate = self
            s.activate()
            apply(s.receivedApplicationContext)
        }
    }

    func apply(_ ctx: [String: Any]) {
        if let ts = ctx["trigger_at"] as? Double, ts > 0 {
            triggerAt = Date(timeIntervalSince1970: ts)
        } else {
            triggerAt = nil
        }
        title = ctx["title"] as? String
        streak = ctx["streak"] as? Int ?? 0
        readiness = ctx["readiness"] as? String
        canSnooze = ctx["can_snooze"] as? Bool ?? false
        canDismiss = ctx["can_dismiss"] as? Bool ?? false
        requiresPhone = ctx["requires_phone"] as? Bool ?? false
    }

    func session(_ session: WCSession,
                 activationDidCompleteWith activationState: WCSessionActivationState,
                 error: Error?) {}
    func session(_ session: WCSession, didReceiveApplicationContext ctx: [String: Any]) {
        DispatchQueue.main.async { self.apply(ctx) }
    }
}

private struct WatchRootView: View {
    @StateObject private var model = WatchModel()

    var body: some View {
        ZStack {
            RadialGradient(
                colors: [Color(red: 0.16, green: 0.12, blue: 0.36),
                         Color(red: 0.06, green: 0.05, blue: 0.15)],
                center: .center, startRadius: 0, endRadius: 200,
            )
            .ignoresSafeArea()
            VStack(spacing: 4) {
                Text("⏰")
                    .font(.system(size: 18))
                    .foregroundStyle(Color.purple)
                Text("다음 알람")
                    .font(.system(size: 11))
                    .foregroundStyle(.white.opacity(0.7))
                if let t = model.triggerAt {
                    Text(t, style: .time)
                        .font(.system(size: 28, weight: .semibold))
                        .foregroundStyle(.white)
                    if let title = model.title, !title.isEmpty {
                        Text(title).font(.system(size: 10))
                            .foregroundStyle(.white.opacity(0.7))
                    }
                } else {
                    Text("없음").font(.title3).foregroundStyle(.white)
                }
                if model.streak > 0 {
                    Text("🔥 \(model.streak)일 연속")
                        .font(.system(size: 11))
                        .foregroundStyle(.orange)
                }
                if let readiness = model.readiness, !readiness.isEmpty {
                    Text("준비 상태 · \(readiness)")
                        .font(.system(size: 10))
                        .foregroundStyle(readiness == "좋음" ? .green : .orange)
                }
                if model.requiresPhone {
                    Text("해제는 iPhone 챌린지에서")
                        .font(.system(size: 9))
                        .foregroundStyle(.orange)
                } else if model.canSnooze || model.canDismiss {
                    Text([
                        model.canSnooze ? "스누즈" : nil,
                        model.canDismiss ? "해제" : nil,
                    ].compactMap { $0 }.joined(separator: " · ") + " 가능")
                        .font(.system(size: 9))
                        .foregroundStyle(.white.opacity(0.7))
                }
            }
            .padding(8)
        }
    }
}
