import SwiftUI

struct PetView: View {
    @EnvironmentObject var store: PetStore
    @State private var toast: String? = nil
    @State private var skinSheet = false

    var body: some View {
        ZStack {
            Color(.systemBackground).ignoresSafeArea()
            ScrollView {
                VStack(spacing: 18) {
                    Text(displayEmoji)
                        .font(.system(size: 110))
                        .padding(.top, 32)
                        .animation(.easeInOut(duration: 0.6), value: store.pet.stage)

                    VStack(spacing: 2) {
                        Text(store.pet.name)
                            .font(.title)
                            .fontWeight(.bold)
                        Text("\(store.pet.stage.label) · Lv \(store.pet.level)")
                            .font(.callout)
                            .foregroundStyle(.secondary)
                    }

                    VStack(alignment: .leading, spacing: 14) {
                        StatRow(label: "다음 레벨까지",
                                value: "\(store.pet.exp % 100) / 100",
                                progress: store.pet.progressToNextLevel,
                                tint: .purple)
                        StatRow(label: store.pet.happinessLabel,
                                value: "\(store.pet.happiness)%",
                                progress: Double(store.pet.happiness) / 100,
                                tint: .orange)
                    }
                    .padding(18)
                    .background(.thinMaterial)
                    .clipShape(RoundedRectangle(cornerRadius: 20))

                    VStack(alignment: .leading, spacing: 8) {
                        Text("성장 방법")
                            .font(.headline)
                        Text("• 알람을 끄면 +10 경험치").font(.subheadline).foregroundStyle(.secondary)
                        Text("• 챌린지를 완수하면 +5 보너스").font(.subheadline).foregroundStyle(.secondary)
                        Text("• 스누즈는 경험치가 쌓이지 않아요").font(.subheadline).foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(18)
                    .background(.thinMaterial)
                    .clipShape(RoundedRectangle(cornerRadius: 20))

                    Button {
                        toast = store.feed() ?? "\(store.pet.name)이(가) 기뻐해요. +8 경험치"
                    } label: {
                        Text("먹이주기").font(.headline)
                            .frame(maxWidth: .infinity, minHeight: 52)
                    }
                    .buttonStyle(.borderedProminent)
                    .clipShape(RoundedRectangle(cornerRadius: 14))

                    Button("스킨 변경 · 현재 \(store.pet.skin.label)") { skinSheet = true }
                        .frame(maxWidth: .infinity, minHeight: 48)
                        .buttonStyle(.bordered)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 32)
            }
        }
        .navigationTitle("나의 펫")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $skinSheet) { skinPicker }
        .overlay(alignment: .bottom) {
            if let t = toast {
                Text(t)
                    .font(.footnote)
                    .padding(.horizontal, 14).padding(.vertical, 10)
                    .background(.regularMaterial)
                    .clipShape(Capsule())
                    .padding(.bottom, 24)
                    .task {
                        try? await Task.sleep(nanoseconds: 1_800_000_000)
                        toast = nil
                    }
            }
        }
    }

    private var displayEmoji: String {
        switch store.pet.skin {
        case .golden:  return "✨\(store.pet.stage.emoji)✨"
        case .rainbow: return "🌈\(store.pet.stage.emoji)🌈"
        default:       return store.pet.stage.emoji
        }
    }

    @ViewBuilder
    private var skinPicker: some View {
        NavigationStack {
            List {
                ForEach(PetSkin.allCases) { skin in
                    let unlocked = store.pet.level >= skin.unlockLevel
                    HStack {
                        VStack(alignment: .leading) {
                            Text(skin.label).fontWeight(.medium)
                            Text(unlocked ? "사용 가능" : "Lv \(skin.unlockLevel) 부터")
                                .font(.caption).foregroundStyle(.secondary)
                        }
                        Spacer()
                        Button(store.pet.skin == skin ? "사용 중" : "선택") {
                            store.setSkin(skin)
                            skinSheet = false
                        }
                        .disabled(!unlocked || store.pet.skin == skin)
                    }
                }
            }
            .navigationTitle("펫 스킨")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("닫기") { skinSheet = false }
                }
            }
        }
    }
}

private struct StatRow: View {
    let label: String
    let value: String
    let progress: Double
    let tint: Color

    var body: some View {
        VStack(spacing: 6) {
            HStack {
                Text(label).font(.footnote).foregroundStyle(.secondary)
                Spacer()
                Text(value).font(.footnote).foregroundStyle(tint).fontWeight(.semibold)
            }
            ProgressView(value: progress).tint(tint)
        }
    }
}
