import Foundation
import Combine

final class PetStore: ObservableObject {
    @Published private(set) var pet: Pet = Pet()
    private let key = "rusty.pet.v1"

    init() { load() }

    func load() {
        if let data = UserDefaults.standard.data(forKey: key),
           let p = try? JSONDecoder().decode(Pet.self, from: data) {
            pet = p
        }
    }

    private func save() {
        if let data = try? JSONEncoder().encode(pet) {
            UserDefaults.standard.set(data, forKey: key)
        }
    }

    func rename(_ name: String) {
        let trimmed = name.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else { return }
        pet.name = trimmed
        save()
    }

    func setSkin(_ skin: PetSkin) {
        pet.skin = skin
        save()
    }

    /// Returns nil on success, or a reason if the cooldown blocks feeding.
    @discardableResult
    func feed() -> String? {
        if let last = pet.lastFedAt {
            let cooldown: TimeInterval = 60 * 60
            let gap = Date().timeIntervalSince(last)
            if gap < cooldown {
                let mins = max(1, Int((cooldown - gap) / 60))
                return "\(mins)분 뒤에 다시 줄 수 있어요"
            }
        }
        pet.exp += 8
        pet.lastFedAt = Date()
        save()
        return nil
    }

    /// Reward EXP after dismissing an alarm.
    func rewardForDismiss(challengeCompleted: Bool) {
        pet.exp += 10 + (challengeCompleted ? 5 : 0)
        save()
    }
}
