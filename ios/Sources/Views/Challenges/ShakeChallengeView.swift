import SwiftUI
import CoreMotion

struct ShakeChallengeView: View {
    let target: Int
    let onSolved: () -> Void

    @State private var count = 0
    private let motion = CMMotionManager()
    @State private var last = (x: 0.0, y: 0.0, z: 0.0)
    @State private var lastTick: TimeInterval = 0

    var body: some View {
        VStack(spacing: 10) {
            Text("휴대폰을 흔들어서 알람을 끄세요!")
                .font(.headline).foregroundStyle(.purple)
            Text("\(count) / \(target)")
                .font(.system(size: 48, weight: .bold))
                .foregroundStyle(.purple)
            ProgressView(value: Double(count) / Double(target)).tint(.purple)
        }
        .padding(18)
        .frame(maxWidth: .infinity)
        .background(.white.opacity(0.05))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .onAppear(perform: start)
        .onDisappear { motion.stopAccelerometerUpdates() }
    }

    private func start() {
        guard motion.isAccelerometerAvailable else { return }
        motion.accelerometerUpdateInterval = 0.1
        motion.startAccelerometerUpdates(to: .main) { data, _ in
            guard let d = data?.acceleration else { return }
            let now = Date().timeIntervalSince1970
            if now - lastTick < 0.1 { return }
            lastTick = now
            let dx = abs(d.x - last.x)
            let dy = abs(d.y - last.y)
            let dz = abs(d.z - last.z)
            if dx + dy + dz > 1.5 {
                count += 1
                if count >= target { onSolved() }
            }
            last = (d.x, d.y, d.z)
        }
    }
}
