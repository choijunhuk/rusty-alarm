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
            }
            .padding(8)
        }
    }
}
