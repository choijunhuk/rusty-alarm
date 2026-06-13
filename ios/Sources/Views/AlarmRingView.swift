import SwiftUI
import AVFoundation
import CoreMotion
import Speech
import AVKit
import WebKit
import AudioToolbox

struct AlarmRingView: View {
    let alarm: Alarm
    let onDismiss: () -> Void
    let onSnooze: () -> Void

    @State private var solved = false
    @State private var routineChecks: [Bool] = []
    private var routineComplete: Bool {
        alarm.routineItems.isEmpty || routineChecks.allSatisfy { $0 }
    }

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(red: 0.06, green: 0.05, blue: 0.15),
                         Color(red: 0.16, green: 0.12, blue: 0.36),
                         Color(red: 0.06, green: 0.05, blue: 0.15)],
                startPoint: .top, endPoint: .bottom,
            )
            .ignoresSafeArea()

            ScrollView {
                VStack(spacing: 18) {
                    Image(systemName: "alarm.fill")
                        .font(.system(size: 36))
                        .foregroundStyle(Color.purple.opacity(0.7))
                        .padding(.top, 24)
                    Text(String(format: "%02d:%02d", alarm.hour, alarm.minute))
                        .font(.system(size: 80, weight: .thin))
                        .foregroundStyle(.purple)
                    Text(alarm.title)
                        .font(.title3)
                        .foregroundStyle(.white.opacity(0.9))

                    if !alarm.message.isEmpty {
                        Text(alarm.message)
                            .padding(14)
                            .frame(maxWidth: .infinity)
                            .background(.purple.opacity(0.4))
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                            .foregroundStyle(.white)
                    }

                    if let url = alarm.youtubeUrl, !url.isEmpty {
                        YouTubePlayerView(url: url)
                            .frame(height: 200)
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                    }

                    if !solved {
                        challengeView
                    }

                    if solved, !alarm.routineItems.isEmpty {
                        routineCard
                    }

                    HStack(spacing: 12) {
                        Button(action: { if canSnooze { onSnooze() } }) {
                            Text(canSnooze ? "5분 뒤" : "🔒 루틴 먼저")
                                .frame(maxWidth: .infinity, minHeight: 56)
                        }
                        .buttonStyle(.bordered)
                        .disabled(!canSnooze)

                        Button(action: { if canDismiss { onDismiss() } }) {
                            Text(dismissLabel).fontWeight(.bold)
                                .frame(maxWidth: .infinity, minHeight: 56)
                        }
                        .buttonStyle(.borderedProminent)
                        .disabled(!canDismiss)
                    }
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 40)
            }
        }
        .onAppear {
            routineChecks = Array(repeating: false, count: alarm.routineItems.count)
            if alarm.challengeType == .none { solved = true }
            playAlarmSound()
            vibrate()
        }
        .onDisappear { stopAlarmSound() }
    }

    private var canSnooze: Bool { routineComplete }
    private var canDismiss: Bool { solved && routineComplete }
    private var dismissLabel: String {
        if !solved { return "🔒 먼저 챌린지를" }
        if !routineComplete { return "🔒 루틴 완료 필요" }
        return "끄기"
    }

    @ViewBuilder private var challengeView: some View {
        switch alarm.challengeType {
        case .none:
            EmptyView()
        case .mathEasy, .mathMedium, .mathHard:
            MathChallengeView(
                difficulty: alarm.challengeType,
                requiredCount: max(1, alarm.mathProblemCount),
                onSolved: { solved = true },
            )
        case .typing:
            TypingChallengeView(onSolved: { solved = true })
        case .squat:
            SquatChallengeView(target: 10, onSolved: { solved = true })
        case .voice:
            VoiceChallengeView(onSolved: { solved = true })
        case .shakeEasy, .shake, .shakeHard:
            ShakeChallengeView(
                target: alarm.challengeType == .shakeEasy ? 5
                    : alarm.challengeType == .shake ? 10 : 25,
                onSolved: { solved = true },
            )
        case .qrScan:
            QrChallengeView(requiredValue: alarm.qrRequiredValue) {
                solved = true
            }
        case .photo:
            PhotoChallengeView { solved = true }
        case .location:
            if let lat = alarm.geofenceLat, let lng = alarm.geofenceLng {
                LocationChallengeView(
                    targetLat: lat, targetLng: lng,
                    radiusMeters: alarm.geofenceRadius,
                    onSolved: { solved = true },
                )
            } else {
                Text("위치가 설정되지 않은 알람이에요")
                    .foregroundStyle(.red)
            }
        case .stepCount:
            StepChallengeView(target: 20, onSolved: { solved = true })
        case .tetris:
            TetrisChallengeView { solved = true }
        }
    }

    private var routineCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("기상 후 루틴")
                .font(.headline).foregroundStyle(.white)
            Text("모두 체크해야 알람이 꺼져요")
                .font(.caption).foregroundStyle(.white.opacity(0.7))
            ForEach(alarm.routineItems.indices, id: \.self) { idx in
                Button {
                    routineChecks[idx].toggle()
                } label: {
                    HStack {
                        Image(systemName: routineChecks[idx]
                                ? "checkmark.square.fill" : "square")
                            .foregroundStyle(.purple)
                        Text(alarm.routineItems[idx]).foregroundStyle(.white)
                        Spacer()
                    }
                }
            }
        }
        .padding(16)
        .background(.white.opacity(0.06))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

// ── Audio helpers ────────────────────────────────────────

private var audioPlayer: AVAudioPlayer?

private func playAlarmSound() {
    let session = AVAudioSession.sharedInstance()
    try? session.setCategory(.playback, mode: .default, options: [.duckOthers])
    try? session.setActive(true)
    // Fallback: a long system sound; production would ship a bundled .m4a
    AudioServicesPlayAlertSound(SystemSoundID(1304))
}

private func stopAlarmSound() {
    audioPlayer?.stop()
    audioPlayer = nil
}

private func vibrate() {
    AudioServicesPlayAlertSound(kSystemSoundID_Vibrate)
}
