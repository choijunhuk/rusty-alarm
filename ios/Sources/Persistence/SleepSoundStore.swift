import AVFoundation

enum NoiseColor: String, CaseIterable, Identifiable {
    case white, pink, brown, rain, ocean

    var id: String { rawValue }

    var label: String {
        switch self {
        case .white: return "화이트 노이즈"
        case .pink:  return "핑크 노이즈"
        case .brown: return "브라운 노이즈"
        case .rain:  return "빗소리"
        case .ocean: return "파도"
        }
    }

    var emoji: String {
        switch self {
        case .white: return "🌫"
        case .pink:  return "🌸"
        case .brown: return "🟤"
        case .rain:  return "🌧"
        case .ocean: return "🌊"
        }
    }
}

/// Generates synthesised noise loops via AVAudioEngine.
/// Mirrors the five sounds from Android's NoiseGenerator (white, pink, brown, rain, ocean).
/// All synthesis state is only touched on the audio render thread after the engine starts.
final class SleepSoundStore: ObservableObject {
    @Published private(set) var playing: NoiseColor? = nil

    private var engine: AVAudioEngine?

    // Synthesis state — accessed only from the real-time audio render callback.
    private var pinkRows    = [Float](repeating: 0, count: 16)
    private var pinkRunning: Float = 0
    private var pinkCount:   Int   = 0
    private var brown:       Float = 0
    private var oceanPhase:  Float = 0

    func play(_ color: NoiseColor) {
        stop()
        resetState()

        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playback, mode: .default)
            try session.setActive(true)
        } catch {}

        let eng    = AVAudioEngine()
        let format = AVAudioFormat(standardFormatWithSampleRate: 44_100, channels: 1)!
        let node   = AVAudioSourceNode(format: format) { [weak self] _, _, frameCount, audioBufferList in
            guard let self else { return noErr }
            let bufList = UnsafeMutableAudioBufferListPointer(audioBufferList)
            guard let dataPtr = bufList.first?.mData?.assumingMemoryBound(to: Float.self) else {
                return noErr
            }
            for i in 0..<Int(frameCount) {
                dataPtr[i] = self.nextSample(for: color)
            }
            return noErr
        }

        eng.attach(node)
        eng.connect(node, to: eng.mainMixerNode, format: format)
        try? eng.start()
        engine  = eng
        playing = color
    }

    func stop() {
        engine?.stop()
        engine  = nil
        playing = nil
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }

    // MARK: - Private

    private func resetState() {
        pinkRows    = [Float](repeating: 0, count: 16)
        pinkRunning = 0
        pinkCount   = 0
        brown       = 0
        oceanPhase  = 0
    }

    private func nextSample(for color: NoiseColor) -> Float {
        switch color {
        case .white:
            return Float.random(in: -1...1) * 0.55
        case .pink:
            return pinkSample() * 0.7
        case .brown:
            return brownSample() * 0.9
        case .rain:
            let base = brownSample() * 0.85
            let drop: Float = Float.random(in: 0..<1) < 0.0025 ? Float.random(in: 0...0.5) : 0
            return (base + drop) * 0.95
        case .ocean:
            oceanPhase += 0.0001
            let env = 0.45 + 0.55 * Float(sin(Double(oceanPhase)))
            return brownSample() * env * 0.95
        }
    }

    // Voss-McCartney 16-row pink noise.
    private func pinkSample() -> Float {
        pinkCount  &+= 1
        let idx     = min(pinkCount.trailingZeroBitCount, pinkRows.count - 1)
        let v       = Float.random(in: -1...1)
        pinkRunning -= pinkRows[idx]
        pinkRows[idx] = v
        pinkRunning  += v
        return pinkRunning / Float(pinkRows.count)
    }

    // Integrated white noise with DC leak (1/f² spectral slope).
    private func brownSample() -> Float {
        let v = Float.random(in: -1...1)
        brown = min(max(brown + 0.02 * v, -1), 1) * 0.999
        return brown
    }
}
