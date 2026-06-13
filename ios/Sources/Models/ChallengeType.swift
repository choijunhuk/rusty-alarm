import Foundation

enum ChallengeType: String, Codable, CaseIterable, Identifiable {
    case none
    case mathEasy
    case mathMedium
    case mathHard
    case shakeEasy
    case shake
    case shakeHard
    case typing
    case tetris
    case stepCount
    case photo
    case location
    case qrScan
    case voice
    case squat

    var id: String { rawValue }

    var label: String {
        switch self {
        case .none:        return "없음"
        case .mathEasy:    return "수학 — 쉬움"
        case .mathMedium:  return "수학 — 보통"
        case .mathHard:    return "수학 — 어려움"
        case .shakeEasy:   return "흔들기 — 5회"
        case .shake:       return "흔들기 — 10회"
        case .shakeHard:   return "흔들기 — 25회"
        case .typing:      return "타이핑 챌린지"
        case .tetris:      return "테트리스 한 줄"
        case .stepCount:   return "걷기 20걸음"
        case .photo:       return "사진 인증"
        case .location:    return "위치 인증"
        case .qrScan:      return "QR 코드 스캔"
        case .voice:       return "음성 인식"
        case .squat:       return "스쿼트 10회"
        }
    }
}
