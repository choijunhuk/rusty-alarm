import SwiftUI

struct MathChallengeView: View {
    let difficulty: ChallengeType
    let requiredCount: Int
    let onSolved: () -> Void

    @State private var problem = MathProblem.generate(.mathEasy)
    @State private var input = ""
    @State private var solvedCount = 0
    @State private var error = false

    var body: some View {
        VStack(spacing: 12) {
            Text("알람을 끄려면 풀어야 해요!")
                .font(.footnote).foregroundStyle(.white.opacity(0.7))
            if requiredCount > 1 {
                Text("\(solvedCount + 1) / \(requiredCount)")
                    .font(.caption).foregroundStyle(.purple)
            }
            Text(problem.expression)
                .font(.system(size: 36, weight: .bold))
                .foregroundStyle(.white)
            TextField("정답", text: $input)
                .keyboardType(.numbersAndPunctuation)
                .multilineTextAlignment(.center)
                .padding(12)
                .background(.white.opacity(0.1))
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .foregroundStyle(.white)
            if error {
                Text("틀렸어요! 다시 시도해보세요.")
                    .font(.caption).foregroundStyle(.red)
            }
            Button("확인") { check() }
                .buttonStyle(.borderedProminent)
                .frame(maxWidth: .infinity, minHeight: 48)
        }
        .padding(18)
        .background(.white.opacity(0.05))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .onAppear { problem = MathProblem.generate(difficulty) }
    }

    private func check() {
        guard let answer = Int(input.trimmingCharacters(in: .whitespaces)),
              answer == problem.answer
        else { error = true; input = ""; return }
        solvedCount += 1
        input = ""
        error = false
        if solvedCount >= requiredCount {
            onSolved()
        } else {
            problem = MathProblem.generate(difficulty)
        }
    }
}

struct MathProblem {
    let expression: String
    let answer: Int

    static func generate(_ difficulty: ChallengeType) -> MathProblem {
        let range: ClosedRange<Int>
        let ops: [Character]
        switch difficulty {
        case .mathEasy:   range = 1...10;   ops = ["+", "-"]
        case .mathMedium: range = 10...50;  ops = ["+", "-", "×"]
        case .mathHard:   range = 20...100; ops = ["+", "-", "×", "÷"]
        default:          range = 1...10;   ops = ["+", "-"]
        }
        var a = Int.random(in: range)
        var b = Int.random(in: range)
        let op = ops.randomElement() ?? "+"
        if op == "÷" {
            b = max(1, b)
            a = b * Int.random(in: 1...10)
        }
        let answer: Int = switch op {
            case "+": a + b
            case "-": a - b
            case "×": a * b
            case "÷": a / b
            default:  0
        }
        return MathProblem(expression: "\(a) \(op) \(b)", answer: answer)
    }
}
