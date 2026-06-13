import SwiftUI

struct TypingChallengeView: View {
    let onSolved: () -> Void
    @State private var phrase = TypingChallengeView.phrases.randomElement() ?? "rise and shine"
    @State private var typed = ""

    static let phrases = [
        "오늘도 좋은 하루 보내세요",
        "rise and shine",
        "일찍 일어나는 새가 벌레를 잡는다",
        "carpe diem",
        "꿈은 이루어진다",
    ]

    var body: some View {
        VStack(spacing: 10) {
            Text("아래 문장을 입력하면 꺼져요!")
                .font(.footnote).foregroundStyle(.white.opacity(0.7))
            Text("\"\(phrase)\"")
                .font(.title3).bold().foregroundStyle(.white)
                .multilineTextAlignment(.center)
            TextField("입력하세요", text: $typed)
                .padding(12)
                .background(.white.opacity(0.1))
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .foregroundStyle(.white)
                .textInputAutocapitalization(.never)
                .disableAutocorrection(true)
                .onChange(of: typed) { _, new in
                    if new == phrase { onSolved() }
                }
        }
        .padding(18)
        .background(.white.opacity(0.05))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}
