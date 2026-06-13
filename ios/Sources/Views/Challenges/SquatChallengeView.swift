import SwiftUI
import CoreMotion

struct SquatChallengeView: View {
    let target: Int
    let onSolved: () -> Void

    @State private var count = 0
    @State private var phase = "준비"
    @State private var smoothedY: Double = 9.8
    @State private var state = "up"

    private let motion = CMMotionManager()

    var body: some View {
        VStack(spacing: 8) {
            Text("스쿼트 \(target)회로 알람 끄기")
                .font(.headline).foregroundStyle(.purple)
            Text("폰을 가슴 주머니나 손에 들고 똑바로 서서 시작하세요.")
                .font(.caption).foregroundStyle(.white.opacity(0.6))
                .multilineTextAlignment(.center)
            Text("\(count) / \(target)")
                .font(.system(size: 40, weight: .bold))
                .foregroundStyle(.purple)
            ProgressView(value: Double(count) / Double(target)).tint(.purple)
            Text(phase).font(.caption).foregroundStyle(.white.opacity(0.7))
        }
        .padding(18)
        .background(.white.opacity(0.05))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .onAppear(perform: start)
        .onDisappear(perform: stop)
    }

    private func start() {
        guard motion.isAccelerometerAvailable else { return }
        motion.accelerometerUpdateInterval = 1.0 / 30.0
        motion.startAccelerometerUpdates(to: .main) { data, _ in
            guard let d = data?.acceleration else { return }
            let v = abs(d.z * 9.8) > abs(d.y * 9.8)
                ? abs(d.z * 9.8) : abs(d.y * 9.8)
            smoothedY = 0.85 * smoothedY + 0.15 * v
            switch state {
            case "up" where smoothedY < 7.0:
                state = "down"; phase = "내려가는 중"
            case "down" where smoothedY > 9.0:
                state = "up"
                count += 1
                phase = "일어남 ✓"
                if count >= target { onSolved() }
            default: break
            }
        }
    }

    private func stop() {
        motion.stopAccelerometerUpdates()
    }
}
