import SwiftUI
import Speech
import AVFoundation

struct VoiceChallengeView: View {
    static let acceptPhrases = ["일어났다", "일어났어", "굿모닝", "good morning", "wake up", "기상"]
    let onSolved: () -> Void

    @State private var heard = ""
    @State private var status = "아래 문구를 또박또박 말해주세요"
    @State private var recognizer = SFSpeechRecognizer(locale: Locale(identifier: "ko-KR"))
    @State private var task: SFSpeechRecognitionTask?
    @State private var request: SFSpeechAudioBufferRecognitionRequest?
    private let engine = AVAudioEngine()

    var body: some View {
        VStack(spacing: 10) {
            Text("🎤 말해서 알람 끄기").font(.headline).foregroundStyle(.purple)
            Text("\"\(Self.acceptPhrases.first ?? "")\" 라고 말해주세요")
                .font(.title3).foregroundStyle(.white)
            Text(status).font(.caption).foregroundStyle(.white.opacity(0.6))
            if !heard.isEmpty {
                Text("들은 말: \"\(heard)\"").font(.caption).foregroundStyle(.orange)
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity)
        .background(.white.opacity(0.05))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .onAppear(perform: start)
        .onDisappear(perform: stop)
    }

    private func start() {
        SFSpeechRecognizer.requestAuthorization { auth in
            guard auth == .authorized else {
                status = "음성 인식 권한이 필요해요"; return
            }
            DispatchQueue.main.async { beginListening() }
        }
    }

    private func beginListening() {
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(.playAndRecord, mode: .measurement, options: .duckOthers)
        try? session.setActive(true, options: .notifyOthersOnDeactivation)

        let req = SFSpeechAudioBufferRecognitionRequest()
        req.shouldReportPartialResults = true
        request = req

        let input = engine.inputNode
        input.removeTap(onBus: 0)
        let fmt = input.outputFormat(forBus: 0)
        input.installTap(onBus: 0, bufferSize: 1024, format: fmt) { buffer, _ in
            req.append(buffer)
        }
        engine.prepare()
        try? engine.start()

        task = recognizer?.recognitionTask(with: req) { result, error in
            if let r = result {
                let text = r.bestTranscription.formattedString
                heard = text
                let matched = Self.acceptPhrases.contains {
                    text.lowercased().contains($0.lowercased())
                }
                if matched {
                    stop(); onSolved()
                }
            }
            if let error {
                status = "오류: \(error.localizedDescription). 다시 시도해요."
                stop()
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { beginListening() }
            }
        }
        status = "듣고 있어요…"
    }

    private func stop() {
        engine.inputNode.removeTap(onBus: 0)
        if engine.isRunning { engine.stop() }
        request?.endAudio()
        task?.cancel()
        task = nil
        request = nil
    }
}
