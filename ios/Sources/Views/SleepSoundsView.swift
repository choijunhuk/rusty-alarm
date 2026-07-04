import SwiftUI

struct SleepSoundsView: View {
    @StateObject private var store = SleepSoundStore()
    @State private var timerMin: Int = 0
    @State private var timerTask: Task<Void, Never>? = nil

    private let timerOptions: [(Int, String)] = [(0, "끔"), (10, "10분"), (30, "30분"), (60, "60분")]

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                if store.playing != nil {
                    Text("🌙 백그라운드에서 계속 재생돼요.")
                        .font(.footnote).foregroundStyle(.purple)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }

                Text("사운드 선택")
                    .font(.subheadline).foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)

                ForEach(NoiseColor.allCases) { color in
                    SoundRow(
                        color: color,
                        selected: store.playing == color,
                        onSelect: {
                            if store.playing == color {
                                stopWithTimer()
                            } else {
                                startWithTimer(color)
                            }
                        }
                    )
                }

                Divider().opacity(0.3)

                Text("타이머")
                    .font(.subheadline).foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)

                HStack(spacing: 8) {
                    ForEach(timerOptions, id: \.0) { option in
                        TimerChip(
                            label: option.1,
                            selected: timerMin == option.0,
                            onTap: { timerMin = option.0 }
                        )
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                if store.playing != nil {
                    Button(action: stopWithTimer) {
                        Label("정지", systemImage: "stop.fill")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .controlSize(.large)
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
            .padding(.bottom, 32)
        }
        .navigationTitle("수면 사운드")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func startWithTimer(_ color: NoiseColor) {
        store.play(color)
        timerTask?.cancel()
        guard timerMin > 0 else { return }
        let seconds = timerMin * 60
        timerTask = Task {
            do {
                try await Task.sleep(nanoseconds: UInt64(seconds) * 1_000_000_000)
                store.stop()
            } catch {
                // Task was cancelled (user changed timer or stopped manually).
            }
        }
    }

    private func stopWithTimer() {
        timerTask?.cancel()
        timerTask = nil
        store.stop()
    }
}

// MARK: - Supporting views

private struct SoundRow: View {
    let color:    NoiseColor
    let selected: Bool
    let onSelect: () -> Void

    var body: some View {
        Button(action: onSelect) {
            HStack(spacing: 12) {
                Text(color.emoji).font(.system(size: 28))
                Text(color.label)
                    .fontWeight(.medium)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Image(systemName: selected ? "stop.fill" : "play.fill")
                    .foregroundStyle(selected ? .purple : .secondary)
            }
            .padding(16)
            .background(
                selected
                    ? Color.purple.opacity(0.15)
                    : Color(.secondarySystemBackground)
            )
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
        .buttonStyle(.plain)
    }
}

private struct TimerChip: View {
    let label:    String
    let selected: Bool
    let onTap:    () -> Void

    var body: some View {
        Button(action: onTap) {
            Text(label)
                .font(.subheadline)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(selected ? Color.purple : Color(.secondarySystemBackground))
                .foregroundStyle(selected ? Color.white : Color.primary)
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}
