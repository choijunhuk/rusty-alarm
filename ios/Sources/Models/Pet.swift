import Foundation

struct Pet: Codable, Equatable {
    var id: Int = 1
    var name: String = "버디"
    var exp: Int = 0
    var lastFedAt: Date? = nil
    var skin: PetSkin = .default
    var createdAt: Date = Date()

    var level: Int { exp / 100 }
    var progressToNextLevel: Double { Double(exp % 100) / 100.0 }

    var stage: PetStage {
        switch level {
        case 0:        return .egg
        case 1:        return .hatching
        case 2..<5:    return .chick
        case 5..<10:   return .young
        case 10..<20:  return .adult
        default:       return .legend
        }
    }

    var happiness: Int {
        guard let last = lastFedAt else { return 60 }
        let hours = Int(Date().timeIntervalSince(last) / 3600)
        return max(0, 100 - hours * 3)
    }

    var happinessLabel: String {
        switch happiness {
        case 80...:  return "기분 최고"
        case 60..<80: return "기분 좋음"
        case 40..<60: return "보통이에요"
        case 20..<40: return "좀 시무룩"
        default:      return "배고파요"
        }
    }
}

enum PetSkin: String, Codable, CaseIterable, Identifiable {
    case `default`
    case golden
    case rainbow

    var id: String { rawValue }
    var label: String {
        switch self {
        case .default: return "기본"
        case .golden:  return "황금"
        case .rainbow: return "무지개"
        }
    }
    var unlockLevel: Int {
        switch self {
        case .default: return 0
        case .golden:  return 5
        case .rainbow: return 10
        }
    }
}

enum PetStage {
    case egg, hatching, chick, young, adult, legend

    var emoji: String {
        switch self {
        case .egg:      return "🥚"
        case .hatching: return "🐣"
        case .chick:    return "🐥"
        case .young:    return "🐔"
        case .adult:    return "🦅"
        case .legend:   return "🦄"
        }
    }
    var label: String {
        switch self {
        case .egg:      return "알"
        case .hatching: return "부화 중"
        case .chick:    return "병아리"
        case .young:    return "청년"
        case .adult:    return "성체"
        case .legend:   return "전설"
        }
    }
}
